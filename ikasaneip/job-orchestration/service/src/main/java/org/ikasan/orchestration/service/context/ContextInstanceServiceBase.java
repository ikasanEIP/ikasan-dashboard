package org.ikasan.orchestration.service.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.util.CronUtils;
import org.ikasan.job.orchestration.context.util.CustomWeekdayOfMonthHelper;
import org.ikasan.job.orchestration.context.util.TimeService;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.event.ContextInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.job.orchestration.model.instance.SchedulerJobInstanceSearchFilterImpl;
import org.ikasan.job.orchestration.model.instance.SchedulerJobInstancesInitialisationParametersImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
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
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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
    protected final JobProvisionService jobProvisionService;
    protected final SchedulerJobService schedulerJobService;
    protected final ContextInstanceStateChangeEventBroadcaster contextInstanceStateChangeEventBroadcaster;
    protected final SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster;
    protected final TimeService timeService;
    protected final JobUtilsService jobUtilsService;
    protected final ObjectMapper objectMapper;
    protected int contextMachineExecutorWaitTimeoutSeconds = -1;
    protected int blackListedMessageMaxRetries = -1;
    protected long errorRetrySleepInterval = -1;
    protected boolean publishRaiseEventsAfterJobPlanInstanceFlush = false;



    /**
     * Constructor for initializing ContextInstanceServiceBase with required dependencies.
     *
     * @param queueDirectory                           The directory for queue
     * @param scheduledContextInstanceService          The service for scheduled context instances
     * @param jobInitiationService                     The service for job initiation
     * @param moduleMetadataService                    The service for module metadata
     * @param internalEventDrivenJobService            The service for internal event-driven jobs
     * @param contextParametersInstanceService         The service for context parameters instances
     * @param contextInstancePublicationService        The service for context instance publication
     * @param jobLockCacheService                      The service for job lock cache
     * @param scheduledContextService                  The service for scheduled context
     * @param schedulerJobInstanceService               The service for scheduler job instances
     * @param contextInstanceStateChangeEventBroadcaster The broadcaster for context instance state change events
     * @param schedulerJobStateChangeEventBroadcaster  The broadcaster for scheduler job state change events
     * @param jobLockCacheInitialisationService        The service for initializing job lock cache
     * @param timeService                              The service for time
     * @param jobUtilsService                          The service for job utilities
     * @param jobProvisionService                      The service for job provision
     * @param schedulerJobService                      The service for scheduler jobs
     */
    public ContextInstanceServiceBase(String queueDirectory,
                                      ScheduledContextInstanceService scheduledContextInstanceService,
                                      JobInitiationService jobInitiationService,
                                      ModuleMetaDataService moduleMetadataService,
                                      InternalEventDrivenJobService internalEventDrivenJobService,
                                      ContextParametersInstanceService contextParametersInstanceService,
                                      ContextInstancePublicationService contextInstancePublicationService,
                                      JobLockCacheService jobLockCacheService,
                                      ScheduledContextService scheduledContextService,
                                      SchedulerJobInstanceService schedulerJobInstanceService,
                                      ContextInstanceStateChangeEventBroadcaster contextInstanceStateChangeEventBroadcaster,
                                      SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster,
                                      JobLockCacheInitialisationService jobLockCacheInitialisationService,
                                      TimeService timeService,
                                      JobUtilsService jobUtilsService,
                                      JobProvisionService jobProvisionService,
                                      SchedulerJobService schedulerJobService) {
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
        this.contextInstancePublicationService = contextInstancePublicationService;
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
        this.timeService = timeService;
        if (this.timeService == null) {
            throw new IllegalArgumentException("timeService cannot be null!");
        }
        this.jobUtilsService = jobUtilsService;
        if (this.jobUtilsService == null) {
            throw new IllegalArgumentException("jobUtilsService cannot be null!");
        }
        this.jobProvisionService = jobProvisionService;
        if (this.jobProvisionService == null) {
            throw new IllegalArgumentException("jobProvisionService cannot be null!");
        }
        this.schedulerJobService = schedulerJobService;
        if (this.schedulerJobService == null) {
            throw new IllegalArgumentException("schedulerJobService cannot be null!");
        }

        this.objectMapper = ConcurrentObjectMapperFactory.newInstance();
    }

    /**
     * Sets the wait timeout duration in seconds for the context machine executor.
     *
     * @param contextMachineExecutorWaitTimeoutSeconds the timeout duration in seconds
     */
    public void setContextMachineExecutorWaitTimeoutSeconds(int contextMachineExecutorWaitTimeoutSeconds) {
        this.contextMachineExecutorWaitTimeoutSeconds = contextMachineExecutorWaitTimeoutSeconds;
    }

    /**
     * Set the maximum number of retries for blacklisted messages.
     *
     * @param blackListedMessageMaxRetries the maximum number of retries allowed for blacklisted messages
     */
    public void setBlackListedMessageMaxRetries(int blackListedMessageMaxRetries) {
        this.blackListedMessageMaxRetries = blackListedMessageMaxRetries;
    }

    /**
     * Set the context machine error retry sleep interval.
     *
     * @param errorRetrySleepInterval
     */
    public void setErrorRetrySleepInterval(long errorRetrySleepInterval) {
        this.errorRetrySleepInterval = errorRetrySleepInterval;
    }

    /**
     * Sets the value for whether to publish raise events after the job plan instance flush.
     *
     * @param publishRaiseEventsAfterJobPlanInstanceFlush a boolean indicating whether to enable or disable
     *                                                   publishing raise events after the job plan instance flush
     */
    public void setPublishRaiseEventsAfterJobPlanInstanceFlush(boolean publishRaiseEventsAfterJobPlanInstanceFlush) {
        this.publishRaiseEventsAfterJobPlanInstanceFlush = publishRaiseEventsAfterJobPlanInstanceFlush;
    }

    /**
     * Saves the provided context instance with the given instance status.
     *
     * @param contextInstance The context instance to be saved.
     * @param instanceStatus  The instance status to be set for the context instance.
     */
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
        scheduledContextInstanceRecord.setContainsRepeatingJobs(contextInstance.isContainsRepeatingJobs());

        scheduledContextInstanceService.save(scheduledContextInstanceRecord);

        this.contextInstanceStateChangeEventBroadcaster.broadcast(new ContextInstanceStateChangeEventImpl(contextInstance.getId(), contextInstance,
            previousStatus, contextInstance.getStatus()));
    }

    /**
     * Saves the provided ContextTemplate by updating the corresponding ScheduledContextRecord in the database.
     *
     * @param contextTemplate The ContextTemplate object to be saved
     */
    protected void saveContextTemplate(ContextTemplate contextTemplate) {
        ScheduledContextRecord contextRecord = this.scheduledContextService.findById(contextTemplate.getName());
        contextRecord.setContext(contextTemplate);
        contextRecord.setModifiedTimestamp(System.currentTimeMillis());
        contextRecord.setModifiedBy("system");
        this.scheduledContextService.save(contextRecord);
    }

    /**
     * Initializes the context machine for a given context and instance.
     *
     * @param context                    The context template.
     * @param instance                   The context instance.
     * @param initialiseJobs             Whether to initialise jobs or not.
     * @param isInitialContextInstantiation   Whether it is the initial context instantiation or not.
     * @param contextParameterInstances  The list of context parameter instances.
     * @throws Exception                 If an error occurs during initialization.
     */
    protected void initialiseContextMachine(ContextTemplate context, ContextInstance instance
        , boolean initialiseJobs, boolean isInitialContextInstantiation, List<ContextParameterInstance> contextParameterInstances) throws Exception {
        // We may need to synchronise the jobs on the agent. This is likely due to the fact that the agent has been
        // redeployed.
        if(context.isDelayAgentSynchronisationUntilNextInstance()
            && context.isRequiresAgentSynchronisation()
            && isInitialContextInstantiation) {
            this.provisionJobs(context);
            context.setRequiresAgentSynchronisation(false);
            this.saveContextTemplate(context);
        }

        if(initialiseJobs) {
            SchedulerJobInstancesInitialisationParameters parameters
                = new SchedulerJobInstancesInitialisationParametersImpl(false);
            schedulerJobInstanceService.initialiseSchedulerJobInstancesForContext(context, instance, parameters);
        }

        Map<String, InternalEventDrivenJobInstance> internalJobs = getAllCommandExecutionJobs(instance.getId());
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
            if(job.getValue().isJobRepeatable()) {
                instance.setContainsRepeatingJobs(true);
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
        Map<String, ContextStartJobInstance> contextStartJobInstanceMap = this.getContextStartJobInstances(instance.getId());
        Map<String, ContextTerminalJobInstance> contextTerminalJobInstanceMap = this.geContextTerminalJobInstances(instance.getId());
        Map<String, LocalEventJobInstance> localEventJobInstanceMap = this.getLocalEventJobs(instance.getId());
        Map<String, BridgingJobInstance> bridgingJobInstanceMap = this.getBridgingJobs(instance.getId());

        ContextMachine contextMachine = new ContextMachine(context, instance, scheduledContextInstanceService, globalEventJobMap,
            quartzScheduleDrivenJobInstanceMap, internalJobs, contextStartJobInstanceMap, contextTerminalJobInstanceMap,
            localEventJobInstanceMap, bridgingJobInstanceMap, queueDirectory, agents,
            moduleMetadataService, initialiseJobLockCache(context, isInitialContextInstantiation), contextParametersInstanceService,
            this.scheduledContextService, this.schedulerJobInstanceService, this.jobLockCacheInitialisationService,
            this.contextInstancePublicationService, this.jobUtilsService);
        contextMachine.setExecutorWaitTimeoutSeconds(this.contextMachineExecutorWaitTimeoutSeconds);
        contextMachine.setBlackListedMessageMaxRetries(this.blackListedMessageMaxRetries);
        contextMachine.setErrorRetrySleepInterval(this.errorRetrySleepInterval);
        contextMachine.setPublishRaiseEventsAfterJobPlanInstanceFlush(this.publishRaiseEventsAfterJobPlanInstanceFlush);
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
            long startMilliEpoch = CronUtils.getEpochMilliOfPreviousFireTime(instance.getTimeWindowStart(), instance.getTimezone());
            instance.setProjectedEndTime(startMilliEpoch+instance.getContextTtlMilliseconds());
            this.saveContextInstance(instance, InstanceStatus.WAITING);
        }

        ContextMachineCache.instance().put(contextMachine);
    }

    /**
     * Prepares the context instance by initializing jobs, setting job skips/holds, and creating a context machine.
     *
     * @param context          The context template.
     * @param instance         The context instance.
     * @param initialiseJobs   Whether to initialize jobs or not.
     * @throws Exception       If an error occurs during preparation.
     */
    protected void prepareContextInstance(ContextTemplate context, ContextInstance instance, boolean initialiseJobs) throws Exception {

        if(context.getContextTtlMilliseconds() == 0) {
            LOG.warn(String.format("Job Plan [%s] has a ttl of zero and a prepared instance will not be created!", context.getName()));
            return;
        }

        SchedulerJobInstancesInitialisationParameters parameters
            = new SchedulerJobInstancesInitialisationParametersImpl(false);

        if(initialiseJobs) {
            schedulerJobInstanceService.initialiseSchedulerJobInstancesForContext(context, instance, parameters);
        }


        Map<String, InternalEventDrivenJobInstance> internalJobs = getAllCommandExecutionJobs(instance.getId());
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
                if(job.getValue().isJobRepeatable()) {
                    instance.setContainsRepeatingJobs(true);
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
        Map<String, ContextStartJobInstance> contextStartJobInstanceMap = this.getContextStartJobInstances(instance.getId());
        Map<String, ContextTerminalJobInstance> contextTerminalJobInstanceMap = this.geContextTerminalJobInstances(instance.getId());
        Map<String, LocalEventJobInstance> localEventJobInstanceMap = this.getLocalEventJobs(instance.getId());
        Map<String, BridgingJobInstance> bridgingJobInstanceMap = this.getBridgingJobs(instance.getId());

        ContextMachine contextMachine = new ContextMachine(context, instance, scheduledContextInstanceService, globalEventJobMap, quartzScheduleDrivenJobInstanceMap,
            internalJobs, contextStartJobInstanceMap, contextTerminalJobInstanceMap, localEventJobInstanceMap, bridgingJobInstanceMap, queueDirectory, agents,
            moduleMetadataService, null, contextParametersInstanceService, this.scheduledContextService, this.schedulerJobInstanceService,
            this.jobLockCacheInitialisationService, this.contextInstancePublicationService, this.jobUtilsService);
        contextMachine.setExecutorWaitTimeoutSeconds(this.contextMachineExecutorWaitTimeoutSeconds);
        contextMachine.setBlackListedMessageMaxRetries(this.blackListedMessageMaxRetries);
        contextMachine.setErrorRetrySleepInterval(this.errorRetrySleepInterval);
        contextMachine.setPublishRaiseEventsAfterJobPlanInstanceFlush(this.publishRaiseEventsAfterJobPlanInstanceFlush);
        // We add a listener to update scheduler job instances when a state change occurs.
        contextMachine.addSchedulerJobStateChangeEventListener(event ->
            this.schedulerJobInstanceService.update(event.getSchedulerJobInstance()));

        // We add a listener to broadcast any job state changes to interested parties.
        contextMachine.addSchedulerJobStateChangeEventListener(event ->
            schedulerJobStateChangeEventBroadcaster.broadcast(event));

        contextMachine.getContext().setStatus(InstanceStatus.PREPARED);
        ContextMachineCache.instance().put(contextMachine);
    }

    /**
     * Prepares a future context instance for execution.
     *
     * @param contextName The name of the context for which to prepare the instance.
     */
    protected void prepareFutureContextInstance(String contextName) {
        ScheduledContextRecord scheduledContextRecord = this.scheduledContextService.findById(contextName);
        if (scheduledContextRecord == null) {
            final String message = String.format("Could not find scheduledContextRecord for context name [%s] when attempting to prepare instance!", contextName);
            LOG.error(message);
            throw new RuntimeException(message);
        }

        if(scheduledContextRecord.getContext().getContextTtlMilliseconds() == 0) {
            LOG.warn(String.format("Job Plan [%s] has a ttl of zero and a prepared instance will not be created!", contextName));
            return;
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
            preparedFutureContextInstance.setStartTime(CronUtils.getEpochMilliOfNextFireTimeAccountingForBlackoutWindow
                (CustomWeekdayOfMonthHelper.determineContextStartCron(context, this.timeService.getLocalDateNow())
                    , context.getBlackoutWindowCronExpressions(), context.getBlackoutWindowDateTimeRanges(), context.getTimezone()));
            preparedFutureContextInstance.setTimeWindowStart(CustomWeekdayOfMonthHelper.determineContextStartCron(context, this.timeService.getLocalDateNow()));

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

    /**
     * Provision scheduler jobs for the given context.
     *
     * @param contextTemplate The context template to provision jobs for.
     */
    protected synchronized void provisionJobs(ContextTemplate contextTemplate) {
        SearchResults<SchedulerJobRecord> jobRecords = this.schedulerJobService.findByContext(contextTemplate.getName(), -1, -1);

        List<String> jobIdentifiersInJobPlan = ContextHelper.getAllJobs(contextTemplate).stream()
            .map(job -> job.getIdentifier())
            .collect(Collectors.toList());

        List<SchedulerJob> schedulerJobs = jobRecords.getResultList().stream()
            .map(record -> record.getJob())
            .filter(job -> jobIdentifiersInJobPlan.contains(job.getIdentifier()))
            .collect(Collectors.toList());

        this.jobProvisionService.provisionJobs(schedulerJobs, "system");
    }

    /**
     * Removes all agent instances associated with a given context instance.
     * This method retrieves all agents associated with the context instance
     * using the getAgents method and removes them one by one using the remove
     * method of the contextInstancePublicationService.
     *
     * @param instance The context instance for which to remove agent instances.
     */
    protected void removeAgentInstances(ContextInstance instance) {
        HashMap<String, ModuleMetaData> agents = getAgents(instance);
        if (!agents.keySet().isEmpty()) {
            for (String key : agents.keySet()) {
                ModuleMetaData agent = agents.get(key);
                contextInstancePublicationService.remove(agent.getUrl(), instance);
            }
        }
    }


    /**
     * Initializes the job lock cache.
     *
     * @param context The context template to initialize the job lock cache.
     * @param isRefresh Whether to refresh the job lock cache or not.
     * @return The initialized JobLockCache.
     */
    private JobLockCache initialiseJobLockCache(ContextTemplate context, boolean isRefresh) {
        this.jobLockCacheInitialisationService.initialiseJobLockCache(context, isRefresh);
        return JobLockCacheImpl.instance();
    }

    /**
     * Retrieves a map of agents associated with the given context.
     *
     * @param context The context object.
     * @return A map of agents, where the key is the agent name and the value is the corresponding ModuleMetaData object.
     */
    private HashMap<String, ModuleMetaData> getAgents(Context context) {
        HashMap<String, ModuleMetaData> agents = new HashMap<>();

        List<String> contextAgents = ContextHelper.getAllAgents(context);

        LOG.info(String.format("Attempting to load agents for context[%s]. Loading following agents[%s]!"
            , context.getName(), String.join(", ", contextAgents)));

        ModuleMetadataSearchResults searchResults = moduleMetadataService
            .find(contextAgents, ModuleType.SCHEDULER_AGENT, -1, -1);

        searchResults.getResultList().forEach(agent -> agents.put(agent.getName(), agent));

        LOG.info("Retrieved the following agents from the datastore: [{}]", agents.keySet());

        return agents;
    }

    /**
     * Retrieves all agents as a HashMap with agent name as the key and ModuleMetaData as the value.
     *
     * @return A HashMap<String, ModuleMetaData> containing all agents.
     */
    private HashMap<String, ModuleMetaData> getAllAgents() {
        HashMap<String, ModuleMetaData> agents = new HashMap<>();

        ModuleMetadataSearchResults searchResults = moduleMetadataService
            .find(List.of(), ModuleType.SCHEDULER_AGENT, -1, -1);

        searchResults.getResultList().forEach(agent -> agents.put(agent.getName(), agent));

        return agents;
    }

    /**
     * Retrieves all command execution jobs associated with a given context instance ID.
     *
     * @param contextInstanceId The ID of the context instance.
     * @return A map of command execution jobs, where the key is the job ID and the value is the corresponding InternalEventDrivenJobInstance.
     */
    protected Map<String, InternalEventDrivenJobInstance> getAllCommandExecutionJobs(String contextInstanceId) {
        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId(contextInstanceId);
        filter.setJobType(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);

        return this.getFilteredCommandExecutionJobs(filter);
    }

    /**
     * Retrieves a filtered map of InternalEventDrivenJobInstance objects based on the provided filter.
     *
     * @param filter The SchedulerJobInstanceSearchFilter used to filter the job instances.
     * @return A map of InternalEventDrivenJobInstance objects, where the key is the concatenation of the identifier and childContextName,
     *         and the value is the corresponding InternalEventDrivenJobInstance object.
     */
    private Map<String, InternalEventDrivenJobInstance> getFilteredCommandExecutionJobs(SchedulerJobInstanceSearchFilter filter) {
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

    /**
     * Retrieves a map of QuartzScheduleDrivenJobInstance objects associated with the specified context instance ID.
     *
     * @param contextInstanceId The ID of the context instance.
     * @return A map of QuartzScheduleDrivenJobInstance objects, where the key is the identifier and the value is
     * the corresponding QuartzScheduleDrivenJobInstance object.
     */
    protected Map<String, QuartzScheduleDrivenJobInstance> getQuartzBasedJobs(String contextInstanceId) {
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

    /**
     * Retrieves a map of ContextStartJobInstance objects with the specified context instance ID.
     *
     * @param contextInstanceId The ID of the context instance.
     * @return A map of ContextStartJobInstance objects, where the key is the concatenation of the identifier and childContextName, and the value is the corresponding ContextStart
     *JobInstance object.
     */
    protected Map<String, ContextStartJobInstance> getContextStartJobInstances(String contextInstanceId) {
        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId(contextInstanceId);
        filter.setJobType(JobConstants.CONTEXT_START_JOB_INSTANCE);
        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults
            = this.schedulerJobInstanceService.getScheduledContextInstancesByFilter(filter, -1, -1, null, null);

        Map<String, ContextStartJobInstance> contextStartJobInstanceMap = globalEventJobRecordSearchResults.getResultList().stream()
            .map(jobInstanceRecord -> (ContextStartJobInstance) jobInstanceRecord.getSchedulerJobInstance())
            .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (a1, a2) -> a1));
        return contextStartJobInstanceMap;
    }

    /**
     * Retrieves a map of ContextTerminalJobInstance objects associated with the specified context instance ID.
     *
     * @param contextInstanceId The ID of the context instance.
     * @return A map of ContextTerminalJobInstance objects, where the key is the concatenation of the identifier and childContextName,
     *         and the value is the corresponding ContextTerminalJobInstance object.
     */
    protected Map<String, ContextTerminalJobInstance> geContextTerminalJobInstances(String contextInstanceId) {
        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId(contextInstanceId);
        filter.setJobType(JobConstants.CONTEXT_TERMINAL_JOB_INSTANCE);
        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults
            = this.schedulerJobInstanceService.getScheduledContextInstancesByFilter(filter, -1, -1, null, null);

        Map<String, ContextTerminalJobInstance> contextStartJobInstanceMap = globalEventJobRecordSearchResults.getResultList().stream()
            .map(jobInstanceRecord -> (ContextTerminalJobInstance) jobInstanceRecord.getSchedulerJobInstance())
            .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (a1, a2) -> a1));
        return contextStartJobInstanceMap;
    }

    protected Map<String, LocalEventJobInstance> getLocalEventJobs(String contextInstanceId) {
        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId(contextInstanceId);
        filter.setJobType(JobConstants.LOCAL_EVENT_JOB_INSTANCE);
        SearchResults<SchedulerJobInstanceRecord> globalEventJobRecordSearchResults
            = this.schedulerJobInstanceService.getScheduledContextInstancesByFilter(filter, -1, -1, null, null);

        Map<String, LocalEventJobInstance> localEventJobInstanceMap = globalEventJobRecordSearchResults.getResultList().stream()
            .map(globalEventJobRecord -> (LocalEventJobInstance) globalEventJobRecord.getSchedulerJobInstance())
            .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (a1, a2) -> a1));
        return localEventJobInstanceMap;
    }

    protected Map<String, BridgingJobInstance> getBridgingJobs(String contextInstanceId) {
        SchedulerJobInstanceSearchFilter filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId(contextInstanceId);
        filter.setJobType(JobConstants.BRIDGING_JOB_INSTANCE);
        SearchResults<SchedulerJobInstanceRecord> bridgingJobRecordSearchResults
            = this.schedulerJobInstanceService.getScheduledContextInstancesByFilter(filter, -1, -1, null, null);

        Map<String, BridgingJobInstance> bridgingJobInstanceMap = bridgingJobRecordSearchResults.getResultList().stream()
            .map(globalEventJobRecord -> (BridgingJobInstance) globalEventJobRecord.getSchedulerJobInstance())
            .collect(Collectors.toMap(key -> key.getIdentifier() + "-" + key.getChildContextName(), Function.identity(), (a1, a2) -> a1));
        return bridgingJobInstanceMap;
    }

    /**
     * Retrieves a map of global event jobs associated with a specific context instance.
     *
     * @param contextInstanceId The ID of the context instance.
     * @return A map of global event jobs, where the key is the concatenation of the
     * identifier and child context name, and the value is the corresponding GlobalEventJobInstance.
     */
    protected Map<String, GlobalEventJobInstance> getGlobalEventJobs(String contextInstanceId) {
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

    /**
     * Removes all context instances associated with agents from the system.
     * This method iterates over all agents, retrieves their URL using the getAllAgents method,
     * and then calls the removeAll method of the contextInstancePublicationService to remove
     * all context instances associated with the agent URL.
     * If there are no agents, the method does nothing.
     */
    protected void removeAllContextInstancesFromAgent() {
        HashMap<String, ModuleMetaData> agents = this.getAllAgents();
        if (!agents.keySet().isEmpty()) {
            for (String key : agents.keySet()) {
                ModuleMetaData agent = agents.get(key);
                contextInstancePublicationService.removeAll(agent.getUrl());
            }
        }
    }

    /**
     * Propagates a context instance to the specified agents.
     *
     * @param contextInstance The context instance to be propagated.
     * @param agents          The agents to which the context instance will be propagated.
     */
    private void propagateContextInstanceToAgents(ContextInstance contextInstance, HashMap<String, ModuleMetaData> agents) {
        if(agents.isEmpty()) {
            LOG.warn(String.format("Could not publish context instance[%s] with id[%s], however the agents are empty!"
                , contextInstance.getName(), contextInstance.getId()));
        }
        if (!agents.keySet().isEmpty()) {
            for (String key : agents.keySet()) {
                ModuleMetaData agent = agents.get(key);
                LOG.warn(String.format("Publishing context instance[%s] with id[%s] to agent[%s] with url[%s]!",
                    contextInstance.getName(), contextInstance.getId(), agent.getName(), agent.getUrl()));
                contextInstancePublicationService.publish(agent.getUrl(), contextInstance);
            }
        }
    }

    /**
     * Set the context parameters on the given context instance.
     *
     * @param contextInstance The context instance for which to set the context parameters.
     * @param internalJobs    A map of internal event driven job instances.
     */
    private void setContextParametersOnInstance(ContextInstance contextInstance, Map<String, InternalEventDrivenJobInstance> internalJobs) {
        contextParametersInstanceService.populateContextParameters();
        contextParametersInstanceService.populateContextParametersOnContextInstance(contextInstance, internalJobs);
    }

    /**
     * Finds prepared context instances with the specified context name.
     *
     * @param contextName The name of the context to search for prepared instances.
     * @return A list of prepared context instances.
     */
    protected List<ContextInstance> findPrepared(String contextName) {
        ContextInstanceSearchFilter filter = new SolrContextInstanceSearchFilterImpl();
        filter.setStatus(InstanceStatus.PREPARED.name());
        filter.setContextInstanceNames(Collections.singletonList(contextName));

        SearchResults<ScheduledContextInstanceRecord> results = this.scheduledContextInstanceService
            .getScheduledContextInstancesByFilter(filter, -1, -1, null, null);


        return results.getResultList().stream()
            .map(scheduledContextInstanceRecord -> scheduledContextInstanceRecord.getContextInstance())
            .collect(Collectors.toList());
    }

    /**
     * Removes all prepared context instances with the specified context name from the system.
     *
     * @param contextName The name of the context for which to remove prepared instances.
     */
    protected void removeAllPrepared(String contextName) {
        ContextInstanceSearchFilter filter = new SolrContextInstanceSearchFilterImpl();
        filter.setStatus(InstanceStatus.PREPARED.name());
        filter.setContextInstanceNames(Collections.singletonList(contextName));

        SearchResults<ScheduledContextInstanceRecord> results = this.scheduledContextInstanceService
            .getScheduledContextInstancesByFilter(filter, -1, -1, null, null);



        results.getResultList().forEach(scheduledContextInstanceRecord -> {
            this.schedulerJobInstanceService.deleteSchedulerJobInstances(scheduledContextInstanceRecord.getContextInstanceId());
            this.scheduledContextInstanceService.deleteById(scheduledContextInstanceRecord.getId());
        });
    }

    /**
     * Removes a context instance from the system.
     *
     * @param contextInstanceId The ID of the context instance to be removed.
     */
    protected void removeContextInstance(String contextInstanceId) {
        this.scheduledContextInstanceService.deleteById(contextInstanceId);
        this.schedulerJobInstanceService.deleteSchedulerJobInstances(contextInstanceId);
    }
}
