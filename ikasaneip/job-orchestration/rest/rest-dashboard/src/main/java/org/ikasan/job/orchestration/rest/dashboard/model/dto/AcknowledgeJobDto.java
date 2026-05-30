package org.ikasan.job.orchestration.rest.dashboard.model.dto;

import java.util.List;

public class AcknowledgeJobDto {

    private String identifier;
    private boolean targetResidingContextOnly;
    private String childContextName;
    private List<String> childContextNames;

    public AcknowledgeJobDto() {}

    public AcknowledgeJobDto(String identifier, boolean targetResidingContextOnly,
                              String childContextName, List<String> childContextNames) {
        this.identifier = identifier;
        this.targetResidingContextOnly = targetResidingContextOnly;
        this.childContextName = childContextName;
        this.childContextNames = childContextNames;
    }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public boolean isTargetResidingContextOnly() { return targetResidingContextOnly; }
    public void setTargetResidingContextOnly(boolean targetResidingContextOnly) {
        this.targetResidingContextOnly = targetResidingContextOnly;
    }

    public String getChildContextName() { return childContextName; }
    public void setChildContextName(String childContextName) { this.childContextName = childContextName; }

    public List<String> getChildContextNames() { return childContextNames; }
    public void setChildContextNames(List<String> childContextNames) { this.childContextNames = childContextNames; }
}
