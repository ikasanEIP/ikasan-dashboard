package org.ikasan.scheduler.core.machine;

import org.ikasan.scheduler.core.event.SchedulerJobInitiationEvent;
import org.ikasan.scheduler.core.model.context.JobDependency;
import org.ikasan.scheduler.core.model.context.LogicalGrouping;
import org.ikasan.scheduler.core.model.instance.InstanceStatus;
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
                if(operator.getLogicalGrouping() != null) {
                    if(!this.assessBaseLogic(operator.getLogicalGrouping(), schedulerJobInstancesMap)) {
                        and.set(false);
                    }
                }
                else {
                    SchedulerJobInstance job = schedulerJobInstancesMap.get(operator.getIdentifier());
                    if (job == null) {
                        throw new RuntimeException(String.format("Could not locate job[%s] when trying to assess logical group and[%s]",
                            operator.getIdentifier(), logicalGrouping));
                    }

                    if (!job.getStatus().equals(InstanceStatus.COMPLETE)) {
                        and.set(false);
                    }
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
                if(operator.getLogicalGrouping() != null) {
                    if(this.assessBaseLogic(operator.getLogicalGrouping(), schedulerJobInstancesMap)) {
                        or.set(true);
                    }
                }
                else {
                    SchedulerJobInstance job = schedulerJobInstancesMap.get(operator.getIdentifier());
                    if (job == null) {
                        throw new RuntimeException(String.format("Could not locate job[%s] when trying to assess logical group or[%s]",
                            operator.getIdentifier(), logicalGrouping));
                    }

                    if (job.getStatus().equals(InstanceStatus.COMPLETE)) {
                        or.set(true);
                    }
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
                if(operator.getLogicalGrouping() != null) {
                    if(this.assessBaseLogic(operator.getLogicalGrouping(), schedulerJobInstancesMap)) {
                        not.set(true);
                    }
                }
                else {
                    SchedulerJobInstance job = schedulerJobInstancesMap.get(operator.getIdentifier());
                    if (job == null) {
                        throw new RuntimeException(String.format("Could not locate job[%s] when trying to assess logical group or[%s]",
                            operator.getIdentifier(), logicalGrouping));
                    }

                    if (job.getStatus().equals(InstanceStatus.COMPLETE)) {
                        not.set(true);
                    }
                }
            });
        }

        return not.get();
    }

    /**
     * This method allows us to have an infinite depth of logical groupings and facilitate the recursion that supports that.
     *
     * @param logicalGrouping
     * @param schedulerJobInstancesMap
     * @return
     */
    private boolean assessBaseLogic(LogicalGrouping logicalGrouping, Map<String, SchedulerJobInstance> schedulerJobInstancesMap) {
        // Here we assess the logic at the current level of the recursion.
        boolean andAssessment = this.assessAnd(logicalGrouping, schedulerJobInstancesMap);
        boolean orAssessment = this.assessOr(logicalGrouping, schedulerJobInstancesMap);
        boolean notAssessment = this.assessNot(logicalGrouping, schedulerJobInstancesMap);

        // Now apply a very simple logical statement to feed back to either the
        // originator or the recursive level above.
        return ((andAssessment || orAssessment) && !notAssessment);
    }
}
