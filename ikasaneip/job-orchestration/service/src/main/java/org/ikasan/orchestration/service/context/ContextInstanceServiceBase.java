package org.ikasan.orchestration.service.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.job.orchestration.model.instance.SchedulerJobInstanceSearchFilterImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
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
    protected final JobInitiationService jobInitiationService;
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
                                      JobInitiationService jobInitiationService,
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
        this.jobInitiationService = jobInitiationService;
        if (this.jobInitiationService == null) {
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

    protected void initialiseContextMachine(ContextTemplate context, ContextInstance instance, boolean isInitialContextInstantiation) throws Exception {
        if(isInitialContextInstantiation) {
            schedulerJobInstanceService.initialiseSchedulerJobInstancesForContext(instance);
        }

        Map<String, InternalEventDrivenJobInstance> internalJobs = getInternalJobs(instance.getId());
        HashMap<String, ModuleMetaData> agents = getAgents(internalJobs);

        ContextMachine contextMachine = new ContextMachine(context, instance, scheduledContextInstanceService, internalJobs, queueDirectory, agents,
            getJobLockCache(context), contextParametersInstanceService, this.scheduledContextService);
        contextMachine.init();

        // We add the listener to write initiation events to the agents.
        contextMachine.setSchedulerJobInitiationEventRaisedListener(event ->
            jobInitiationService.raiseSchedulerJobInitiationEvent(event.getAgentUrl(), event));

        // We add a listener to broadcast any context state changes to interested parties.
        contextMachine.addContextInstanceStateChangeEventListener(event ->
            contextInstanceStateChangeEventBroadcaster.broadcast(event));

        // We add a listener to broadcast any job state changes to interested parties.
        contextMachine.addSchedulerJobStateChangeEventListener(event ->
            schedulerJobStateChangeEventBroadcaster.broadcast(event));

        // We add a listener to update scheduler job instances when a state change occurs.
        contextMachine.addSchedulerJobStateChangeEventListener(event ->
            this.schedulerJobInstanceService.update(event.getSchedulerJobInstance()));

        // set the parameters on the instance every time
        setContextParametersOnInstance(instance);

        if (isInitialContextInstantiation) {
            populateParamsWithAgent(instance, agents);
            this.saveContextInstance(instance, InstanceStatus.WAITING);
        }

        ContextMachineCache.instance().put(contextMachine);
    }

    protected void removeAgentInstances(ContextInstance instance) {
        Map<String, InternalEventDrivenJobInstance> internalJobs = getInternalJobs(instance.getId());
        HashMap<String, ModuleMetaData> agents = getAgents(internalJobs);
        if (!agents.keySet().isEmpty()) {
            for (String key : agents.keySet()) {
                ModuleMetaData agent = agents.get(key);
                contextParametersUpdateService.remove(agent.getUrl(), instance);
            }
        }
    }

    private JobLockCache getJobLockCache(ContextTemplate context) {
        JobLockCache jobLockCache = JobLockCacheImpl.instance();
        JobLockCacheRecord jobLockCacheRecord = jobLockCacheService.get();
        /**
         * TODO we need to focus on exactly how the job lock cache is initialised / recovered.
         * What happens when we have resolve the persisted cache, but the context has been updated
         * to have more or less jobs in a lock, a lock has been removed, or a new lock added?
         */
        if (jobLockCacheRecord == null) {
            // should never happen we are recovering so should exist but just in case
            jobLockCache.setJobLockCacheService(jobLockCacheService);
            jobLockCache.addLocks(context.getAllNestedJobLocks());
        } else {
            // do not set the locks as should all be in there already
            jobLockCache.setJobLockCacheService(jobLockCacheService);
            jobLockCache.setJobLockCacheRecord(jobLockCacheRecord);
        }
        return jobLockCache;
    }

    private HashMap<String, ModuleMetaData> getAgents(Map<String, InternalEventDrivenJobInstance> internalJobs) {
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

    private Map<String, InternalEventDrivenJobInstance> getInternalJobs(String contextInstanceId) {
        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId(contextInstanceId);
        filter.setJobType("internalEventDrivenJobInstance");
        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults
            = this.schedulerJobInstanceService.getScheduledContextInstancesByFilter(filter, -1, -1, null, null);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobMap = internalEventDrivenJobRecordSearchResults.getResultList().stream()
            .map(internalEventDrivenJobRecord -> (InternalEventDrivenJobInstance)internalEventDrivenJobRecord.getSchedulerJobInstance())
            .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity()));
        return internalEventDrivenJobMap;
    }

    private void populateParamsWithAgent(ContextInstance contextInstance, HashMap<String, ModuleMetaData> agents) {
        if (!agents.keySet().isEmpty()) {
            for (String key : agents.keySet()) {
                ModuleMetaData agent = agents.get(key);
                contextParametersUpdateService.publish(agent.getUrl(), contextInstance);
            }
        }
    }

    private void setContextParametersOnInstance(ContextInstance contextInstance) {
        contextParametersInstanceService.populateContextParameters();
        List<ContextParameterInstance> allContextParameters = contextParametersInstanceService.getAllContextParameters(contextInstance.getName());
        contextInstance.setContextParameters(allContextParameters);
    }
}
