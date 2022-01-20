package org.ikasan.job.orchestration.builder;

import org.ikasan.job.orchestration.model.context.AndImpl;
import org.ikasan.spec.scheduled.context.model.And;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;

public class JobAndBuilder {
    protected String agentName;
    protected String jobName;
    protected LogicalGrouping logicalGrouping;

    protected JobAndBuilder() {
    }

    public JobAndBuilder withAgentName(String agentName) {
        this.agentName = agentName;

        return this;
    }

    public JobAndBuilder withJobName(String jobName) {
        this.jobName = jobName;

        return this;
    }

    public JobAndBuilder withLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;

        return this;
    }

    public And build() {
        And and = new AndImpl();
        if(this.agentName != null && this.jobName != null) {
            and.setIdentifier(this.agentName + "-" + this.jobName);
        }
        and.setLogicalGrouping(this.logicalGrouping);

        return and;
    }
}
