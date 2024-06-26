package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.JobDependencyImpl;
import org.ikasan.spec.scheduled.context.model.JobDependency;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;

public class JobDependencyBuilder {
    protected String agentName;
    protected String jobName;
    private LogicalGrouping logicalGrouping;

    /**
     * A builder class for creating JobDependency objects.
     */
    protected JobDependencyBuilder() {
    }

    /**
     * Sets the agent name for the JobDependencyBuilder.
     *
     * @param agentName the agent name to set
     * @return the JobDependencyBuilder instance
     */
    public JobDependencyBuilder withAgentName(String agentName) {
        this.agentName = agentName;

        return this;
    }

    /**
     * Sets the job name for the JobDependencyBuilder.
     *
     * @param jobName the name of the job
     * @return the JobDependencyBuilder instance
     */
    public JobDependencyBuilder withJobName(String jobName) {
        this.jobName = jobName;

        return this;
    }

    /**
     * Sets the logical grouping for the JobDependencyBuilder.
     *
     * @param logicalGrouping the logical grouping to be set
     * @return the JobDependencyBuilder instance
     */
    public JobDependencyBuilder withLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;

        return this;
    }

    /**
     * Builds a JobDependency object using the provided agent name, job name, and logical grouping.
     *
     * @return a JobDependency object
     * @throws ContextBuilderException if either agent name or job name is null
     */
    public JobDependency build() {
        if(this.agentName == null || this.jobName == null) {
            throw new ContextBuilderException("Both agent name and job name must not be null!");
        }

        JobDependency jobDependency = new JobDependencyImpl();
        jobDependency.setJobIdentifier(this.agentName+"-"+this.jobName);
        jobDependency.setLogicalGrouping(this.logicalGrouping);

        return jobDependency;
    }
}
