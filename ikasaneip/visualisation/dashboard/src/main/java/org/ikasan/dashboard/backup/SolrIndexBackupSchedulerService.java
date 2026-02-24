package org.ikasan.dashboard.backup;

import org.ikasan.quartz.AbstractDashboardSchedulerService;
import org.ikasan.scheduler.ScheduledJobFactory;
import org.ikasan.spec.solr.SolrGeneralService;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;

public class SolrIndexBackupSchedulerService extends AbstractDashboardSchedulerService {
    /** Logger for this class */
    private static Logger logger = LoggerFactory.getLogger(SolrIndexBackupSchedulerService.class);

    private SolrGeneralService solrGeneralService;
    private String backupLocationPath;
    private int numberOfBackupsToKeep;
    private String cronExpression;
    private int indexValidityRetries;
    private int indexValidityRetryInterval;
    private boolean terminateApplicationIfIndexCorrupted;
    private ApplicationContext applicationContext;


    /**
     * Constructor for SolrIndexBackupSchedulerService.
     *
     * @param scheduler                   the scheduler instance
     * @param scheduledJobFactory         the factory for creating scheduled jobs
     * @param solrGeneralService          the SolrGeneralService instance
     * @param backupLocationPath          the path where backups will be stored
     * @param cronExpression              the cron expression for scheduling backups
     * @param applicationContext           the application context
     * @param numberOfBackupsToKeep       the number of backups to keep
     * @param indexValidityRetries        the number of retries for checking index validity
     * @param indexValidityRetryInterval  the interval between index validity retries
     * @param terminateApplicationIfIndexCorrupted  boolean flag to specify whether application should terminate if index is corrupted
     */
    public SolrIndexBackupSchedulerService(Scheduler scheduler, ScheduledJobFactory scheduledJobFactory
        , SolrGeneralService solrGeneralService, String backupLocationPath, String cronExpression, ApplicationContext applicationContext
        , int numberOfBackupsToKeep, int indexValidityRetries, int indexValidityRetryInterval, boolean terminateApplicationIfIndexCorrupted)
    {
        super(scheduler, scheduledJobFactory);
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
        this.numberOfBackupsToKeep = numberOfBackupsToKeep;
        this.indexValidityRetries = indexValidityRetries;
        this.indexValidityRetryInterval = indexValidityRetryInterval;
        this.terminateApplicationIfIndexCorrupted = terminateApplicationIfIndexCorrupted;
    }

    @PostConstruct
    public void registerJobs() {
        SolrIndexBackupJob job = new SolrIndexBackupJob(this.solrGeneralService, this.backupLocationPath,
            this.cronExpression, this.applicationContext, this.numberOfBackupsToKeep, this.indexValidityRetries
            , this.indexValidityRetryInterval, this.terminateApplicationIfIndexCorrupted);
        JobDetail jobDetail = this.scheduledJobFactory.createJobDetail
            (job, SolrIndexBackupJob.class, job.getJobName(), "solr-backup");

        super.dashboardJobDetailsMap.put(job.getJobName(), jobDetail);
        super.dashboardJobsMap.put(jobDetail.getKey().toString(), job);
        this.addJob(jobDetail);
    }

}
