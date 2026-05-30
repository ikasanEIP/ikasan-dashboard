package org.ikasan.dashboard.cluster.service;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.ikasan.dashboard.cluster.config.ZooKeeperLeaderElectionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

/**
 * ZooKeeper-based implementation of the LeaderElectionService using Apache Curator.
 */
@Service
@ConditionalOnProperty(name = "ikasan.dashboard.cluster.provider", havingValue = "zookeeper", matchIfMissing = true)
public class ZooKeeperLeaderElectionService implements LeaderElectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZooKeeperLeaderElectionService.class);

    private final ZooKeeperLeaderElectionProperties properties;
    private CuratorFramework curatorClient;
    private LeaderLatch leaderLatch;
    private final Set<LeadershipListener> listeners = new CopyOnWriteArraySet<>();
    private volatile boolean started = false;

    public ZooKeeperLeaderElectionService(ZooKeeperLeaderElectionProperties properties) {
        this.properties = properties;
    }

    @Override
    public void start() throws Exception {
        if (!properties.isEnabled()) {
            LOGGER.info("Leader election is disabled");
            return;
        }

        if (started) {
            LOGGER.warn("Leader election service already started");
            return;
        }

        LOGGER.info("Starting ZooKeeper leader election service");
        LOGGER.info("Connection string: {}", properties.getConnectionString());

        // Create Curator client
        curatorClient = CuratorFrameworkFactory.builder()
            .connectString(properties.getConnectionString())
            .sessionTimeoutMs(properties.getSessionTimeoutMs())
            .connectionTimeoutMs(properties.getConnectionTimeoutMs())
            .retryPolicy(new ExponentialBackoffRetry(
                properties.getBaseSleepTimeMs(),
                properties.getMaxRetries()
            ))
            .namespace(properties.getNamespace())
            .build();

        curatorClient.start();

        // Wait for connection
        if (!curatorClient.blockUntilConnected(
                properties.getConnectionTimeoutMs(),
                TimeUnit.MILLISECONDS)) {
            throw new IllegalStateException(
                "Failed to connect to ZooKeeper within timeout"
            );
        }

        LOGGER.info("Connected to ZooKeeper");

        // Use the configured node URL as the participant ID so peers can resolve the leader's
        // URL directly from ZooKeeper. Fall back to hostname-timestamp if not configured, but
        // warn because leader-directed routing will not work without a nodeUrl.
        String nodeUrl = properties.getNodeUrl();
        String instanceId;
        if (nodeUrl != null && !nodeUrl.isBlank()) {
            instanceId = nodeUrl.toLowerCase();
        } else {
            instanceId = InetAddress.getLocalHost().getHostName() + "-" + System.currentTimeMillis();
            LOGGER.warn("ikasan.dashboard.cluster.zookeeper.node-url is not configured. " +
                "Leader-directed REST routing will fall back to fan-out. " +
                "Set this to the base URL of this node (e.g. http://node1:9090).");
        }

        // Create leader latch
        leaderLatch = new LeaderLatch(
            curatorClient,
            properties.getLeaderPath(),
            instanceId
        );

        // Add internal listener to propagate events
        leaderLatch.addListener(new LeaderLatchListener() {
            @Override
            public void isLeader() {
                LOGGER.info("This instance is now the LEADER");
                notifyLeadershipAcquired();
            }

            @Override
            public void notLeader() {
                LOGGER.info("This instance is now a FOLLOWER");
                notifyLeadershipLost();
            }
        });

        leaderLatch.start();
        started = true;

        LOGGER.info("Leader election service started with ID: {}", instanceId);
    }

    @Override
    public String getLeaderUrl() {
        if (!properties.isEnabled() || leaderLatch == null) return null;
        try {
            return leaderLatch.getLeader().getId();
        } catch (Exception e) {
            LOGGER.warn("Failed to retrieve leader URL from ZooKeeper: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @PreDestroy
    public void stop() {
        if (!started) {
            return;
        }

        LOGGER.info("Stopping ZooKeeper leader election service");

        try {
            if (leaderLatch != null) {
                leaderLatch.close();
            }
        } catch (IOException e) {
            LOGGER.error("Error closing leader latch", e);
        }

        if (curatorClient != null) {
            curatorClient.close();
        }

        started = false;
        LOGGER.info("Leader election service stopped");
    }

    @Override
    public boolean isLeader() {
        if (!properties.isEnabled() || leaderLatch == null) {
            return true; // If disabled, treat as leader
        }
        return leaderLatch.hasLeadership();
    }

    @Override
    public void awaitLeadership() throws InterruptedException {
        if (!properties.isEnabled() || leaderLatch == null) {
            return; // If disabled, return immediately
        }
        try {
            leaderLatch.await();
        } catch (EOFException e) {
            LOGGER.error("Connection closed while waiting for leadership", e);
            throw new InterruptedException("Connection closed while waiting for leadership");
        }
    }

    @Override
    public void addLeadershipListener(LeadershipListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeLeadershipListener(LeadershipListener listener) {
        listeners.remove(listener);
    }

    private void notifyLeadershipAcquired() {
        for (LeadershipListener listener : listeners) {
            try {
                listener.onLeadershipAcquired();
            } catch (Exception e) {
                LOGGER.error("Error notifying listener of leadership acquired", e);
            }
        }
    }

    private void notifyLeadershipLost() {
        for (LeadershipListener listener : listeners) {
            try {
                listener.onLeadershipLost();
            } catch (Exception e) {
                LOGGER.error("Error notifying listener of leadership lost", e);
            }
        }
    }
}
