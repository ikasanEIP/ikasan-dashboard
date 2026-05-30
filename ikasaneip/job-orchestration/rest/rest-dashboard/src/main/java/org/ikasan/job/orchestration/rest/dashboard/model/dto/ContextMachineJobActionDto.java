package org.ikasan.job.orchestration.rest.dashboard.model.dto;

public class ContextMachineJobActionDto {

    private String jobIdentifier;
    private String childContextName;
    private boolean skipFlag;

    public ContextMachineJobActionDto() {}

    public ContextMachineJobActionDto(String jobIdentifier, String childContextName) {
        this.jobIdentifier = jobIdentifier;
        this.childContextName = childContextName;
    }

    public ContextMachineJobActionDto(String jobIdentifier, String childContextName, boolean skipFlag) {
        this.jobIdentifier = jobIdentifier;
        this.childContextName = childContextName;
        this.skipFlag = skipFlag;
    }

    public String getJobIdentifier() { return jobIdentifier; }
    public void setJobIdentifier(String jobIdentifier) { this.jobIdentifier = jobIdentifier; }

    public String getChildContextName() { return childContextName; }
    public void setChildContextName(String childContextName) { this.childContextName = childContextName; }

    public boolean isSkipFlag() { return skipFlag; }
    public void setSkipFlag(boolean skipFlag) { this.skipFlag = skipFlag; }
}
