package org.ikasan.dashboard.cluster.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for ZooKeeper leader election in a clustered dashboard deployment.
 */
@ConfigurationProperties(prefix = "ikasan.dashboard.cluster.zookeeper")
public class ZooKeeperLeaderElectionProperties {

    private boolean enabled = false;
    private String connectionString;
    private int sessionTimeoutMs = 30000;
    private int connectionTimeoutMs = 15000;
    private int baseSleepTimeMs = 1000;
    private int maxRetries = 3;
    private String namespace = "ikasan";
    private String leaderPath = "/leader";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public int getSessionTimeoutMs() {
        return sessionTimeoutMs;
    }

    public void setSessionTimeoutMs(int sessionTimeoutMs) {
        this.sessionTimeoutMs = sessionTimeoutMs;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public int getBaseSleepTimeMs() {
        return baseSleepTimeMs;
    }

    public void setBaseSleepTimeMs(int baseSleepTimeMs) {
        this.baseSleepTimeMs = baseSleepTimeMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getLeaderPath() {
        return leaderPath;
    }

    public void setLeaderPath(String leaderPath) {
        this.leaderPath = leaderPath;
    }
}
