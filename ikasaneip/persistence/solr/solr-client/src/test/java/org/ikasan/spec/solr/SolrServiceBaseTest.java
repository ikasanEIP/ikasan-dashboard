package org.ikasan.spec.solr;

import org.junit.Assert;
import org.junit.Test;

public class SolrServiceBaseTest {

    /**
     * Concrete implementation of SolrServiceBase for testing purposes
     */
    private static class TestSolrService extends SolrServiceBase<String> {
        // Concrete implementation for testing

        public String getSolrUsername() {
            return this.solrUsername;
        }

        public String getSolrPassword() {
            return this.solrPassword;
        }
    }

    @Test
    public void test_setSolrUsername() {
        TestSolrService service = new TestSolrService();

        service.setSolrUsername("testUser");

        Assert.assertEquals("testUser", service.getSolrUsername());
    }

    @Test
    public void test_setSolrPassword() {
        TestSolrService service = new TestSolrService();

        service.setSolrPassword("testPassword");

        Assert.assertEquals("testPassword", service.getSolrPassword());
    }

    @Test
    public void test_setSolrUsername_null() {
        TestSolrService service = new TestSolrService();

        service.setSolrUsername("testUser");
        Assert.assertEquals("testUser", service.getSolrUsername());

        // Set to null
        service.setSolrUsername(null);

        Assert.assertNull(service.getSolrUsername());
    }

    @Test
    public void test_setSolrPassword_null() {
        TestSolrService service = new TestSolrService();

        service.setSolrPassword("testPassword");
        Assert.assertEquals("testPassword", service.getSolrPassword());

        // Set to null
        service.setSolrPassword(null);

        Assert.assertNull(service.getSolrPassword());
    }

    @Test
    public void test_setSolrUsername_empty() {
        TestSolrService service = new TestSolrService();

        service.setSolrUsername("");

        Assert.assertEquals("", service.getSolrUsername());
    }

    @Test
    public void test_setSolrPassword_empty() {
        TestSolrService service = new TestSolrService();

        service.setSolrPassword("");

        Assert.assertEquals("", service.getSolrPassword());
    }

    @Test
    public void test_setSolrUsername_with_special_characters() {
        TestSolrService service = new TestSolrService();

        String usernameWithSpecialChars = "user@domain.com";
        service.setSolrUsername(usernameWithSpecialChars);

        Assert.assertEquals(usernameWithSpecialChars, service.getSolrUsername());
    }

    @Test
    public void test_setSolrPassword_with_special_characters() {
        TestSolrService service = new TestSolrService();

        String passwordWithSpecialChars = "P@ssw0rd!#$%";
        service.setSolrPassword(passwordWithSpecialChars);

        Assert.assertEquals(passwordWithSpecialChars, service.getSolrPassword());
    }

    @Test
    public void test_setSolrUsername_with_spaces() {
        TestSolrService service = new TestSolrService();

        String usernameWithSpaces = "test user";
        service.setSolrUsername(usernameWithSpaces);

        Assert.assertEquals(usernameWithSpaces, service.getSolrUsername());
    }

    @Test
    public void test_setSolrPassword_with_spaces() {
        TestSolrService service = new TestSolrService();

        String passwordWithSpaces = "test password";
        service.setSolrPassword(passwordWithSpaces);

        Assert.assertEquals(passwordWithSpaces, service.getSolrPassword());
    }

    @Test
    public void test_multiple_username_updates() {
        TestSolrService service = new TestSolrService();

        service.setSolrUsername("user1");
        Assert.assertEquals("user1", service.getSolrUsername());

        service.setSolrUsername("user2");
        Assert.assertEquals("user2", service.getSolrUsername());

        service.setSolrUsername("user3");
        Assert.assertEquals("user3", service.getSolrUsername());
    }

    @Test
    public void test_multiple_password_updates() {
        TestSolrService service = new TestSolrService();

        service.setSolrPassword("password1");
        Assert.assertEquals("password1", service.getSolrPassword());

        service.setSolrPassword("password2");
        Assert.assertEquals("password2", service.getSolrPassword());

        service.setSolrPassword("password3");
        Assert.assertEquals("password3", service.getSolrPassword());
    }

