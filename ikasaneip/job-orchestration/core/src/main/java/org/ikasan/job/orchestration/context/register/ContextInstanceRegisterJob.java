package org.ikasan.job.orchestration.context.register;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduler.DashboardJob;
import org.ikasan.spec.search.SearchResults;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private InternalEventDrivenJobService internalEventDrivenJobService;
    private String queueDirectory;

    public ContextInstanceRegisterJob(String jobName, String cronExpression, ScheduledContextService scheduledContextService,
                                      ScheduledContextInstanceService scheduledContextInstanceService, SchedulerService schedulerService,
                                      InternalEventDrivenJobService internalEventDrivenJobService, String queueDirectory) {
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
        this.internalEventDrivenJobService = internalEventDrivenJobService;
        if(this.internalEventDrivenJobService == null) {
            throw new IllegalArgumentException("internalEventDrivenJobService cannot be null!");
        }
        this.queueDirectory = queueDirectory;
        if(this.queueDirectory == null) {
            throw new IllegalArgumentException("queueDirectory cannot be null!");
        }

        this.objectMapper = ObjectMapperFactory.newInstance();;
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
            ContextTemplate context = this.objectMapper
                .readValue(this.objectMapper.writeValueAsBytes(scheduledContextRecord.getContext()), ContextTemplateImpl.class);
            ContextInstanceImpl contextInstance = this.objectMapper
                .readValue(this.objectMapper.writeValueAsBytes(scheduledContextRecord.getContext()), ContextInstanceImpl.class);

            SearchResults<InternalEventDrivenJobRecord> internalEventDrivenJobRecordSearchResults
                = this.internalEventDrivenJobService.findByContext(scheduledContextRecord.getContextName(), -1, -1);

            Map<String, InternalEventDrivenJob> internalEventDrivenJobMap = internalEventDrivenJobRecordSearchResults.getResultList().stream()
                .map(internalEventDrivenJobRecord -> internalEventDrivenJobRecord.getInternalEventDrivenJob())
                .collect(Collectors.toMap(InternalEventDrivenJob::getIdentifier, Function.identity()));

            // todo sort out agents
            ContextMachine contextMachine = new ContextMachine(context, contextInstance, this.scheduledContextInstanceService, internalEventDrivenJobMap,
                this.queueDirectory, new HashMap<>());
            contextMachine.setSchedulerJobInitiationEventRaisedListener(event -> {
                this.schedulerService.raiseSchedulerJobInitiationEvent(event.getAgentUrl(), event);
            });

            ContextMachineCache.instance().put(contextMachine);
        }
        catch (Exception e) {
            logger.error(String.format("An error has occurred executing ContextInstanceRegisterJob[%s]", e.getMessage()), e);
            throw new JobExecutionException(e);
        }
    }
}
