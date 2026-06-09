package org.ikasan.dashboard.ui.visualisation.model.designer.business.stream;

public class Flow {

    private final String moduleName;
    private final String flowName;

    /**
     * Constructs a Flow instance with the specified module name and flow name.
     *
     * @param moduleName the name of the module associated with this flow
     * @param flowName the name of the flow
     */
    public Flow(String moduleName, String flowName) {
        this.moduleName = moduleName;
        this.flowName = flowName;
    }

    /**
     * Retrieves the name of the module associated with this Flow instance.
     *
     * @return the name of the module as a String.
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Retrieves the name of the flow.
     *
     * @return the name of the flow as a String.
     */
    public String getFlowName() {
        return flowName;
    }
}
