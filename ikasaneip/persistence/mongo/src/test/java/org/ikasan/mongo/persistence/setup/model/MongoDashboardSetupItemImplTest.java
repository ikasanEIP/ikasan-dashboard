package org.ikasan.mongo.persistence.setup.model;

import org.junit.Assert;
import org.junit.Test;

/**
 * Comprehensive unit test for MongoDashboardSetupItemImpl
 */
public class MongoDashboardSetupItemImplTest {

    @Test
    public void test_default_constructor() {
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl();
        Assert.assertNull(item.getName());
        Assert.assertNull(item.getStatus());
        Assert.assertEquals(0, item.getExecutionTimestamp());
    }

    @Test
    public void test_constructor_with_all_fields() {
        long timestamp = System.currentTimeMillis();
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl("test-item", "COMPLETE", timestamp);

        Assert.assertEquals("test-item", item.getName());
        Assert.assertEquals("COMPLETE", item.getStatus());
        Assert.assertEquals(timestamp, item.getExecutionTimestamp());
    }

    @Test
    public void test_setters_and_getters() {
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl();
        long timestamp = System.currentTimeMillis();

        item.setName("setup-db");
        item.setStatus("IN_PROGRESS");
        item.setExecutionTimestamp(timestamp);

        Assert.assertEquals("setup-db", item.getName());
        Assert.assertEquals("IN_PROGRESS", item.getStatus());
        Assert.assertEquals(timestamp, item.getExecutionTimestamp());
    }

    @Test
    public void test_setName() {
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl();
        item.setName("database-migration");
        Assert.assertEquals("database-migration", item.getName());
    }

    @Test
    public void test_setName_with_null() {
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl("initial", "COMPLETE", 12345L);
        item.setName(null);
        Assert.assertNull(item.getName());
    }

    @Test
    public void test_setName_with_empty_string() {
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl();
        item.setName("");
        Assert.assertEquals("", item.getName());
    }

    @Test
    public void test_setStatus() {
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl();
        item.setStatus("FAILED");
        Assert.assertEquals("FAILED", item.getStatus());
    }

    @Test
    public void test_setStatus_with_null() {
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl("test", "COMPLETE", 12345L);
        item.setStatus(null);
        Assert.assertNull(item.getStatus());
    }

    @Test
    public void test_setStatus_various_values() {
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl();

        String[] statuses = {"PENDING", "IN_PROGRESS", "COMPLETE", "FAILED", "SKIPPED", "CANCELLED"};

        for (String status : statuses) {
            item.setStatus(status);
            Assert.assertEquals(status, item.getStatus());
        }
    }

    @Test
    public void test_setExecutionTimestamp() {
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl();
        long timestamp = System.currentTimeMillis();
        item.setExecutionTimestamp(timestamp);
        Assert.assertEquals(timestamp, item.getExecutionTimestamp());
    }

    @Test
    public void test_setExecutionTimestamp_with_zero() {
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl("test", "COMPLETE", 12345L);
        item.setExecutionTimestamp(0);
        Assert.assertEquals(0, item.getExecutionTimestamp());
    }

    @Test
    public void test_setExecutionTimestamp_with_negative() {
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl();
        item.setExecutionTimestamp(-1L);
        Assert.assertEquals(-1L, item.getExecutionTimestamp());
    }

    @Test
    public void test_toString() {
        long timestamp = 1234567890L;
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl("test-task", "FAILED", timestamp);

        String result = item.toString();
        Assert.assertTrue(result.contains("test-task"));
        Assert.assertTrue(result.contains("FAILED"));
        Assert.assertTrue(result.contains("1234567890"));
        Assert.assertTrue(result.contains("MongoDashboardSetupItemImpl"));
    }

    @Test
    public void test_toString_with_null_fields() {
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl();
        String result = item.toString();
        Assert.assertTrue(result.contains("name='null'"));
        Assert.assertTrue(result.contains("status='null'"));
        Assert.assertTrue(result.contains("executionTimestamp=0"));
    }

