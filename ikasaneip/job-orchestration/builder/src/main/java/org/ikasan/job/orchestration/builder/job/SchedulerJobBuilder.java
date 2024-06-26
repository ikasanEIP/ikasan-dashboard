package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.builder.context.ContextBuilderException;
import org.ikasan.job.orchestration.model.job.SchedulerJobImpl;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.ArrayList;
import java.util.List;

public class SchedulerJobBuilder {
    protected String agentName;
    protected String jobName;
    protected String displayName;
    protected String contextName;
    protected List<String> childContextNames;
    protected String description;
    protected String startupControlType = "AUTOMATIC";
    protected int ordinal = -1;

    /**
     * The SchedulerJobBuilder class is used to build instances of SchedulerJob and its subclasses.
     */
    public SchedulerJobBuilder() {
    }

    /**
     * Sets the agent name for the SchedulerJobBuilder.
     *
     * @param agentName the name of the agent
     * @return the SchedulerJobBuilder instance
     */
    public SchedulerJobBuilder withAgentName(String agentName) {
        this.agentName = agentName;

        return this;
    }

    /**
     * Sets the job name for the SchedulerJobBuilder.
     *
     * @param jobName the job name to set
     * @return the SchedulerJobBuilder instance
     */
    public SchedulerJobBuilder withJobName(String jobName) {
        this.jobName = jobName;

        return this;
    }

    /**
     * Sets the context name for the SchedulerJobBuilder.
     *
     * @param contextName the context name to set
     * @return the SchedulerJobBuilder instance
     */
    public SchedulerJobBuilder withContextName(String contextName) {
        this.contextName = contextName;

        return this;
    }

    /**
     * Adds a child context ID to the SchedulerJobBuilder.
     *
     * @param childContextId the child context ID to add
     * @return the SchedulerJobBuilder instance
     */
    public SchedulerJobBuilder addChildContextId(String childContextId) {
        if(this.childContextNames == null) {
            this.childContextNames = new ArrayList<>();
        }

        this.childContextNames.add(childContextId);

        return this;
    }

    /**
     * Sets the description for the SchedulerJobBuilder.
     *
     * @param description the description to set
     * @return the SchedulerJobBuilder instance
     */
    public SchedulerJobBuilder withDescription(String description) {
        this.description = description;

        return this;
    }

    /**
     * Sets the startup control type for the SchedulerJobBuilder.
     * The startup control type determines how the scheduler job will be started.
     *
     * @param startupControlType the startup control type to set
     * @return the SchedulerJobBuilder instance
     */
    public SchedulerJobBuilder withStartupControlType(String startupControlType) {
        this.startupControlType = startupControlType;

        return this;
    }

    /**
     * Sets the ordinal value for the SchedulerJobBuilder.
     *
     * @param ordinal the ordinal value to set
     * @return the SchedulerJobBuilder instance with the ordinal value set
     */
    public SchedulerJobBuilder withOrdinal(int ordinal) {
        this.ordinal = ordinal;

        return this;
    }

    /**
     * Sets the display name for the SchedulerJobBuilder.
     *
     * @param displayName the display name to set
     * @return the SchedulerJobBuilder instance
     */
    public SchedulerJobBuilder withDisplayName(String displayName) {
        this.displayName = displayName;

        return this;
    }


    /**
     * Builds a SchedulerJob object based on the provided parameters.
     * @return the built SchedulerJob object
     * @throws ContextBuilderException if both agent name and job name are null
     */
    public SchedulerJob build() {
        if(this.agentName == null || this.jobName == null) {
            throw new ContextBuilderException("Both agent name and job name must no be null!");
        }

        SchedulerJob schedulerJob = new SchedulerJobImpl();
        schedulerJob.setIdentifier(this.agentName+"-"+this.jobName);
        schedulerJob.setAgentName(this.agentName);
        schedulerJob.setJobName(this.jobName);
        schedulerJob.setJobDescription(this.description);
        schedulerJob.setStartupControlType(this.startupControlType);
        schedulerJob.setOrdinal(this.ordinal);
        schedulerJob.setDisplayName(displayName);

        return schedulerJob;
    }
}
