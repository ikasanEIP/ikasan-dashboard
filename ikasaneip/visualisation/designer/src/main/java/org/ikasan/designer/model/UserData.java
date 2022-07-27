package org.ikasan.designer.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserData {
    private String jobName;
    private String agentName;
    private String identifier;

    /**
     * Constructor
     *
     * @param jobName
     * @param agentName
     * @param identifier
     */
    public UserData(@JsonProperty("jobName") String jobName, @JsonProperty("agentName") String agentName, @JsonProperty("identifier") String identifier) {
        this.jobName = jobName;
        this.agentName = agentName;
        this.identifier = identifier;
    }

    public String getJobName() {
        return jobName;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getIdentifier() {
        return identifier;
    }
}
