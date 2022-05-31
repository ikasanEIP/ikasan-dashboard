package org.ikasan.job.orchestration.context.recovery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ContextParametersUpdateService;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ContextInstanceRecoveryBackFiller implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ContextInstanceRecoveryBackFiller.class);

    private ObjectMapper objectMapper;

    private ScheduledContextRecord scheduledContextRecord;
    private String queueDirectory;
    private ScheduledContextInstanceService scheduledContextInstanceService;
    private InternalEventDrivenJobService internalEventDrivenJobRecordService;
    private SchedulerService schedulerService;
    private ModuleMetaDataService moduleMetadataService;
    private JobLockCacheService jobLockCacheService;
    private ContextParametersInstanceService contextParametersInstanceService;
    private JobLockCache jobLockCache;
    private ContextParametersUpdateService<ContextInstance> contextParametersUpdateService;

    public ContextInstanceRecoveryBackFiller(
        ScheduledContextRecord scheduledContextRecord,
        ScheduledContextInstanceService scheduledContextInstanceService,
        String queueDirectory,
        InternalEventDrivenJobService internalEventDrivenJobRecordService,
        SchedulerService schedulerService,
        ModuleMetaDataService moduleMetadataService,
        JobLockCacheService jobLockCacheService,
        ContextParametersInstanceService contextParametersInstanceService,
        ContextParametersUpdateService contextParametersUpdateService,
        JobLockCache jobLockCache) {

        this.scheduledContextRecord = scheduledContextRecord;
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        this.queueDirectory = queueDirectory;
        this.internalEventDrivenJobRecordService = internalEventDrivenJobRecordService;
        this.schedulerService = schedulerService;
        this.moduleMetadataService = moduleMetadataService;
        this.jobLockCacheService = jobLockCacheService;
        this.contextParametersUpdateService = contextParametersUpdateService;
        this.contextParametersInstanceService = contextParametersInstanceService;
        this.jobLockCache = jobLockCache;

        this.objectMapper = ObjectMapperFactory.newInstance();
    }

    @Override
    public void run() {
        LOG.info("Back filling instance for context " + scheduledContextRecord.getContextName());

        try {
            ContextInstanceImpl contextInstance = this.objectMapper
                .readValue(this.objectMapper.writeValueAsBytes(scheduledContextRecord.getContext()), ContextInstanceImpl.class);

            Map<String, InternalEventDrivenJob> internalJobs = getInternalJobs(scheduledContextRecord.getContextName());
            HashMap<String, ModuleMetaData> agents = getAgents(internalJobs);

            ContextMachine contextMachine = new ContextMachine(scheduledContextRecord.getContext(), contextInstance,
                this.scheduledContextInstanceService, internalJobs, this.queueDirectory, agents,
                this.jobLockCache, this.contextParametersInstanceService);

            contextMachine.setSchedulerJobInitiationEventRaisedListener(event -> {
                this.schedulerService.raiseSchedulerJobInitiationEvent(event.getAgentUrl(), event);
            });

            if (!agents.keySet().isEmpty()) {
                contextParametersInstanceService.populateContextParameters();
                List<ContextParameterInstance> allContextParameters = contextParametersInstanceService.getAllContextParameters(contextInstance.getName());
                contextInstance.setContextParameters(allContextParameters);
                for (String key : agents.keySet()) {
                    ModuleMetaData agent = agents.get(key);
                    contextParametersUpdateService.update(agent.getUrl(), contextInstance);
                }
            }

            ContextMachineCache.instance().put(contextMachine);
            // save it so we do not create it again if restarted
            saveContextInstance(contextInstance);
        } catch (Exception e) {
            LOG.error("Got error back filling context " + scheduledContextRecord.getContextName() + ". Error " + e);
        }
    }

    private HashMap<String, ModuleMetaData> getAgents(Map<String, InternalEventDrivenJob> internalJobs) {
        HashMap<String, ModuleMetaData> agents = new HashMap<>();
        internalJobs.values().forEach(job -> {
            if (!agents.containsKey(job.getAgentName())) {
                ModuleMetaData moduleMetadataById = moduleMetadataService.findById(job.getAgentName());
                if (moduleMetadataById == null) {
                    LOG.error("Could not find ModuleMetaData for agent name " + job.getAgentName());
                } else {
                    agents.put(job.getAgentName(), moduleMetadataById);
                }
            }
        });
        return agents;
    }

    private Map<String, InternalEventDrivenJob> getInternalJobs(String contextName) {
        SearchResults<InternalEventDrivenJobRecord> internalEventDrivenJobRecordSearchResults
            = this.internalEventDrivenJobRecordService.findByContext(contextName, -1, -1);

        Map<String, InternalEventDrivenJob> internalEventDrivenJobMap = internalEventDrivenJobRecordSearchResults.getResultList().stream()
            .map(internalEventDrivenJobRecord -> internalEventDrivenJobRecord.getInternalEventDrivenJob())
            .collect(Collectors.toMap(InternalEventDrivenJob::getIdentifier, Function.identity()));
        return internalEventDrivenJobMap;
    }

    private void saveContextInstance(ContextInstance contextInstance) {
        ScheduledContextInstanceRecord scheduledContextInstanceRecord
            = new ScheduledContextInstanceRecordImpl();
        scheduledContextInstanceRecord.setContextName(contextInstance.getName());
        scheduledContextInstanceRecord.setContextInstance(contextInstance);
        scheduledContextInstanceRecord.setTimestamp(contextInstance.getCreatedDateTime());
        scheduledContextInstanceRecord.setStatus(contextInstance.getStatus().name());

        scheduledContextInstanceService.save(scheduledContextInstanceRecord);
    }

    @Override
    public boolean equals(Object other) {
        return EqualsBuilder.reflectionEquals(this, other, List.of("objectMapper"));
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, "objectMapper");
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
