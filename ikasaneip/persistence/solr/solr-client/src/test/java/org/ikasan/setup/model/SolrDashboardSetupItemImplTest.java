package org.ikasan.setup.model;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for SolrDashboardSetupItemImpl
 */
public class SolrDashboardSetupItemImplTest {

    @Test
    public void test_default_constructor() {
        SolrDashboardSetupItemImpl item = new SolrDashboardSetupItemImpl();
        Assert.assertNull(item.getName());
        Assert.assertNull(item.getStatus());
        Assert.assertEquals(0, item.getExecutionTimestamp());
    }

    @Test
    public void test_constructor_with_all_fields() {
        long timestamp = System.currentTimeMillis();
        SolrDashboardSetupItemImpl item = new SolrDashboardSetupItemImpl("test-item", "COMPLETE", timestamp);

        Assert.assertEquals("test-item", item.getName());
        Assert.assertEquals("COMPLETE", item.getStatus());
        Assert.assertEquals(timestamp, item.getExecutionTimestamp());
    }

    @Test
    public void test_setters_and_getters() {
        SolrDashboardSetupItemImpl item = new SolrDashboardSetupItemImpl();
        long timestamp = System.currentTimeMillis();

        item.setName("setup-db");
        item.setStatus("IN_PROGRESS");
        item.setExecutionTimestamp(timestamp);

        Assert.assertEquals("setup-db", item.getName());
        Assert.assertEquals("IN_PROGRESS", item.getStatus());
        Assert.assertEquals(timestamp, item.getExecutionTimestamp());
    }

    @Test
    public void test_toString() {
        long timestamp = 1234567890L;
        SolrDashboardSetupItemImpl item = new SolrDashboardSetupItemImpl("test", "FAILED", timestamp);

        String result = item.toString();
        Assert.assertTrue(result.contains("test"));
        Assert.assertTrue(result.contains("FAILED"));
        Assert.assertTrue(result.contains("1234567890"));
    }
}
