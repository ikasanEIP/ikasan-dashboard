package org.ikasan.job.orchestration.core.machine;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.util.JobThreadFactory;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.model.JobDependency;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.event.model.*;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class JobLogicMachine extends AbstractLogicMachine<SchedulerJobInstance> {
    private Logger logger = LoggerFactory.getLogger(JobLogicMachine.class);

    private List<SchedulerJobInstanceStateChangeEventListener> schedulerJobInstanceStateChangeEventListeners;
    private ExecutorService executor;
    private Map<String, ModuleMetaData> agents;
    private ModuleMetaDataService moduleMetaDataService;
    private JobLockCache jobLockCache;
    private ContextParametersInstanceService contextParametersInstanceService;

    /**
     * Constructor for JobLogicMachine class.
     *
     * @param agents A map containing agent names as keys and ModuleMetaData objects as values.
     * @param moduleMetaDataService Service for handling module metadata.
     * @param jobLockCache Cache for managing job locks.
     * @param contextParametersInstanceService Service for managing context parameters instances.
     */
    public JobLogicMachine(Map<String, ModuleMetaData> agents, ModuleMetaDataService moduleMetaDataService, JobLockCache jobLockCache, ContextParametersInstanceService contextParametersInstanceService) {
        this.agents = agents;
        this.moduleMetaDataService = moduleMetaDataService;
        this.schedulerJobInstanceStateChangeEventListeners = new ArrayList<>();
        // todo make pool size configurable
        this.executor = Executors.newFixedThreadPool(5, new JobThreadFactory("JobLogicMachine"));
        this.jobLockCache = jobLockCache;
        this.contextParametersInstanceService = contextParametersInstanceService;
    }


    /**
     * Retrieves a list of job initiation events based on the provided contextual parameters and job instance mappings.
     *
     * @param scheduledProcessEvent The scheduled process event associated with the operation.
     * @param contextInstance The context instance for which the job initiation events are being retrieved.
     * @param dryRunParameters Parameters governing the behavior of the dry-run operation.
     * @param globalEventJobInstanceMap A mapping of global event job instance names to their corresponding instances.
     * @param internalEventDrivenJobs A mapping of internal event-driven job instance names to their corresponding instances.
     * @param contextStartJobInstanceMap A mapping of context start job instance names to their corresponding instances.
     * @param contextTerminalJobInstanceMap A mapping of context terminal job instance names to their corresponding instances.
     * @param localEventJobInstanceMap A mapping of local event job instance names to their corresponding instances.
     * @param bridgingJobInstanceMap A mapping of bridging job instance names to their corresponding instances.
     * @param contextParameters A list of contextual parameter instances used to influence job initiation.
     * @param parentContextInstance The parent context instance, if applicable, from which context is inherited.
     * @param lockRaised A mutable boolean indicating whether a lock has been raised as part of the operation.
     * @param markAsRaised A flag indicating whether to mark the relevant job events as raised.
     * @return A list of {@code SchedulerJobInitiationEvent} objects representing the job initiation events derived from the input parameters.
     */
    protected List<SchedulerJobInitiationEvent> getJobInitiationEvents(ContextualisedScheduledProcessEvent scheduledProcessEvent
        , ContextInstance contextInstance, DryRunParameters dryRunParameters
        , Map<String, GlobalEventJobInstance> globalEventJobInstanceMap
        , Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs
        , Map<String, ContextStartJobInstance> contextStartJobInstanceMap
        , Map<String, ContextTerminalJobInstance> contextTerminalJobInstanceMap
        , Map<String, LocalEventJobInstance> localEventJobInstanceMap
        , Map<String, BridgingJobInstance> bridgingJobInstanceMap
        , List<ContextParameterInstance> contextParameters
        , ContextInstance parentContextInstance
        , MutableBoolean lockRaised
        , boolean markAsRaised) {
        return this.getJobInitiationEvents(scheduledProcessEvent, contextInstance, dryRunParameters, globalEventJobInstanceMap,
            internalEventDrivenJobs, contextStartJobInstanceMap, contextTerminalJobInstanceMap, localEventJobInstanceMap, bridgingJobInstanceMap,
            contextParameters, parentContextInstance, lockRaised, markAsRaised, true);
    }


    /**
     * Retrieves a list of job initiation events based on the provided scheduled process event and context data.
     *
     * @param scheduledProcessEvent The event representing the scheduled process that may trigger job initiation events.
     * @param contextInstance The current context instance under which the jobs are being processed.
     * @param dryRunParameters Parameters indicating if the operation is being performed as a dry run.
     * @param globalEventJobInstanceMap A map of global event job instances keyed by their identifiers.
     * @param internalEventDrivenJobs A map of internal event-driven job instances keyed by their identifiers.
     * @param contextStartJobInstanceMap A map of context start job instances keyed by their identifiers.
     * @param contextTerminalJobInstanceMap A map of context terminal job instances keyed by their identifiers.
     * @param localEventJobInstanceMap A map of local event job instances keyed by their identifiers.
     * @param bridgingJobInstanceMap A map of bridging job instances keyed by their identifiers.
     * @param contextParameters A list of context parameter instances relevant to the current context.
     * @param parentContextInstance The parent context instance of the current context.
     * @param lockRaised A mutable boolean indicating whether a lock was raised during the job initiation process.
     * @param markAsRaised A flag indicating whether the jobs should be marked as raised during processing.
     * @param updateState A flag indicating whether the state of the context or jobs should be updated.
     * @return A list of {@code SchedulerJobInitiationEvent} objects representing the job initiation events that were computed based on the input parameters.
     */
    protected List<SchedulerJobInitiationEvent> getJobInitiationEvents(ContextualisedScheduledProcessEvent scheduledProcessEvent
        , ContextInstance contextInstance, DryRunParameters dryRunParameters
        , Map<String, GlobalEventJobInstance> globalEventJobInstanceMap
        , Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs
        , Map<String, ContextStartJobInstance> contextStartJobInstanceMap
        , Map<String, ContextTerminalJobInstance> contextTerminalJobInstanceMap
        , Map<String, LocalEventJobInstance> localEventJobInstanceMap
        , Map<String, BridgingJobInstance> bridgingJobInstanceMap
        , List<ContextParameterInstance> contextParameters
        , ContextInstance parentContextInstance
        , MutableBoolean lockRaised
        , boolean markAsRaised
        , boolean updateState) {
        SchedulerJobInstance schedulerJobInstance = contextInstance.getScheduledJobsMap()
            .get(scheduledProcessEvent.getAgentName() + "-" + scheduledProcessEvent.getJobName());

        if(schedulerJobInstance == null) {
            logger.warn("Scheduler job instance is null! Attempted lookup using job identifier[{}]"
                , scheduledProcessEvent.getAgentName() + "-" + scheduledProcessEvent.getJobName());
        }

        if(scheduledProcessEvent.getChildContextNames() != null && scheduledProcessEvent.getChildContextNames().contains(contextInstance.getName())) {
            StringBuffer childContextNames = new StringBuffer("[");
            scheduledProcessEvent.getChildContextNames().forEach(id -> childContextNames.append("{").append(id).append("}"));
            childContextNames.append("]");

            logger.info("Processing Schedule Process Event [{}], for Context Instance [{}] with identifier [{}], with Child Ids {}", scheduledProcessEvent.getJobName()
                , contextInstance.getName(), parentContextInstance.getId(), childContextNames);
        }

        InstanceStatus transientStatus = null;

        // Firstly the status of the job is set on the instance.
        if(schedulerJobInstance != null &&
            (scheduledProcessEvent.getChildContextNames() == null || scheduledProcessEvent.getChildContextNames().isEmpty()
            || scheduledProcessEvent.getChildContextNames().contains(contextInstance.getName()))) {
            // we update the job result with the event if it is relevant in this context. A null or empty
            // collection of child contexts means the event is valid for all contexts.
            InstanceStatus currentJobState = schedulerJobInstance.getStatus();

            schedulerJobInstance.setScheduledProcessEvent(scheduledProcessEvent);
            schedulerJobInstance.setChildContextName(contextInstance.getName());
            schedulerJobInstance.setContextInstanceId(parentContextInstance.getId());

            // We need to set a transient status to be able to get events that can be raised,
            // then reset the job back to its original state.
            if(!updateState) {
                transientStatus = schedulerJobInstance.getStatus();
            }

            if (scheduledProcessEvent.isRaisedDueToFailureResubmission()) {
                if (scheduledProcessEvent.getInternalEventDrivenJob().getChildContextName() != null
                    && scheduledProcessEvent.getInternalEventDrivenJob().getChildContextName().equals(contextInstance.getName())) {
                    if (!schedulerJobInstance.getStatus().equals(InstanceStatus.ERROR)) {
                        throw new ContextMachineException(String.format("Job[%s], Context[%s], Child Context[%s] was in a State[%s] when attempting" +
                                " to raise events dues to a failure resubmission. The job must be in ERROR to resubmit due to failure.", schedulerJobInstance.getIdentifier(),
                            schedulerJobInstance.getContextName(), schedulerJobInstance.getChildContextName(), schedulerJobInstance.getStatus().name()));
                    }
                    // we temporarily set the job to complete so the downstream logic will be assessed
                    schedulerJobInstance.setStatus(InstanceStatus.COMPLETE);
                }
            } else {
                if (scheduledProcessEvent.isJobStarting()) {
                    if(!schedulerJobInstance.getStatus().equals(InstanceStatus.COMPLETE) &&
                        !schedulerJobInstance.getStatus().equals(InstanceStatus.SKIPPED_COMPLETE)) {
                        if (schedulerJobInstance.isSkip()) {
                            schedulerJobInstance.setStatus(InstanceStatus.SKIPPED_RUNNING);
                        } else {
                            schedulerJobInstance.setStatus(InstanceStatus.RUNNING);
                        }
                    }
                } else if (scheduledProcessEvent.isSuccessful()) {
                    if (schedulerJobInstance.isSkip() || (scheduledProcessEvent.getOutcome() != null
                        && scheduledProcessEvent.getOutcome().equals(Outcome.EXECUTION_INVOKED_IGNORED_DAY_OF_WEEK.name()))) {
                        schedulerJobInstance.setStatus(InstanceStatus.SKIPPED_COMPLETE);
                    } else {
                        schedulerJobInstance.setStatus(InstanceStatus.COMPLETE);
                    }
                } else {
                    schedulerJobInstance.setStatus(InstanceStatus.ERROR);
                }

                this.issueSchedulerJobStateChangeEvent(new SchedulerJobInstanceStateChangeEventImpl(schedulerJobInstance, parentContextInstance
                    , currentJobState, schedulerJobInstance.getStatus()));
            }
        }

        List<SchedulerJobInitiationEvent> schedulerJobInitiationEvents = new ArrayList<>();

        getScheduledJobInitiationEventsThatCanBeRaised(scheduledProcessEvent, contextInstance, dryRunParameters
            , globalEventJobInstanceMap, internalEventDrivenJobs, contextStartJobInstanceMap, contextTerminalJobInstanceMap
            , localEventJobInstanceMap, bridgingJobInstanceMap, contextParameters, parentContextInstance
            , schedulerJobInitiationEvents, markAsRaised);

        if(markAsRaised) {
            schedulerJobInitiationEvents = this.manageJobLocks(scheduledProcessEvent, contextInstance
                , parentContextInstance, schedulerJobInitiationEvents, lockRaised);
        }

        if(schedulerJobInstance != null && scheduledProcessEvent.isRaisedDueToFailureResubmission()) {
            // we now revert this back to error
            schedulerJobInstance.setStatus(InstanceStatus.ERROR);
        }

        if(schedulerJobInstance != null && !updateState) {
            schedulerJobInstance.setStatus(transientStatus);
        }

        return schedulerJobInitiationEvents;
    }

    /**
     * This method is responsible for determining which events can be raised on the back of the receipt of
     * this scheduled process event.
     *
     * @param scheduledProcessEvent
     * @param contextInstance
     * @param dryRunParameters
     * @param globalEventJobInstanceMap
     * @param internalEventDrivenJobs
     * @param contextParameters
     * @param parentContextInstance
     * @param schedulerJobInitiationEvents
     * @param markAsRaised
     */
    protected void getScheduledJobInitiationEventsThatCanBeRaised(ContextualisedScheduledProcessEvent scheduledProcessEvent,
                                                                ContextInstance contextInstance,
                                                                DryRunParameters dryRunParameters,
                                                                Map<String, GlobalEventJobInstance> globalEventJobInstanceMap,
                                                                Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs,
                                                                Map<String, ContextStartJobInstance> contextStartJobInstanceMap,
                                                                Map<String, ContextTerminalJobInstance> contextTerminalJobInstanceMap,
                                                                Map<String, LocalEventJobInstance> localEventJobInstanceMap,
                                                                Map<String, BridgingJobInstance> bridgingJobInstanceMap,
                                                                List<ContextParameterInstance> contextParameters,
                                                                ContextInstance parentContextInstance,
                                                                List<SchedulerJobInitiationEvent> schedulerJobInitiationEvents,
                                                                boolean markAsRaised) {

        if(contextInstance.getJobDependencies() != null) {
            for (JobDependency jobDependency : contextInstance.getJobDependencies()) {
                String jobIdentifier = scheduledProcessEvent.getAgentName() + "-" + scheduledProcessEvent.getJobName();
                if (this.shouldRaiseEvent(jobDependency.getLogicalGrouping(), contextInstance.getScheduledJobsMap(), jobIdentifier)
                    && jobIdentifierIsInLogicalGrouping(jobDependency.getLogicalGrouping(), jobIdentifier)) {
                    SchedulerJobInstance jobInstance = contextInstance.getScheduledJobsMap().get(jobDependency.getJobIdentifier());
                    if(jobInstance == null) {
                        logger.info("Encountered job dependency[{}] but no scheduled job in job map!", jobDependency.getJobIdentifier());
                        continue;
                    }

                    InternalEventDrivenJobInstance internalEventDrivenJob = internalEventDrivenJobs.get(jobDependency.getJobIdentifier()
                        + "-" + contextInstance.getName());

                    GlobalEventJobInstance globalEventJobInstance = globalEventJobInstanceMap.get(jobDependency.getJobIdentifier()
                        + "-" + contextInstance.getName());

                    ContextStartJobInstance contextStartJobInstance = contextStartJobInstanceMap.get(jobDependency.getJobIdentifier()
                        + "-" + contextInstance.getName());

                    ContextTerminalJobInstance contextTerminalJobInstance = contextTerminalJobInstanceMap.get(jobDependency.getJobIdentifier()
                        + "-" + contextInstance.getName());

                    LocalEventJobInstance localEventJobInstance = localEventJobInstanceMap.get(jobDependency.getJobIdentifier()
                        + "-" + contextInstance.getName());

                    BridgingJobInstance bridgingJobInstance = bridgingJobInstanceMap.get(jobDependency.getJobIdentifier()
                        + "-" + contextInstance.getName());

                    // Find events that can be raised on the back of a GlobalEvents 
                    if (!jobInstance.isInitiationEventRaised() && globalEventJobInstance != null) {
                        if(markAsRaised) jobInstance.setInitiationEventRaised(true);

                        SchedulerJobInitiationEvent event = createGlobalSchedulerJobInitiationEvent(jobInstance, globalEventJobInstance
                            , dryRunParameters, parentContextInstance, contextInstance, scheduledProcessEvent);

                        if (event != null) {
                            schedulerJobInitiationEvents.add(event);
                        }
                    }
                    else if (!jobInstance.isInitiationEventRaised() && contextStartJobInstance != null) {
                        if(markAsRaised) jobInstance.setInitiationEventRaised(true);

                        SchedulerJobInitiationEvent event = createContextStartJobInitiationEvent(contextStartJobInstance
                            , dryRunParameters, parentContextInstance, scheduledProcessEvent);

                        if (event != null) {
                            schedulerJobInitiationEvents.add(event);
                        }
                    }
                    else if (!jobInstance.isInitiationEventRaised() && contextTerminalJobInstance != null) {
                        if(markAsRaised) jobInstance.setInitiationEventRaised(true);

                        SchedulerJobInitiationEvent event = createContextTerminalJobInitiationEvent(contextTerminalJobInstance
                            , dryRunParameters, parentContextInstance, scheduledProcessEvent);

                        if (event != null) {
                            schedulerJobInitiationEvents.add(event);
                        }
                    }
                    else if (!jobInstance.isInitiationEventRaised() && localEventJobInstance != null) {
                        if(markAsRaised) jobInstance.setInitiationEventRaised(true);

                        SchedulerJobInitiationEvent event = createLocalEventJobInitiationEvent(localEventJobInstance
                            , dryRunParameters, parentContextInstance, scheduledProcessEvent);

                        if (event != null) {
                            schedulerJobInitiationEvents.add(event);
                        }
                    }
                    else if (bridgingJobInstance != null) {
                        // if the event that is the catalyst for initiating the bridging job is an internal event driven job
                        // and it is a repeating job the the bridging job will always be initiated.
                        if ((scheduledProcessEvent.getInternalEventDrivenJob() != null
                            && scheduledProcessEvent.getInternalEventDrivenJob().isJobRepeatable()) ||
                            // if the bridging job has yet to be initiated it will be initiated.
                            !jobInstance.isInitiationEventRaised()) {
                            if (markAsRaised) jobInstance.setInitiationEventRaised(true);

                            SchedulerJobInitiationEvent event = createBridgingJobInitiationEvent(bridgingJobInstance
                                , dryRunParameters, parentContextInstance, scheduledProcessEvent);

                            if (event != null) {
                                schedulerJobInitiationEvents.add(event);
                            }
                        }
                    }
                    else if (jobInstance.getStatus().equals(InstanceStatus.ON_HOLD) ||
                        (!jobInstance.isInitiationEventRaised()
                            && !jobInstance.getStatus().equals(InstanceStatus.COMPLETE)
                            && !jobInstance.getStatus().equals(InstanceStatus.ERROR)) ||
                        (internalEventDrivenJob != null
                            && internalEventDrivenJob.isJobRepeatable()
                            && !internalEventDrivenJob.getJobName().equals(scheduledProcessEvent.getJobName()))) {
                        if(markAsRaised) jobInstance.setInitiationEventRaised(true);

                        SchedulerJobInitiationEvent event = createSchedulerJobInitiationEvent(jobInstance, internalEventDrivenJob
                            , dryRunParameters, contextParameters, parentContextInstance, scheduledProcessEvent, contextInstance);

                        if (event != null) {
                            schedulerJobInitiationEvents.add(event);
                        }
                    }
                }
            }
        }
    }

    /**
     * This method is responsible for managing all lock related logic.
     *
     * @param scheduledProcessEvent
     * @param contextInstance
     * @param schedulerJobInitiationEvents
     * @return
     */
    private synchronized List<SchedulerJobInitiationEvent> manageJobLocks(ContextualisedScheduledProcessEvent scheduledProcessEvent, ContextInstance contextInstance
            ,ContextInstance parentContextInstance, List<SchedulerJobInitiationEvent> schedulerJobInitiationEvents, MutableBoolean lockRaised) {
        List<SchedulerJobInitiationEvent> finalSchedulerJobInitiationEvents = new ArrayList<>();

        String jobIdentifier = scheduledProcessEvent.getAgentName() + "-" + scheduledProcessEvent.getJobName();

        // Determine if the scheduled process event received is currently holding the lock. There are a
        // couple of things to note here.
        //      1. We are only interested in events that are tied to an internal event driven job as they are the only
        //         job types that can participate in a lock.
        //      2. We are not interested in jobs that are flagged as starting as they CANNOT release jobs when starting.
        if(!lockRaised.booleanValue() && scheduledProcessEvent.getInternalEventDrivenJob() != null && !scheduledProcessEvent.isJobStarting() &&
            this.jobLockCache.hasLock(jobIdentifier, contextInstance.getName(), parentContextInstance.getEnvironmentGroup())) {
            lockRaised.setTrue();

            logger.info("Release {}", scheduledProcessEvent.getInternalEventDrivenJob());
            // Once we have determined that the job is holding the lock, release it.
            this.jobLockCache.release(jobIdentifier, contextInstance.getName(), parentContextInstance.getEnvironmentGroup());

            // Now determine if there are any queued initiation events waiting for the lock to be released.
            List<ContextualisedSchedulerJobInitiationEvent> queuedEvents = this.jobLockCache.pollSchedulerJobInitiationEventWaitQueue
                (jobIdentifier, contextInstance.getName(), parentContextInstance.getEnvironmentGroup());

            if (queuedEvents != null) {
                // Having determined that there is a queued event, it then takes a lock.
                queuedEvents.forEach(contextualisedSchedulerJobInitiationEvent -> {
                    this.jobLockCache.lock(contextualisedSchedulerJobInitiationEvent.getSchedulerJobInitiationEvent()
                        .getInternalEventDrivenJob().getIdentifier(), contextualisedSchedulerJobInitiationEvent.getContextName()
                        , parentContextInstance.getEnvironmentGroup());

                    // And finally we add it to the finalSchedulerJobInitiationEvents so that the initiation event will be sent to
                    // the relevant agent.
                    finalSchedulerJobInitiationEvents.add(contextualisedSchedulerJobInitiationEvent.getSchedulerJobInitiationEvent());
                });
            }
        }

        // Now iterate over the candidate job initiation events and determine which jobs actually participate in a lock.
        // For jobLock, check if internalEventDrivenJob exist on the event, else just add the event
        schedulerJobInitiationEvents.forEach(event -> {
            if(event.getInternalEventDrivenJob() != null &&
                this.jobLockCache.doesJobParticipateInLock(event.getInternalEventDrivenJob().getIdentifier(), contextInstance.getName()
                    , parentContextInstance.getEnvironmentGroup())) {
                logger.info("Job participates in lock {}", event.getInternalEventDrivenJob());


                // Now that we have determined that a job participates in a lock, we determine if the lock it participates in
                // is already locked.
                if (this.jobLockCache.locked(event.getInternalEventDrivenJob().getIdentifier(), contextInstance.getName()
                    , parentContextInstance.getEnvironmentGroup())) {
                    this.addQueuedSchedulerJobInitiationEvent(contextInstance, parentContextInstance, event.getInternalEventDrivenJob().getIdentifier()
                        , event);
                } else {
                    // Otherwise the job takes a lock and adds the initiation event to the finalSchedulerJobInitiationEvents so that
                    // the initiation event will be sent to the relevant agent.
                    this.jobLockCache.lock(event.getInternalEventDrivenJob().getIdentifier(), contextInstance.getName()
                        , parentContextInstance.getEnvironmentGroup());
                    logger.info("Lock {}", event.getInternalEventDrivenJob());
                    finalSchedulerJobInitiationEvents.add(event);
                }
            }
            else {
                // Initiation events for jobs that do not participate in a lock simply get sent to the agent in order to be processed.
                finalSchedulerJobInitiationEvents.add(event);
            }
        });

        return finalSchedulerJobInitiationEvents;
    }

    /**
     * Adds a queued scheduler job initiation event.
     *
     * @param contextInstance The context instance associated with the event.
     * @param parentContextInstance The parent context instance associated with the event.
     * @param jobIdentifier The identifier of the job to add.
     * @param event The scheduler job initiation event to add.
     */
    protected void addQueuedSchedulerJobInitiationEvent(ContextInstance contextInstance, ContextInstance parentContextInstance
        , String jobIdentifier, SchedulerJobInitiationEvent event) {
        if(!contextInstance.getScheduledJobsMap().get(event.getInternalEventDrivenJob().getIdentifier()).getStatus()
            .equals(InstanceStatus.ON_HOLD)) {
            logger.info("Add queued scheduler job {}", event.getInternalEventDrivenJob());
            if(this.jobLockCache == null) {
                this.jobLockCache = JobLockCacheImpl.instance();
            }
            // If already locked, we add the job to the queued jobs, as the lock is held by another job.
            this.jobLockCache.addQueuedSchedulerJobInitiationEvent(jobIdentifier, contextInstance.getName(), event
                , parentContextInstance.getEnvironmentGroup());

            SchedulerJobInstance schedulerJobInstance = event.getInternalEventDrivenJob();

            InstanceStatus currentJobState = schedulerJobInstance.getStatus();


            schedulerJobInstance.setStatus(InstanceStatus.LOCK_QUEUED);
            contextInstance.getScheduledJobsMap().get(jobIdentifier).setStatus(InstanceStatus.LOCK_QUEUED);

            this.issueSchedulerJobStateChangeEvent(new SchedulerJobInstanceStateChangeEventImpl(schedulerJobInstance, parentContextInstance
                , currentJobState, schedulerJobInstance.getStatus()));
        }
        else {
            logger.info("Job {} was ON HOLD and will NOT be added to job lock queue!", event.getInternalEventDrivenJob());
        }
    }

    /**
     * Add a SchedulerJobInstanceStateChangeEventListener.
     *
     * @param listener
     */
    public void addSchedulerJobStateChangeEventListener(SchedulerJobInstanceStateChangeEventListener listener) {
        if(!this.schedulerJobInstanceStateChangeEventListeners.contains(listener)) {
            this.schedulerJobInstanceStateChangeEventListeners.add(listener);
        }
    }

    /**
     * Remove a SchedulerJobInstanceStateChangeEventListener.
     * @param listener
     */
    public void removeSchedulerJobStateChangeEventListener(SchedulerJobInstanceStateChangeEventListener listener) {
        if(this.schedulerJobInstanceStateChangeEventListeners.contains(listener)) {
            this.schedulerJobInstanceStateChangeEventListeners.remove(listener);
        }
    }

    /**
     * Issue a SchedulerJobInstanceStateChangeEvent to all registered listeners.
     *
     * @param event
     */
    protected void issueSchedulerJobStateChangeEvent(SchedulerJobInstanceStateChangeEventImpl event) {
        this.executor.submit(() -> this.schedulerJobInstanceStateChangeEventListeners
            .forEach(listener -> listener.onSchedulerJobInstanceStateChangeEvent(event)));
    }


    /**
     * Creates a SchedulerJobInitiationEvent with the given parameters.
     *
     * @param schedulerJobInstance       The SchedulerJobInstance associated with the event.
     * @param internalEventDrivenJob     The InternalEventDrivenJobInstance associated with the event.
     * @param dryRunParameters          The DryRunParameters associated with the event.
     * @param contextParameters         The list of ContextParameterInstance objects associated with the event.
     * @param parentContextInstance     The parent ContextInstance associated with the event.
     * @param scheduledProcessEvent     The ContextualisedScheduledProcessEvent associated with the event.
     * @param contextInstance           The ContextInstance associated with the event.
     * @return The created SchedulerJobInitiationEvent object.
     */
    private SchedulerJobInitiationEvent createSchedulerJobInitiationEvent(SchedulerJobInstance schedulerJobInstance,
                                                                          InternalEventDrivenJobInstance internalEventDrivenJob,
                                                                          DryRunParameters dryRunParameters,
                                                                          List<ContextParameterInstance> contextParameters,
                                                                          ContextInstance parentContextInstance,
                                                                          ContextualisedScheduledProcessEvent scheduledProcessEvent,
                                                                          ContextInstance contextInstance) {
        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setAgentName(schedulerJobInstance.getAgentName());
        schedulerJobInitiationEvent.setJobName(schedulerJobInstance.getJobName());
        schedulerJobInitiationEvent.setContextName(parentContextInstance.getName());
        schedulerJobInitiationEvent.setContextInstanceId(parentContextInstance.getId());
        schedulerJobInitiationEvent.setDryRun(dryRunParameters != null);
        schedulerJobInitiationEvent.setDryRunParameters(dryRunParameters);
        schedulerJobInitiationEvent.setCatalystEvent(scheduledProcessEvent);

        if(schedulerJobInstance.isSkip()) {
            schedulerJobInitiationEvent.setSkipped(true);
            internalEventDrivenJob.setSkip(true);
        }

        if(contextInstance.getContextParameters() != null && !contextInstance.getContextParameters().isEmpty()
            // Get context parameters from child context
            && internalEventDrivenJob != null && internalEventDrivenJob.getContextParameters() != null) {
            this.setContextParametersOnInitiationEventFromCollection(schedulerJobInitiationEvent, internalEventDrivenJob,
                contextInstance.getContextParameters(), parentContextInstance);
        }
        else if(contextParameters != null && internalEventDrivenJob != null && internalEventDrivenJob.getContextParameters() != null) {
            // Get context parameters from parent context
            this.setContextParametersOnInitiationEventFromCollection(schedulerJobInitiationEvent, internalEventDrivenJob,
                contextParameters, parentContextInstance);
        }
        else if((contextParameters == null || contextParameters.isEmpty()) && internalEventDrivenJob != null
            && internalEventDrivenJob.getContextParameters() != null && !internalEventDrivenJob.getContextParameters().isEmpty()) {
            schedulerJobInitiationEvent.setContextParameters(internalEventDrivenJob.getContextParameters().stream()
                .map(contextParameter -> {
                    ContextParameterInstanceImpl contextParameterInstance = new ContextParameterInstanceImpl();
                    contextParameterInstance.setName(contextParameter.getName());
                    contextParameterInstance.setValue(contextParameter.getDefaultValue());
                    contextParameterInstance.setDefaultValue(contextParameter.getDefaultValue());

                    return contextParameterInstance;
                })
                .collect(Collectors.toList()));
        }

        if(internalEventDrivenJob != null) {
            schedulerJobInitiationEvent.setInternalEventDrivenJob(internalEventDrivenJob);

            if (internalEventDrivenJob.isTargetResidingContextOnly() && !internalEventDrivenJob.isJobRepeatable()) {
                if (this.isAlreadyComplete(parentContextInstance, schedulerJobInstance.getAgentName()
                    , schedulerJobInstance.getJobName(), scheduledProcessEvent.getChildContextNames())) {
                    schedulerJobInitiationEvent.setChildContextNames(List.of(contextInstance.getName()));
                } else {
                    schedulerJobInitiationEvent.setChildContextNames(scheduledProcessEvent.getChildContextNames());
                }
            } else {
                schedulerJobInitiationEvent.setChildContextNames(internalEventDrivenJob.getChildContextNames());
            }
        }

        if(this.agents.containsKey(schedulerJobInstance.getAgentName())) {
            // Find the url from solr. If it does not exist (maybe due to accidental removal) then use what's given at the start of the Context Instance creation
            String url;
            ModuleMetaData agentMetaFromSolr = moduleMetaDataService.findById(schedulerJobInstance.getAgentName());
            if (agentMetaFromSolr == null || StringUtils.isBlank(agentMetaFromSolr.getUrl())) {
                url = this.agents.get(schedulerJobInstance.getAgentName()).getUrl();
            } else {
                url = agentMetaFromSolr.getUrl();
            }
            schedulerJobInitiationEvent.setAgentUrl(url);
        }

        return schedulerJobInitiationEvent;
    }

    /**
     * Sets context parameters on initiation event from a collection of context parameter instances.
     *
     * @param schedulerJobInitiationEvent The initiation event of the scheduler job.
     * @param internalEventDrivenJob The internal event-driven job instance.
     * @param contextParameters The list of context parameter instances.
     * @param parentContextInstance The parent context instance.
     */
    private void setContextParametersOnInitiationEventFromCollection(SchedulerJobInitiationEvent schedulerJobInitiationEvent,
                                                                     InternalEventDrivenJobInstance internalEventDrivenJob,
                                                                     List<ContextParameterInstance> contextParameters,
                                                                     ContextInstance parentContextInstance) {
        if(internalEventDrivenJob.getContextParameters() != null) {
            schedulerJobInitiationEvent.setContextParameters(internalEventDrivenJob.getContextParameters().stream()
                .map(contextParameter -> {
                    AtomicReference<ContextParameterInstance> instance = new AtomicReference<>();
                    contextParameters.forEach(contextParameterInstance -> {
                        if (contextParameter.getName().equals(contextParameterInstance.getName())) {
                            instance.set(contextParameterInstance);
                        }
                    });

                    if (instance.get() != null) {
                        return this.replaceParamIfNotSet(parentContextInstance.getName(), instance.get());
                    } else {
                        ContextParameterInstance defaultInstance = new ContextParameterInstanceImpl();
                        defaultInstance.setName(contextParameter.getName());
                        defaultInstance.setValue(contextParameter.getDefaultValue());
                        defaultInstance.setDefaultValue(contextParameter.getDefaultValue());

                        return defaultInstance;
                    }
                }).collect(Collectors.toList()));
        }
    }

    /**
     * Helper method to create the SchedulerJobInitiationEvent for a Global Event that is published when the next job
     * in a context can be initiated.
     *      
     * @param schedulerJobInstance
     * @param globalEventJobInstance
     * @param dryRunParameters
     * @param parentContextInstance
     * @return
     */
    private SchedulerJobInitiationEvent createGlobalSchedulerJobInitiationEvent(SchedulerJobInstance schedulerJobInstance
        , GlobalEventJobInstance globalEventJobInstance, DryRunParameters dryRunParameters, ContextInstance parentContextInstance
        , ContextInstance contextInstance, ScheduledProcessEvent scheduledProcessEvent) {
        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setAgentName(schedulerJobInstance.getAgentName());
        schedulerJobInitiationEvent.setJobName(schedulerJobInstance.getJobName());
        schedulerJobInitiationEvent.setContextName(parentContextInstance.getName());
        schedulerJobInitiationEvent.setContextInstanceId(parentContextInstance.getId());
        schedulerJobInitiationEvent.setDryRun(dryRunParameters != null);
        schedulerJobInitiationEvent.setDryRunParameters(dryRunParameters);
        schedulerJobInitiationEvent.setCatalystEvent(scheduledProcessEvent);

        if(schedulerJobInstance.isSkip()) {
            schedulerJobInitiationEvent.setSkipped(true);
            globalEventJobInstance.setSkip(true);
        }

        schedulerJobInitiationEvent.setChildContextNames(globalEventJobInstance.getChildContextNames());
        
        return schedulerJobInitiationEvent;
    }

    /**
     * Creates a SchedulerJobInitiationEvent object for a given ContextTerminalJobInstance.
     *
     * @param contextTerminalJobInstance   The ContextTerminalJobInstance associated with the event.
     * @param dryRunParameters             The DryRunParameters associated with the event.
     * @param parentContextInstance        The parent ContextInstance associated with the event.
     * @param scheduledProcessEvent        The ScheduledProcessEvent associated with the event.
     * @return The created SchedulerJobInitiationEvent object.
     */
    private SchedulerJobInitiationEvent createContextTerminalJobInitiationEvent(ContextTerminalJobInstance contextTerminalJobInstance
        , DryRunParameters dryRunParameters, ContextInstance parentContextInstance, ScheduledProcessEvent scheduledProcessEvent) {
        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setAgentName(contextTerminalJobInstance.getAgentName());
        schedulerJobInitiationEvent.setJobName(contextTerminalJobInstance.getJobName());
        schedulerJobInitiationEvent.setContextName(parentContextInstance.getName());
        schedulerJobInitiationEvent.setContextInstanceId(parentContextInstance.getId());
        schedulerJobInitiationEvent.setDryRun(dryRunParameters != null);
        schedulerJobInitiationEvent.setDryRunParameters(dryRunParameters);
        schedulerJobInitiationEvent.setCatalystEvent(scheduledProcessEvent);
        schedulerJobInitiationEvent.setChildContextNames(contextTerminalJobInstance.getChildContextNames());

        return schedulerJobInitiationEvent;
    }

    /**
     * Creates a SchedulerJobInitiationEvent for a given ContextStartJobInstance.
     *
     * @param contextStartJobInstance      The ContextStartJobInstance associated with the event.
     * @param dryRunParameters             The DryRunParameters associated with the event.
     * @param parentContextInstance        The parent ContextInstance associated with the event.
     * @param scheduledProcessEvent        The ScheduledProcessEvent associated with the event.
     * @return The created SchedulerJobInitiationEvent object.
     */
    private SchedulerJobInitiationEvent createContextStartJobInitiationEvent(ContextStartJobInstance contextStartJobInstance
        , DryRunParameters dryRunParameters, ContextInstance parentContextInstance, ScheduledProcessEvent scheduledProcessEvent) {
        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setAgentName(contextStartJobInstance.getAgentName());
        schedulerJobInitiationEvent.setJobName(contextStartJobInstance.getJobName());
        schedulerJobInitiationEvent.setContextName(parentContextInstance.getName());
        schedulerJobInitiationEvent.setContextInstanceId(parentContextInstance.getId());
        schedulerJobInitiationEvent.setDryRun(dryRunParameters != null);
        schedulerJobInitiationEvent.setDryRunParameters(dryRunParameters);
        schedulerJobInitiationEvent.setCatalystEvent(scheduledProcessEvent);
        schedulerJobInitiationEvent.setChildContextNames(contextStartJobInstance.getChildContextNames());

        return schedulerJobInitiationEvent;
    }

    /**
     * Creates a SchedulerJobInitiationEvent for a LocalEventJobInstance associated with a scheduled process event.
     *
     * @param localEventJobInstance The LocalEventJobInstance associated with the event.
     * @param dryRunParameters The DryRunParameters associated with the event.
     * @param parentContextInstance The parent ContextInstance associated with the event.
     * @param scheduledProcessEvent The ScheduledProcessEvent associated with the event.
     * @return The created SchedulerJobInitiationEvent object.
     */
    private SchedulerJobInitiationEvent createLocalEventJobInitiationEvent(LocalEventJobInstance localEventJobInstance
        , DryRunParameters dryRunParameters, ContextInstance parentContextInstance, ScheduledProcessEvent scheduledProcessEvent) {
        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setAgentName(localEventJobInstance.getAgentName());
        schedulerJobInitiationEvent.setJobName(localEventJobInstance.getJobName());
        schedulerJobInitiationEvent.setContextName(parentContextInstance.getName());
        schedulerJobInitiationEvent.setContextInstanceId(parentContextInstance.getId());
        schedulerJobInitiationEvent.setDryRun(dryRunParameters != null);
        schedulerJobInitiationEvent.setDryRunParameters(dryRunParameters);
        schedulerJobInitiationEvent.setCatalystEvent(scheduledProcessEvent);
        schedulerJobInitiationEvent.setChildContextNames(localEventJobInstance.getChildContextNames());

        return schedulerJobInitiationEvent;
    }

    /**
     * Creates a SchedulerJobInitiationEvent for bridging job.
     *
     * @param bridgingJobInstance the bridging job instance to create event for
     * @param dryRunParameters the dry run parameters for the job, can be null
     * @param parentContextInstance the parent context instance for the job
     * @param scheduledProcessEvent the scheduled process event associated with the job
     *
     * @return a SchedulerJobInitiationEvent object for the bridging job
     */
    private SchedulerJobInitiationEvent createBridgingJobInitiationEvent(BridgingJobInstance bridgingJobInstance
        , DryRunParameters dryRunParameters, ContextInstance parentContextInstance, ScheduledProcessEvent scheduledProcessEvent) {
        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setAgentName(bridgingJobInstance.getAgentName());
        schedulerJobInitiationEvent.setJobName(bridgingJobInstance.getJobName());
        schedulerJobInitiationEvent.setContextName(parentContextInstance.getName());
        schedulerJobInitiationEvent.setContextInstanceId(parentContextInstance.getId());
        schedulerJobInitiationEvent.setDryRun(dryRunParameters != null);
        schedulerJobInitiationEvent.setDryRunParameters(dryRunParameters);
        schedulerJobInitiationEvent.setCatalystEvent(scheduledProcessEvent);
        schedulerJobInitiationEvent.setChildContextNames(bridgingJobInstance.getChildContextNames());

        return schedulerJobInitiationEvent;
    }

    /**
     * Replaces the value of a ContextParameterInstance if the value is not set or is empty.
     *
     * @param contextName the name of the context
     * @param instance the ContextParameterInstance to check and potentially replace its value
     * @return the updated ContextParameterInstance with the value replaced if necessary
     */
    private ContextParameterInstance replaceParamIfNotSet(String contextName, ContextParameterInstance instance) {
        if(instance.getValue() == null || instance.getValue().isEmpty()) {
            String replacementForContextParamName = contextParametersInstanceService.getContextParameterValue(contextName, instance.getName());
            if (replacementForContextParamName != null) {
                instance.setValue(replacementForContextParamName);
            }
            else {
                instance.setValue(instance.getDefaultValue());
            }
        }
        return instance;
    }

    /**
     * This method assesses the logic defined in a LogicalGrouping to determine if an event should be raised. The LogicalGrouping
     * data structure allows for nested logical groupings that are analogous to brackets used when defining complex nested logic.
     * Therefore, this method employs recursion in order to assess the nested nature of logical statements.
     *
     * @param logicalGrouping
     * @param schedulerJobInstancesMap
     * @return
     */
    private boolean shouldRaiseEvent(LogicalGrouping logicalGrouping, Map<String, SchedulerJobInstance> schedulerJobInstancesMap
        , String jobIdentifier) {
        boolean result = true;

        if(logicalGrouping == null) {
            return false;
        }

        if(logicalGrouping.getLogicalGrouping() != null) {
            // recursively work our way through nested logic
            result = this.shouldRaiseEvent(logicalGrouping.getLogicalGrouping(), schedulerJobInstancesMap, jobIdentifier);
        }

        return result && this.assessBaseLogic(logicalGrouping, schedulerJobInstancesMap);
    }



    /**
     * Checks if a specific job instance is already complete.
     *
     * @param contextInstance The context instance containing the scheduled jobs and contexts
     * @param agentName The name of the agent associated with the job
     * @param jobName The name of the job
     * @param childContextIds The list of child context IDs
     * @return true if the job instance is already complete, false otherwise
     */
    private boolean isAlreadyComplete(ContextInstance contextInstance, String agentName, String jobName, List<String> childContextIds) {
        if(contextInstance.getScheduledJobsMap() != null && !contextInstance.getScheduledJobsMap().isEmpty()) {
            SchedulerJobInstance schedulerJob = contextInstance.getScheduledJobsMap().get(agentName+"-"+jobName);
            if(schedulerJob != null && (schedulerJob.getStatus().equals(InstanceStatus.COMPLETE)
                || schedulerJob.getStatus().equals(InstanceStatus.ERROR))
                && (((ContextualisedScheduledProcessEvent)schedulerJob.getScheduledProcessEvent())
                .getChildContextNames() != null
                && ((ContextualisedScheduledProcessEvent)schedulerJob.getScheduledProcessEvent())
                    .getChildContextNames().equals(childContextIds))) {
                return true;
            }
        }

        AtomicReference<Boolean> contextId = new AtomicReference<>(false);
        if (contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()) {
            contextInstance.getContexts().forEach(c -> {
                if (isAlreadyComplete(c, agentName, jobName, childContextIds)) {
                    contextId.set(true);
                }
            });
        }

        return contextId.get();
    }

    /**
     * Use to gain access to the executors for shutdown when tearing down
     *
     * @return the executor
     */
    ExecutorService getExecutor() {
        return executor;
    }
}
