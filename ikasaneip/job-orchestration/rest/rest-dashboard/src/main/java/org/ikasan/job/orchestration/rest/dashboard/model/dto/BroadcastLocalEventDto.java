package org.ikasan.job.orchestration.rest.dashboard.model.dto;

import java.util.List;

public class BroadcastLocalEventDto {

    private String agentName;
    private String jobName;
    private String contextName;
    private String contextInstanceId;
    private List<String> childContextNames;

    public BroadcastLocalEventDto() {}

    public BroadcastLocalEventDto(String agentName, String jobName, String contextName,
                                   String contextInstanceId, List<String> childContextNames) {
        this.agentName = agentName;
        this.jobName = jobName;
        this.contextName = contextName;
        this.contextInstanceId = contextInstanceId;
        this.childContextNames = childContextNames;
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
}
