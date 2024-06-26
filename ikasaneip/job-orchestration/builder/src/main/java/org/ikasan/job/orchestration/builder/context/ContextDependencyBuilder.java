package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.ContextDependencyImpl;
import org.ikasan.spec.scheduled.context.model.ContextDependency;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;

public class ContextDependencyBuilder {
    private String contextIdentifier;
    private String contextDependencyName;
    private LogicalGrouping logicalGrouping;

    /**
     * The ContextDependencyBuilder class is used to build instances of ContextDependency.
     * It provides a fluent builder interface to set the properties of the ContextDependency object.
     */
    protected ContextDependencyBuilder() {
    }

    /**
     * Sets the context identifier for the ContextDependencyBuilder.
     *
     * @param contextIdentifier the context identifier to be set
     * @return the instance of the ContextDependencyBuilder with the updated context identifier
     */
    public ContextDependencyBuilder withContextIdentifier(String contextIdentifier) {
        this.contextIdentifier = contextIdentifier;

        return this;
    }

    /**
     * Sets the context dependency name for the ContextDependencyBuilder.
     *
     * @param contextDependencyName the context dependency name to be set
     * @return the instance of the ContextDependencyBuilder with the updated context dependency name
     */
    public ContextDependencyBuilder withContextDependencyName(String contextDependencyName) {
        this.contextDependencyName = contextDependencyName;

        return this;
    }

    /**
     * Sets the logical grouping for the ContextDependencyBuilder.
     *
     * @param logicalGrouping the logical grouping to be set
     * @return the instance of the ContextDependencyBuilder with the updated logical grouping
     */
    public ContextDependencyBuilder withLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;

        return this;
    }

    /**
     * Builds an instance of ContextDependency using the properties set in the builder.
     *
     * @return the built ContextDependency object
     */
    public ContextDependency build() {
        ContextDependency contextDependency = new ContextDependencyImpl();
        contextDependency.setContextIdentifier(this.contextIdentifier);
        contextDependency.setContextDependencyName(this.contextDependencyName);
        contextDependency.setLogicalGrouping(this.logicalGrouping);

        return contextDependency;
    }
}
