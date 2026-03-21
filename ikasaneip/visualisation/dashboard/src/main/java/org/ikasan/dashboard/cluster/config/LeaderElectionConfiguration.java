package org.ikasan.dashboard.cluster.config;

import org.ikasan.dashboard.cluster.service.LeaderElectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Spring configuration for leader election in a clustered dashboard deployment.
 */
@Configuration
@EnableConfigurationProperties(ZooKeeperLeaderElectionProperties.class)
public class LeaderElectionConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeaderElectionConfiguration.class);

    private final LeaderElectionService leaderElectionService;

    public LeaderElectionConfiguration(LeaderElectionService leaderElectionService) {
        this.leaderElectionService = leaderElectionService;
    }

    @EventListener(ContextRefreshedEvent.class)
    @ConditionalOnProperty(
        name = "ikasan.dashboard.cluster.enabled",
        havingValue = "true"
    )
    public void onApplicationReady() {
        try {
            LOGGER.info("Starting leader election service on application ready");
            leaderElectionService.start();
        } catch (Exception e) {
            LOGGER.error("Failed to start leader election service", e);
            throw new RuntimeException("Failed to start leader election", e);
        }
    }
}
