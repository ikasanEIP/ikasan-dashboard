package org.ikasan.scheduler.context.builder;

import org.ikasan.scheduler.core.model.context.OrImpl;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.Or;

public class JobOrBuilder {
    protected String agentName;
    protected String jobName;
    protected LogicalGrouping logicalGrouping;

    public JobOrBuilder withAgentName(String agentName) {
        this.agentName = agentName;

        return this;
    }

    public JobOrBuilder withJobName(String jobName) {
        this.jobName = jobName;

        return this;
    }

    public JobOrBuilder withLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;

        return this;
    }

    public Or build() {
        Or or = new OrImpl();
        or.setIdentifier(this.agentName+"-"+this.jobName);
        or.setLogicalGrouping(this.logicalGrouping);

        return or;
    }
}
