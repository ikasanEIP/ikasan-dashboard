package org.ikasan.job.orchestration.builder;

import org.ikasan.job.orchestration.model.context.NotImpl;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.Not;

public class JobNotBuilder {
    protected String agentName;
    protected String jobName;
    protected LogicalGrouping logicalGrouping;

    protected JobNotBuilder() {
    }

    public JobNotBuilder withAgentName(String agentName) {
        this.agentName = agentName;

        return this;
    }

    public JobNotBuilder withJobName(String jobName) {
        this.jobName = jobName;

        return this;
    }

    public JobNotBuilder withLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;

        return this;
    }

    public Not build() {
        Not not = new NotImpl();

        if(this.agentName != null && this.jobName != null) {
            not.setIdentifier(this.agentName + "-" + this.jobName);
        }
        not.setLogicalGrouping(this.logicalGrouping);

        return not;
    }
}
