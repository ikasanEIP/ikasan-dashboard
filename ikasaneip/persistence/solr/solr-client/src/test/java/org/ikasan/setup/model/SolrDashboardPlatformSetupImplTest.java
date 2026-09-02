package org.ikasan.setup.model;

import org.ikasan.spec.persistence.model.DashboardSetupItem;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for SolrDashboardPlatformSetupImpl
 */
public class SolrDashboardPlatformSetupImplTest {

    @Test
    public void test_setters_and_getters() {
        SolrDashboardPlatformSetupImpl setup = new SolrDashboardPlatformSetupImpl();

        setup.setId("platform-setup-1");
        setup.setType("INITIAL_SETUP");
        setup.setTimestamp(1000L);
        setup.setModifiedTimestamp(2000L);

        Assert.assertEquals("platform-setup-1", setup.getId());
        Assert.assertEquals("INITIAL_SETUP", setup.getType());
        Assert.assertEquals(1000L, setup.getTimestamp());
        Assert.assertEquals(2000L, setup.getModifiedTimestamp());
    }

    @Test
    public void test_platform_setup_items_serialization_deserialization() {
        SolrDashboardPlatformSetupImpl setup = new SolrDashboardPlatformSetupImpl();

        // Create setup items
        List<DashboardSetupItem> items = new ArrayList<>();
        SolrDashboardSetupItemImpl item1 = new SolrDashboardSetupItemImpl("database-init", "COMPLETE", System.currentTimeMillis());
        SolrDashboardSetupItemImpl item2 = new SolrDashboardSetupItemImpl("solr-init", "IN_PROGRESS", System.currentTimeMillis());
        items.add(item1);
        items.add(item2);

        // Set items
        setup.setPlatformSetupItems(items);

        // Retrieve items
        List<DashboardSetupItem> retrievedItems = setup.getPlatformSetupItems();

        Assert.assertNotNull(retrievedItems);
        Assert.assertEquals(2, retrievedItems.size());
        Assert.assertEquals("database-init", retrievedItems.get(0).getName());
        Assert.assertEquals("COMPLETE", retrievedItems.get(0).getStatus());
        Assert.assertEquals("solr-init", retrievedItems.get(1).getName());
        Assert.assertEquals("IN_PROGRESS", retrievedItems.get(1).getStatus());
    }

    @Test
    public void test_platform_setup_items_empty_list() {
        SolrDashboardPlatformSetupImpl setup = new SolrDashboardPlatformSetupImpl();
        List<DashboardSetupItem> emptyList = new ArrayList<>();

        setup.setPlatformSetupItems(emptyList);
        List<DashboardSetupItem> retrievedItems = setup.getPlatformSetupItems();

        Assert.assertNotNull(retrievedItems);
        Assert.assertEquals(0, retrievedItems.size());
    }

    @Test
    public void test_toString() {
        SolrDashboardPlatformSetupImpl setup = new SolrDashboardPlatformSetupImpl();
        setup.setId("test-id");
        setup.setType("TEST_TYPE");
        setup.setTimestamp(1000L);
        setup.setModifiedTimestamp(2000L);

        String result = setup.toString();
        Assert.assertTrue(result.contains("test-id"));
        Assert.assertTrue(result.contains("TEST_TYPE"));
        Assert.assertTrue(result.contains("1000"));
        Assert.assertTrue(result.contains("2000"));
    }
}
