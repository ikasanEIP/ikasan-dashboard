package org.ikasan.job.orchestration.context.register;

import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
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

    public ContextInstanceRegisterJob(String jobName, String cronExpression
        , String timezone, ContextInstanceRegistrationService contextInstanceRegistrationService) {
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
            contextInstanceRegistrationService.register(jobName, null);
        } catch (Exception e) {
            // TODO hook in notification here
            logger.error(String.format("An error has occurred executing ContextInstanceRegisterJob[%s]", e.getMessage()), e);
            throw new JobExecutionException(e);
        }
    }
}
