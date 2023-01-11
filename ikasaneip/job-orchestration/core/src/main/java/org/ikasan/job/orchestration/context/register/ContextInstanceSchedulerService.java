package org.ikasan.job.orchestration.context.register;

import org.ikasan.quartz.AbstractDashboardSchedulerService;
import org.ikasan.scheduler.ScheduledJobFactory;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.provision.ContextProvisionService;
import org.ikasan.spec.search.SearchResults;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

import static org.ikasan.job.orchestration.context.register.ContextInstanceEndJob.END_JOB_EXTENSION;

public class ContextInstanceSchedulerService extends AbstractDashboardSchedulerService {
    /**
     * Logger for this class
     */
    private static final Logger LOG = LoggerFactory.getLogger(ContextInstanceSchedulerService.class);
    private final ScheduledContextService scheduledContextService;
    private final ContextInstanceRegistrationService contextInstanceRegistrationService;
    private final boolean isContextLifeCycleActive;

    public ContextInstanceSchedulerService(Scheduler scheduler, ScheduledJobFactory scheduledJobFactory,
                                           ScheduledContextService scheduledContextService,
                                           ContextInstanceRegistrationService contextInstanceRegistrationService,
                                           boolean isContextLifeCycleActive) {

        super(scheduler, scheduledJobFactory);
        this.scheduledContextService = scheduledContextService;
        if (this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }

        this.contextInstanceRegistrationService = contextInstanceRegistrationService;
        if (this.contextInstanceRegistrationService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }

        this.isContextLifeCycleActive = isContextLifeCycleActive;
    }

    /**
     * Once the beans are created, interrogate the data source for the saved Schedule Details
     * Kick off a start trigger for each contextName
     */
    @PostConstruct
    public void registerJobs() {
        LOG.info("ContextInstanceSchedulerService Registering Jobs!");
        if (!isContextLifeCycleActive) {
            LOG.info("ContextInstanceSchedulerService not running as usePostConstructs is false");
            return;
        }

        try {
            SearchResults<ScheduledContextRecord> scheduledContextRecords
                = (SearchResults<ScheduledContextRecord>) this.scheduledContextService.findAll();

            for (ScheduledContextRecord scheduledContextRecord : scheduledContextRecords.getResultList()) {
                registerStartJobAndTrigger(scheduledContextRecord.getContextName(), scheduledContextRecord.getContext().getTimeWindowStart(),
                    scheduledContextRecord.getContext().getTimezone());
            }

        } catch (Exception ex) {
            // todo need to add some notifications here
            LOG.error(String.format("An exception has occurred registering contexts [%s]", ex.getMessage()), ex);
        }
    }

    /**
     * This is the job for the start of context.
     * Note that we don't register the destroy job yet, we wait till the job actually fires so we can pass
     * the correct contextID to the destroy job.
     * @param contextName to start
     * @param cronExpressionToTriggerJob to start at
     * @param timezone for the tme window
     */
    public void registerStartJobAndTrigger(String contextName, String cronExpressionToTriggerJob, String timezone) {
        final ContextInstanceRegisterJob job = new ContextInstanceRegisterJob(contextName, cronExpressionToTriggerJob, timezone,
            this.contextInstanceRegistrationService);
        final JobDetail jobDetail = this.scheduledJobFactory.createJobDetail(job, ContextInstanceRegisterJob.class, job.getJobName(), CONTEXT);
        super.dashboardJobDetailsMap.put(job.getJobName(), jobDetail);
        super.dashboardJobsMap.put(jobDetail.getKey().toString(), job);
        LOG.info(String.format("Registering context instance job [%s]", jobDetail.getKey().getName()));
        this.scheduleTrigger(jobDetail);
    }

    /**
     * This sets up the context instance destroy job and its trigger.
     * It will be typically called when the context instance is actually created / initialised
     * @param contextName for the starting context to which this will be paired
     * @param cronExpressionToTriggerJob for this instance
     * @param timezone for the cron expression
     * @param contextInstanceId used to pair the destroy context instance with the correct create context instance.
     */
    public void registerEndJobAndTrigger(String contextName, String cronExpressionToTriggerJob, String timezone, String contextInstanceId) {
        ContextInstanceEndJob endJob = new ContextInstanceEndJob(contextName + END_JOB_EXTENSION, cronExpressionToTriggerJob, timezone, this.contextInstanceRegistrationService);
        JobDetail endJobDetail = this.scheduledJobFactory.createJobDetail(endJob, ContextInstanceEndJob.class, endJob.getJobName(), CONTEXT);

        super.dashboardJobDetailsMap.put(endJob.getJobName(), endJobDetail);
        super.dashboardJobsMap.put(endJobDetail.getKey().toString(), endJob);
        LOG.info(String.format("Registering context instance job [%s] and instance [%s]", endJobDetail.getKey().getName(), contextInstanceId));
        this.scheduleEndTrigger(endJobDetail, contextInstanceId);
    }
}
