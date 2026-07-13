package org.ikasan.dashboard.cluster.config;

import org.ikasan.dashboard.cluster.service.LeaderElectionService;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.rest.client.ContextMachineRestServiceImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for ContextMachineClusterConfiguration.
 */
public class ContextMachineClusterConfigurationTest {

    private final LeaderElectionService leaderElectionService = mock(LeaderElectionService.class);
    private final ContextMachineClusterConfiguration configuration = new ContextMachineClusterConfiguration();

    @Before
    public void setUp() {
        ContextMachineCache.instance().resetAllCache();
        ReflectionTestUtils.setField(configuration, "leaderElectionService", leaderElectionService);
        ReflectionTestUtils.setField(configuration, "contextMachineRestServices", new ArrayList<ContextMachineRestServiceImpl>());
        ReflectionTestUtils.setField(configuration, "leaderLookupRetries", 3);
        ReflectionTestUtils.setField(configuration, "leaderLookupRetryIntervalMs", 500L);
    }

    @After
    public void tearDown() {
        ContextMachineCache.instance().registerLeaderProvider(null);
        ContextMachineCache.instance().registerFallbackProvider(null);
        ContextMachineCache.instance().resetAllCache();
    }

    @Test
    public void testLeaderProviderIsAlwaysRegisteredEvenWithoutPeers() {
        when(leaderElectionService.isLeader()).thenReturn(true);

        configuration.registerContextMachineFallback();

        Assert.assertTrue("Leader provider must be registered so isLeader() reflects the election " +
            "service even in a single-node (no peers) deployment", ContextMachineCache.instance().isLeader());
    }

    @Test
    public void testLeaderProviderReflectsFollowerStatus() {
        when(leaderElectionService.isLeader()).thenReturn(false);

        configuration.registerContextMachineFallback();

        Assert.assertFalse(ContextMachineCache.instance().isLeader());
    }

    @Test
    public void testNoFallbackProviderRegisteredWhenNoPeersConfigured() {
        // Pre-register a sentinel to prove registerContextMachineFallback() leaves it untouched
        // rather than overwriting it with a no-op when there are no peers.
        ContextMachineCache.FallbackProvider sentinel = id -> mock(ContextMachine.class);
        ContextMachineCache.instance().registerFallbackProvider(sentinel);

        configuration.registerContextMachineFallback();

        Assert.assertNotNull("registerContextMachineFallback() must not touch the fallback provider " +
            "when contextMachineRestServices is empty (single-node deployment)",
            ContextMachineCache.instance().getByContextInstanceId("unknown-instance-id"));
    }

    @Test
    public void testFallbackReturnsNullWhenThisNodeIsLeader() {
        ContextMachineRestServiceImpl peer = mock(ContextMachineRestServiceImpl.class);
        when(peer.getBaseUrl()).thenReturn("http://peer:9090");
        ReflectionTestUtils.setField(configuration, "contextMachineRestServices", List.of(peer));
        when(leaderElectionService.isLeader()).thenReturn(true);

        configuration.registerContextMachineFallback();

        Assert.assertNull("The leader owns all its local context instances; a cache miss means the " +
            "instance genuinely doesn't exist, so the fallback must return null rather than proxying",
            ContextMachineCache.instance().getByContextInstanceId("unknown-instance-id"));
    }

    @Test
    public void testFallbackReturnsRestProxyWhenThisNodeIsFollower() {
        ContextMachineRestServiceImpl peer = mock(ContextMachineRestServiceImpl.class);
        when(peer.getBaseUrl()).thenReturn("http://peer:9090");
        ReflectionTestUtils.setField(configuration, "contextMachineRestServices", List.of(peer));
        when(leaderElectionService.isLeader()).thenReturn(false);

        configuration.registerContextMachineFallback();

        ContextMachine result = ContextMachineCache.instance().getByContextInstanceId("unknown-instance-id");
        Assert.assertNotNull("A follower must proxy an unknown context instance to the leader " +
            "rather than returning null", result);
    }
}
