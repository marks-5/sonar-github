package org.kohsuke.github;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

/**
 * Unit test for {@link GitHub}.
 */
public class GitHubTest extends TestCase {

    public void testGitHubServerWithHttp() throws Exception {
        GitHub hub = GitHub.connectToEnterprise("http://enterprise.kohsuke.org/api/v3", "bogus","bogus");
        assertEquals("http://enterprise.kohsuke.org/api/v3/test", hub.getApiURL("/test").toString());
    }

    public void testGitHubServerWithHttps() throws Exception {
        GitHub hub = GitHub.connectToEnterprise("https://enterprise.kohsuke.org/api/v3", "bogus","bogus");
        assertEquals("https://enterprise.kohsuke.org/api/v3/test", hub.getApiURL("/test").toString());
    }

    public void testGitHubServerWithoutServer() throws Exception {
        GitHub hub = GitHub.connectUsingPassword("kohsuke", "bogus");
        assertEquals("https://api.github.com/test", hub.getApiURL("/test").toString());
    }
    
    public void testGitHubBuilderFromEnvironment() throws IOException {
        
        Map<String, String>props = new HashMap<String, String>();
        
        props.put("login", "bogus");
        props.put("oauth", "bogus");
        props.put("password", "bogus");
        
        setupEnvironment(props);
        
        GitHubBuilder builder = GitHubBuilder.fromEnvironment();
        
        assertEquals("bogus", builder.user);
        assertEquals("bogus", builder.oauthToken);
        assertEquals("bogus", builder.password);
        
    }
    
    /*
     * Copied from StackOverflow: http://stackoverflow.com/a/7201825/2336755
     * 
     * This allows changing the in memory process environment.
     * 
     * Its used to wire in values for the github credentials to test that the GitHubBuilder works properly to resolve them.
     */
    private void setupEnvironment(Map<String, String> newenv) {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            try {
                Class[] classes = Collections.class.getDeclaredClasses();
                Map<String, String> env = System.getenv();
                for (Class cl : classes) {
                    if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                        Field field = cl.getDeclaredField("m");
                        field.setAccessible(true);
                        Object obj = field.get(env);
                        Map<String, String> map = (Map<String, String>) obj;
                        map.clear();
                        map.putAll(newenv);
                    }
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    public void testGitHubBuilderFromCustomEnvironment() throws IOException {
        Map<String, String> props = new HashMap<String, String>();

        props.put("customLogin", "bogusLogin");
        props.put("customOauth", "bogusOauth");
        props.put("customPassword", "bogusPassword");
        props.put("customEndpoint", "bogusEndpoint");

        setupEnvironment(props);

        GitHubBuilder builder = GitHubBuilder.fromEnvironment("customLogin", "customPassword", "customOauth", "customEndpoint");

        assertEquals("bogusLogin", builder.user);
        assertEquals("bogusOauth", builder.oauthToken);
        assertEquals("bogusPassword", builder.password);
        assertEquals("bogusEndpoint", builder.endpoint);
    }

}
