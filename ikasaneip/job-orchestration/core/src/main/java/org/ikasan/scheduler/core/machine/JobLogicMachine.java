package org.ikasan.scheduler.core.machine;

import org.ikasan.scheduler.core.event.SchedulerJobInitiationEvent;
import org.ikasan.scheduler.core.model.context.JobDependency;
import org.ikasan.scheduler.core.model.context.LogicalGrouping;
import org.ikasan.scheduler.core.spec.InstanceStatus;
import org.ikasan.scheduler.core.model.instance.SchedulerJobInstance;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JobLogicMachine extends AbstractLogicMachine<SchedulerJobInstance> {

    /**
     *
     * @param scheduledProcessEvent
     * @param schedulerJobInstancesMap
     * @param jobDependencies
     * @return
     */
    public List<SchedulerJobInitiationEvent> getJobInitiationEvents(ScheduledProcessEvent scheduledProcessEvent
        , Map<String, SchedulerJobInstance> schedulerJobInstancesMap, List<JobDependency> jobDependencies) {
        SchedulerJobInstance schedulerJobInstance = schedulerJobInstancesMap
            .get(scheduledProcessEvent.getAgentName() + "-" + scheduledProcessEvent.getJobName());

        if(schedulerJobInstance != null) {
            // we update the job result with the event if it is relevant in this context.
            if(scheduledProcessEvent.isSuccessful()) {
                schedulerJobInstance.setStatus(InstanceStatus.COMPLETE);
            }
            else {
                schedulerJobInstance.setStatus(InstanceStatus.ERROR);
            }

            schedulerJobInstance.setScheduledProcessEvent(scheduledProcessEvent);
        }

        List<SchedulerJobInitiationEvent> results = new ArrayList<>();

        for(JobDependency jobDependency: jobDependencies) {
            if(this.shouldRaiseEvent(jobDependency.getLogicalGrouping(), schedulerJobInstancesMap)) {
                SchedulerJobInstance instance = schedulerJobInstancesMap.get(jobDependency.getJobIdentifier());

                // We only want to raise the job initiation event once!
                if(!instance.isInitiationEventRaised()) {
                    instance.setInitiationEventRaised(true);
                    results.add(new SchedulerJobInitiationEvent(instance.getAgentName(), instance.getJobName()));
                }
            }
        }

        return results;
    }

    /**
     * This method assesses the logic defined in a LogicalGrouping to determine if an event should be raised. The LogicalGrouping
     * data structure allows for nested logical groupings that are analogous to brackets used defining complex nested logic.
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
