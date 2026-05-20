package org.ikasan.dashboard.backup;

import org.ikasan.scheduler.CachingScheduledJobFactory;
import org.ikasan.scheduler.SchedulerFactory;
import org.ikasan.spec.solr.SolrGeneralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class SolrIndexBackupConfiguration {

    @Value("${solr.backup.location:}")
    private String backupLocationPath;
    @Value("${solr.backup.number.to.keep:2}")
    private int numberOfBackupsToKeep;
    @Value("${solr.backup.cron.expression:0 0/30 * * * ? *}")
    private String cronExpression;
    @Value("${solr.backup.number.index.validity.retries:5}")
    private int indexValidityRetries;
    @Value("${solr.backup.number.index.validity.retry.interval:2000}")
    private int indexValidityRetryInterval;
    @Value("${solr.backup.terminate.application.if.index.corrupted:true}")
    private boolean terminateApplicationIfIndexCorrupted;
    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    @ConditionalOnProperty(name = "solr.backup.enabled", havingValue = "true", matchIfMissing = true)
    public SolrIndexBackupSchedulerService solrIndexBackupSchedulerService(SolrGeneralService solrGeneralService) {
        return new SolrIndexBackupSchedulerService(SchedulerFactory.getInstance().getScheduler()
            , CachingScheduledJobFactory.getInstance(), solrGeneralService, this.backupLocationPath
            , this.cronExpression, this.applicationContext, this.numberOfBackupsToKeep, this.indexValidityRetries
            , this.indexValidityRetryInterval, this.terminateApplicationIfIndexCorrupted);
    }
}
