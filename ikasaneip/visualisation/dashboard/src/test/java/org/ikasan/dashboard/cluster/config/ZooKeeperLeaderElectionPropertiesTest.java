package org.ikasan.dashboard.cluster.config;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for ZooKeeperLeaderElectionProperties.
 */
public class ZooKeeperLeaderElectionPropertiesTest {

    @Test
    public void testDefaultValues() {
        ZooKeeperLeaderElectionProperties properties = new ZooKeeperLeaderElectionProperties();

        Assert.assertFalse("Leader election should be disabled by default", properties.isEnabled());
        Assert.assertNull("Connection string should be null by default", properties.getConnectionString());
        Assert.assertEquals("Default session timeout should be 30000ms", 30000, properties.getSessionTimeoutMs());
        Assert.assertEquals("Default connection timeout should be 15000ms", 15000, properties.getConnectionTimeoutMs());
        Assert.assertEquals("Default base sleep time should be 1000ms", 1000, properties.getBaseSleepTimeMs());
        Assert.assertEquals("Default max retries should be 3", 3, properties.getMaxRetries());
        Assert.assertEquals("Default namespace should be /ikasan", "ikasan", properties.getNamespace());
        Assert.assertEquals("Default leader path should be /leader", "/leader", properties.getLeaderPath());
    }

    @Test
    public void testSettersAndGetters() {
        ZooKeeperLeaderElectionProperties properties = new ZooKeeperLeaderElectionProperties();

        properties.setEnabled(true);
        Assert.assertTrue(properties.isEnabled());

        properties.setConnectionString("localhost:2181,localhost:2182");
        Assert.assertEquals("localhost:2181,localhost:2182", properties.getConnectionString());

        properties.setSessionTimeoutMs(60000);
        Assert.assertEquals(60000, properties.getSessionTimeoutMs());

        properties.setConnectionTimeoutMs(20000);
        Assert.assertEquals(20000, properties.getConnectionTimeoutMs());

        properties.setBaseSleepTimeMs(2000);
        Assert.assertEquals(2000, properties.getBaseSleepTimeMs());

        properties.setMaxRetries(5);
        Assert.assertEquals(5, properties.getMaxRetries());

        properties.setNamespace("/custom-namespace");
        Assert.assertEquals("/custom-namespace", properties.getNamespace());

        properties.setLeaderPath("/custom-leader");
        Assert.assertEquals("/custom-leader", properties.getLeaderPath());
    }

    @Test
    public void testModifyProperties() {
        ZooKeeperLeaderElectionProperties properties = new ZooKeeperLeaderElectionProperties();
        properties.setEnabled(false);
        properties.setConnectionString("zk1:2181");

        Assert.assertFalse(properties.isEnabled());
        Assert.assertEquals("zk1:2181", properties.getConnectionString());

        // Modify values
        properties.setEnabled(true);
        properties.setConnectionString("zk2:2181,zk3:2181");

        Assert.assertTrue(properties.isEnabled());
        Assert.assertEquals("zk2:2181,zk3:2181", properties.getConnectionString());
    }
}
