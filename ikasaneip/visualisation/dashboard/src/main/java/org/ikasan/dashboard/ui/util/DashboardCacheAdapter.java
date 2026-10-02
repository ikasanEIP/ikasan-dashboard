package org.ikasan.dashboard.ui.util;

import org.ikasan.dashboard.broadcast.FlowState;
import org.ikasan.dashboard.broadcast.State;
import org.ikasan.dashboard.cache.FlowStateCache;
import org.ikasan.rest.dashboard.model.flow.FlowStateImpl;
import org.ikasan.rest.dashboard.model.metadata.module.ModuleMetaDataImpl;
import org.ikasan.spec.cache.FlowStateCacheAdapter;

public class DashboardCacheAdapter implements FlowStateCacheAdapter
{

    @Override
    public void put(String moduleName, String flowName, String state)
    {
        FlowState flowState = new FlowState(moduleName, flowName, State.getState(state));

        FlowStateCache.instance().put(flowState);
    }

    @Override
    public org.ikasan.spec.flow.FlowState get(String moduleName, String flowName) {
        ModuleMetaDataImpl moduleMetaData = new ModuleMetaDataImpl();
        moduleMetaData.setName(moduleName);
        if(FlowStateCache.instance().contains(moduleName, flowName)) {
            FlowState state = FlowStateCache.instance().get(moduleMetaData, flowName);
            FlowStateImpl flowState =  new FlowStateImpl();
            flowState.setModuleName(moduleName);
            flowState.setFlowName(flowName);
            flowState.setState(state.getState().getFlowState());
            return flowState;
        }

        return null;
    }
}
