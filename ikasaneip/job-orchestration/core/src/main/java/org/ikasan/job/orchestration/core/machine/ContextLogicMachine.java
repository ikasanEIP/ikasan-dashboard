package org.ikasan.job.orchestration.core.machine;

import org.ikasan.spec.scheduled.context.model.ContextDependency;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

import java.util.List;
import java.util.Map;

public class ContextLogicMachine extends AbstractLogicMachine<ContextInstance   > {


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
     *
     * @param contextDependency
     * @param contextInstanceMap
     * @return
     */
    private boolean isContextDependencySatisfied(ContextDependency contextDependency, Map<String, ContextInstance> contextInstanceMap) {
        boolean result;

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
