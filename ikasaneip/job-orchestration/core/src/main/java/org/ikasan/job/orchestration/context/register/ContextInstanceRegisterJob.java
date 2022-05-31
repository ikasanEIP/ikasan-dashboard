package org.ikasan.job.orchestration.context.register;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ContextParametersUpdateService;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.scheduler.DashboardJob;
import org.ikasan.spec.search.SearchResults;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    private JobLockCacheService jobLockCacheService;
    private ModuleMetaDataService moduleMetadataService;
    private ContextParametersInstanceService contextParametersInstanceService;
    private ContextParametersUpdateService<ContextInstance> contextParametersUpdateService;

    public ContextInstanceRegisterJob(String jobName, String cronExpression, ScheduledContextService scheduledContextService,
                                      ScheduledContextInstanceService scheduledContextInstanceService, SchedulerService schedulerService,
                                      InternalEventDrivenJobService internalEventDrivenJobService, String queueDirectory,
                                      JobLockCacheService jobLockCacheService,
                                      ContextParametersInstanceService contextParametersInstanceService,
                                      ModuleMetaDataService moduleMetadataService, ContextParametersUpdateService contextParametersUpdateService) {
        this.jobName = jobName;
        if (this.jobName == null) {
            throw new IllegalArgumentException("jobName cannot be null!");
        }
        this.cronExpression = cronExpression;
        if (this.cronExpression == null) {
            throw new IllegalArgumentException("cronExpression cannot be null!");
        }
        this.scheduledContextService = scheduledContextService;
        if (this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if (this.scheduledContextInstanceService == null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }
        this.schedulerService = schedulerService;
        if (this.schedulerService == null) {
            throw new IllegalArgumentException("schedulerService cannot be null!");
        }
        this.internalEventDrivenJobService = internalEventDrivenJobService;
        if (this.internalEventDrivenJobService == null) {
            throw new IllegalArgumentException("internalEventDrivenJobService cannot be null!");
        }
        this.queueDirectory = queueDirectory;
        if (this.queueDirectory == null) {
            throw new IllegalArgumentException("queueDirectory cannot be null!");
        }
        this.jobLockCacheService = jobLockCacheService;
        if (this.jobLockCacheService == null) {
            throw new IllegalArgumentException("jobLockCacheService cannot be null!");
        }
        this.contextParametersInstanceService = contextParametersInstanceService;
        if (this.contextParametersInstanceService == null) {
            throw new IllegalArgumentException("contextParametersInstanceService cannot be null!");
        }
        this.moduleMetadataService = moduleMetadataService;
        if (this.moduleMetadataService == null) {
            throw new IllegalArgumentException("moduleMetaDataService cannot be null!");
        }
        this.contextParametersUpdateService = contextParametersUpdateService;
        if (this.contextParametersUpdateService == null) {
            throw new IllegalArgumentException("contextParametersUpdateService cannot be null!");
        }

        this.objectMapper = ObjectMapperFactory.newInstance();
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
        logger.info("Executing jobExecutionContext start context " + jobName);
        try {
            ScheduledContextRecord scheduledContextRecord = this.scheduledContextService.findById(this.jobName);
            if (scheduledContextRecord == null) {
                logger.error("Could not find scheduledContextRecord for " + jobName);
                return;
            }
            ContextTemplate context = this.objectMapper
                .readValue(this.objectMapper.writeValueAsBytes(scheduledContextRecord.getContext()), ContextTemplateImpl.class);
            ContextInstanceImpl contextInstance = this.objectMapper
                .readValue(this.objectMapper.writeValueAsBytes(scheduledContextRecord.getContext()), ContextInstanceImpl.class);

            SearchResults<InternalEventDrivenJobRecord> internalEventDrivenJobRecordSearchResults
                = this.internalEventDrivenJobService.findByContext(scheduledContextRecord.getContextName(), -1, -1);

            Map<String, InternalEventDrivenJob> internalEventDrivenJobMap = internalEventDrivenJobRecordSearchResults.getResultList().stream()
                .map(internalEventDrivenJobRecord -> internalEventDrivenJobRecord.getInternalEventDrivenJob())
                .collect(Collectors.toMap(InternalEventDrivenJob::getIdentifier, Function.identity()));

            HashMap<String, ModuleMetaData> agents = getModuleMetaDataMap(internalEventDrivenJobMap);

            // if we are creating new context we add the all the locks
            JobLockCache jobLockCache = JobLockCacheImpl.instance();
            jobLockCache.setJobLockCacheService(jobLockCacheService);
            jobLockCache.addLocks(context.getAllNestedJobLocks());
            
            ContextMachine contextMachine = new ContextMachine(context, contextInstance, this.scheduledContextInstanceService, internalEventDrivenJobMap,
                this.queueDirectory, agents, jobLockCache, this.contextParametersInstanceService);

            contextMachine.setSchedulerJobInitiationEventRaisedListener(event -> {
                this.schedulerService.raiseSchedulerJobInitiationEvent(event.getAgentUrl(), event);
            });

            if (!agents.keySet().isEmpty()) {
                contextParametersInstanceService.populateContextParameters();
                List<ContextParameterInstance> allContextParameters = contextParametersInstanceService.getAllContextParameters(context.getName());
                contextInstance.setContextParameters(allContextParameters);
                for (String key : agents.keySet()) {
                    ModuleMetaData agent = agents.get(key);
                    contextParametersUpdateService.update(agent.getUrl(), contextInstance);
                }
            }

            ContextMachineCache.instance().put(contextMachine);
        } catch (Exception e) {
            logger.error(String.format("An error has occurred executing ContextInstanceRegisterJob[%s]", e.getMessage()), e);
            throw new JobExecutionException(e);
        }
    }

    private HashMap<String, ModuleMetaData> getModuleMetaDataMap(Map<String, InternalEventDrivenJob> internalEventDrivenJobMap) {
        HashMap<String, ModuleMetaData> agents = new HashMap<>();
        internalEventDrivenJobMap.values().forEach(job -> {
            if (!agents.containsKey(job.getAgentName())) {
                ModuleMetaData moduleMetadataById = moduleMetadataService.findById(job.getAgentName());
                if (moduleMetadataById == null) {
                    logger.error("Could not find ModuleMetaData for agent name " + job.getAgentName());
                } else {
                    agents.put(job.getAgentName(), moduleMetadataById);
                }
            }
        });
        return agents;
    }
}
