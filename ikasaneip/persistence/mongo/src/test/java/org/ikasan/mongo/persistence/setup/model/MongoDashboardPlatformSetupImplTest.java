package org.ikasan.mongo.persistence.setup.model;

import org.ikasan.spec.persistence.model.DashboardSetupItem;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive unit test for MongoDashboardPlatformSetupImpl
 */
public class MongoDashboardPlatformSetupImplTest {

    @Test
    public void test_default_constructor() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        Assert.assertNull(setup.getId());
        Assert.assertNull(setup.getType());
        Assert.assertNotNull(setup.getPlatformSetupItems());
        Assert.assertEquals(0, setup.getPlatformSetupItems().size());
        Assert.assertEquals(0, setup.getTimestamp());
        Assert.assertEquals(0, setup.getModifiedTimestamp());
    }

    @Test
    public void test_setId_and_getId() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setId("test-id");
        Assert.assertEquals("test-id", setup.getId());
    }

    @Test
    public void test_setId_with_null() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setId("initial-id");
        setup.setId(null);
        Assert.assertNull(setup.getId());
    }

    @Test
    public void test_setType_and_getType() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setType("INITIAL_SETUP");
        Assert.assertEquals("INITIAL_SETUP", setup.getType());
    }

    @Test
    public void test_setType_with_null() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setType("INITIAL");
        setup.setType(null);
        Assert.assertNull(setup.getType());
    }

    @Test
    public void test_setPlatformSetupItems_and_getPlatformSetupItems() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();

        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new MongoDashboardSetupItemImpl("item1", "COMPLETE", System.currentTimeMillis()));
        items.add(new MongoDashboardSetupItemImpl("item2", "IN_PROGRESS", System.currentTimeMillis()));

        setup.setPlatformSetupItems(items);

        Assert.assertNotNull(setup.getPlatformSetupItems());
        Assert.assertEquals(2, setup.getPlatformSetupItems().size());
        Assert.assertEquals("item1", setup.getPlatformSetupItems().get(0).getName());
        Assert.assertEquals("item2", setup.getPlatformSetupItems().get(1).getName());
    }

    @Test
    public void test_getPlatformSetupItems_returns_empty_list_when_null() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setPlatformSetupItems(null);

        List<DashboardSetupItem> result = setup.getPlatformSetupItems();
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void test_setPlatformSetupItems_with_empty_list() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setPlatformSetupItems(new ArrayList<>());

        Assert.assertNotNull(setup.getPlatformSetupItems());
        Assert.assertEquals(0, setup.getPlatformSetupItems().size());
    }

    @Test
    public void test_setTimestamp_and_getTimestamp() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        long timestamp = System.currentTimeMillis();
        setup.setTimestamp(timestamp);
        Assert.assertEquals(timestamp, setup.getTimestamp());
    }

    @Test
    public void test_setTimestamp_with_zero() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setTimestamp(12345L);
        setup.setTimestamp(0);
        Assert.assertEquals(0, setup.getTimestamp());
    }

    @Test
    public void test_setModifiedTimestamp_and_getModifiedTimestamp() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        long timestamp = System.currentTimeMillis();
        setup.setModifiedTimestamp(timestamp);
        Assert.assertEquals(timestamp, setup.getModifiedTimestamp());
    }

    @Test
    public void test_setModifiedTimestamp_with_zero() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setModifiedTimestamp(12345L);
        setup.setModifiedTimestamp(0);
        Assert.assertEquals(0, setup.getModifiedTimestamp());
    }

    @Test
    public void test_toString() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setId("test-id");
        setup.setType("TEST_TYPE");
        setup.setTimestamp(12345L);
        setup.setModifiedTimestamp(67890L);

        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new MongoDashboardSetupItemImpl("item1", "COMPLETE", System.currentTimeMillis()));
        setup.setPlatformSetupItems(items);

        String result = setup.toString();
        Assert.assertTrue(result.contains("test-id"));
        Assert.assertTrue(result.contains("TEST_TYPE"));
        Assert.assertTrue(result.contains("12345"));
        Assert.assertTrue(result.contains("67890"));
        Assert.assertTrue(result.contains("MongoDashboardPlatformSetupImpl"));
    }

    @Test
    public void test_toString_with_null_fields() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        String result = setup.toString();
        Assert.assertTrue(result.contains("id='null'"));
        Assert.assertTrue(result.contains("type='null'"));
    }

    @Test
    public void test_complete_workflow() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();

        // Set all fields
        setup.setId("dashboardPlatformSetup");
        setup.setType("COMPLETE_WORKFLOW");
        long timestamp = System.currentTimeMillis();
        setup.setTimestamp(timestamp);
        setup.setModifiedTimestamp(timestamp + 1000);

        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new MongoDashboardSetupItemImpl("database-init", "COMPLETE", timestamp));
        items.add(new MongoDashboardSetupItemImpl("mongo-init", "COMPLETE", timestamp + 100));
        items.add(new MongoDashboardSetupItemImpl("user-setup", "IN_PROGRESS", timestamp + 200));
        setup.setPlatformSetupItems(items);

        // Verify all fields
        Assert.assertEquals("dashboardPlatformSetup", setup.getId());
        Assert.assertEquals("COMPLETE_WORKFLOW", setup.getType());
        Assert.assertEquals(timestamp, setup.getTimestamp());
        Assert.assertEquals(timestamp + 1000, setup.getModifiedTimestamp());
        Assert.assertEquals(3, setup.getPlatformSetupItems().size());
        Assert.assertEquals("database-init", setup.getPlatformSetupItems().get(0).getName());
        Assert.assertEquals("COMPLETE", setup.getPlatformSetupItems().get(0).getStatus());
    }

    @Test
    public void test_multiple_item_modifications() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();

        // First set of items
        List<DashboardSetupItem> items1 = new ArrayList<>();
        items1.add(new MongoDashboardSetupItemImpl("item1", "PENDING", System.currentTimeMillis()));
        setup.setPlatformSetupItems(items1);
        Assert.assertEquals(1, setup.getPlatformSetupItems().size());

        // Second set of items
        List<DashboardSetupItem> items2 = new ArrayList<>();
        items2.add(new MongoDashboardSetupItemImpl("item1", "COMPLETE", System.currentTimeMillis()));
        items2.add(new MongoDashboardSetupItemImpl("item2", "IN_PROGRESS", System.currentTimeMillis()));
        setup.setPlatformSetupItems(items2);
        Assert.assertEquals(2, setup.getPlatformSetupItems().size());
        Assert.assertEquals("COMPLETE", setup.getPlatformSetupItems().get(0).getStatus());
    }

    @Test
    public void test_large_number_of_items() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();

        List<DashboardSetupItem> items = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            items.add(new MongoDashboardSetupItemImpl("item-" + i, "COMPLETE", System.currentTimeMillis()));
        }
        setup.setPlatformSetupItems(items);

        Assert.assertEquals(100, setup.getPlatformSetupItems().size());
        Assert.assertEquals("item-0", setup.getPlatformSetupItems().get(0).getName());
        Assert.assertEquals("item-99", setup.getPlatformSetupItems().get(99).getName());
    }

    @Test
    public void test_timestamp_boundaries() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();

        // Test with Long.MAX_VALUE
        setup.setTimestamp(Long.MAX_VALUE);
        setup.setModifiedTimestamp(Long.MAX_VALUE);
        Assert.assertEquals(Long.MAX_VALUE, setup.getTimestamp());
        Assert.assertEquals(Long.MAX_VALUE, setup.getModifiedTimestamp());

        // Test with Long.MIN_VALUE
        setup.setTimestamp(Long.MIN_VALUE);
        setup.setModifiedTimestamp(Long.MIN_VALUE);
        Assert.assertEquals(Long.MIN_VALUE, setup.getTimestamp());
        Assert.assertEquals(Long.MIN_VALUE, setup.getModifiedTimestamp());
    }

    @Test
    public void test_id_with_special_characters() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setId("setup-with-hyphens_and_underscores.and.dots");
        Assert.assertEquals("setup-with-hyphens_and_underscores.and.dots", setup.getId());
    }

    @Test
    public void test_type_with_various_values() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();

        String[] types = {"INITIAL_SETUP", "UPDATE", "MIGRATION", "ROLLBACK", "CUSTOM"};
        for (String type : types) {
            setup.setType(type);
            Assert.assertEquals(type, setup.getType());
        }
    }

    @Test
    public void test_field_independence() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();

        // Set ID only
        setup.setId("test-id");
        Assert.assertEquals("test-id", setup.getId());
        Assert.assertNull(setup.getType());
        Assert.assertEquals(0, setup.getTimestamp());
        Assert.assertEquals(0, setup.getModifiedTimestamp());

        // Reset and set Type only
        setup = new MongoDashboardPlatformSetupImpl();
        setup.setType("TEST_TYPE");
        Assert.assertNull(setup.getId());
        Assert.assertEquals("TEST_TYPE", setup.getType());
        Assert.assertEquals(0, setup.getTimestamp());

        // Reset and set Timestamp only
        setup = new MongoDashboardPlatformSetupImpl();
        setup.setTimestamp(12345L);
        Assert.assertNull(setup.getId());
        Assert.assertNull(setup.getType());
        Assert.assertEquals(12345L, setup.getTimestamp());
        Assert.assertEquals(0, setup.getModifiedTimestamp());
    }

    @Test
    public void test_items_list_immutability() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();

        List<DashboardSetupItem> originalItems = new ArrayList<>();
        originalItems.add(new MongoDashboardSetupItemImpl("item1", "COMPLETE", System.currentTimeMillis()));
        setup.setPlatformSetupItems(originalItems);

        // Modify the original list
        originalItems.add(new MongoDashboardSetupItemImpl("item2", "IN_PROGRESS", System.currentTimeMillis()));

        // The JSON-based implementation provides immutability - changes to the original list
        // should NOT be reflected in the setup since it was serialized to JSON
        List<DashboardSetupItem> retrievedItems = setup.getPlatformSetupItems();
        Assert.assertEquals(1, retrievedItems.size());
        Assert.assertEquals("item1", retrievedItems.get(0).getName());
    }

    @Test
    public void test_null_safety_of_getPlatformSetupItems() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();

        // Initially should return empty list, not null
        Assert.assertNotNull(setup.getPlatformSetupItems());
        Assert.assertEquals(0, setup.getPlatformSetupItems().size());

        // After setting to null, should still return empty list
        setup.setPlatformSetupItems(null);
        Assert.assertNotNull(setup.getPlatformSetupItems());
        Assert.assertEquals(0, setup.getPlatformSetupItems().size());

        // After setting to empty list
        setup.setPlatformSetupItems(new ArrayList<>());
        Assert.assertNotNull(setup.getPlatformSetupItems());
        Assert.assertEquals(0, setup.getPlatformSetupItems().size());
    }

    @Test
    public void test_timestamp_sequence() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();

        long timestamp1 = System.currentTimeMillis();
        setup.setTimestamp(timestamp1);

        // Simulate some processing time
        long timestamp2 = timestamp1 + 1000;
        setup.setModifiedTimestamp(timestamp2);

        Assert.assertEquals(timestamp1, setup.getTimestamp());
        Assert.assertEquals(timestamp2, setup.getModifiedTimestamp());
        Assert.assertTrue(setup.getModifiedTimestamp() > setup.getTimestamp());
    }

    @Test
    public void test_items_with_different_statuses() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();

        List<DashboardSetupItem> items = new ArrayList<>();
        items.add(new MongoDashboardSetupItemImpl("task1", "COMPLETE", System.currentTimeMillis()));
        items.add(new MongoDashboardSetupItemImpl("task2", "IN_PROGRESS", System.currentTimeMillis()));
        items.add(new MongoDashboardSetupItemImpl("task3", "PENDING", System.currentTimeMillis()));
        items.add(new MongoDashboardSetupItemImpl("task4", "FAILED", System.currentTimeMillis()));
        items.add(new MongoDashboardSetupItemImpl("task5", "SKIPPED", System.currentTimeMillis()));
        setup.setPlatformSetupItems(items);

        Assert.assertEquals(5, setup.getPlatformSetupItems().size());
        Assert.assertEquals("COMPLETE", setup.getPlatformSetupItems().get(0).getStatus());
        Assert.assertEquals("IN_PROGRESS", setup.getPlatformSetupItems().get(1).getStatus());
        Assert.assertEquals("PENDING", setup.getPlatformSetupItems().get(2).getStatus());
        Assert.assertEquals("FAILED", setup.getPlatformSetupItems().get(3).getStatus());
        Assert.assertEquals("SKIPPED", setup.getPlatformSetupItems().get(4).getStatus());
    }

    @Test
    public void test_toString_format() {
        MongoDashboardPlatformSetupImpl setup = new MongoDashboardPlatformSetupImpl();
        setup.setId("id123");
        setup.setType("TYPE");
        setup.setPlatformSetupItems(new ArrayList<>());

        String result = setup.toString();
        Assert.assertTrue(result.startsWith("MongoDashboardPlatformSetupImpl{"));
        Assert.assertTrue(result.endsWith("}"));
        Assert.assertTrue(result.contains("id="));
        Assert.assertTrue(result.contains("type="));
        Assert.assertTrue(result.contains("platformSetupItems="));
        Assert.assertTrue(result.contains("timestamp="));
        Assert.assertTrue(result.contains("modifiedTimestamp="));
    }
}
