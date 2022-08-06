package org.ikasan.designer.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserData {
    public static final String INTERNAL_EVENT_DRIVEN_JOB = "INTERNAL_EVENT_DRIVEN_JOB";
    public static final String QUARTZ_EVENT_DRIVEN_JOB = "QUARTZ_EVENT_DRIVEN_JOB";
    public static final String FILE_EVENT_DRIVEN_JOB = "FILE_EVENT_DRIVEN_JOB";
    public static final String CONTEXT = "CONTEXT";

    private String jobName;
    private String agentName;
    private String identifier;
    private String contextName;
    private String itemType;


    /**
     * Constructor
     *
     * @param jobName
     * @param agentName
     * @param identifier
     * @param contextName
     * @param itemType
     */
    public UserData(@JsonProperty("jobName") String jobName, @JsonProperty("agentName") String agentName, @JsonProperty("identifier") String identifier,
        @JsonProperty("contextName") String contextName, @JsonProperty("itemType") String itemType) {
        this.jobName = jobName;
        this.agentName = agentName;
        this.identifier = identifier;
        this.contextName = contextName;
        this.itemType = itemType;
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

    public String getContextName() {
        return contextName;
    }

    public String getItemType() {
        return itemType;
    }
}
