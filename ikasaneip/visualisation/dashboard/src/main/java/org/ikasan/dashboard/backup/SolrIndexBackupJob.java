package org.ikasan.dashboard.backup;

import org.ikasan.spec.scheduler.DashboardJob;
import org.ikasan.spec.solr.SolrGeneralService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;

public class SolrIndexBackupJob implements DashboardJob {

    private static Logger logger = LoggerFactory.getLogger(SolrIndexBackupJob.class);

    private SolrGeneralService solrGeneralService;
    private String backupLocationPath;
    private int numberOfBackupsToKeep;
    private String cronExpression;
    private String timezone = ZoneId.systemDefault().getId();


    /**
     * Constructs a SolrIndexBackupJob object with the provided parameters.
     *
     * @param solrGeneralService the SolrGeneralService instance to use for index backup
     * @param backupLocationPath the path where the backup will be stored
     * @param cronExpression the cron expression for scheduling the backup job
     * @param numberOfBackupsToKeep the number of backups to retain
     */
    public SolrIndexBackupJob(SolrGeneralService solrGeneralService, String backupLocationPath
        , String cronExpression, int numberOfBackupsToKeep) {
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
        this.numberOfBackupsToKeep = numberOfBackupsToKeep;
    }


    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            this.solrGeneralService.backupIndex(this.backupLocationPath, this.numberOfBackupsToKeep);
        }
        catch (Exception e) {
            logger.error("Error performing SOLR backup!", e);
            throw new JobExecutionException(e);
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
