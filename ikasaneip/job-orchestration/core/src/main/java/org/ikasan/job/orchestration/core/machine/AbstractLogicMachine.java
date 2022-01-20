package org.ikasan.job.orchestration.core.machine;

import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.StatefulEntity;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class AbstractLogicMachine<STATEFUL_ENTITY extends StatefulEntity> {

    /**
     * Assess the outcome of the And grouping within the LogicalGrouping.
     *
     * @param logicalGrouping
     * @param statefulEntityMap
     * @return
     */
    protected boolean assessAnd(LogicalGrouping logicalGrouping, Map<String, STATEFUL_ENTITY> statefulEntityMap) {
        AtomicBoolean and = new AtomicBoolean(false);

        if(logicalGrouping.getAnd() != null && !logicalGrouping.getAnd().isEmpty()) {
            and.set(true);
            logicalGrouping.getAnd().forEach(operator -> {
                if(operator.getLogicalGrouping() != null) {
                    if(!this.assessBaseLogic(operator.getLogicalGrouping(), statefulEntityMap)) {
                        and.set(false);
                    }
                }
                else {
                    STATEFUL_ENTITY statefulEntity = statefulEntityMap.get(operator.getIdentifier());
                    if (statefulEntity == null) {
                        throw new ContextMachineException(String.format("Could not locate stateful entity[%s] when trying to assess logical group and[%s]",
                            operator.getIdentifier(), logicalGrouping));
                    }

                    if (!statefulEntity.getStatus().equals(InstanceStatus.COMPLETE)) {
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
     * @param statefulEntityMap
     * @return
     */
    protected boolean assessOr(LogicalGrouping logicalGrouping, Map<String, STATEFUL_ENTITY> statefulEntityMap) {
        AtomicBoolean or = new AtomicBoolean(false);
        if(logicalGrouping.getOr() != null && !logicalGrouping.getOr().isEmpty()) {
            logicalGrouping.getOr().forEach(operator -> {
                if(operator.getLogicalGrouping() != null) {
                    if(this.assessBaseLogic(operator.getLogicalGrouping(), statefulEntityMap)) {
                        or.set(true);
                    }
                }
                else {
                    STATEFUL_ENTITY statefulEntity = statefulEntityMap.get(operator.getIdentifier());
                    if (statefulEntity == null) {
                        throw new ContextMachineException(String.format("Could not locate stateful entity[%s] when trying to assess logical group or[%s]",
                            operator.getIdentifier(), logicalGrouping));
                    }

                    if (statefulEntity.getStatus().equals(InstanceStatus.COMPLETE)) {
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
     * @param statefulEntityMap
     * @return
     */
    protected boolean assessNot(LogicalGrouping logicalGrouping, Map<String, STATEFUL_ENTITY> statefulEntityMap) {
        AtomicBoolean not = new AtomicBoolean(false);
        if(logicalGrouping.getNot() != null && !logicalGrouping.getNot().isEmpty()) {
            logicalGrouping.getNot().forEach(operator -> {
                if(operator.getLogicalGrouping() != null) {
                    if(this.assessBaseLogic(operator.getLogicalGrouping(), statefulEntityMap)) {
                        not.set(true);
                    }
                }
                else {
                    STATEFUL_ENTITY statefulEntity = statefulEntityMap.get(operator.getIdentifier());
                    if (statefulEntity == null) {
                        throw new ContextMachineException(String.format("Could not locate stateful entity[%s] when trying to assess logical group or[%s]",
                            operator.getIdentifier(), logicalGrouping));
                    }

                    if (statefulEntity.getStatus().equals(InstanceStatus.COMPLETE)) {
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
     * @param statefulEntityMap
     * @return
     */
    protected boolean assessBaseLogic(LogicalGrouping logicalGrouping, Map<String, STATEFUL_ENTITY> statefulEntityMap) {
        // Here we assess the logic at the current level of the recursion.
        boolean andAssessment = this.assessAnd(logicalGrouping, statefulEntityMap);
        boolean orAssessment = this.assessOr(logicalGrouping, statefulEntityMap);
        boolean notAssessment = this.assessNot(logicalGrouping, statefulEntityMap);

        // Now apply a very simple logical statement to feed back to either the
        // originator or the recursive level above.
        return ((andAssessment || orAssessment) && !notAssessment);
    }
}
