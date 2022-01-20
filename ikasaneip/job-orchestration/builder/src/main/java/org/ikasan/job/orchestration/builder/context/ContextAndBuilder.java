package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.AndImpl;
import org.ikasan.spec.scheduled.context.model.And;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;

public class ContextAndBuilder {
    protected String identifier;
    protected LogicalGrouping logicalGrouping;

    protected ContextAndBuilder() {
    }

    public ContextAndBuilder withIdentifier(String identifier) {
        this.identifier = identifier;

        return this;
    }

    public ContextAndBuilder withLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;

        return this;
    }

    public And build() {
        And and = new AndImpl();
        and.setIdentifier(this.identifier);
        and.setLogicalGrouping(this.logicalGrouping);

        return and;
    }
}
