package org.ikasan.dashboard.backup;

import org.ikasan.quartz.AbstractDashboardSchedulerService;
import org.ikasan.scheduler.ScheduledJobFactory;
import org.ikasan.spec.solr.SolrGeneralService;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

public class SolrIndexBackupSchedulerService extends AbstractDashboardSchedulerService {
    /** Logger for this class */
    private static Logger logger = LoggerFactory.getLogger(SolrIndexBackupSchedulerService.class);

    private SolrGeneralService solrGeneralService;
    private String backupLocationPath;
    private int numberOfBackupsToKeep;
    private String cronExpression;


    public SolrIndexBackupSchedulerService(Scheduler scheduler, ScheduledJobFactory scheduledJobFactory
        , SolrGeneralService solrGeneralService, String backupLocationPath, String cronExpression
        , int numberOfBackupsToKeep)
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
        this.numberOfBackupsToKeep = numberOfBackupsToKeep;
    }

    @PostConstruct
    public void registerJobs() {
        SolrIndexBackupJob job = new SolrIndexBackupJob(this.solrGeneralService, this.backupLocationPath,
            this.cronExpression, this.numberOfBackupsToKeep);
        JobDetail jobDetail = this.scheduledJobFactory.createJobDetail
            (job, SolrIndexBackupJob.class, job.getJobName(), "solr-backup");

        super.dashboardJobDetailsMap.put(job.getJobName(), jobDetail);
        super.dashboardJobsMap.put(jobDetail.getKey().toString(), job);
        this.addJob(jobDetail);
    }

}
