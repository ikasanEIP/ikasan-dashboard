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

    /**
     * The base URL of this node (e.g. http://node1:9090). Registered as the LeaderLatch
     * participant ID so that follower nodes can resolve the leader's URL directly from ZooKeeper
     * rather than fan-out probing every peer. Must match one of the peer URLs configured on
     * other nodes' ikasan.dashboard.cluster.node-urls. Required when ZooKeeper clustering is enabled.
     */
    private String nodeUrl;

    /**
     * How many times a follower will re-query ZooKeeper for the current leader URL and retry
     * the REST call when the targeted leader returns no result (e.g. during a leadership
     * transition). Defaults to 3.
     */
    private int leaderLookupRetries = 3;

    /**
     * Milliseconds to wait between leader lookup retries. Defaults to 500ms.
     */
    private long leaderLookupRetryIntervalMs = 500;

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

    public String getNodeUrl() {
        return nodeUrl;
    }

    public void setNodeUrl(String nodeUrl) {
        this.nodeUrl = nodeUrl;
    }

    public int getLeaderLookupRetries() {
        return leaderLookupRetries;
    }

    public void setLeaderLookupRetries(int leaderLookupRetries) {
        this.leaderLookupRetries = leaderLookupRetries;
    }

    public long getLeaderLookupRetryIntervalMs() {
        return leaderLookupRetryIntervalMs;
    }

    public void setLeaderLookupRetryIntervalMs(long leaderLookupRetryIntervalMs) {
        this.leaderLookupRetryIntervalMs = leaderLookupRetryIntervalMs;
    }
}
