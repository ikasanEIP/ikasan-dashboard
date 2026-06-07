package org.ikasan.designer.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class UserData {
    public static final String INTERNAL_EVENT_DRIVEN_JOB = "INTERNAL_EVENT_DRIVEN_JOB";
    public static final String QUARTZ_EVENT_DRIVEN_JOB = "QUARTZ_EVENT_DRIVEN_JOB";
    public static final String FILE_EVENT_DRIVEN_JOB = "FILE_EVENT_DRIVEN_JOB";
    public static final String GLOBAL_EVENT_DRIVEN_JOB = "GLOBAL_EVENT_DRIVEN_JOB";
    public static final String CONTEXT_START_JOB = "CONTEXT_START_JOB";
    public static final String CONTEXT_TERMINAL_JOB = "CONTEXT_TERMINAL_JOB";
    public static final String BRIDGING_JOB = "BRIDGING_JOB";
    public static final String LOCAL_EVENT_JOB = "LOCAL_EVENT_JOB";
    public static final String CONTEXT = "CONTEXT";
    public static final String REPEATABLE = "REPEATABLE";

    private String jobName;
    private String agentName;
    private String identifier;
    private String contextName;
    private String itemType;
    private String componentName;
    private List<String> previousJobIdentifiers;
    private List<String> subsequentJobIdentifiers;

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
        @JsonProperty("contextName") String contextName, @JsonProperty("itemType") String itemType, @JsonProperty("componentName") String componentName,
        @JsonProperty("previousJobIdentifiers") List<String> previousJobIdentifiers, @JsonProperty("subsequentJobIdentifiers") List<String> subsequentJobIdentifiers) {
        this.jobName = jobName;
        this.agentName = agentName;
        this.identifier = identifier;
        this.contextName = contextName;
        this.itemType = itemType;
        this.componentName = componentName;
        this.previousJobIdentifiers = previousJobIdentifiers;
        this.subsequentJobIdentifiers = subsequentJobIdentifiers;
    }

    /**
     * Retrieves the name of the job associated with this instance.
     *
     * @return the job name as a {@code String}.
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * Retrieves the name of the agent associated with the current job.
     *
     * @return the agent name as a {@code String}.
     */
    public String getAgentName() {
        return agentName;
    }

    /**
     * Retrieves the unique identifier associated with this instance.
     *
     * @return the identifier as a String, representing the unique identifier for this object.
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Retrieves the name of the context associated with this instance.
     *
     * @return the context name as a String.
     */
    public String getContextName() {
        return contextName;
    }

    /**
     * Retrieves the type of the item associated with this instance.
     *
     * @return a String representing the item type
     */
    public String getItemType() {
        return itemType;
    }

    /**
     * Retrieves the name of the component associated with this instance.
     *
     * @return the name of the component as a String.
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * Retrieves the list of identifiers for the jobs that precede the current job.
     *
     * @return a list of strings representing the identifiers of the previous jobs.
     */
    public List<String> getPreviousJobIdentifiers() {
        return previousJobIdentifiers;
    }

    /**
     * Sets the list of identifiers for the previous jobs associated with this instance.
     *
     * @param previousJobIdentifiers a list of job identifiers representing the preceding jobs
     */
    public void setPreviousJobIdentifiers(List<String> previousJobIdentifiers) {
        this.previousJobIdentifiers = previousJobIdentifiers;
    }

    /**
     * Retrieves the list of identifiers for jobs that are to be executed
     * subsequent to the current job.
     *
     * @return a list of strings representing the identifiers of the subsequent jobs.
     */
    public List<String> getSubsequentJobIdentifiers() {
        return subsequentJobIdentifiers;
    }
}
