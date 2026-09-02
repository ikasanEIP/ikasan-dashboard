package org.ikasan.dashboard.backup;

import org.ikasan.spec.scheduler.DashboardJob;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.ikasan.spec.search.service.ESBSearchService;
import org.ikasan.spec.solr.SolrGeneralService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

import java.time.ZoneId;
import java.util.List;

public class SolrIndexBackupJob implements DashboardJob {

    private static Logger logger = LoggerFactory.getLogger(SolrIndexBackupJob.class);

    private SolrGeneralService<IkasanESBDocument, IkasanDocumentSearchResults> solrGeneralService;
    private String backupLocationPath;
    private int numberOfBackupsToKeep;
    private String cronExpression;
    private int indexValidityRetries;
    private int indexValidityRetryInterval;
    private boolean terminateApplicationIfIndexCorrupted;
    private ApplicationContext applicationContext;
    private String timezone = ZoneId.systemDefault().getId();


    /**
     * Initializes a SolrIndexBackupJob with the provided parameters.
     *
     * @param solrGeneralService the SolrGeneralService instance
     * @param backupLocationPath the path where backups will be stored
     * @param cronExpression the cron expression for scheduling backups
     * @param applicationContext the application context
     * @param numberOfBackupsToKeep the number of backups to keep
     * @param indexValidityRetries the number of retries for checking index validity
     * @param indexValidityRetryInterval the interval between index validity retries
     * @param terminateApplicationIfIndexCorrupted boolean flag to specify whether the application should terminate if the index is corrupted
     */
    public SolrIndexBackupJob(SolrGeneralService solrGeneralService, String backupLocationPath
        , String cronExpression, ApplicationContext applicationContext, int numberOfBackupsToKeep
        , int indexValidityRetries, int indexValidityRetryInterval, boolean terminateApplicationIfIndexCorrupted) {
        this.solrGeneralService = solrGeneralService;
        if(this.solrGeneralService == null) {
            throw new IllegalArgumentException("solrGeneralService cannot be null!");
        }
        this.backupLocationPath = backupLocationPath;
        if(this.backupLocationPath == null) {
            throw new IllegalArgumentException("backupLocationPath cannot be null!");
        }
        this.cronExpression = cronExpression;
        if(this.cronExpression == null) {
            throw new IllegalArgumentException("cronExpression cannot be null!");
        }
        this.applicationContext = applicationContext;
        if(this.applicationContext == null) {
            throw new IllegalArgumentException("applicationContext cannot be null!");
        }
        this.terminateApplicationIfIndexCorrupted = terminateApplicationIfIndexCorrupted;
        this.numberOfBackupsToKeep = numberOfBackupsToKeep;
        this.indexValidityRetries = indexValidityRetries;
        this.indexValidityRetryInterval = indexValidityRetryInterval;
    }


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            int retryCount = 0;

            while(retryCount < this.indexValidityRetries) {
                if (checkIndexValidity()) {
                    break;
                }
                else {
                    retryCount++;
                    logger.warn("Detected invalid Solr index. Retry[{}] of [{}].", retryCount, this.indexValidityRetries);
                    Thread.sleep(this.indexValidityRetryInterval);
                }
            }

            if(retryCount == this.indexValidityRetries) {
                throw new IndexValidityRetriesExceededException("Index validity check failed after "
                    + this.indexValidityRetries + " retries!");
            }

            this.solrGeneralService.backupIndex(this.backupLocationPath, this.numberOfBackupsToKeep);
        }
        catch (IndexValidityRetriesExceededException e) {
            if(this.terminateApplicationIfIndexCorrupted) {
                logger.error("Detected corrupted SOLR index! Terminating the Ikasan Dashboard as a precaution!", e);
                int exitCode = SpringApplication.exit(this.applicationContext, () -> 1);
                System.exit(exitCode);
            }
            else {
                logger.error("Detected corrupted SOLR index! Not terminating the Ikasan Dashboard as configuration property" +
                    " 'solr.backup.terminate.application.if.index.corrupted' is set to false!", e);
                throw new JobExecutionException(e);
            }
        }
        catch (Exception e) {
            logger.error("Error performing SOLR backup!", e);
            throw new JobExecutionException(e);
        }
    }

    /**
     * Check the validity of the index by performing a search operation in the Solr index.
     *
     * @return true if the total number of results in the search operation is greater than 0, false otherwise
     */
    private boolean checkIndexValidity() {
        try {
            IkasanDocumentSearchResults results = ((ESBSearchService<IkasanESBDocument, IkasanDocumentSearchResults>)this.solrGeneralService)
                .search("*", 0, System.currentTimeMillis(), 0, 1
                , List.of(), false, null, null);

            return results.getTotalNumberOfResults() > 0;
        }
        catch (Exception e) {
            logger.error("Error checking SOLR index validity!", e);
            return false;
        }
    }

    @Override
    public String getJobName() {
        return "SolrIndexBackupJob";
    }

    @Override
    public String getCronExpression() {
        return this.cronExpression;
    }

    @Override
    public String getTimezone() {
        return this.timezone;
    }
}
