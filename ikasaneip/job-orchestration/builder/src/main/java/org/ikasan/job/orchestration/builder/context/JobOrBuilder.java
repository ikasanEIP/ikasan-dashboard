package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.OrImpl;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.Or;

public class JobOrBuilder {
    protected String agentName;
    protected String jobName;
    protected LogicalGrouping logicalGrouping;

    /**
     * Protected constructor for the JobOrBuilder class.
     *
     * This constructor is used to create an instance of the JobOrBuilder class.
     * It does not have any parameters.
     *
     * @see JobOrBuilder
     */
    protected JobOrBuilder() {
    }

    /**
     * Sets the agent name for the JobOrBuilder object.
     *
     * @param agentName the agent name to set
     * @return the JobOrBuilder object
     */
    public JobOrBuilder withAgentName(String agentName) {
        this.agentName = agentName;

        return this;
    }

    /**
     * Sets the job name of the JobOrBuilder object.
     *
     * @param jobName the name of the job
     * @return the JobOrBuilder object
     */
    public JobOrBuilder withJobName(String jobName) {
        this.jobName = jobName;

        return this;
    }

    /**
     * Sets the logical grouping for the JobOrBuilder.
     *
     * @param logicalGrouping the logical grouping to set
     * @return the updated JobOrBuilder object
     */
    public JobOrBuilder withLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;

        return this;
    }

    /**
     * Builds and returns an instance of the Or interface.
     *
     * @return an instance of the Or interface
     */
    public Or build() {
        Or or = new OrImpl();
        if(this.agentName != null && this.jobName != null) {
            or.setIdentifier(this.agentName + "-" + this.jobName);
        }
        or.setLogicalGrouping(this.logicalGrouping);

        return or;
    }
}
