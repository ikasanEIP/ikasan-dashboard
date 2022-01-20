package org.ikasan.job.orchestration.builder;

import org.ikasan.job.orchestration.model.context.OrImpl;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.Or;

public class ContextOrBuilder {
    protected String identifier;
    protected LogicalGrouping logicalGrouping;

    protected ContextOrBuilder() {
    }

    public ContextOrBuilder withIdentifier(String identifier) {
        this.identifier = identifier;

        return this;
    }

    public ContextOrBuilder withLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;

        return this;
    }

    public Or build() {
        Or or = new OrImpl();
        or.setIdentifier(this.identifier);
        or.setLogicalGrouping(this.logicalGrouping);

        return or;
    }
}
