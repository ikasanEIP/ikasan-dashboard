package org.ikasan.scheduler.core.machine;

import org.ikasan.scheduler.core.event.SchedulerJobInitiationEvent;
import org.ikasan.scheduler.core.model.context.JobDependency;
import org.ikasan.scheduler.core.model.context.LogicalGrouping;
import org.ikasan.scheduler.core.model.instance.SchedulerJobInstance;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class JobLogicMachine {

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
            schedulerJobInstance.setCompletedSuccessfully(scheduledProcessEvent.isSuccessful());
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

        if(logicalGrouping.getLogicalGrouping() != null) {
            // recursively work our way through nested logic
            result = this.shouldRaiseEvent(logicalGrouping.getLogicalGrouping(), schedulerJobInstancesMap);
        }

        // Here we assess the logic at the current level of the recursion.
        boolean andAssessment = this.assessAnd(logicalGrouping, schedulerJobInstancesMap);
        boolean orAssessment = this.assessOr(logicalGrouping, schedulerJobInstancesMap);
        boolean notAssessment = this.assessNot(logicalGrouping, schedulerJobInstancesMap);

        // Now apply a very simple logical statement to feed back to either the
        // originator or the recursive level above.
        result = result && ((andAssessment || orAssessment) && !notAssessment);

        return result;
    }

    /**
     * Assess the outcome of the And grouping within the LogicalGrouping.
     *
     * @param logicalGrouping
     * @param schedulerJobInstancesMap
     * @return
     */
    private boolean assessAnd(LogicalGrouping logicalGrouping, Map<String, SchedulerJobInstance> schedulerJobInstancesMap) {
        AtomicBoolean and = new AtomicBoolean(false);
        if(logicalGrouping.getAnd() != null && !logicalGrouping.getAnd().isEmpty()) {
            and.set(true);
            logicalGrouping.getAnd().forEach(operator -> {
                SchedulerJobInstance job = schedulerJobInstancesMap.get(operator.getIdentifier());
                if (job == null) {
                    throw new RuntimeException(String.format("Could not locate job[%s] when trying to assess logical group and[%s]",
                        operator.getIdentifier(), logicalGrouping));
                }

                if (!job.isCompletedSuccessfully()) {
                    and.set(false);
                }
            });
        }

        return and.get();
    }

    /**
     * Assess the outcome of the Or grouping within the LogicalGrouping.
     *
     * @param logicalGrouping
     * @param schedulerJobInstancesMap
     * @return
     */
    private boolean assessOr(LogicalGrouping logicalGrouping, Map<String, SchedulerJobInstance> schedulerJobInstancesMap) {
        AtomicBoolean or = new AtomicBoolean(false);
        if(logicalGrouping.getOr() != null && !logicalGrouping.getOr().isEmpty()) {
            logicalGrouping.getOr().forEach(operator -> {
                SchedulerJobInstance job = schedulerJobInstancesMap.get(operator.getIdentifier());
                if (job == null) {
                    throw new RuntimeException(String.format("Could not locate job[%s] when trying to assess logical group or[%s]",
                        operator.getIdentifier(), logicalGrouping));
                }

                if (job.isCompletedSuccessfully()) {
                    or.set(true);
                }
            });
        }

        return or.get();
    }

    /**
     * Assess the outcome of the Not grouping within the LogicalGrouping.
     *
     * @param logicalGrouping
     * @param schedulerJobInstancesMap
     * @return
     */
    private boolean assessNot(LogicalGrouping logicalGrouping, Map<String, SchedulerJobInstance> schedulerJobInstancesMap) {
        AtomicBoolean not = new AtomicBoolean(false);
        if(logicalGrouping.getNot() != null && !logicalGrouping.getNot().isEmpty()) {
            logicalGrouping.getNot().forEach(operator -> {
                SchedulerJobInstance job = schedulerJobInstancesMap.get(operator.getIdentifier());
                if (job == null) {
                    throw new RuntimeException(String.format("Could not locate job[%s] when trying to assess logical group or[%s]",
                        operator.getIdentifier(), logicalGrouping));
                }

                if (job.isCompletedSuccessfully()) {
                    not.set(true);
                }
            });
        }

        return not.get();
    }
}
