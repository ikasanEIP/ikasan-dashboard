package org.ikasan.rest.dashboard.util;

import org.ikasan.spec.cache.FlowStateCacheAdapter;
import org.ikasan.spec.flow.FlowState;

import java.util.HashMap;
import java.util.Map;

public class TestCacheAdapter implements FlowStateCacheAdapter
{
    private Map<String, FlowState> flowStates = new HashMap<>();

    @Override
    public void put(String moduleName, String flowName, String state)
    {
        flowStates.put(moduleName + flowName, new TestFlowState(moduleName, flowName, state));
    }

    public String get(String key)
    {
        FlowState state = flowStates.get(key);
        return state != null ? state.getState() : null;
    }

    @Override
    public FlowState get(String moduleName, String flowName) {
        return flowStates.get(moduleName + flowName);
    }

    // Test helper methods

    public void clear() {
        flowStates.clear();
    }

    public void putFlowState(String moduleName, String flowName, String state) {
        put(moduleName, flowName, state);
    }

    /**
     * Test implementation of FlowState
     */
    public static class TestFlowState implements FlowState {
        private String moduleName;
        private String flowName;
        private String state;

        public TestFlowState(String moduleName, String flowName, String state) {
            this.moduleName = moduleName;
            this.flowName = flowName;
            this.state = state;
        }

        @Override
        public String getModuleName() {
            return moduleName;
        }

        @Override
        public void setModuleName(String moduleName) {
            this.moduleName = moduleName;
        }

        @Override
        public String getFlowName() {
            return flowName;
        }

        @Override
        public void setFlowName(String flowName) {
            this.flowName = flowName;
        }

        @Override
        public String getState() {
            return state;
        }

        @Override
        public void setState(String state) {
            this.state = state;
        }
    }
}
