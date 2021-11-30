package org.ikasan.scheduler.context.register;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.scheduler.context.cache.ContextMachineCache;
import org.ikasan.scheduler.core.machine.ContextMachine;
import org.ikasan.scheduler.core.model.instance.ContextInstance;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduler.DashboardJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextInstanceRegisterJob implements DashboardJob {

    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(ContextInstanceRegisterJob.class);

    private String jobName;
    private String cronExpression;
    private ScheduledContextService scheduledContextService;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private ObjectMapper objectMapper;

    public ContextInstanceRegisterJob(String jobName, String cronExpression, ScheduledContextService scheduledContextService,
                                      ScheduledContextInstanceService scheduledContextInstanceService) {
        this.jobName = jobName;
        if(this.jobName == null) {
            throw new IllegalArgumentException("jobName cannot be null!");
        }
        this.cronExpression = cronExpression;
        if(this.jobName == null) {
            throw new IllegalArgumentException("cronExpression cannot be null!");
        }
        this.scheduledContextService = scheduledContextService;
        if(this.jobName == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if(this.scheduledContextInstanceService == null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }

        this.objectMapper = new ObjectMapper();
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
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        try {
            ScheduledContextRecord scheduledContextRecord = this.scheduledContextService.findById(this.jobName);
            ContextInstance contextInstance = this.objectMapper.readValue(scheduledContextRecord.getContext(), ContextInstance.class);
            ContextMachineCache.instance().put(this.jobName, new ContextMachine(contextInstance, this.scheduledContextInstanceService));
        }
        catch (Exception e) {
            logger.error(String.format("An error has occurred executing ContextInstanceRegisterJob[%s]", e.getMessage()), e);
            throw new JobExecutionException(e);
        }
    }
}
