/*
 * SonarQube :: GitHub Plugin
 * Copyright (C) 2015 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.github;

import java.io.File;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.ProjectIssues;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;

import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PullRequestIssuePostJobTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private PullRequestIssuePostJob pullRequestIssuePostJob;
  private PullRequestFacade pullRequestFacade;
  private File baseDir;
  private ProjectIssues issues;
  private InputFileCache cache;

  @Before
  public void prepare() throws Exception {
    baseDir = temp.newFolder();
    pullRequestFacade = mock(PullRequestFacade.class);
    GitHubPluginConfiguration config = mock(GitHubPluginConfiguration.class);
    issues = mock(ProjectIssues.class);
    cache = mock(InputFileCache.class);
    pullRequestIssuePostJob = new PullRequestIssuePostJob(config, pullRequestFacade, issues, cache);

  }

  @Test
  public void testPullRequestAnalysisNoIssue() {
    when(issues.issues()).thenReturn(Arrays.<Issue>asList());
    pullRequestIssuePostJob.executeOn(null, null);
    verify(pullRequestFacade).addGlobalComment("SonarQube analysis reported no new issues.");
  }

  @Test
  public void testPullRequestAnalysisWithNewIssues() {
    Issue newIssue = mock(Issue.class);
    DefaultInputFile inputFile1 = new DefaultInputFile("src/Foo.php");
    when(cache.byKey("foo:src/Foo.php")).thenReturn(inputFile1);
    when(newIssue.componentKey()).thenReturn("foo:src/Foo.php");
    when(newIssue.line()).thenReturn(1);
    when(newIssue.ruleKey()).thenReturn(RuleKey.of("repo", "rule"));
    when(newIssue.severity()).thenReturn(Severity.BLOCKER);
    when(newIssue.isNew()).thenReturn(true);
    when(newIssue.message()).thenReturn("msg");
    when(pullRequestFacade.getGithubUrl(inputFile1, 1)).thenReturn("http://github/blob/abc123/src/Foo.php#L1");

    Issue lineNotVisible = mock(Issue.class);
    when(cache.byKey("foo:src/Foo.php")).thenReturn(inputFile1);
    when(lineNotVisible.componentKey()).thenReturn("foo:src/Foo.php");
    when(lineNotVisible.line()).thenReturn(2);
    when(lineNotVisible.ruleKey()).thenReturn(RuleKey.of("repo", "rule"));
    when(lineNotVisible.severity()).thenReturn(Severity.BLOCKER);
    when(lineNotVisible.isNew()).thenReturn(true);
    when(lineNotVisible.message()).thenReturn("msg");

    Issue fileNotInPR = mock(Issue.class);
    DefaultInputFile inputFile2 = new DefaultInputFile("src/Foo2.php");
    when(cache.byKey("foo:src/Foo2.php")).thenReturn(inputFile1);
    when(fileNotInPR.componentKey()).thenReturn("foo:src/Foo2.php");
    when(fileNotInPR.line()).thenReturn(1);
    when(fileNotInPR.ruleKey()).thenReturn(RuleKey.of("repo", "rule"));
    when(fileNotInPR.severity()).thenReturn(Severity.BLOCKER);
    when(fileNotInPR.isNew()).thenReturn(true);
    when(fileNotInPR.message()).thenReturn("msg");

    Issue notNewIssue = mock(Issue.class);
    when(cache.byKey("foo:src/Foo.php")).thenReturn(inputFile1);
    when(notNewIssue.componentKey()).thenReturn("foo:src/Foo.php");
    when(notNewIssue.line()).thenReturn(1);
    when(notNewIssue.ruleKey()).thenReturn(RuleKey.of("repo", "rule"));
    when(notNewIssue.severity()).thenReturn(Severity.BLOCKER);
    when(notNewIssue.isNew()).thenReturn(false);
    when(notNewIssue.message()).thenReturn("msg");

    Issue issueOnDir = mock(Issue.class);
    when(cache.byKey("foo:src")).thenReturn(null);
    when(issueOnDir.componentKey()).thenReturn("foo:src");
    when(issueOnDir.ruleKey()).thenReturn(RuleKey.of("repo", "rule"));
    when(issueOnDir.severity()).thenReturn(Severity.BLOCKER);
    when(issueOnDir.isNew()).thenReturn(true);
    when(issueOnDir.message()).thenReturn("msg");

    Issue issueOnProject = mock(Issue.class);
    when(issueOnProject.ruleKey()).thenReturn(RuleKey.of("repo", "rule"));
    when(issueOnProject.componentKey()).thenReturn("foo");
    when(issueOnProject.severity()).thenReturn(Severity.BLOCKER);
    when(issueOnProject.isNew()).thenReturn(true);
    when(issueOnProject.message()).thenReturn("msg");

    Issue globalIssue = mock(Issue.class);
    when(cache.byKey("foo:src/Foo.php")).thenReturn(inputFile1);
    when(globalIssue.componentKey()).thenReturn("foo:src/Foo.php");
    when(globalIssue.line()).thenReturn(null);
    when(globalIssue.ruleKey()).thenReturn(RuleKey.of("repo", "rule"));
    when(globalIssue.severity()).thenReturn(Severity.BLOCKER);
    when(globalIssue.isNew()).thenReturn(true);
    when(globalIssue.message()).thenReturn("msg");

    when(issues.issues()).thenReturn(Arrays.<Issue>asList(newIssue, globalIssue, issueOnProject, issueOnDir, fileNotInPR, lineNotVisible, notNewIssue));
    when(pullRequestFacade.hasFile(inputFile1)).thenReturn(true);
    when(pullRequestFacade.hasFileLine(inputFile1, 1)).thenReturn(true);

    pullRequestIssuePostJob.executeOn(null, null);
    verify(pullRequestFacade).addGlobalComment(contains("SonarQube analysis reported 6 new issues:"));
    verify(pullRequestFacade)
      .addGlobalComment(
        contains("* [msg](http://github/blob/abc123/src/Foo.php#L1) [![rule](https://raw.githubusercontent.com/SonarCommunity/sonar-github/master/images/rule.png)](http://nemo.sonarqube.org/coding_rules#rule_key=repo%3Arule)"));
  }
}
