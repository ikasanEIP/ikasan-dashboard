package org.ikasan.job.orchestration.context.register;

import org.ikasan.job.orchestration.context.util.CustomWeekdayOfMonthHelper;
import org.ikasan.job.orchestration.context.util.TimeService;
import org.ikasan.quartz.AbstractDashboardSchedulerService;
import org.ikasan.scheduler.ScheduledJobFactory;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ContextInstanceSchedulerService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.search.SearchResults;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

import static org.ikasan.job.orchestration.context.register.ContextInstanceEndJob.END_JOB_EXTENSION;

/**
 * ContextInstanceSchedulerServiceImpl is a concrete implementation of ContextInstanceSchedulerService interface
 * and extends AbstractDashboardSchedulerService. It is responsible for managing and scheduling context instances.
 * It provides functionality to register jobs to start and destroy context instances based on provided configurations.
 * The class requires a Scheduler, ScheduledJobFactory, ScheduledContextService, ContextInstanceRegistrationService,
 * TimeService, isContextLifeCycleActive flag, and isIkasanEnterpriseSchedulerInstance flag for initialization.
 */
public class ContextInstanceSchedulerServiceImpl extends AbstractDashboardSchedulerService implements ContextInstanceSchedulerService {
    /**
     * Logger for this class
     */
    private static final Logger LOG = LoggerFactory.getLogger(ContextInstanceSchedulerServiceImpl.class);
    private final ScheduledContextService scheduledContextService;
    private final ContextInstanceRegistrationService contextInstanceRegistrationService;
    private final TimeService timeService;
    private final boolean isContextLifeCycleActive;
    private final boolean isIkasanEnterpriseSchedulerInstance;
    private int registrationJobAttempts;
    private int registrationJobRetryInterval;

    /**
     * Constructor for ContextInstanceSchedulerServiceImpl class.
     *
     * @param scheduler the Scheduler instance
     * @param scheduledJobFactory the ScheduledJobFactory instance
     * @param scheduledContextService the ScheduledContextService instance
     * @param contextInstanceRegistrationService the ContextInstanceRegistrationService instance
     * @param timeService the TimeService instance
     * @param isContextLifeCycleActive flag indicating if context lifecycle is active
     * @param isIkasanEnterpriseSchedulerInstance flag indicating if an enterprise scheduler instance is used
     * @param registrationJobAttempts number of retries for registration job
     * @param registrationJobRetryInterval interval between registration job retries
     */
    public ContextInstanceSchedulerServiceImpl(Scheduler scheduler, ScheduledJobFactory scheduledJobFactory,
                                               ScheduledContextService scheduledContextService,
                                               ContextInstanceRegistrationService contextInstanceRegistrationService,
                                               TimeService timeService,
                                               boolean isContextLifeCycleActive,
                                               boolean isIkasanEnterpriseSchedulerInstance,
                                               int registrationJobAttempts,
                                               int registrationJobRetryInterval) {

        super(scheduler, scheduledJobFactory);
        this.scheduledContextService = scheduledContextService;
        if (this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }

        this.contextInstanceRegistrationService = contextInstanceRegistrationService;
        if (this.contextInstanceRegistrationService == null) {
            throw new IllegalArgumentException("contextInstanceRegistrationService cannot be null!");
        }

        this.timeService = timeService;
        if (this.timeService == null) {
            throw new IllegalArgumentException("timeService cannot be null!");
        }

        this.isContextLifeCycleActive = isContextLifeCycleActive;
        this.isIkasanEnterpriseSchedulerInstance = isIkasanEnterpriseSchedulerInstance;
        this.registrationJobAttempts = registrationJobAttempts;
        this.registrationJobRetryInterval = registrationJobRetryInterval;
    }

    /**
     * Once the beans are created, interrogate the data source for the saved Schedule Details
     * Kick off a start trigger for each contextTemplate
     */
    @PostConstruct
    public void registerJobs() {
        LOG.info("ContextInstanceSchedulerServiceImpl Registering Jobs!");
        if (!isIkasanEnterpriseSchedulerInstance) {
            LOG.info("ContextInstanceSchedulerServiceImpl not running as dashboard is not configured as scheduler instance");
            return;
        }
        if (!isContextLifeCycleActive) {
            LOG.info("ContextInstanceSchedulerServiceImpl not running as usePostConstructs is false");
            return;
        }

        try {
            SearchResults<ScheduledContextRecord> scheduledContextRecords =
                (SearchResults<ScheduledContextRecord>) this.scheduledContextService.findAll();

            for (ScheduledContextRecord scheduledContextRecord : scheduledContextRecords.getResultList()) {
                registerStartJobAndTrigger( scheduledContextRecord.getContext(),
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
     * @param contextTemplate to start
     * @param timezone for the tme window
     */
    public void registerStartJobAndTrigger(ContextTemplate contextTemplate, String timezone) {
        final ContextInstanceRegisterJob job = new ContextInstanceRegisterJob(
            contextTemplate.getName(), CustomWeekdayOfMonthHelper.determineContextStartCron(contextTemplate, this.timeService.getLocalDateNow())
            , timezone, contextInstanceRegistrationService, this, contextTemplate, this.registrationJobAttempts
            , this.registrationJobRetryInterval);
        final JobDetail jobDetail = scheduledJobFactory.createJobDetail(job, ContextInstanceRegisterJob.class, job.getJobName(), CONTEXT_START_GROUP);

        // Overwrite if already in map
        super.dashboardJobDetailsMap.put(job.getJobName(), jobDetail);
        super.dashboardJobsMap.put(jobDetail.getKey().toString(), job);
        LOG.info(String.format("Registering context instance job [%s]", jobDetail.getKey().getName()));
        try {
            this.contextInstanceRegistrationService.prepareFutureContextInstance(contextTemplate.getName());
            this.addJob(jobDetail);
        } catch (RuntimeException e) {
            //TODO alert that we were not able to re-schedule the start up job
            LOG.warn("Unable to register the start trigger job for the context instance job [{}]", jobDetail.getKey().getName(), e);
        }
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
        ContextInstanceEndJob endJob = new ContextInstanceEndJob(contextName + END_JOB_EXTENSION, cronExpressionToTriggerJob, timezone
            , this.contextInstanceRegistrationService, this);
        JobDetail endJobDetail = this.scheduledJobFactory.createJobDetail(endJob, ContextInstanceEndJob.class, endJob.getJobName(), CONTEXT_END_GROUP);

        // Overwrite if already in map
        super.dashboardJobDetailsMap.put(endJob.getJobName(), endJobDetail);
        super.dashboardJobsMap.put(endJobDetail.getKey().toString(), endJob);
        LOG.info(String.format("Registering context instance job [%s] and instance [%s]", endJobDetail.getKey().getName(), contextInstanceId));
        try {
            this.scheduleEndTrigger(endJobDetail, contextInstanceId);
        } catch (RuntimeException e) {
            //TODO alert that we were not able to re-schedule the start up job
            LOG.warn("Unable to register the end trigger job for the context instance job [{}]", endJobDetail.getKey().getName(), e);
        }
    }
}