    @Test
    public void test_toString_format() {
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl("my-task", "COMPLETE", 999L);
        String result = item.toString();

        // Verify the format includes the class name and field names
        Assert.assertTrue(result.startsWith("MongoDashboardSetupItemImpl{"));
        Assert.assertTrue(result.endsWith("}"));
        Assert.assertTrue(result.contains("name="));
        Assert.assertTrue(result.contains("status="));
        Assert.assertTrue(result.contains("executionTimestamp="));
    }

    @Test
    public void test_name_with_special_characters() {
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl();
        item.setName("task-with-hyphens_and_underscores.and.dots");
        Assert.assertEquals("task-with-hyphens_and_underscores.and.dots", item.getName());
    }

    @Test
    public void test_name_with_unicode() {
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl();
        item.setName("タスク-日本語");
        Assert.assertEquals("タスク-日本語", item.getName());
    }

    @Test
    public void test_status_with_lowercase() {
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl();
        item.setStatus("complete");
        Assert.assertEquals("complete", item.getStatus());
    }

    @Test
    public void test_timestamp_boundary_values() {
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl();

        // Test with Long.MAX_VALUE
        item.setExecutionTimestamp(Long.MAX_VALUE);
        Assert.assertEquals(Long.MAX_VALUE, item.getExecutionTimestamp());

        // Test with Long.MIN_VALUE
        item.setExecutionTimestamp(Long.MIN_VALUE);
        Assert.assertEquals(Long.MIN_VALUE, item.getExecutionTimestamp());
    }

    @Test
    public void test_multiple_modifications() {
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl("initial", "PENDING", 100L);

        // First modification
        item.setName("modified");
        item.setStatus("IN_PROGRESS");
        item.setExecutionTimestamp(200L);

        Assert.assertEquals("modified", item.getName());
        Assert.assertEquals("IN_PROGRESS", item.getStatus());
        Assert.assertEquals(200L, item.getExecutionTimestamp());

        // Second modification
        item.setName("final");
        item.setStatus("COMPLETE");
        item.setExecutionTimestamp(300L);

        Assert.assertEquals("final", item.getName());
        Assert.assertEquals("COMPLETE", item.getStatus());
        Assert.assertEquals(300L, item.getExecutionTimestamp());
    }

    @Test
    public void test_field_independence() {
        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl();

        // Set name only
        item.setName("test");
        Assert.assertEquals("test", item.getName());
        Assert.assertNull(item.getStatus());
        Assert.assertEquals(0, item.getExecutionTimestamp());

        // Set status only
        item = new MongoDashboardSetupItemImpl();
        item.setStatus("COMPLETE");
        Assert.assertNull(item.getName());
        Assert.assertEquals("COMPLETE", item.getStatus());
        Assert.assertEquals(0, item.getExecutionTimestamp());

        // Set timestamp only
        item = new MongoDashboardSetupItemImpl();
        item.setExecutionTimestamp(123L);
        Assert.assertNull(item.getName());
        Assert.assertNull(item.getStatus());
        Assert.assertEquals(123L, item.getExecutionTimestamp());
    }

    @Test
    public void test_immutability_of_constructor_parameters() {
        String name = "original-name";
        String status = "ORIGINAL_STATUS";
        long timestamp = 12345L;

        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl(name, status, timestamp);

        // Modify the original variables
        name = "modified-name";
        status = "MODIFIED_STATUS";
        timestamp = 99999L;

        // Verify the item still has the original values
        Assert.assertEquals("original-name", item.getName());
        Assert.assertEquals("ORIGINAL_STATUS", item.getStatus());
        Assert.assertEquals(12345L, item.getExecutionTimestamp());
    }

    @Test
    public void test_long_name() {
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longName.append("a");
        }

        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl();
        item.setName(longName.toString());
        Assert.assertEquals(1000, item.getName().length());
        Assert.assertEquals(longName.toString(), item.getName());
    }

    @Test
    public void test_long_status() {
        StringBuilder longStatus = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            longStatus.append("STATUS_");
        }

        MongoDashboardSetupItemImpl item = new MongoDashboardSetupItemImpl();
        item.setStatus(longStatus.toString());
        Assert.assertEquals(longStatus.toString(), item.getStatus());
    }
}
