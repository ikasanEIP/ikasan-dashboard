package org.ikasan.orchestration.service.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import liquibase.pro.packaged.L;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerService;
import org.ikasan.job.orchestration.context.util.CronUtils;
import org.ikasan.job.orchestration.context.util.TimeService;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.event.ContextInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.job.orchestration.model.instance.SchedulerJobInstanceSearchFilterImpl;
import org.ikasan.job.orchestration.model.instance.SchedulerJobInstancesInitialisationParametersImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.orchestration.service.context.recovery.ContextInstanceRecoveryServiceImpl;
import org.ikasan.scheduled.instance.model.SolrContextInstanceSearchFilterImpl;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.module.ModuleType;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.*;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
public abstract class ContextInstanceServiceBase {
    private static final Logger LOG = LoggerFactory.getLogger(ContextInstanceRecoveryServiceImpl.class);

    protected final String queueDirectory;
    protected final ScheduledContextInstanceService scheduledContextInstanceService;
    protected final JobInitiationService jobInitiationService;
    protected final ModuleMetaDataService moduleMetadataService;
    protected final InternalEventDrivenJobService internalEventDrivenJobService;
    protected final ContextParametersInstanceService contextParametersInstanceService;
    protected final ContextInstancePublicationService<ContextInstance> contextInstancePublicationService;
    protected final JobLockCacheService jobLockCacheService;
    protected final ScheduledContextService scheduledContextService;
    protected final SchedulerJobInstanceService schedulerJobInstanceService;
    protected final JobLockCacheInitialisationService jobLockCacheInitialisationService;
    protected final ContextInstanceStateChangeEventBroadcaster contextInstanceStateChangeEventBroadcaster;
    protected final SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster;
    protected final ContextInstanceSchedulerService contextInstanceSchedulerService;
    protected final TimeService timeService;

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
                                      SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster,
                                      JobLockCacheInitialisationService jobLockCacheInitialisationService,
                                      ContextInstanceSchedulerService contextInstanceSchedulerService,
                                      TimeService timeService) {
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
        this.contextInstancePublicationService = contextParametersUpdateService;
        if (this.contextInstancePublicationService == null) {
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
        this.jobLockCacheInitialisationService = jobLockCacheInitialisationService;
        if (this.jobLockCacheInitialisationService == null) {
            throw new IllegalArgumentException("jobLockCacheInitialisationService cannot be null!");
        }
        this.contextInstanceSchedulerService = contextInstanceSchedulerService;
        if (this.contextInstanceSchedulerService == null) {
            throw new IllegalArgumentException("contextInstanceSchedulerService cannot be null!");
        }
        this.timeService = timeService;
        if (this.timeService == null) {
            throw new IllegalArgumentException("timeService cannot be null!");
        }
        this.objectMapper = ObjectMapperFactory.newInstance();
    }

    protected void saveContextInstance(ContextInstance contextInstance, InstanceStatus instanceStatus) {
        InstanceStatus previousStatus = contextInstance.getStatus();
        contextInstance.setStatus(instanceStatus);
        ScheduledContextInstanceRecord scheduledContextInstanceRecord = new ScheduledContextInstanceRecordImpl();
        scheduledContextInstanceRecord.setContextName(contextInstance.getName());
        scheduledContextInstanceRecord.setContextInstance(contextInstance);
        scheduledContextInstanceRecord.setTimestamp(contextInstance.getCreatedDateTime());
        scheduledContextInstanceRecord.setStartTime(contextInstance.getStartTime());
        scheduledContextInstanceRecord.setEndTime(contextInstance.getEndTime());
        scheduledContextInstanceRecord.setStatus(contextInstance.getStatus().name());

        scheduledContextInstanceService.save(scheduledContextInstanceRecord);

        this.contextInstanceStateChangeEventBroadcaster.broadcast(new ContextInstanceStateChangeEventImpl(contextInstance.getId(), contextInstance,
            previousStatus, contextInstance.getStatus()));
    }

    protected void initialiseContextMachine(ContextTemplate context, ContextInstance instance
        , boolean initialiseJobs, boolean isInitialContextInstantiation, List<ContextParameterInstance> contextParameterInstances) throws Exception {
        if(initialiseJobs) {
            SchedulerJobInstancesInitialisationParameters parameters
                = new SchedulerJobInstancesInitialisationParametersImpl(false);
            schedulerJobInstanceService.initialiseSchedulerJobInstancesForContext(instance, parameters);
        }

        Map<String, InternalEventDrivenJobInstance> internalJobs = getInternalJobs(instance.getId());
        HashMap<String, ModuleMetaData> agents = getAgents(context);

        internalJobs.entrySet().forEach(job -> {
            if(job.getValue().isSkip()) {
                // FIXME this may cause null pointer exception if the child context name is reused. So we need to decide if all context names need to be unique. This will not recover
                ContextInstance child = ContextHelper.getChildContextInstance(job.getValue().getChildContextName(), instance);
                if(child == null) {
                    LOG.warn("Could not load child context[{}] from context instance name[{}] context instance id[{}] when attempting to initialise the context machine. " +
                        "This is likely due to the child context name being duplicated in the context. The context instance will not have been recovered with skipped jobs set correctly.");
                }
                else if(!child.getScheduledJobsMap().containsKey(job.getValue().getIdentifier())){
                    LOG.warn("Could not set job to skip as job with identifier [{}] was not found in child context [{}].",
                        job.getValue().getIdentifier(), child.getName());
                }
                else {
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setSkip(job.getValue().isSkip());
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setStatus(job.getValue().getStatus());
                }
            }
            if(job.getValue().isHeld()) {
                // FIXME this may cause null pointer exception if the child context name is reused. So we need to decide if all context names need to be unique. This will not recover
                ContextInstance child = ContextHelper.getChildContextInstance(job.getValue().getChildContextName(), instance);
                if(child == null) {
                    LOG.warn("Could not load child context[{}] from context instance name[{}] context instance id[{}] when attempting to initialise the context machine. " +
                        "This is likely due to the child context name being duplicated in the context. The context instance will not have been recovered with held jobs set " +
                        "correctly");
                }
                else if(!child.getScheduledJobsMap().containsKey(job.getValue().getIdentifier())){
                    LOG.warn("Could not set job to hold as job with identifier [{}] was not found in child context [{}].",
                        job.getValue().getIdentifier(), child.getName());
                }
                else {
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setHeld(job.getValue().isHeld());
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setStatus(job.getValue().getStatus());
                }
            }
        });

        Map<String, GlobalEventJobInstance> globalEventJobMap = this.getGlobalEventJobs(instance.getId());

        globalEventJobMap.entrySet().forEach(job -> {
            if(job.getValue().getSkippedContexts() != null && job.getValue().getSkippedContexts().containsKey(instance.getName())) {
                ContextInstance child = ContextHelper.getChildContextInstance(job.getValue().getChildContextName(), instance);
                if(child == null) {
                    LOG.warn("Could not load child context[{}] from context instance name[{}] context instance id[{}] when attempting to initialise the context machine. " +
                        "This is likely due to the child context name being duplicated in the context. The context instance will not have been recovered with skipped jobs set correctly.");
                }
                else if(!child.getScheduledJobsMap().containsKey(job.getValue().getIdentifier())){
                    LOG.warn("Could not set job to skip as job with identifier [{}] was not found in child context [{}].",
                        job.getValue().getIdentifier(), child.getName());
                }
                else {
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setSkip(job.getValue().isSkip());
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setStatus(job.getValue().getStatus());
                }
            }
        });

        Map<String, QuartzScheduleDrivenJobInstance> quartzScheduleDrivenJobInstanceMap = this.getQuartzBasedJobs(instance.getId());

        ContextMachine contextMachine = new ContextMachine(context, instance, scheduledContextInstanceService, globalEventJobMap, quartzScheduleDrivenJobInstanceMap, internalJobs, queueDirectory, agents,
            initialiseJobLockCache(context, isInitialContextInstantiation), contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService,
            this.jobLockCacheInitialisationService, this.contextInstancePublicationService);
        contextMachine.init();

        // We add the listener to write initiation events to the agents.
        contextMachine.setSchedulerJobInitiationEventRaisedListener(event ->
            jobInitiationService.raiseSchedulerJobInitiationEvent(event.getAgentUrl(), event));

        // We add a listener to update scheduler job instances when a state change occurs.
        contextMachine.addSchedulerJobStateChangeEventListener(event ->
            this.schedulerJobInstanceService.update(event.getSchedulerJobInstance()));

        // We add a listener to broadcast any context state changes to interested parties.
        contextMachine.addContextInstanceStateChangeEventListener(event ->
            contextInstanceStateChangeEventBroadcaster.broadcast(event));

        // We add a listener to broadcast any job state changes to interested parties.
        contextMachine.addSchedulerJobStateChangeEventListener(event ->
            schedulerJobStateChangeEventBroadcaster.broadcast(event));

        // set the parameters on the instance every time
        if(contextParameterInstances == null) {
            setContextParametersOnInstance(instance, internalJobs);
        }
        else {
            instance.setContextParameters(contextParameterInstances);
        }

        propagateContextInstanceToAgents(instance, agents);

        if (isInitialContextInstantiation) {
            instance.setStartTime(System.currentTimeMillis());
            instance.setProjectedEndTime(CronUtils.getEpochMilliOfPreviousFireTime(instance.getTimeWindowStart())+instance.getContextTtlMilliseconds());
            this.saveContextInstance(instance, InstanceStatus.WAITING);
        }

        ContextMachineCache.instance().put(contextMachine);
    }

    protected void prepareContextInstance(ContextTemplate context, ContextInstance instance, boolean initialiseJobs) throws Exception {

        SchedulerJobInstancesInitialisationParameters parameters
            = new SchedulerJobInstancesInitialisationParametersImpl(false);

        if(initialiseJobs) {
            schedulerJobInstanceService.initialiseSchedulerJobInstancesForContext(instance, parameters);
        }


        Map<String, InternalEventDrivenJobInstance> internalJobs = getInternalJobs(instance.getId());
        HashMap<String, ModuleMetaData> agents = getAgents(context);

        internalJobs.entrySet().forEach(job -> {
            if(job.getValue().isSkip()) {
                // FIXME this may cause null pointer exception if the child context name is reused. So we need to decide if all context names need to be unique. This will not recover
                ContextInstance child = ContextHelper.getChildContextInstance(job.getValue().getChildContextName(), instance);
                if(child == null) {
                    LOG.warn("Could not load child context[{}] from context instance name[{}] context instance id[{}] when attempting to initialise the context machine. " +
                        "This is likely due to the child context name being duplicated in the context. The context instance will not have been recovered with skipped jobs set correctly.");
                }
                else if(!child.getScheduledJobsMap().containsKey(job.getValue().getIdentifier())){
                    LOG.warn("Could not set job to skip as job with identifier [{}] was not found in child context [{}].",
                        job.getValue().getIdentifier(), child.getName());
                }
                else {
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setSkip(job.getValue().isSkip());
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setStatus(job.getValue().getStatus());
                }
            }
            if(job.getValue().isHeld()) {
                // FIXME this may cause null pointer exception if the child context name is reused. So we need to decide if all context names need to be unique. This will not recover
                ContextInstance child = ContextHelper.getChildContextInstance(job.getValue().getChildContextName(), instance);
                if(child == null) {
                    LOG.warn("Could not load child context[{}] from context instance name[{}] context instance id[{}] when attempting to initialise the context machine. " +
                        "This is likely due to the child context name being duplicated in the context. The context instance will not have been recovered with held jobs set " +
                        "correctly");
                }
                else if(!child.getScheduledJobsMap().containsKey(job.getValue().getIdentifier())){
                    LOG.warn("Could not set job to hold as job with identifier [{}] was not found in child context [{}].",
                        job.getValue().getIdentifier(), child.getName());
                }
                else {
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setHeld(job.getValue().isHeld());
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setStatus(job.getValue().getStatus());
                }
            }
        });

        Map<String, GlobalEventJobInstance> globalEventJobMap = this.getGlobalEventJobs(instance.getId());

        globalEventJobMap.entrySet().forEach(job -> {
            if(job.getValue().getSkippedContexts() != null && job.getValue().getSkippedContexts().containsKey(instance.getName())) {
                ContextInstance child = ContextHelper.getChildContextInstance(job.getValue().getChildContextName(), instance);
                if(child == null) {
                    LOG.warn("Could not load child context[{}] from context instance name[{}] context instance id[{}] when attempting to initialise the context machine. " +
                        "This is likely due to the child context name being duplicated in the context. The context instance will not have been recovered with skipped jobs set correctly.");
                }
                else if(!child.getScheduledJobsMap().containsKey(job.getValue().getIdentifier())){
                    LOG.warn("Could not set job to skip as job with identifier [{}] was not found in child context [{}].",
                        job.getValue().getIdentifier(), child.getName());
                }
                else {
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setSkip(job.getValue().isSkip());
                    child.getScheduledJobsMap().get(job.getValue().getIdentifier()).setStatus(job.getValue().getStatus());
                }
            }
        });

        Map<String, QuartzScheduleDrivenJobInstance> quartzScheduleDrivenJobInstanceMap = this.getQuartzBasedJobs(instance.getId());

        ContextMachine contextMachine = new ContextMachine(context, instance, scheduledContextInstanceService, globalEventJobMap, quartzScheduleDrivenJobInstanceMap, internalJobs, queueDirectory, agents,
            null, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService,
            this.jobLockCacheInitialisationService, this.contextInstancePublicationService);

        // We add a listener to update scheduler job instances when a state change occurs.
        contextMachine.addSchedulerJobStateChangeEventListener(event ->
            this.schedulerJobInstanceService.update(event.getSchedulerJobInstance()));

        // We add a listener to broadcast any job state changes to interested parties.
        contextMachine.addSchedulerJobStateChangeEventListener(event ->
            schedulerJobStateChangeEventBroadcaster.broadcast(event));

        contextMachine.getContext().setStatus(InstanceStatus.PREPARED);
        ContextMachineCache.instance().put(contextMachine);
    }

    protected void prepareFutureContextInstance(String contextName) {
        ScheduledContextRecord scheduledContextRecord = this.scheduledContextService.findById(contextName);
        if (scheduledContextRecord == null) {
            final String message = String.format("Could not find scheduledContextRecord for context name [%s] when attempting to prepare instance!", contextName);
            LOG.error(message);
            throw new RuntimeException(message);
        }

        if (scheduledContextRecord.isDisabled()) {
            LOG.info(String.format("Context name [%s] is disabled and will not be prepared!", contextName));
            return;
        }

        try {
            byte[] scheduledContextRecordContext = objectMapper.writeValueAsBytes(scheduledContextRecord.getContext());
            ContextTemplate context = objectMapper.readValue(scheduledContextRecordContext, ContextTemplateImpl.class);
            List<ContextInstance> contextInstances = this.findPrepared(context.getName());

            ContextInstanceImpl preparedFutureContextInstance = objectMapper.readValue(scheduledContextRecordContext, ContextInstanceImpl.class);
            preparedFutureContextInstance.setStartTime(CronUtils.getEpochMilliOfNextFireTimeAccountingForBlackoutWindow(context.getTimeWindowStart()
                , context.getBlackoutWindowCronExpressions(), context.getBlackoutWindowDateTimeRanges(), context.getTimezone()));

            AtomicBoolean preparedInstanceExists = new AtomicBoolean(false);

            contextInstances.forEach(contextInstance -> {
                if(preparedFutureContextInstance.getStartTime() == contextInstance.getStartTime()) {
                    preparedInstanceExists.set(true);
                }
            });

            if(!preparedInstanceExists.get() && preparedFutureContextInstance.getStartTime() > 0) {
                this.prepareContextInstance(context, preparedFutureContextInstance, true);
                this.saveContextInstance(preparedFutureContextInstance, InstanceStatus.PREPARED);
            }

        } catch (Exception e) {
            LOG.error(String.format("An error has occurred executing registering job [%s]", e.getMessage()), e);
            throw new RuntimeException(e);
        }
    }

    protected void removeAgentInstances(ContextInstance instance) {
        HashMap<String, ModuleMetaData> agents = getAgents(instance);
        if (!agents.keySet().isEmpty()) {
            for (String key : agents.keySet()) {
                ModuleMetaData agent = agents.get(key);
                contextInstancePublicationService.remove(agent.getUrl(), instance);
            }
        }
    }


    private JobLockCache initialiseJobLockCache(ContextTemplate context, boolean isRefresh) {
        this.jobLockCacheInitialisationService.initialiseJobLockCache(context, isRefresh);
        return JobLockCacheImpl.instance();
    }

    private HashMap<String, ModuleMetaData> getAgents(Context context) {
        HashMap<String, ModuleMetaData> agents = new HashMap<>();

        List<String> contextAgents = ContextHelper.getAllAgents(context);

        ModuleMetadataSearchResults searchResults = moduleMetadataService
            .find(contextAgents, ModuleType.SCHEDULER_AGENT, -1, -1);

        searchResults.getResultList().forEach(agent -> agents.put(agent.getName(), agent));

        return agents;
    }

    private HashMap<String, ModuleMetaData> getAllAgents() {
        HashMap<String, ModuleMetaData> agents = new HashMap<>();

        ModuleMetadataSearchResults searchResults = moduleMetadataService
            .find(List.of(), ModuleType.SCHEDULER_AGENT, -1, -1);

        searchResults.getResultList().forEach(agent -> agents.put(agent.getName(), agent));

        return agents;
    }

    private Map<String, InternalEventDrivenJobInstance> getInternalJobs(String contextInstanceId) {
        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId(contextInstanceId);
        filter.setJobType(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults
            = this.schedulerJobInstanceService.getScheduledContextInstancesByFilter(filter, -1, -1, null, null);

        Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobMap = internalEventDrivenJobRecordSearchResults.getResultList().stream()
            .map(internalEventDrivenJobRecord -> (InternalEventDrivenJobInstance)internalEventDrivenJobRecord.getSchedulerJobInstance())
            .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity()));
        internalEventDrivenJobMap.entrySet().forEach(entry -> {
            if(entry.getValue().getStatus().equals(InstanceStatus.SKIPPED)) {
                entry.getValue().setSkip(true);
            }
        });
        return internalEventDrivenJobMap;
    }

    private Map<String, QuartzScheduleDrivenJobInstance> getQuartzBasedJobs(String contextInstanceId) {
        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId(contextInstanceId);
        filter.setJobType(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE);
        SearchResults<SchedulerJobInstanceRecord> internalEventDrivenJobRecordSearchResults
            = this.schedulerJobInstanceService.getScheduledContextInstancesByFilter(filter, -1, -1, null, null);

        Map<String, QuartzScheduleDrivenJobInstance> quartzScheduleDrivenJobInstanceMap = internalEventDrivenJobRecordSearchResults.getResultList().stream()
            .map(internalEventDrivenJobRecord -> (QuartzScheduleDrivenJobInstance)internalEventDrivenJobRecord.getSchedulerJobInstance())
            .collect(Collectors.toMap(key -> key.getIdentifier(), Function.identity(), (a1, a2) -> a1));

        return quartzScheduleDrivenJobInstanceMap;
    }

    private Map<String, GlobalEventJobInstance> getGlobalEventJobs(String contextInstanceId) {
        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId(contextInstanceId);
        filter.setJobType(JobConstants.GLOBAL_EVENT_JOB_INSTANCE);
        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults
            = this.schedulerJobInstanceService.getScheduledContextInstancesByFilter(filter, -1, -1, null, null);

        Map<String, GlobalEventJobInstance> globalEventJobMap = globalEventJobRecordSearchResults.getResultList().stream()
            .map(globalEventJobRecord -> (GlobalEventJobInstance) globalEventJobRecord.getSchedulerJobInstance())
            .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (a1, a2) -> a1));
        return globalEventJobMap;
    }

    protected void removeAllContextInstancesFromAgent() {
        HashMap<String, ModuleMetaData> agents = this.getAllAgents();
        if (!agents.keySet().isEmpty()) {
            for (String key : agents.keySet()) {
                ModuleMetaData agent = agents.get(key);
                contextInstancePublicationService.removeAll(agent.getUrl());
            }
        }
    }

    private void propagateContextInstanceToAgents(ContextInstance contextInstance, HashMap<String, ModuleMetaData> agents) {
        if (!agents.keySet().isEmpty()) {
            for (String key : agents.keySet()) {
                ModuleMetaData agent = agents.get(key);
                contextInstancePublicationService.publish(agent.getUrl(), contextInstance);
            }
        }
    }

    private void setContextParametersOnInstance(ContextInstance contextInstance, Map<String, InternalEventDrivenJobInstance> internalJobs) {
        contextParametersInstanceService.populateContextParameters();
        contextParametersInstanceService.populateContextParametersOnContextInstance(contextInstance, internalJobs);
    }

    protected List<ContextInstance> findPrepared(String contextName) {
        ContextInstanceSearchFilter filter = new SolrContextInstanceSearchFilterImpl();
        filter.setStatus(InstanceStatus.PREPARED.name());
        filter.setContextSearchFilter(contextName);

        SearchResults<ScheduledContextInstanceRecord> results = this.scheduledContextInstanceService
            .getScheduledContextInstancesByFilter(filter, -1, -1, null, null);


        return results.getResultList().stream()
            .map(scheduledContextInstanceRecord -> scheduledContextInstanceRecord.getContextInstance())
            .collect(Collectors.toList());
    }

    protected void removeAllPrepared(String contextName) {
        ContextInstanceSearchFilter filter = new SolrContextInstanceSearchFilterImpl();
        filter.setStatus(InstanceStatus.PREPARED.name());
        filter.setContextSearchFilter(contextName);

        SearchResults<ScheduledContextInstanceRecord> results = this.scheduledContextInstanceService
            .getScheduledContextInstancesByFilter(filter, -1, -1, null, null);



        results.getResultList().forEach(scheduledContextInstanceRecord -> {
            this.schedulerJobInstanceService.deleteSchedulerJobInstances(scheduledContextInstanceRecord.getContextInstanceId());
            this.scheduledContextInstanceService.deleteById(scheduledContextInstanceRecord.getId());
        });
    }

    protected void removeContextInstance(String contextInstanceId) {
        this.scheduledContextInstanceService.deleteById(contextInstanceId);
        this.schedulerJobInstanceService.deleteSchedulerJobInstances(contextInstanceId);
    }
}
