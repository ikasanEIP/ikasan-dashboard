package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.NotImpl;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.Not;

public class JobNotBuilder {
    protected String agentName;
    protected String jobName;
    protected LogicalGrouping logicalGrouping;

    /**
     * JobNotBuilder is a builder class for constructing instances of Not.
     *
     * Usage example:
     * JobNotBuilder notBuilder = new JobNotBuilder();
     * Not not = notBuilder.withAgentName("agentName")
     *                     .withJobName("jobName")
     *                     .withLogicalGrouping(LogicalGrouping.OR)
     *                     .build();
     */
    protected JobNotBuilder() {
    }

    /**
     * Sets the agent name for the JobNotBuilder.
     *
     * @param agentName the name of the agent
     *
     * @return the JobNotBuilder instance with the agent name set
     */
    public JobNotBuilder withAgentName(String agentName) {
        this.agentName = agentName;

        return this;
    }

    /**
     * Sets the job name for the JobNotBuilder.
     *
     * @param jobName the job name to be set
     * @return the JobNotBuilder with the specified job name
     */
    public JobNotBuilder withJobName(String jobName) {
        this.jobName = jobName;

        return this;
    }

    /**
     * Sets the logical grouping for the JobNotBuilder.
     *
     * @param logicalGrouping the logical grouping to be set
     * @return the JobNotBuilder instance
     */
    public JobNotBuilder withLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;

        return this;
    }

    /**
     * Builds and returns a {@link Not} object.
     *
     * @return A {@link Not} object.
     */
    public Not build() {
        Not not = new NotImpl();

        if(this.agentName != null && this.jobName != null) {
            not.setIdentifier(this.agentName + "-" + this.jobName);
        }
        not.setLogicalGrouping(this.logicalGrouping);

        return not;
    }
}
