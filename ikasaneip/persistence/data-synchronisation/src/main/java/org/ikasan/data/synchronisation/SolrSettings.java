package org.ikasan.data.synchronisation;

import org.springframework.core.env.Environment;

/**
 * The Solr connection and retention settings, read from the same {@code solr.*} properties (and with the same
 * defaults) as {@code SolrClientAutoConfiguration}, so a dashboard switching between Solr and Mongo persistence needs
 * no extra configuration for the synchronisation.
 */
public record SolrSettings(String mode,
                           String url,
                           String cloudZkHosts,
                           String username,
                           String password,
                           int retentionDays,
                           int schedulerInstanceRetentionDays,
                           int jobLockCacheAuditRetentionDays,
                           int metricsQueryLimit,
                           int connectionTimeoutMillis) {

    public static SolrSettings from(Environment environment) {
        return new SolrSettings(
            environment.getProperty("solr.mode", "standalone"),
            environment.getProperty("solr.url", ""),
            environment.getProperty("solr.cloud.zk.hosts", ""),
            environment.getProperty("solr.username", ""),
            environment.getProperty("solr.password", ""),
            environment.getProperty("solr.retention.days", Integer.class, 30),
            environment.getProperty("solr.scheduler.instance.retention.days", Integer.class, 90),
            environment.getProperty("solr.joblockcacheaudit.retention.days", Integer.class, 30),
            environment.getProperty("solr.metrics.query.limit", Integer.class, 200),
            environment.getProperty("solr.connection.timeout.milli", Integer.class, 15000));
    }

    public boolean isCloud() {
        return "cloud".equalsIgnoreCase(mode);
    }
}
