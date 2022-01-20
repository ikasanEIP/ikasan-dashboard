package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.OrImpl;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.Or;

public class JobOrBuilder {
    protected String agentName;
    protected String jobName;
    protected LogicalGrouping logicalGrouping;

    protected JobOrBuilder() {
    }

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
        if(this.agentName != null && this.jobName != null) {
            or.setIdentifier(this.agentName + "-" + this.jobName);
        }
        or.setLogicalGrouping(this.logicalGrouping);

        return or;
    }
}
