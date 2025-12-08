package org.ikasan.job.orchestration.context.register;

import org.ikasan.job.orchestration.context.util.CustomWeekdayOfMonthHelper;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ContextInstanceSchedulerService;
import org.ikasan.spec.scheduler.DashboardJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;

public class ContextInstanceRegisterJob implements DashboardJob {

    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(ContextInstanceRegisterJob.class);
    private final String jobName;
    private final String cronExpression;
    private String timezone;
    private final ContextInstanceRegistrationService contextInstanceRegistrationService;
    private final ContextInstanceSchedulerService contextInstanceSchedulerService;
    private ContextTemplate contextTemplate;

    /**
     * Constructs a new ContextInstanceRegisterJob with the given parameters.
     *
     * @param jobName the name of the job, must not be null
     * @param cronExpression the cron expression for scheduling the job, must not be null
     * @param timezone the timezone to use for the job, if null, uses the system default timezone
     * @param contextInstanceRegistrationService the service for registering context instances, must not be null
     * @param contextInstanceSchedulerService the service for managing context instance schedulers, must not be null
     * @param contextTemplate the context template to be used for the job, must not be null
     */
    public ContextInstanceRegisterJob(String jobName, String cronExpression
        , String timezone, ContextInstanceRegistrationService contextInstanceRegistrationService
        , ContextInstanceSchedulerService contextInstanceSchedulerService, ContextTemplate contextTemplate) {
        this.jobName = jobName;
        if (this.jobName == null) {
            throw new IllegalArgumentException("jobName cannot be null!");
        }
        this.cronExpression = cronExpression;
        if (this.cronExpression == null) {
            throw new IllegalArgumentException("cronExpression cannot be null!");
        }
        this.timezone = timezone;
        if (this.timezone == null) {
            this.timezone = ZoneId.systemDefault().getId();
        }
        this.contextInstanceRegistrationService = contextInstanceRegistrationService;
        if (this.contextInstanceRegistrationService == null) {
            throw new IllegalArgumentException("contextInstanceRegistrationService cannot be null!");
        }
        this.contextInstanceSchedulerService = contextInstanceSchedulerService;
        if (this.contextInstanceSchedulerService == null) {
            throw new IllegalArgumentException("contextInstanceSchedulerService cannot be null!");
        }
        this.contextTemplate = contextTemplate;
        if (this.contextTemplate == null) {
            throw new IllegalArgumentException("contextTemplate cannot be null!");
        }
    }

    @Override
    public String getJobName() {
        return this.jobName;
    }

    @Override
    public String getCronExpression() {
        return this.cronExpression;
    }

    @Override
    public String getTimezone() {
        return timezone;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        logger.info("Executing jobExecutionContext start context " + jobName);
        try {
                contextInstanceRegistrationService.register(this.jobName, this.contextInstanceSchedulerService);

            // If the context template (job plan template) is using the custom version of the
            // week day of month cron syntax, we need to remove the regular context start job
            // ans reschedule it based on a newly derived cron for the nth business day of the
            // month next month.
            if(CustomWeekdayOfMonthHelper.isCustomWeekdayOfMonth(this.contextTemplate)) {
                logger.info(String.format("Job Plan[%s] to be rescheduled using custom cron expression[%s]"
                    , this.contextTemplate.getName(), this.contextTemplate.getTimeWindowStart()));
                this.contextInstanceSchedulerService.removeJob(this.jobName);
                this.contextInstanceSchedulerService.registerStartJobAndTrigger(this.contextTemplate, this.contextTemplate.getTimezone());
                logger.info(String.format("Successfully rescheduled Job Plan[%s] using custom cron expression[%s]!"
                    , this.contextTemplate.getName(), this.contextTemplate.getTimeWindowStart()));
            }
        } catch (Exception e) {
            // TODO hook in notification here
            logger.error(String.format("An error has occurred executing ContextInstanceRegisterJob[%s]", e.getMessage()), e);
            throw new JobExecutionException(e);
        }
    }
}
