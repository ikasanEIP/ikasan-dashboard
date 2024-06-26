package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.AndImpl;
import org.ikasan.spec.scheduled.context.model.And;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;

public class JobAndBuilder {
    protected String agentName;
    protected String jobName;
    protected LogicalGrouping logicalGrouping;

    /**
     * JobAndBuilder is a builder class for creating an And object, which represents a logical AND operation
     * in a job or context template.
     */
    protected JobAndBuilder() {
    }

    /**
     * Sets the agent name for the JobAndBuilder object.
     *
     * @param agentName the agent name to set
     * @return the JobAndBuilder object with the agent name set
     */
    public JobAndBuilder withAgentName(String agentName) {
        this.agentName = agentName;

        return this;
    }

    /**
     * Sets the job name for the job builder.
     *
     * @param jobName the name of the job
     * @return the updated JobAndBuilder object
     */
    public JobAndBuilder withJobName(String jobName) {
        this.jobName = jobName;

        return this;
    }

    /**
     * Sets the logical grouping for the JobAndBuilder.
     *
     * @param logicalGrouping the logical grouping to set
     * @return the updated JobAndBuilder instance
     */
    public JobAndBuilder withLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;

        return this;
    }

    /**
     * Builds an instance of the 'And' class with the provided properties.
     * If the agentName and jobName properties are not null, it sets the identifier of the 'And' instance as agentName-jobName.
     * It also sets the logicalGrouping property of the 'And' instance as the value of the logicalGrouping property in the builder.
     *
     * @return The built instance of the 'And' class.
     */
    public And build() {
        And and = new AndImpl();
        if(this.agentName != null && this.jobName != null) {
            and.setIdentifier(this.agentName + "-" + this.jobName);
        }
        and.setLogicalGrouping(this.logicalGrouping);

        return and;
    }
}
