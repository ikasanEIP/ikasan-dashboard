package org.ikasan.scheduler.context.register;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.scheduler.context.cache.ContextMachineCache;
import org.ikasan.scheduler.core.machine.ContextMachine;
import org.ikasan.scheduler.core.model.instance.ContextInstanceImpl;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
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
    private SchedulerService schedulerService;

    public ContextInstanceRegisterJob(String jobName, String cronExpression, ScheduledContextService scheduledContextService,
                                      ScheduledContextInstanceService scheduledContextInstanceService, SchedulerService schedulerService) {
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
        this.schedulerService = schedulerService;
        if(this.schedulerService == null) {
            throw new IllegalArgumentException("schedulerService cannot be null!");
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
            ContextInstanceImpl contextInstance = this.objectMapper
                .readValue(this.objectMapper.writeValueAsBytes(scheduledContextRecord.getContext()), ContextInstanceImpl.class);

            ContextMachine contextMachine = new ContextMachine(contextInstance, this.scheduledContextInstanceService);
            contextMachine.setSchedulerJobInitiationEventRaisedListener(event -> {
                this.schedulerService.raiseSchedulerJobInitiationEvent("", event);
            });

            ContextMachineCache.instance().put(contextMachine);
        }
        catch (Exception e) {
            logger.error(String.format("An error has occurred executing ContextInstanceRegisterJob[%s]", e.getMessage()), e);
            throw new JobExecutionException(e);
        }
    }
}
