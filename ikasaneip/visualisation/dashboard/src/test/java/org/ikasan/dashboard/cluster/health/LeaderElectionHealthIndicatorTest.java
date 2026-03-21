package org.ikasan.dashboard.cluster.health;

import org.ikasan.dashboard.cluster.service.LeaderElectionService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.mockito.Mockito.*;

/**
 * Unit test for LeaderElectionHealthIndicator.
 */
public class LeaderElectionHealthIndicatorTest {

    private LeaderElectionService leaderElectionService;
    private LeaderElectionHealthIndicator healthIndicator;

    @Before
    public void setUp() {
        leaderElectionService = mock(LeaderElectionService.class);
        healthIndicator = new LeaderElectionHealthIndicator(leaderElectionService);
    }

    @Test
    public void testHealthWhenLeader() {
        when(leaderElectionService.isLeader()).thenReturn(true);

        Health health = healthIndicator.health();

        Assert.assertNotNull(health);
        Assert.assertEquals(Status.UP, health.getStatus());
        Assert.assertTrue((Boolean) health.getDetails().get("leader"));
        Assert.assertEquals("LEADER", health.getDetails().get("status"));

        verify(leaderElectionService, times(1)).isLeader();
    }

    @Test
    public void testHealthWhenFollower() {
        when(leaderElectionService.isLeader()).thenReturn(false);

        Health health = healthIndicator.health();

        Assert.assertNotNull(health);
        Assert.assertEquals(Status.UP, health.getStatus());
        Assert.assertFalse((Boolean) health.getDetails().get("leader"));
        Assert.assertEquals("FOLLOWER", health.getDetails().get("status"));

        verify(leaderElectionService, times(1)).isLeader();
    }

    @Test
    public void testHealthIndicatorAlwaysReturnsUp() {
        // Test when leader
        when(leaderElectionService.isLeader()).thenReturn(true);
        Health healthAsLeader = healthIndicator.health();
        Assert.assertEquals(Status.UP, healthAsLeader.getStatus());

        // Test when follower
        when(leaderElectionService.isLeader()).thenReturn(false);
        Health healthAsFollower = healthIndicator.health();
        Assert.assertEquals(Status.UP, healthAsFollower.getStatus());
    }

    @Test
    public void testHealthDetailsContainCorrectKeys() {
        when(leaderElectionService.isLeader()).thenReturn(true);

        Health health = healthIndicator.health();

        Assert.assertTrue(health.getDetails().containsKey("leader"));
        Assert.assertTrue(health.getDetails().containsKey("status"));
    }

    @Test
    public void testMultipleHealthChecks() {
        when(leaderElectionService.isLeader()).thenReturn(true);

        // First check
        Health health1 = healthIndicator.health();
        Assert.assertEquals("LEADER", health1.getDetails().get("status"));

        // Second check
        Health health2 = healthIndicator.health();
        Assert.assertEquals("LEADER", health2.getDetails().get("status"));

        verify(leaderElectionService, times(2)).isLeader();
    }

    @Test
    public void testHealthCheckStateChange() {
        // Initially leader
        when(leaderElectionService.isLeader()).thenReturn(true);
        Health healthAsLeader = healthIndicator.health();
        Assert.assertEquals("LEADER", healthAsLeader.getDetails().get("status"));

        // Now follower
        when(leaderElectionService.isLeader()).thenReturn(false);
        Health healthAsFollower = healthIndicator.health();
        Assert.assertEquals("FOLLOWER", healthAsFollower.getDetails().get("status"));

        verify(leaderElectionService, times(2)).isLeader();
    }
}
