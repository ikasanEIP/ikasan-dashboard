package org.ikasan.dashboard.cluster.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 *  All the Cluster-related work here is temporary to support development and testing
 *  The real implementation of clustering and leader elected will be dealt with by a different piece of work by Mick
 * Fixed-role implementation of LeaderElectionService for development and testing.
 * Allows a node to be configured as leader or follower via a property without
 * requiring a running ZooKeeper cluster.
 * Activated by setting ikasan.dashboard.cluster.provider=fixed
 * Role is controlled by ikasan.dashboard.cluster.fixed-role (leader|follower, defaults to leader)
 */
@Service
@ConditionalOnProperty(name = "ikasan.dashboard.cluster.provider", havingValue = "fixed")
public class FixedRoleLeaderElectionService implements LeaderElectionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FixedRoleLeaderElectionService.class);

    private final boolean isLeader;

    @Value("${ikasan.dashboard.cluster.fixed.leader-url:http://127.0.0.1:9090}")
    private String leaderUrl;

    public FixedRoleLeaderElectionService(
            @Value("${ikasan.dashboard.cluster.fixed-role:leader}") String role) {
        this.isLeader = "leader".equalsIgnoreCase(role);
        LOGGER.info("FixedRoleLeaderElectionService initialised with role: {}", role);
    }

    @Override
    public void start() {
        LOGGER.info("FixedRoleLeaderElectionService started - this node is configured as: {}",
            isLeader ? "LEADER" : "FOLLOWER");
    }

    @Override
    public void stop() {}

    @Override
    public boolean isLeader() {
        return isLeader;
    }

    @Override
    public void awaitLeadership() throws InterruptedException {
        if (!isLeader) {
            throw new UnsupportedOperationException("Fixed-role follower will never become leader");
        }
    }

    @Override
    public String getLeaderUrl() {
        return leaderUrl;
    }

    @Override
    public void addLeadershipListener(LeadershipListener listener) {}

    @Override
    public void removeLeadershipListener(LeadershipListener listener) {}
}
