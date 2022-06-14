package org.ikasan.job.orchestration.core.machine;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.scheduled.context.model.JobDependency;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.ContextualisedSchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.model.DryRunParameters;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class JobLogicMachine extends AbstractLogicMachine<SchedulerJobInstance> {

    public static final String PASS_THROUGH = "PASS_THROUGH";

    private Logger logger = LoggerFactory.getLogger(JobLogicMachine.class);

    private List<SchedulerJobInstanceStateChangeEventListener> schedulerJobInstanceStateChangeEventListeners;
    private ExecutorService executor;
    private Map<String, ModuleMetaData> agents;
    private JobLockCache jobLockCache;
    private ContextParametersInstanceService contextParametersInstanceService;

    public JobLogicMachine(Map<String, ModuleMetaData> agents, JobLockCache jobLockCache, ContextParametersInstanceService contextParametersInstanceService) {
        this.agents = agents;
        this.schedulerJobInstanceStateChangeEventListeners = new ArrayList<>();
        // todo make pool size configurable
        executor = Executors.newFixedThreadPool(5);
        this.jobLockCache = jobLockCache;
        this.contextParametersInstanceService = contextParametersInstanceService;
    }

    /**
     * The method is responsible for determining if any job initiation events can be raised based on the receipt of a
     * ContextualisedScheduledProcessEvent. It delegates to methods to assess the logic associated with the job dependencies.
     *
     * @param scheduledProcessEvent
     * @param contextInstance
     *
     * @return
     */
    public List<SchedulerJobInitiationEvent> getJobInitiationEvents(ContextualisedScheduledProcessEvent scheduledProcessEvent
        , ContextInstance contextInstance, DryRunParameters dryRunParameters, Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs
        , List<ContextParameterInstance> contextParameters, ContextInstance parentContextInstance, MutableBoolean lockRaised) {
        SchedulerJobInstance schedulerJobInstance = contextInstance.getScheduledJobsMap()
            .get(scheduledProcessEvent.getAgentName() + "-" + scheduledProcessEvent.getJobName());

        if(schedulerJobInstance == null) {
            logger.warn("Scheduler job instance is null! Attempted lookup using job identifier[{}]"
                , scheduledProcessEvent.getAgentName() + "-" + scheduledProcessEvent.getJobName());
        }

        if(scheduledProcessEvent.getChildContextIds() != null) {
            StringBuffer childIds = new StringBuffer("[ ");
            scheduledProcessEvent.getChildContextIds().forEach(id -> childIds.append("{").append(id).append("}"));
            childIds.append("]");

            logger.info("Processing Schedule Process Event [{}], for Context Instance [{}], with Child Ids {}", scheduledProcessEvent.getJobName()
                , contextInstance.getName(), childIds.toString());
        }

        // Firstly the status of the job is set on the instance.
        if(schedulerJobInstance != null &&
            (scheduledProcessEvent.getChildContextIds() == null || scheduledProcessEvent.getChildContextIds().isEmpty()
            || scheduledProcessEvent.getChildContextIds().contains(contextInstance.getName()))) {
            // we update the job result with the event if it is relevant in this context. A null or empty
            // collection of child contexts means the event is valid for all contexts.
            InstanceStatus currentJobState = schedulerJobInstance.getStatus();

            if(scheduledProcessEvent.isJobStarting()) {
                schedulerJobInstance.setStatus(InstanceStatus.RUNNING);
            }
            else if(scheduledProcessEvent.isSuccessful()) {
                schedulerJobInstance.setStatus(InstanceStatus.COMPLETE);
            }
            else {
                schedulerJobInstance.setStatus(InstanceStatus.ERROR);
            }

            schedulerJobInstance.setScheduledProcessEvent(scheduledProcessEvent);
            schedulerJobInstance.setChildContextName(contextInstance.getName());
            schedulerJobInstance.setContextInstanceId(parentContextInstance.getId());

            this.issueSchedulerJobStateChangeEvent(new SchedulerJobInstanceStateChangeEventImpl(schedulerJobInstance, parentContextInstance
                , currentJobState, schedulerJobInstance.getStatus()));
        }

        List<SchedulerJobInitiationEvent> schedulerJobInitiationEvents = new ArrayList<>();

        getScheduledJobInitiationEventsThatCanBeRaised(scheduledProcessEvent, contextInstance, dryRunParameters, internalEventDrivenJobs, contextParameters
            , parentContextInstance, schedulerJobInitiationEvents);

        schedulerJobInitiationEvents = this.manageJobLocks(scheduledProcessEvent, contextInstance, schedulerJobInitiationEvents, lockRaised);

        return schedulerJobInitiationEvents;
    }

    /**
     * This method is responsible for determining which events can be raised on the back of the receipt of
     * this scheduled process event.
     *
     * @param scheduledProcessEvent
     * @param contextInstance
     * @param dryRunParameters
     * @param internalEventDrivenJobs
     * @param contextParameters
     * @param parentContextInstance
     * @param schedulerJobInitiationEvents
     */
    private void getScheduledJobInitiationEventsThatCanBeRaised(ContextualisedScheduledProcessEvent scheduledProcessEvent,
                                                                ContextInstance contextInstance,
                                                                DryRunParameters dryRunParameters,
                                                                Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs,
                                                                List<ContextParameterInstance> contextParameters,
                                                                ContextInstance parentContextInstance,
                                                                List<SchedulerJobInitiationEvent> schedulerJobInitiationEvents) {

        for (JobDependency jobDependency : contextInstance.getJobDependencies()) {
            if (this.shouldRaiseEvent(jobDependency.getLogicalGrouping(), contextInstance.getScheduledJobsMap())) {

                SchedulerJobInstance jobInstance = contextInstance.getScheduledJobsMap().get(jobDependency.getJobIdentifier());

                InternalEventDrivenJobInstance internalEventDrivenJob = internalEventDrivenJobs.get(jobDependency.getJobIdentifier() + "-" + contextInstance.getName());

                if (!jobInstance.isInitiationEventRaised()) {
                    jobInstance.setInitiationEventRaised(true);

                    SchedulerJobInitiationEvent event = createSchedulerJobInitiationEvent(jobInstance, internalEventDrivenJob, dryRunParameters
                        , contextParameters, parentContextInstance, scheduledProcessEvent, contextInstance);

                    if(event != null) {
                        schedulerJobInitiationEvents.add(event);
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
    private List<SchedulerJobInitiationEvent> manageJobLocks(ContextualisedScheduledProcessEvent scheduledProcessEvent,
                                ContextInstance contextInstance, List<SchedulerJobInitiationEvent> schedulerJobInitiationEvents, MutableBoolean lockRaised) {
        List<SchedulerJobInitiationEvent> finalSchedulerJobInitiationEvents = new ArrayList<>();

        String jobIdentifier = scheduledProcessEvent.getAgentName() + "-" + scheduledProcessEvent.getJobName();

        // Determine if the scheduled process event received is currently holding the lock. There are a
        // couple of things to note here.
        //      1. We are only interested in events that are tied to an internal event driven job as they are the only
        //         job types that can participate in a lock.
        //      2. We are not interested in jobs that are flagged as starting as they CANNOT release jobs when starting.
        if(!lockRaised.booleanValue() && scheduledProcessEvent.getInternalEventDrivenJob() != null && !scheduledProcessEvent.isJobStarting() &&
            this.jobLockCache.hasLock(jobIdentifier, contextInstance.getName())) {
            lockRaised.setTrue();
            logger.info("Locked {}", scheduledProcessEvent.getInternalEventDrivenJob());

            // Once we have determined that the job is holding the lock, release it.
            this.jobLockCache.release(jobIdentifier, contextInstance.getName());

            // Now determine if there are any queued initiation events waiting for the lock to be released.
            ContextualisedSchedulerJobInitiationEvent queuedEvent = this.jobLockCache.pollSchedulerJobInitiationEventWaitQueue
                (jobIdentifier, contextInstance.getName());

            if (queuedEvent != null) {
                // Having determined that there is a queued event, it then takes a lock.
                this.jobLockCache.lock(queuedEvent.getSchedulerJobInitiationEvent()
                    .getInternalEventDrivenJob().getIdentifier(), queuedEvent.getContextName());

                // And finally we add it to the finalSchedulerJobInitiationEvents so that the initiation event will be sent to
                // the relevant agent.
                finalSchedulerJobInitiationEvents.add(queuedEvent.getSchedulerJobInitiationEvent());
            }
        }

        // Now iterate over the candidate job initiation events and determine which jobs actually participate in a lock.
        schedulerJobInitiationEvents.forEach(event -> {
            if(this.jobLockCache.doesJobParticipateInLock(event.getInternalEventDrivenJob().getIdentifier(), contextInstance.getName())) {
                logger.info("Job participates in lock {}", event.getInternalEventDrivenJob());

                // Now that we have determined that a job participates in a lock, we determine if the lock it participates in
                // is already locked.
                if(this.jobLockCache.locked(event.getInternalEventDrivenJob().getIdentifier(), contextInstance.getName())) {
                    logger.info("Locked {}", event.getInternalEventDrivenJob());
                    // If already locked, we add the job to the queued jobs, as the lock is held by another job.
                    this.jobLockCache.addQueuedSchedulerJobInitiationEvent(event.getInternalEventDrivenJob().getIdentifier()
                        , contextInstance.getName(), event);
                }
                else {
                    // Otherwise the job takes a lock and adds the initiation event to the finalSchedulerJobInitiationEvents so that
                    // the initiation event will be sent to the relevant agent.
                    this.jobLockCache.lock(event.getInternalEventDrivenJob().getIdentifier(), contextInstance.getName());
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
    private void issueSchedulerJobStateChangeEvent(SchedulerJobInstanceStateChangeEventImpl event) {
        this.executor.submit(() -> this.schedulerJobInstanceStateChangeEventListeners
            .forEach(listener -> listener.onSchedulerJobInstanceStateChangeEvent(event)));
    }

    /**
     * Helper method to create the SchedulerJobInitiationEvent that is published when the next job in a context can
     * be initiated.
     *
     * @param schedulerJobInstance
     * @param internalEventDrivenJob
     * @param dryRunParameters
     * @param contextParameters
     * @param parentContextInstance
     * @param scheduledProcessEvent
     * @return
     */
    private SchedulerJobInitiationEvent createSchedulerJobInitiationEvent(SchedulerJobInstance schedulerJobInstance
        , InternalEventDrivenJobInstance internalEventDrivenJob, DryRunParameters dryRunParameters, List<ContextParameterInstance> contextParameters
        , ContextInstance parentContextInstance, ContextualisedScheduledProcessEvent scheduledProcessEvent, ContextInstance contextInstance) {
        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setAgentName(schedulerJobInstance.getAgentName());
        schedulerJobInitiationEvent.setJobName(schedulerJobInstance.getJobName());
        schedulerJobInitiationEvent.setContextId(parentContextInstance.getName());
        schedulerJobInitiationEvent.setContextInstanceId(parentContextInstance.getId());
        schedulerJobInitiationEvent.setDryRun(dryRunParameters != null);
        schedulerJobInitiationEvent.setDryRunParameters(dryRunParameters);

        boolean shouldSkip = contextParametersInstanceService.isSkipped(parentContextInstance.getName(), schedulerJobInstance.getJobName());
        schedulerJobInitiationEvent.setSkipped(shouldSkip);

        if(contextParameters != null) {
            schedulerJobInitiationEvent.setContextParameters(contextParameters.stream()
                .filter(contextParameterInstance -> internalEventDrivenJob
                    .getContextParameters()
                    .stream()
                    .filter(contextParameter -> contextParameterInstance.getName().equals(contextParameter.getName()))
                    .map(instance -> replaceParamIfSet(parentContextInstance.getName(), contextParameterInstance))
                    .collect(Collectors.toList()).size() > 0)
                .collect(Collectors.toList()));
        }
        schedulerJobInitiationEvent.setInternalEventDrivenJob(internalEventDrivenJob);

        if(internalEventDrivenJob.getChildContextIds() != null && internalEventDrivenJob.getChildContextIds().contains(PASS_THROUGH)) {
            if(this.isAlreadyComplete(parentContextInstance, schedulerJobInstance.getAgentName()
                , schedulerJobInstance.getJobName(), scheduledProcessEvent.getChildContextIds())) {

                schedulerJobInitiationEvent.setChildContextIds(List.of(contextInstance.getName()));
            }
            else {
                schedulerJobInitiationEvent.setChildContextIds(scheduledProcessEvent.getChildContextIds());
            }
        }
        else {
            schedulerJobInitiationEvent.setChildContextIds(internalEventDrivenJob.getChildContextIds());
        }

        if(this.agents.containsKey(schedulerJobInstance.getAgentName())) {
            schedulerJobInitiationEvent.setAgentUrl(this.agents.get(schedulerJobInstance.getAgentName()).getUrl());
        }

        return schedulerJobInitiationEvent;
    }

    private ContextParameterInstance replaceParamIfSet(String contextName, ContextParameterInstance instance) {
        Object replacementForContextParamName = contextParametersInstanceService.getContextParameterValue(contextName, instance.getName());
        if (replacementForContextParamName != null) {
            instance.setValue(replacementForContextParamName);
        }
        return instance;
    }

    /**
     * This method assesses the logic defined in a LogicalGrouping to determine if an event should be raised. The LogicalGrouping
     * data structure allows for nested logical groupings that are analogous to brackets used when defining complex nested logic.
     * Therefore this method employs recursion in order to assess the nested nature of logical statements.
     *
     * @param logicalGrouping
     * @param schedulerJobInstancesMap
     * @return
     */
    private boolean shouldRaiseEvent(LogicalGrouping logicalGrouping, Map<String, SchedulerJobInstance> schedulerJobInstancesMap) {
        boolean result = true;

        if(logicalGrouping == null) {
            return false;
        }

        if(logicalGrouping.getLogicalGrouping() != null) {
            // recursively work our way through nested logic
            result = this.shouldRaiseEvent(logicalGrouping.getLogicalGrouping(), schedulerJobInstancesMap);
        }

        return result && this.assessBaseLogic(logicalGrouping, schedulerJobInstancesMap);
    }

    /**
     * This method is used to determine if a job is already complete.
     *
     * @param contextInstance
     * @param agentName
     * @param jobName
     * @param childContextIds
     * @return
     */
    private boolean isAlreadyComplete(ContextInstance contextInstance, String agentName, String jobName, List<String> childContextIds) {
        if(contextInstance.getScheduledJobsMap() != null && !contextInstance.getScheduledJobsMap().isEmpty()) {
            SchedulerJobInstance schedulerJob = contextInstance.getScheduledJobsMap().get(agentName+"-"+jobName);
            if(schedulerJob != null && (schedulerJob.getStatus().equals(InstanceStatus.COMPLETE)
                || schedulerJob.getStatus().equals(InstanceStatus.ERROR))
                && ((ContextualisedScheduledProcessEvent)schedulerJob.getScheduledProcessEvent())
                    .getChildContextIds().equals(childContextIds)) {
                return true;
            }
        }

        AtomicReference<Boolean> contextId = new AtomicReference<>(false);
        if(contextInstance.getContexts() != null && !contextInstance.getContexts().isEmpty()) {
            contextInstance.getContexts().forEach(c -> {
                if(isAlreadyComplete(c, agentName, jobName, childContextIds)) {
                    contextId.set(true);
                }
            });
        }

        return contextId.get();
    }
}