    @Test
    public void test_setBothUsernameAndPassword() {
        TestSolrService service = new TestSolrService();

        service.setSolrUsername("testUser");
        service.setSolrPassword("testPassword");

        Assert.assertEquals("testUser", service.getSolrUsername());
        Assert.assertEquals("testPassword", service.getSolrPassword());
    }

    @Test
    public void test_default_username_is_null() {
        TestSolrService service = new TestSolrService();

        Assert.assertNull(service.getSolrUsername());
    }

    @Test
    public void test_default_password_is_null() {
        TestSolrService service = new TestSolrService();

        Assert.assertNull(service.getSolrPassword());
    }

    @Test
    public void test_username_independence_across_instances() {
        TestSolrService service1 = new TestSolrService();
        TestSolrService service2 = new TestSolrService();

        service1.setSolrUsername("user1");
        service2.setSolrUsername("user2");

        Assert.assertEquals("user1", service1.getSolrUsername());
        Assert.assertEquals("user2", service2.getSolrUsername());
    }

    @Test
    public void test_password_independence_across_instances() {
        TestSolrService service1 = new TestSolrService();
        TestSolrService service2 = new TestSolrService();

        service1.setSolrPassword("password1");
        service2.setSolrPassword("password2");

        Assert.assertEquals("password1", service1.getSolrPassword());
        Assert.assertEquals("password2", service2.getSolrPassword());
    }

    @Test
    public void test_setSolrUsername_with_long_string() {
        TestSolrService service = new TestSolrService();

        String longUsername = "a".repeat(1000);
        service.setSolrUsername(longUsername);

        Assert.assertEquals(longUsername, service.getSolrUsername());
        Assert.assertEquals(1000, service.getSolrUsername().length());
    }

    @Test
    public void test_setSolrPassword_with_long_string() {
        TestSolrService service = new TestSolrService();

        String longPassword = "b".repeat(1000);
        service.setSolrPassword(longPassword);

        Assert.assertEquals(longPassword, service.getSolrPassword());
        Assert.assertEquals(1000, service.getSolrPassword().length());
    }

    @Test
    public void test_setSolrUsername_with_unicode_characters() {
        TestSolrService service = new TestSolrService();

        String unicodeUsername = "用户名测试";
        service.setSolrUsername(unicodeUsername);

        Assert.assertEquals(unicodeUsername, service.getSolrUsername());
    }

    @Test
    public void test_setSolrPassword_with_unicode_characters() {
        TestSolrService service = new TestSolrService();

        String unicodePassword = "密码测试";
        service.setSolrPassword(unicodePassword);

        Assert.assertEquals(unicodePassword, service.getSolrPassword());
    }

    @Test
    public void test_credentials_update_order_does_not_matter() {
        TestSolrService service1 = new TestSolrService();
        TestSolrService service2 = new TestSolrService();

        // Set username first, then password
        service1.setSolrUsername("user");
        service1.setSolrPassword("pass");

        // Set password first, then username
        service2.setSolrPassword("pass");
        service2.setSolrUsername("user");

        Assert.assertEquals(service1.getSolrUsername(), service2.getSolrUsername());
        Assert.assertEquals(service1.getSolrPassword(), service2.getSolrPassword());
    }

    @Test
    public void test_setSolrUsername_idempotent() {
        TestSolrService service = new TestSolrService();

        service.setSolrUsername("testUser");
        service.setSolrUsername("testUser");
        service.setSolrUsername("testUser");

        Assert.assertEquals("testUser", service.getSolrUsername());
    }

    @Test
    public void test_setSolrPassword_idempotent() {
        TestSolrService service = new TestSolrService();

        service.setSolrPassword("testPassword");
        service.setSolrPassword("testPassword");
        service.setSolrPassword("testPassword");

        Assert.assertEquals("testPassword", service.getSolrPassword());
    }
}
