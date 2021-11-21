package org.ikasan.scheduler.core.machine;

import org.ikasan.scheduler.core.model.context.ContextDependency;
import org.ikasan.scheduler.core.model.instance.ContextInstance;
import org.ikasan.scheduler.core.model.instance.InstanceStatus;

import java.util.List;
import java.util.Map;

public class ContextLogicMachine extends AbstractLogicMachine<ContextInstance> {


    public boolean contextLogicSatisfied(Map<String, ContextInstance> contextInstanceMap, List<ContextDependency> contextDependencies) {
        boolean satisifed = true;

        for(ContextDependency contextDependency: contextDependencies) {
            if(!this.isContextDependencySatisfied(contextDependency, contextInstanceMap)) {
                satisifed = false;
            }
        }

        return satisifed;
    }


    /**
     * This method assesses the logic defined in a LogicalGrouping to determine if an event should be raised. The LogicalGrouping
     * data structure allows for nested logical groupings that are analogous to brackets used defining complex nested logic.
     * Therefore this method employs recursion in order to assess the nested nature of logical statements.
     *
     * @param logicalGrouping
     * @param contextInstanceMap
     * @return
     */
    private boolean isContextDependencySatisfied(ContextDependency contextDependency, Map<String, ContextInstance> contextInstanceMap) {
        boolean result = true;

        // todo need to work out what we want to do when a job dependency has a null logical grouping
        if(contextDependency.getLogicalGrouping() == null) {
            return false;
        }

        if(contextDependency.getLogicalGrouping().getLogicalGrouping() != null) {
            // recursively work our way through nested logic
            result = this.isContextDependencySatisfied(contextDependency, contextInstanceMap);
        }
        else {
            result = contextInstanceMap.get(contextDependency.getContextIdentifier()).getStatus().equals(InstanceStatus.COMPLETE);
        }

        return result && this.assessBaseLogic(contextDependency.getLogicalGrouping(), contextInstanceMap);
    }
}
