package org.ikasan.job.orchestration.core.machine;

import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInstanceStateChangeEventImpl;
import org.ikasan.spec.metadata.ModuleMetaData;
import org.ikasan.spec.scheduled.context.model.JobDependency;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.event.model.DryRunParameters;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class JobLogicMachine extends AbstractLogicMachine<SchedulerJobInstance> {

    private Logger logger = LoggerFactory.getLogger(JobLogicMachine.class);

    private List<SchedulerJobInstanceStateChangeEventListener> schedulerJobInstanceStateChangeEventListeners;
    private ExecutorService executor;
    private Map<String, ModuleMetaData> agents;

    public JobLogicMachine(Map<String, ModuleMetaData> agents) {
        this.agents = agents;
        this.schedulerJobInstanceStateChangeEventListeners = new ArrayList<>();
        executor = Executors.newSingleThreadExecutor();
    }

    /**
     *
     * @param scheduledProcessEvent
     * @param contextInstance
     *
     * @return
     */
    public List<SchedulerJobInitiationEvent> getJobInitiationEvents(ScheduledProcessEvent scheduledProcessEvent
        , ContextInstance contextInstance, DryRunParameters dryRunParameters, Map<String, InternalEventDrivenJob> internalEventDrivenJobs
        , List<ContextParameterInstance> contextParameters, ContextInstance parentContextInstance) {
        SchedulerJobInstance schedulerJobInstance = contextInstance.getScheduledJobsMap()
            .get(scheduledProcessEvent.getAgentName() + "-" + scheduledProcessEvent.getJobName());

        // Firstly the status of the job is set on the instance.
        if(schedulerJobInstance != null) {
            // we update the job result with the event if it is relevant in this context.
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

        // Now we want to check if the job we have received is part of a job lock as this may be the catalyst to release
        // another job that is part of that lock.
        Map.Entry<String, List<SchedulerJob>> entry = this.getAssociatedJobLock(contextInstance, schedulerJobInstance);
        if(schedulerJobInstance != null && schedulerJobInstance.getStatus().equals(InstanceStatus.COMPLETE)) {
            if (entry != null) {
                if (contextInstance.getLockHolders().containsKey(entry.getKey()) && contextInstance.getLockHolders()
                    .get(entry.getKey()).equals(schedulerJobInstance.getIdentifier())) {
                    contextInstance.getLockHolders().remove(entry.getKey());
                    for (SchedulerJob job : contextInstance.getJobLocks().get(entry.getKey())) {
                        SchedulerJobInstance jobInstance = contextInstance.getScheduledJobsMap().get(job.getIdentifier());
                        InternalEventDrivenJob internalEventDrivenJob = internalEventDrivenJobs.get(job.getIdentifier());

                        // Get the first job that is WAITING and release it. Assign the job lock to it.
                        if (jobInstance.getStatus().equals(InstanceStatus.WAITING)) {
                            contextInstance.getLockHolders().put(entry.getKey(), jobInstance.getIdentifier());
                            results.add(this.createSchedulerJobInitiationEvent(contextInstance, jobInstance
                                , internalEventDrivenJob, dryRunParameters, contextParameters, parentContextInstance));
                            break;
                        }
                    }
                }
            }
        }

        // Now iterate over the job dependencies to determine if any job initiation events can be raised.
        for(JobDependency jobDependency: contextInstance.getJobDependencies()) {
            if(this.shouldRaiseEvent(jobDependency.getLogicalGrouping(), contextInstance.getScheduledJobsMap())) {
                SchedulerJobInstance instance = contextInstance.getScheduledJobsMap().get(jobDependency.getJobIdentifier());
                InternalEventDrivenJob internalEventDrivenJob = internalEventDrivenJobs.get(jobDependency.getJobIdentifier());

                entry = this.getAssociatedJobLock(contextInstance, instance);
                boolean raiseEventDueToLock = true;

                if(entry != null) {
                    if(contextInstance.getLockHolders().containsKey(entry.getKey())) {
                        raiseEventDueToLock = false;
                    }
                    else {
                        contextInstance.getLockHolders().put(entry.getKey(), instance.getIdentifier());
                    }
                }

                // We only want to raise the job initiation event once!
                if (!instance.isInitiationEventRaised() && raiseEventDueToLock) {
                    instance.setInitiationEventRaised(true);

                    results.add(this.createSchedulerJobInitiationEvent(contextInstance, instance, internalEventDrivenJob
                        , dryRunParameters, contextParameters, parentContextInstance));
                }
            }
        }

        return results;
    }

    public void addSchedulerJobStateChangeEventListener(SchedulerJobInstanceStateChangeEventListener listener) {
        this.schedulerJobInstanceStateChangeEventListeners.add(listener);
    }

    private void issueSchedulerJobStateChangeEvent(SchedulerJobInstanceStateChangeEventImpl event) {
        this.executor.submit(() -> this.schedulerJobInstanceStateChangeEventListeners
            .forEach(listener -> listener.onSchedulerJobInstanceStateChangeEvent(event)));
    }

    private SchedulerJobInitiationEvent createSchedulerJobInitiationEvent(ContextInstance contextInstance, SchedulerJobInstance instance
        , InternalEventDrivenJob internalEventDrivenJob, DryRunParameters dryRunParameters, List<ContextParameterInstance> contextParameters
        , ContextInstance parentContextInstance) {
        SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
        schedulerJobInitiationEvent.setAgentName(instance.getAgentName());
        schedulerJobInitiationEvent.setJobName(instance.getJobName());
        schedulerJobInitiationEvent.setContextId(parentContextInstance.getName());
        schedulerJobInitiationEvent.setContextInstanceId(parentContextInstance.getId());
        schedulerJobInitiationEvent.setDryRun(dryRunParameters != null);
        schedulerJobInitiationEvent.setDryRunParameters(dryRunParameters);
        schedulerJobInitiationEvent.setSkipped(instance.isSkip());
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

        if(this.agents.containsKey(instance.getAgentName())) {
            schedulerJobInitiationEvent.setAgentUrl(this.agents.get(instance.getAgentName()).getUrl());
        }

        return schedulerJobInitiationEvent;
    }

    private Map.Entry<String, List<SchedulerJob>> getAssociatedJobLock(ContextInstance contextInstance, SchedulerJob schedulerJob) {
        if(contextInstance.getJobLocks() == null || contextInstance.getJobLocks().isEmpty()) {
            return null;
        }

        Optional lock = contextInstance.getJobLocks().entrySet()
            .stream()
            .filter(entry -> entry.getValue().stream()
                .filter(job -> job.getIdentifier().equals(schedulerJob.getIdentifier()))
                .findFirst()
                .isPresent())
            .findFirst();

        if(lock.isPresent()) {
            return (Map.Entry<String, List<SchedulerJob>>) lock.get();
        }
        else {
            return null;
        }
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
