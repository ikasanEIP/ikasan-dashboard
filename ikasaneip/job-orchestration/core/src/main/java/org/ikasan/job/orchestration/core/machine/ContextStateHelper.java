package org.ikasan.job.orchestration.core.machine;

import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ContextStateHelper extends AbstractLogicMachine<SchedulerJobInstance> {

    public boolean isAllLogicSatisfied(ContextInstance contextInstance, Map<String, SchedulerJobInstance> statefulEntityMap) {
        AtomicBoolean allLogicSatisfied = new AtomicBoolean(true);

        if(contextInstance.getJobDependencies() != null) {
            contextInstance.getJobDependencies().forEach(jobDependency -> {
                if (jobDependency.getLogicalGrouping() != null) {
                    allLogicSatisfied.set(determineNestedLogic(jobDependency.getLogicalGrouping(), statefulEntityMap));
                }
            });
        }

        return allLogicSatisfied.get();
    }

    private boolean determineNestedLogic(LogicalGrouping logicalGrouping, Map<String, SchedulerJobInstance> schedulerJobInstancesMap) {
        boolean result = true;

        if(logicalGrouping == null) {
            return false;
        }

        if(logicalGrouping.getLogicalGrouping() != null) {
            // recursively work our way through nested logic
            result = this.determineNestedLogic(logicalGrouping.getLogicalGrouping(), schedulerJobInstancesMap);
        }

        return result && this.assessBaseLogic(logicalGrouping, schedulerJobInstancesMap);
    }
}
