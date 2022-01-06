package org.ikasan.scheduler.context.builder;

import org.ikasan.scheduler.core.model.context.ContextDependencyImpl;
import org.ikasan.spec.scheduled.context.model.ContextDependency;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;

public class ContextDependencyBuilder {
    private String contextIdentifier;
    private String contextDependencyName;
    private LogicalGrouping logicalGrouping;

    public ContextDependencyBuilder withContextIdentifier(String contextIdentifier) {
        this.contextIdentifier = contextIdentifier;

        return this;
    }

    public ContextDependencyBuilder withContextDependencyName(String contextDependencyName) {
        this.contextDependencyName = contextDependencyName;

        return this;
    }

    public ContextDependencyBuilder withLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;

        return this;
    }

    public ContextDependency build() {
        ContextDependency contextDependency = new ContextDependencyImpl();
        contextDependency.setContextIdentifier(this.contextIdentifier);
        contextDependency.setContextDependencyName(this.contextDependencyName);
        contextDependency.setLogicalGrouping(this.logicalGrouping);

        return contextDependency;
    }
}
