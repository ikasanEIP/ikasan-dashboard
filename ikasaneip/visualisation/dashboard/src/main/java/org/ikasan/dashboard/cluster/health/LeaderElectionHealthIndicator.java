package org.ikasan.dashboard.cluster.health;

import org.ikasan.dashboard.cluster.service.LeaderElectionService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for the leader election service.
 * Provides visibility into the leadership status of this dashboard instance.
 */
@Component
public class LeaderElectionHealthIndicator implements HealthIndicator {

    private final LeaderElectionService leaderElectionService;

    public LeaderElectionHealthIndicator(LeaderElectionService leaderElectionService) {
        this.leaderElectionService = leaderElectionService;
    }

    @Override
    public Health health() {
        boolean isLeader = leaderElectionService.isLeader();

        return Health.up()
            .withDetail("leader", isLeader)
            .withDetail("status", isLeader ? "LEADER" : "FOLLOWER")
            .build();
    }
}
