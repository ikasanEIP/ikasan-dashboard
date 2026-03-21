package org.ikasan.dashboard.cluster.service;

import org.ikasan.dashboard.cluster.config.ZooKeeperLeaderElectionProperties;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * Unit test for ZooKeeperLeaderElectionService.
 */
public class ZooKeeperLeaderElectionServiceTest {

    private ZooKeeperLeaderElectionProperties properties;
    private ZooKeeperLeaderElectionService service;

    @Before
    public void setUp() {
        properties = new ZooKeeperLeaderElectionProperties();
        service = new ZooKeeperLeaderElectionService(properties);
    }

    @Test
    public void testStartWhenDisabled() throws Exception {
        properties.setEnabled(false);

        service.start();

        // Should return true when disabled (treated as leader)
        Assert.assertTrue(service.isLeader());
    }

    @Test
    public void testIsLeaderWhenDisabled() {
        properties.setEnabled(false);

        // Should return true when disabled
        Assert.assertTrue(service.isLeader());
    }

    @Test
    public void testAwaitLeadershipWhenDisabled() throws InterruptedException {
        properties.setEnabled(false);

        // Should return immediately when disabled
        service.awaitLeadership();

        // No exception should be thrown
        Assert.assertTrue(true);
    }

    @Test
    public void testAddAndRemoveLeadershipListener() {
        LeadershipListener listener = mock(LeadershipListener.class);

        service.addLeadershipListener(listener);
        service.removeLeadershipListener(listener);

        // Test passes if no exception is thrown
        Assert.assertTrue(true);
    }

    @Test
    public void testMultipleListeners() {
        LeadershipListener listener1 = mock(LeadershipListener.class);
        LeadershipListener listener2 = mock(LeadershipListener.class);

        service.addLeadershipListener(listener1);
        service.addLeadershipListener(listener2);

        service.removeLeadershipListener(listener1);
        service.removeLeadershipListener(listener2);

        // Test passes if no exception is thrown
        Assert.assertTrue(true);
    }

    @Test
    public void testStopWhenNotStarted() {
        // Should handle stop gracefully when not started
        service.stop();

        Assert.assertTrue(true);
    }

    @Test
    public void testStopWhenDisabled() {
        properties.setEnabled(false);

        service.stop();

        // Should handle stop gracefully when disabled
        Assert.assertTrue(true);
    }

    @Test
    public void testPropertiesInjection() {
        ZooKeeperLeaderElectionProperties customProperties = new ZooKeeperLeaderElectionProperties();
        customProperties.setEnabled(false);
        customProperties.setConnectionString("test:2181");
        customProperties.setSessionTimeoutMs(5000);

        ZooKeeperLeaderElectionService customService = new ZooKeeperLeaderElectionService(customProperties);

        // Service should use injected properties
        Assert.assertNotNull(customService);
        Assert.assertTrue(customService.isLeader()); // Returns true when disabled
    }
}
