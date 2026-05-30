package org.ikasan.dashboard.cluster.config;

import org.ikasan.dashboard.cluster.service.LeaderElectionService;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.rest.client.ContextMachineRestServiceImpl;
import org.ikasan.job.orchestration.rest.dashboard.ContextMachineRestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All the Cluster-related work here is temporary to support development and testing
 * The real implementation of clustering and leader elected will be dealt with by a different peice of work by Mick
 * -
 * Registers a leader-aware fallback provider with {@link ContextMachineCache}.
 * -
 * When a context instance is not in the local cache:
 * - If this node is the <strong>leader</strong>, the context instance has ended or does not exist —
 *   the fallback returns {@code null} so the caller can surface the appropriate error.
 * - If this node is a <strong>follower</strong>, the context instance lives on the leader —
 *   the fallback returns a {@link ContextMachineRestImpl} that proxies operations directly to
 *   the leader node discovered via ZooKeeper, retrying up to leaderLookupRetries times with a
 *   configurable interval on leadership transitions.
 * -
 * No fallback is registered when there are no peer URLs configured (single-node deployment).
 */
@Configuration
public class ContextMachineClusterConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(ContextMachineClusterConfiguration.class);

    @Autowired
    private LeaderElectionService leaderElectionService;

    @Autowired
    private List<ContextMachineRestServiceImpl> contextMachineRestServices;

    @Value("${ikasan.dashboard.cluster.zookeeper.leader-lookup-retries:3}")
    private int leaderLookupRetries;

    @Value("${ikasan.dashboard.cluster.zookeeper.leader-lookup-retry-interval-ms:500}")
    private long leaderLookupRetryIntervalMs;

    @PostConstruct
    public void registerContextMachineFallback() {
        // Always register the leader provider so UI guards (isLeader()) work correctly on all
        // nodes, regardless of whether peer REST services are configured.
        ContextMachineCache.instance().registerLeaderProvider(leaderElectionService::isLeader);

        if (contextMachineRestServices.isEmpty()) {
            LOG.info("No peer cluster nodes configured — ContextMachine REST proxy fallback will not be registered");
            return;
        }

        Map<String, ContextMachineRestServiceImpl> peersByUrl = new LinkedHashMap<>();
        for (ContextMachineRestServiceImpl svc : contextMachineRestServices) {
            peersByUrl.put(svc.getBaseUrl(), svc);
        }

        ContextMachineCache.instance().registerFallbackProvider(contextInstanceId -> {
            if (leaderElectionService.isLeader()) {
                return null;
            }
            return new ContextMachineRestImpl(
                contextInstanceId,
                leaderElectionService::getLeaderUrl,
                peersByUrl,
                leaderLookupRetries,
                leaderLookupRetryIntervalMs
            );
        });

        LOG.info("ContextMachine REST proxy fallback registered for {} peer node(s) — " +
            "leader-directed routing enabled (retries={}, intervalMs={})",
            contextMachineRestServices.size(), leaderLookupRetries, leaderLookupRetryIntervalMs);
    }
}
