package org.ikasan.job.orchestration.context.register;

import static org.ikasan.job.orchestration.context.register.ContextInstanceEndJob.END_JOB_EXTENSION;
import static org.ikasan.job.orchestration.context.util.QuartzTimeWindowChecker.outsideOfOperatingWindow;

import java.util.Date;

import javax.annotation.PostConstruct;

import org.ikasan.quartz.AbstractDashboardSchedulerService;
import org.ikasan.scheduler.ScheduledJobFactory;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ContextParametersUpdateService;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.search.SearchResults;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextInstanceSchedulerService extends AbstractDashboardSchedulerService {
    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(ContextInstanceSchedulerService.class);

    /**
     * Scheduler
     */
    private ScheduledContextService scheduledContextService;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private SchedulerService schedulerService;
    private InternalEventDrivenJobService internalEventDrivenJobService;
    private String queueDirectory;
    private JobLockCacheService jobLockCacheService;
    private ModuleMetaDataService moduleMetadataService;
    private ContextParametersInstanceService contextParametersInstanceService;
    private ContextParametersUpdateService contextParametersUpdateService;
    private boolean usePostConstructs;

    public ContextInstanceSchedulerService(Scheduler scheduler, ScheduledJobFactory scheduledJobFactory
        , ScheduledContextService scheduledContextService, ScheduledContextInstanceService scheduledContextInstanceService
        , SchedulerService schedulerService, InternalEventDrivenJobService internalEventDrivenJobService, String queueDirectory
        , JobLockCacheService jobLockCacheService, ContextParametersInstanceService contextParametersInstanceService
        , ModuleMetaDataService moduleMetadataService, ContextParametersUpdateService contextParametersUpdateService
        , boolean usePostConstructs) {

        super(scheduler, scheduledJobFactory);

        this.scheduledContextService = scheduledContextService;
        if (this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if (this.scheduledContextInstanceService == null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }
        this.schedulerService = schedulerService;
        if (this.schedulerService == null) {
            throw new IllegalArgumentException("schedulerService cannot be null!");
        }
        this.internalEventDrivenJobService = internalEventDrivenJobService;
        if (this.internalEventDrivenJobService == null) {
            throw new IllegalArgumentException("internalEventDrivenJobService cannot be null!");
        }
        this.queueDirectory = queueDirectory;
        if (this.queueDirectory == null) {
            throw new IllegalArgumentException("queueDirectory cannot be null!");
        }
        this.jobLockCacheService = jobLockCacheService;
        if (this.jobLockCacheService == null) {
            throw new IllegalArgumentException("jobLockCacheService cannot be null!");
        }
        this.contextParametersInstanceService = contextParametersInstanceService;
        if (this.contextParametersInstanceService == null) {
            throw new IllegalArgumentException("contextParametersInstanceService cannot be null!");
        }
        this.moduleMetadataService = moduleMetadataService;
        if (this.moduleMetadataService == null) {
            throw new IllegalArgumentException("moduleMetaDataService cannot be null!");
        }
        this.contextParametersUpdateService = contextParametersUpdateService;
        if (this.contextParametersUpdateService == null) {
            throw new IllegalArgumentException("contextParametersUpdateService cannot be null!");
        }

        this.usePostConstructs = usePostConstructs;
    }

    @PostConstruct
    public void registerJobs() {
        // jobs get registered here in a post construct after ContextInstanceRecoveryManager
        logger.info("ContextInstanceSchedulerService Registering Jobs!");
        if (!usePostConstructs) {
            logger.info("ContextInstanceSchedulerService not running as usePostConstructs is false");
            return;
        }

        try {
            SearchResults<ScheduledContextRecord> scheduledContextRecords
                = (SearchResults<ScheduledContextRecord>) this.scheduledContextService.findAll();

            Date now = new Date();
            for (ScheduledContextRecord scheduledContextRecord : scheduledContextRecords.getResultList()) {
                if (outsideOfOperatingWindow(scheduledContextRecord.getContext().getTimeWindowStart(), scheduledContextRecord.getContext().getTimeWindowEnd(), now)) {

                    ContextInstanceRegisterJob job = new ContextInstanceRegisterJob(scheduledContextRecord.getContextName(),
                        scheduledContextRecord.getContext().getTimeWindowStart(), this.scheduledContextService
                        , this.scheduledContextInstanceService, this.schedulerService, this.internalEventDrivenJobService
                        , this.queueDirectory, this.jobLockCacheService, this.contextParametersInstanceService, this.moduleMetadataService
                        , this.contextParametersUpdateService);

                    ContextInstanceEndJob endJob = new ContextInstanceEndJob(scheduledContextRecord.getContextName() + END_JOB_EXTENSION,
                        scheduledContextRecord.getContext().getTimeWindowEnd(), scheduledContextInstanceService);

                    JobDetail jobDetail = this.scheduledJobFactory.createJobDetail(job, ContextInstanceRegisterJob.class, job.getJobName(), "context");

                    JobDetail endJobDetail = this.scheduledJobFactory.createJobDetail(endJob, ContextInstanceEndJob.class, endJob.getJobName(), "context");

                    super.dashboardJobDetailsMap.put(job.getJobName(), jobDetail);
                    super.dashboardJobDetailsMap.put(endJob.getJobName(), endJobDetail);
                    
                    super.dashboardJobsMap.put(jobDetail.getKey().toString(), job);
                    super.dashboardJobsMap.put(endJobDetail.getKey().toString(), endJob);
                }
            }

            for (JobDetail jobDetail : super.dashboardJobDetailsMap.values()) {
                logger.info(String.format("Registering context instance job[%s]", jobDetail.getKey().getName()));
                this.addJob(jobDetail.getKey().getName());
            }
        } catch (Exception ex) {
            // todo need to add some notifications here
            logger.error(String.format("An exception has occurred registering contexts [%s]", ex.getMessage()), ex);
        }
    }
}
