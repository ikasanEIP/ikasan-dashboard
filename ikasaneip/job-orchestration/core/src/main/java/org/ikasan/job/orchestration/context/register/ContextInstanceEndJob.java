package org.ikasan.job.orchestration.context.register;

import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduler.DashboardJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;

import static org.ikasan.quartz.AbstractDashboardSchedulerService.CONTEXT_INSTANCE_ID;

public class ContextInstanceEndJob implements DashboardJob {

    public static final String END_JOB_EXTENSION = "-EndJob";

    private static final Logger LOG = LoggerFactory.getLogger(ContextInstanceEndJob.class);

    private final String jobName;
    private final String cronExpressionEndTime;
    private String timezone;
    private final ContextInstanceRegistrationService contextInstanceRegistrationService;

    public ContextInstanceEndJob(String jobName, String cronExpressionEndTime, String timezone
        , ContextInstanceRegistrationService contextInstanceRegistrationService) {

        this.jobName = jobName;
        if (this.jobName == null) {
            throw new IllegalArgumentException("jobName cannot be null!");
        }
        if (!this.jobName.endsWith(END_JOB_EXTENSION)) {
            throw new IllegalArgumentException("jobName does not end correctly with -EndJob !");
        }
        this.cronExpressionEndTime = cronExpressionEndTime;
        if (this.cronExpressionEndTime == null) {
            throw new IllegalArgumentException("cronExpressionEndTime cannot be null!");
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
        return this.cronExpressionEndTime;
    }

    @Override
    public String getTimezone() {
        return timezone;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            final String contextInstanceId = (String)context.getTrigger().getJobDataMap().get(CONTEXT_INSTANCE_ID);
            LOG.info("Executing jobExecutionContext end context " + jobName + " context Instance ID [" + contextInstanceId + "]");
            contextInstanceRegistrationService.deRegisterById(contextInstanceId, context);
        } catch (Exception e) {
            // TODO hook in notification here
            LOG.error(String.format("An error has occurred executing ContextInstanceEndJob[%s]", e.getMessage()), e);
            throw new JobExecutionException(e);
        }
    }
}
