package org.ikasan.job.orchestration.context.register;

import static org.ikasan.job.orchestration.context.register.ContextInstanceEndJob.END_JOB_EXTENSION;

import java.util.Date;

import javax.annotation.PostConstruct;

import org.ikasan.quartz.AbstractDashboardSchedulerService;
import org.ikasan.scheduler.ScheduledJobFactory;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
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

    private final ScheduledContextService scheduledContextService;
    private final ContextInstanceRegistrationService contextInstanceRegistrationService;
    private final boolean usePostConstructs;

    public ContextInstanceSchedulerService(Scheduler scheduler, ScheduledJobFactory scheduledJobFactory,
                                           ScheduledContextService scheduledContextService,
                                           ContextInstanceRegistrationService contextInstanceRegistrationService,
                                           boolean usePostConstructs) {

        super(scheduler, scheduledJobFactory);
        this.scheduledContextService = scheduledContextService;
        if (this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }

        this.contextInstanceRegistrationService = contextInstanceRegistrationService;
        if (this.contextInstanceRegistrationService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
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
                ContextInstanceRegisterJob job = new ContextInstanceRegisterJob(scheduledContextRecord.getContextName(),
                    scheduledContextRecord.getContext().getTimeWindowStart(), this.contextInstanceRegistrationService);

                ContextInstanceEndJob endJob = new ContextInstanceEndJob(scheduledContextRecord.getContextName() + END_JOB_EXTENSION,
                    scheduledContextRecord.getContext().getTimeWindowEnd(), this.contextInstanceRegistrationService);

                JobDetail jobDetail = this.scheduledJobFactory.createJobDetail(job, ContextInstanceRegisterJob.class, job.getJobName(), "context");

                JobDetail endJobDetail = this.scheduledJobFactory.createJobDetail(endJob, ContextInstanceEndJob.class, endJob.getJobName(), "context");

                super.dashboardJobDetailsMap.put(job.getJobName(), jobDetail);
                super.dashboardJobDetailsMap.put(endJob.getJobName(), endJobDetail);

                super.dashboardJobsMap.put(jobDetail.getKey().toString(), job);
                super.dashboardJobsMap.put(endJobDetail.getKey().toString(), endJob);
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
