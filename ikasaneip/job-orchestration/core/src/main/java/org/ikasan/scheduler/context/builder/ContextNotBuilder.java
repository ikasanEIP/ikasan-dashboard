package org.ikasan.scheduler.context.builder;

import org.ikasan.scheduler.core.model.context.NotImpl;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.Not;

public class ContextNotBuilder {
    protected String identifier;
    protected LogicalGrouping logicalGrouping;

    public ContextNotBuilder withIdentifier(String identifier) {
        this.identifier = identifier;

        return this;
    }

    public ContextNotBuilder withLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;

        return this;
    }

    public Not build() {
        Not not = new NotImpl();
        not.setIdentifier(this.identifier);
        not.setLogicalGrouping(this.logicalGrouping);

        return not;
    }
}
