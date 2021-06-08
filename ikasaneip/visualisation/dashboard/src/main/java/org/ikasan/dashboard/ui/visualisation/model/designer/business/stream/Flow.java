package org.ikasan.dashboard.ui.visualisation.model.designer.business.stream;

public class Flow {

    private String moduleName;
    private String flowName;

    public Flow(String moduleName, String flowName) {
        this.moduleName = moduleName;
        this.flowName = flowName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getFlowName() {
        return flowName;
    }
}
