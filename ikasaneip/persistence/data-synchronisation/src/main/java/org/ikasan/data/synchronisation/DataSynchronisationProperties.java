package org.ikasan.data.synchronisation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration of the Mongo to Solr data synchronisation, bound from {@code ikasan.data-synchronisation.*}.
 *
 * <p>The Solr connection and retention are configured with the same {@code solr.*} properties as
 * {@code SolrClientAutoConfiguration}, see {@link SolrSettings}.
 */
@ConfigurationProperties(prefix = DataSynchronisationProperties.PREFIX)
public class DataSynchronisationProperties {

    public static final String PREFIX = "ikasan.data-synchronisation";

    /** Off by default; nothing is synchronised unless this is true. */
    private boolean enabled = false;

    /** How writes are detected. */
    private SyncMode mode = SyncMode.AUTO;

    /** Maximum number of changes written to Solr in one request. */
    private int batchSize = 200;

    /** Maximum time to wait for a batch to fill before writing what has arrived. */
    private long batchMaxWaitMs = 500;

    /** Capacity of the in-memory queue between the command listener and the Solr writer. */
    private int queueCapacity = 10000;

    /** Mongo {@code type} values never synchronised. */
    private List<String> excludedTypes = new ArrayList<>();

    /** Maximum back-off between retries when Solr cannot be written to. */
    private long retryMaxBackoffMs = 60000;

    private final Reconcile reconcile = new Reconcile();

    private final Lease lease = new Lease();

    public static class Reconcile {
        /** Load everything from Mongo into Solr the first time the synchronisation starts. */
        private boolean onFirstStart = true;

        /** Spring cron expression for a periodic reconcile; empty means the mode's default (off for change streams,
         * nightly for the command listener), "-" disables it. */
        private String cron = "";

        public boolean isOnFirstStart() {
            return onFirstStart;
        }

        public void setOnFirstStart(boolean onFirstStart) {
            this.onFirstStart = onFirstStart;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }

    public static class Lease {
        /** Only the instance holding the lease synchronises, so several dashboards can share one Mongo. */
        private boolean enabled = true;

        /** How long a lease lasts without being renewed. */
        private long durationSeconds = 30;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getDurationSeconds() {
            return durationSeconds;
        }

        public void setDurationSeconds(long durationSeconds) {
            this.durationSeconds = durationSeconds;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public SyncMode getMode() {
        return mode;
    }

    public void setMode(SyncMode mode) {
        this.mode = mode;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getBatchMaxWaitMs() {
        return batchMaxWaitMs;
    }

    public void setBatchMaxWaitMs(long batchMaxWaitMs) {
        this.batchMaxWaitMs = batchMaxWaitMs;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public List<String> getExcludedTypes() {
        return excludedTypes;
    }

    public void setExcludedTypes(List<String> excludedTypes) {
        this.excludedTypes = excludedTypes;
    }

    public long getRetryMaxBackoffMs() {
        return retryMaxBackoffMs;
    }

    public void setRetryMaxBackoffMs(long retryMaxBackoffMs) {
        this.retryMaxBackoffMs = retryMaxBackoffMs;
    }

    public Reconcile getReconcile() {
        return reconcile;
    }

    public Lease getLease() {
        return lease;
    }
}
