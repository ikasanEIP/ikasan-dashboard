package org.ikasan.scheduler.context.builder;

import org.ikasan.scheduler.core.model.context.AndImpl;
import org.ikasan.spec.scheduled.context.model.And;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;

public class ContextAndBuilder {
    protected String identifier;
    protected LogicalGrouping logicalGrouping;

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
