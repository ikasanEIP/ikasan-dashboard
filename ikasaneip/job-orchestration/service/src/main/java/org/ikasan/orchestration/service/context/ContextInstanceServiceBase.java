package org.ikasan.orchestration.service.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.scheduled.rest.agent.client.ContextInstancePublicationService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class ContextInstanceServiceBase {
    private static final Logger LOG = LoggerFactory.getLogger(ContextInstanceServiceBase.class);

    protected final String queueDirectory;
    protected final ScheduledContextInstanceService scheduledContextInstanceService;
    protected final SchedulerService schedulerService;
    protected final ModuleMetaDataService moduleMetadataService;
    protected final InternalEventDrivenJobService internalEventDrivenJobService;
    protected final ContextParametersInstanceService contextParametersInstanceService;
    protected final ContextInstancePublicationService<ContextInstance> contextParametersUpdateService;
    protected final JobLockCacheService jobLockCacheService;
    protected final ScheduledContextService scheduledContextService;
    protected final SchedulerJobInstanceService schedulerJobInstanceService;
    protected final ContextInstanceStateChangeEventBroadcaster contextInstanceStateChangeEventBroadcaster;
    protected final SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster;

    protected final ObjectMapper objectMapper;


    public ContextInstanceServiceBase(String queueDirectory,
                                      ScheduledContextInstanceService scheduledContextInstanceService,
                                      SchedulerService schedulerService,
                                      ModuleMetaDataService moduleMetadataService,
                                      InternalEventDrivenJobService internalEventDrivenJobService,
                                      ContextParametersInstanceService contextParametersInstanceService,
                                      ContextInstancePublicationService contextParametersUpdateService,
                                      JobLockCacheService jobLockCacheService,
                                      ScheduledContextService scheduledContextService,
                                      SchedulerJobInstanceService schedulerJobInstanceService,
                                      ContextInstanceStateChangeEventBroadcaster contextInstanceStateChangeEventBroadcaster,
                                      SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster) {
        this.queueDirectory = queueDirectory;
        if (this.queueDirectory == null) {
            throw new IllegalArgumentException("queueDirectory cannot be null!");
        }
        this.scheduledContextInstanceService = scheduledContextInstanceService;
        if (this.scheduledContextInstanceService == null) {
            throw new IllegalArgumentException("scheduledContextInstanceService cannot be null!");
        }
        this.schedulerService = schedulerService;
        if (this.schedulerService == null) {
            throw new IllegalArgumentException("schedulerService cannot be null!");
        }
        this.moduleMetadataService = moduleMetadataService;
        if (this.moduleMetadataService == null) {
            throw new IllegalArgumentException("moduleMetadataService cannot be null!");
        }
        this.internalEventDrivenJobService = internalEventDrivenJobService;
        if (this.internalEventDrivenJobService == null) {
            throw new IllegalArgumentException("internalEventDrivenJobService cannot be null!");
        }
        this.contextParametersInstanceService = contextParametersInstanceService;
        if (this.contextParametersInstanceService == null) {
            throw new IllegalArgumentException("contextParametersInstanceService cannot be null!");
        }
        this.contextParametersUpdateService = contextParametersUpdateService;
        if (this.contextParametersUpdateService == null) {
            throw new IllegalArgumentException("contextParametersUpdateService cannot be null!");
        }
        this.jobLockCacheService = jobLockCacheService;
        if (this.jobLockCacheService == null) {
            throw new IllegalArgumentException("jobLockCacheService cannot be null!");
        }
        this.scheduledContextService = scheduledContextService;
        if (this.scheduledContextService == null) {
            throw new IllegalArgumentException("scheduledContextService cannot be null!");
        }
        this.schedulerJobInstanceService = schedulerJobInstanceService;
        if (this.schedulerJobInstanceService == null) {
            throw new IllegalArgumentException("schedulerJobInstanceService cannot be null!");
        }
        this.contextInstanceStateChangeEventBroadcaster = contextInstanceStateChangeEventBroadcaster;
        if (this.contextInstanceStateChangeEventBroadcaster == null) {
            throw new IllegalArgumentException("contextInstanceStateChangeEventBroadcaster cannot be null!");
        }
        this.schedulerJobStateChangeEventBroadcaster = schedulerJobStateChangeEventBroadcaster;
        if (this.schedulerJobStateChangeEventBroadcaster == null) {
            throw new IllegalArgumentException("schedulerJobStateChangeEventBroadcaster cannot be null!");
        }

        this.objectMapper = ObjectMapperFactory.newInstance();
    }

    protected void saveContextInstance(ContextInstance contextInstance, InstanceStatus instanceStatus) {
        contextInstance.setStatus(instanceStatus);
        ScheduledContextInstanceRecord scheduledContextInstanceRecord = new ScheduledContextInstanceRecordImpl();
        scheduledContextInstanceRecord.setContextName(contextInstance.getName());
        scheduledContextInstanceRecord.setContextInstance(contextInstance);
        scheduledContextInstanceRecord.setTimestamp(contextInstance.getCreatedDateTime());
        scheduledContextInstanceRecord.setStatus(contextInstance.getStatus().name());

        scheduledContextInstanceService.save(scheduledContextInstanceRecord);
    }

    protected void initialiseContextMachine(ContextTemplate context, ContextInstance instance) throws Exception {
        schedulerJobInstanceService.initialiseSchedulerJobInstancesForContext(instance);

        Map<String, InternalEventDrivenJob> internalJobs = getInternalJobs(context.getName());
        HashMap<String, ModuleMetaData> agents = getAgents(internalJobs);

        ContextMachine contextMachine = new ContextMachine(context, instance, scheduledContextInstanceService, internalJobs, queueDirectory, agents,
            getJobLockCache(context), contextParametersInstanceService);
        contextMachine.init();

        // We add the listener to write initiation events to the agents.
        contextMachine.setSchedulerJobInitiationEventRaisedListener(event ->
            schedulerService.raiseSchedulerJobInitiationEvent(event.getAgentUrl(), event));

        // We add a listener to broadcast any context state changes to interested parties.
        contextMachine.addContextInstanceStateChangeEventListener(event ->
            contextInstanceStateChangeEventBroadcaster.broadcast(event));

        // We add a listener to broadcast any job state changes to interested parties.
        contextMachine.addSchedulerJobStateChangeEventListener(event ->
            schedulerJobStateChangeEventBroadcaster.broadcast(event));

        // We add a listener to update scheduler job instances when a state change occurs.
        contextMachine.addSchedulerJobStateChangeEventListener(event ->
            this.schedulerJobInstanceService.update(event.getSchedulerJobInstance()));

        populateParamsWithAgent(instance, agents);

        ContextMachineCache.instance().put(contextMachine);
    }

    private JobLockCache getJobLockCache(ContextTemplate context) {
        JobLockCache jobLockCache;
        JobLockCacheRecord jobLockCacheRecord = jobLockCacheService.get();
        if (jobLockCacheRecord == null) {
            // should never happen we are recovering so should exist but just in case
            jobLockCache = JobLockCacheImpl.instance();
            jobLockCache.setJobLockCacheService(jobLockCacheService);
            jobLockCache.addLocks(context.getAllNestedJobLocks());
        } else {
            // do not set the locks as should all be in there already
            jobLockCache = jobLockCacheRecord.getJobLockCache();
            jobLockCache.setJobLockCacheService(jobLockCacheService);
        }
        return jobLockCache;
    }

    private HashMap<String, ModuleMetaData> getAgents(Map<String, InternalEventDrivenJob> internalJobs) {
        HashMap<String, ModuleMetaData> agents = new HashMap<>();
        // TODO we only need to get the module metadata for distinct agents!
        internalJobs.values().forEach(job -> {
            if (!agents.containsKey(job.getAgentName())) {
                ModuleMetaData moduleMetadataById = moduleMetadataService.findById(job.getAgentName());
                if (moduleMetadataById == null) {
                    // TODO is this an exception case? Do we need to raise exceptions?
                    LOG.warn("Could not find ModuleMetaData for agent name " + job.getAgentName());
                } else {
                    agents.put(job.getAgentName(), moduleMetadataById);
                }
            }
        });
        return agents;
    }

    private Map<String, InternalEventDrivenJob> getInternalJobs(String contextName) {
        SearchResults<InternalEventDrivenJobRecord> internalEventDrivenJobRecordSearchResults
            = this.internalEventDrivenJobService.findByContext(contextName, -1, -1);

        Map<String, InternalEventDrivenJob> internalEventDrivenJobMap = internalEventDrivenJobRecordSearchResults.getResultList().stream()
            .map(internalEventDrivenJobRecord -> internalEventDrivenJobRecord.getInternalEventDrivenJob())
            .collect(Collectors.toMap(InternalEventDrivenJob::getIdentifier, Function.identity()));
        return internalEventDrivenJobMap;
    }

    private void populateParamsWithAgent(ContextInstance contextInstance, HashMap<String, ModuleMetaData> agents) {
        if (!agents.keySet().isEmpty()) {
            contextParametersInstanceService.populateContextParameters();
            List<ContextParameterInstance> allContextParameters = contextParametersInstanceService.getAllContextParameters(contextInstance.getName());
            contextInstance.setContextParameters(allContextParameters);
            for (String key : agents.keySet()) {
                ModuleMetaData agent = agents.get(key);
                contextParametersUpdateService.publish(agent.getUrl(), contextInstance);
            }
        }
    }
}
