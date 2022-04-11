package org.ikasan.job.orchestration.core.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.scheduled.context.model.JobDependency;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.DryRunParameters;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobLogicMachine extends AbstractLogicMachine<SchedulerJobInstance> {

    private static final String PASS_THROUGH = "PASS_THROUGH";

    private Logger logger = LoggerFactory.getLogger(JobLogicMachine.class);

    private List<SchedulerJobInstanceStateChangeEventListener> schedulerJobInstanceStateChangeEventListeners;
    private ExecutorService executor;
    private Map<String, ModuleMetaData> agents;
    private JobLockCache jobLockCache;

    public JobLogicMachine(Map<String, ModuleMetaData> agents, JobLockCache jobLockCache) {
        this.agents = agents;
        this.schedulerJobInstanceStateChangeEventListeners = new ArrayList<>();
        executor = Executors.newSingleThreadExecutor();
        this.jobLockCache = jobLockCache;
    }

    /**
     *
     * @param scheduledProcessEvent
     * @param contextInstance
     *
     * @return
     */
    public List<SchedulerJobInitiationEvent> getJobInitiationEvents(ContextualisedScheduledProcessEvent scheduledProcessEvent
        , ContextInstance contextInstance, DryRunParameters dryRunParameters, Map<String, InternalEventDrivenJob> internalEventDrivenJobs
        , List<ContextParameterInstance> contextParameters, ContextInstance parentContextInstance) {
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

            this.issueSchedulerJobStateChangeEvent(new SchedulerJobInstanceStateChangeEventImpl(schedulerJobInstance, currentJobState,
                schedulerJobInstance.getStatus()));
        }

        List<SchedulerJobInitiationEvent> results = new ArrayList<>();

        checkNextAvailableJobs(scheduledProcessEvent, contextInstance, dryRunParameters, internalEventDrivenJobs, contextParameters, parentContextInstance, schedulerJobInstance, results);

        raiseEvents(scheduledProcessEvent, contextInstance, dryRunParameters, internalEventDrivenJobs, contextParameters, parentContextInstance, results);

        return results;
    }

    private void checkNextAvailableJobs(ContextualisedScheduledProcessEvent scheduledProcessEvent,
                                        ContextInstance contextInstance,
                                        DryRunParameters dryRunParameters,
                                        Map<String, InternalEventDrivenJob> internalEventDrivenJobs,
                                        List<ContextParameterInstance> contextParameters,
                                        ContextInstance parentContextInstance,
                                        SchedulerJobInstance schedulerJobInstance,
                                        List<SchedulerJobInitiationEvent> results) {

        if (schedulerJobInstance != null && schedulerJobInstance.getStatus().equals(InstanceStatus.COMPLETE)) {
            String identifier = schedulerJobInstance.getIdentifier();
            if (jobLockCache.locked(identifier) && jobLockCache.hasLock(identifier)) {
                jobLockCache.release(identifier);
                for (SchedulerJob job : jobLockCache.getJobsForIdentifier(identifier)) {
                    SchedulerJobInstance jobInstance = contextInstance.getScheduledJobsMap().get(job.getIdentifier());
                    InternalEventDrivenJob internalEventDrivenJob = internalEventDrivenJobs.get(job.getIdentifier());
                    if (jobInstance != null && jobInstance.getStatus().equals(InstanceStatus.WAITING) && !jobLockCache.hasLock(jobInstance.getIdentifier())) {
                        jobLockCache.lock(jobInstance.getIdentifier());
                        results.add(this.createSchedulerJobInitiationEvent(jobInstance, internalEventDrivenJob, dryRunParameters, contextParameters
                            , parentContextInstance, scheduledProcessEvent));

                        break;
                    }
                }
            }
        }
    }

    private void raiseEvents(ContextualisedScheduledProcessEvent scheduledProcessEvent,
                             ContextInstance contextInstance,
                             DryRunParameters dryRunParameters,
                             Map<String, InternalEventDrivenJob> internalEventDrivenJobs,
                             List<ContextParameterInstance> contextParameters,
                             ContextInstance parentContextInstance,
                             List<SchedulerJobInitiationEvent> results) {

        for (JobDependency jobDependency : contextInstance.getJobDependencies()) {
            if (this.shouldRaiseEvent(jobDependency.getLogicalGrouping(), contextInstance.getScheduledJobsMap())) {

                SchedulerJobInstance jobInstance = contextInstance.getScheduledJobsMap().get(jobDependency.getJobIdentifier());

                InternalEventDrivenJob internalEventDrivenJob = internalEventDrivenJobs.get(jobDependency.getJobIdentifier());

                boolean raiseEventDueToLock = false;
                if (!jobLockCache.locked(jobDependency.getJobIdentifier()) && jobInstance.getStatus() != InstanceStatus.COMPLETE) {
                    // adding the lock here will only add it if it's not already added and exists in the cache
                    // hence we try and add it every time regardless of whether it exists or not or has the lock as faster to do so
                    jobLockCache.lock(jobInstance.getIdentifier());
                    raiseEventDueToLock = true;
                }

                // We only want to raise the job initiation event once!
                if (!jobInstance.isInitiationEventRaised() && raiseEventDueToLock) {
                    jobInstance.setInitiationEventRaised(true);
                    results.add(createSchedulerJobInitiationEvent(jobInstance, internalEventDrivenJob, dryRunParameters, contextParameters
                        , parentContextInstance, scheduledProcessEvent));
                }
            }
        }
    }

    public void addSchedulerJobStateChangeEventListener(SchedulerJobInstanceStateChangeEventListener listener) {
        if(!this.schedulerJobInstanceStateChangeEventListeners.contains(listener)) {
            this.schedulerJobInstanceStateChangeEventListeners.add(listener);
        }
    }

    public void removeSchedulerJobStateChangeEventListener(SchedulerJobInstanceStateChangeEventListener listener) {
        if(this.schedulerJobInstanceStateChangeEventListeners.contains(listener)) {
            this.schedulerJobInstanceStateChangeEventListeners.remove(listener);
        }
    }

    private void issueSchedulerJobStateChangeEvent(SchedulerJobInstanceStateChangeEventImpl event) {
        this.executor.submit(() -> this.schedulerJobInstanceStateChangeEventListeners
            .forEach(listener -> listener.onSchedulerJobInstanceStateChangeEvent(event)));
    }

    private SchedulerJobInitiationEvent createSchedulerJobInitiationEvent(SchedulerJobInstance schedulerJobInstance
        , InternalEventDrivenJob internalEventDrivenJob, DryRunParameters dryRunParameters, List<ContextParameterInstance> contextParameters
        , ContextInstance parentContextInstance, ContextualisedScheduledProcessEvent scheduledProcessEvent) {
        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setAgentName(schedulerJobInstance.getAgentName());
        schedulerJobInitiationEvent.setJobName(schedulerJobInstance.getJobName());
        schedulerJobInitiationEvent.setContextId(parentContextInstance.getName());
        schedulerJobInitiationEvent.setContextInstanceId(parentContextInstance.getId());
        schedulerJobInitiationEvent.setDryRun(dryRunParameters != null);
        schedulerJobInitiationEvent.setDryRunParameters(dryRunParameters);
        schedulerJobInitiationEvent.setSkipped(schedulerJobInstance.isSkip());
        if(contextParameters != null) {
            schedulerJobInitiationEvent.setContextParameters(contextParameters.stream()
                .filter(contextParameterInstance -> internalEventDrivenJob
                    .getContextParameters()
                    .stream()
                    .filter(contextParameter -> contextParameterInstance.getName().equals(contextParameter.getName()))
                    .collect(Collectors.toList()).size() > 0)
                .collect(Collectors.toList()));
        }
        schedulerJobInitiationEvent.setInternalEventDrivenJob(internalEventDrivenJob);

        if(internalEventDrivenJob.getChildContextIds() != null && internalEventDrivenJob.getChildContextIds().contains(PASS_THROUGH)) {
            schedulerJobInitiationEvent.setChildContextIds(scheduledProcessEvent.getChildContextIds());
        }
        else {
            schedulerJobInitiationEvent.setChildContextIds(internalEventDrivenJob.getChildContextIds());
        }

        if(this.agents.containsKey(schedulerJobInstance.getAgentName())) {
            schedulerJobInitiationEvent.setAgentUrl(this.agents.get(schedulerJobInstance.getAgentName()).getUrl());
        }

        return schedulerJobInitiationEvent;
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

        // todo need to work out what we want to do when a job dependency has a null logical grouping
        if(logicalGrouping == null) {
            return false;
        }

        if(logicalGrouping.getLogicalGrouping() != null) {
            // recursively work our way through nested logic
            result = this.shouldRaiseEvent(logicalGrouping.getLogicalGrouping(), schedulerJobInstancesMap);
        }

        return result && this.assessBaseLogic(logicalGrouping, schedulerJobInstancesMap);
    }
}
