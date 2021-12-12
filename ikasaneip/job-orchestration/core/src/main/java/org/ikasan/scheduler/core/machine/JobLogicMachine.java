package org.ikasan.scheduler.core.machine;

import org.ikasan.scheduler.core.event.SchedulerJobInitiationEventImpl;
import org.ikasan.scheduler.core.event.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.scheduler.core.listener.SchedulerJobInstanceStateChangeEventListener;
import org.ikasan.scheduler.core.model.context.JobDependency;
import org.ikasan.scheduler.core.model.context.LogicalGrouping;
import org.ikasan.scheduler.core.model.instance.ContextInstance;
import org.ikasan.scheduler.core.spec.InstanceStatus;
import org.ikasan.scheduler.core.model.instance.SchedulerJobInstance;
import org.ikasan.spec.scheduled.event.model.DryRunParameters;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JobLogicMachine extends AbstractLogicMachine<SchedulerJobInstance> {

    private List<SchedulerJobInstanceStateChangeEventListener> schedulerJobInstanceStateChangeEventListeners;
    private ExecutorService executor;

    public JobLogicMachine() {
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
        , ContextInstance contextInstance, DryRunParameters dryRunParameters) {
        SchedulerJobInstance schedulerJobInstance = contextInstance.getScheduledJobsMap()
            .get(scheduledProcessEvent.getAgentName() + "-" + scheduledProcessEvent.getJobName());

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

            this.issueSchedulerJobStateChangeEvent(new SchedulerJobInstanceStateChangeEvent(schedulerJobInstance, currentJobState,
                schedulerJobInstance.getStatus()));
        }

        List<SchedulerJobInitiationEvent> results = new ArrayList<>();

        for(JobDependency jobDependency: contextInstance.getJobDependencies()) {
            if(this.shouldRaiseEvent(jobDependency.getLogicalGrouping(), contextInstance.getScheduledJobsMap())) {
                SchedulerJobInstance instance = contextInstance.getScheduledJobsMap().get(jobDependency.getJobIdentifier());

                // We only want to raise the job initiation event once!
                if(!instance.isInitiationEventRaised()) {
                    instance.setInitiationEventRaised(true);

                    SchedulerJobInitiationEvent schedulerJobInitiationEvent = new SchedulerJobInitiationEventImpl();
                    schedulerJobInitiationEvent.setAgentName(instance.getAgentName());
                    schedulerJobInitiationEvent.setJobName(instance.getJobName());
                    schedulerJobInitiationEvent.setContextId(contextInstance.getName());
                    schedulerJobInitiationEvent.setContextInstanceId(contextInstance.getId());
                    schedulerJobInitiationEvent.setDryRun(dryRunParameters != null);
                    schedulerJobInitiationEvent.setDryRunParameters(dryRunParameters);

                    results.add(schedulerJobInitiationEvent);
                }
            }
        }

        return results;
    }

    public void addSchedulerJobStateChangeEventListener(SchedulerJobInstanceStateChangeEventListener listener) {
        this.schedulerJobInstanceStateChangeEventListeners.add(listener);
    }

    private void issueSchedulerJobStateChangeEvent(SchedulerJobInstanceStateChangeEvent event) {
        this.executor.submit(() -> this.schedulerJobInstanceStateChangeEventListeners
            .forEach(listener -> listener.onSchedulerJobInstanceStateChangeEvent(event)));
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
