package org.ikasan.job.orchestration.rest.dashboard.model.dto;

import java.util.List;

public class BroadcastGlobalEventsDto {

    private String agentName;
    private String jobName;
    private String contextName;
    private String contextInstanceId;
    private List<String> childContextNames;
    private boolean ignoreEnvironmentGroup;
    private boolean forceSending;

    public BroadcastGlobalEventsDto() {}

    public BroadcastGlobalEventsDto(String agentName, String jobName, String contextName,
                                     String contextInstanceId, List<String> childContextNames,
                                     boolean ignoreEnvironmentGroup, boolean forceSending) {
        this.agentName = agentName;
        this.jobName = jobName;
        this.contextName = contextName;
        this.contextInstanceId = contextInstanceId;
        this.childContextNames = childContextNames;
        this.ignoreEnvironmentGroup = ignoreEnvironmentGroup;
        this.forceSending = forceSending;
    }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }
    public String getContextName() { return contextName; }
    public void setContextName(String contextName) { this.contextName = contextName; }
    public String getContextInstanceId() { return contextInstanceId; }
    public void setContextInstanceId(String contextInstanceId) { this.contextInstanceId = contextInstanceId; }
    public List<String> getChildContextNames() { return childContextNames; }
    public void setChildContextNames(List<String> childContextNames) { this.childContextNames = childContextNames; }
    public boolean isIgnoreEnvironmentGroup() { return ignoreEnvironmentGroup; }
    public void setIgnoreEnvironmentGroup(boolean ignoreEnvironmentGroup) { this.ignoreEnvironmentGroup = ignoreEnvironmentGroup; }
    public boolean isForceSending() { return forceSending; }
    public void setForceSending(boolean forceSending) { this.forceSending = forceSending; }
}
