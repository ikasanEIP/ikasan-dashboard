package org.ikasan.job.orchestration.context.register;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduler.DashboardJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextInstanceEndJob implements DashboardJob {

    public static final String END_JOB_EXTENSION = "-EndJob";

    private static final Logger LOG = LoggerFactory.getLogger(ContextInstanceEndJob.class);

    private final String jobName;
    private final String cronExpressionEndTime;
    private final ScheduledContextInstanceService scheduledContextInstanceService;

    public ContextInstanceEndJob(String jobName, String cronExpressionEndTime, ScheduledContextInstanceService scheduledContextInstanceService) {

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
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if (this.scheduledContextInstanceService == null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
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
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOG.info("Executing jobExecutionContext end context " + jobName);
        try {
            String contextName = jobName.substring(0, jobName.length() - END_JOB_EXTENSION.length());
            ContextMachine contextMachine = ContextMachineCache.instance().getByContextName(contextName);
            if (contextMachine == null) {
                LOG.error("Could not find context machine for " + contextName);
                return;
            }
            ContextInstance instance = contextMachine.getContext();
            if (instance == null) {
                LOG.error("Could not find instance in ContextMachine for " + contextName);
                return;
            }


            instance.setStatus(InstanceStatus.ENDED);
            ScheduledContextInstanceRecord scheduledContextInstanceRecord = new ScheduledContextInstanceRecordImpl();
            scheduledContextInstanceRecord.setContextName(instance.getName());
            scheduledContextInstanceRecord.setContextInstance(instance);
            scheduledContextInstanceRecord.setTimestamp(instance.getCreatedDateTime());
            scheduledContextInstanceRecord.setStatus(InstanceStatus.ENDED.name());

            scheduledContextInstanceService.save(scheduledContextInstanceRecord);

            // remove it from the context machine
            ContextMachineCache.instance().remove(contextMachine);

            // TODO get all instances in WAITING or RUNNING and see if they should be finished ?

        } catch (Exception e) {
            LOG.error(String.format("An error has occurred executing ContextInstanceEndJob[%s]", e.getMessage()), e);
            throw new JobExecutionException(e);
        }
    }
}
