package org.ikasan.scheduler.context.builder;

import org.ikasan.scheduler.core.model.context.JobDependencyImpl;
import org.ikasan.spec.scheduled.context.model.JobDependency;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;

public class JobDependencyBuilder {
    protected String agentName;
    protected String jobName;
    private LogicalGrouping logicalGrouping;

    public JobDependencyBuilder withAgentName(String agentName) {
        this.agentName = agentName;

        return this;
    }

    public JobDependencyBuilder withJobName(String jobName) {
        this.jobName = jobName;

        return this;
    }

    public JobDependencyBuilder withLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;

        return this;
    }

    public JobDependency build() {
        if(this.agentName == null || this.jobName == null) {
            throw new ContextBuilderException("Both agent name and job name must no be null!");
        }

        JobDependency jobDependency = new JobDependencyImpl();
        jobDependency.setJobIdentifier(this.agentName+"-"+this.jobName);
        jobDependency.setLogicalGrouping(this.logicalGrouping);

        return jobDependency;
    }
}
