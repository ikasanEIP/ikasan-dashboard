package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.AndImpl;
import org.ikasan.spec.scheduled.context.model.And;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;

public class ContextAndBuilder {
    protected String identifier;
    protected LogicalGrouping logicalGrouping;

    /**
     * This class represents a builder for creating an instance of ContextAndBuilder.
     * It allows for setting the identifier and logicalGrouping of the ContextAndBuilder.
     * The build method returns an instance of And.
     */
    protected ContextAndBuilder() {
    }

    /**
     * Sets the identifier for the builder.
     *
     * @param identifier the identifier to set
     * @return the updated instance of ContextAndBuilder
     */
    public ContextAndBuilder withIdentifier(String identifier) {
        this.identifier = identifier;

        return this;
    }

    /**
     * Sets the logical grouping for the builder.
     *
     * @param logicalGrouping the logical grouping to set
     * @return the updated instance of ContextAndBuilder
     */
    public ContextAndBuilder withLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;

        return this;
    }

    /**
     * Builds an instance of type And.
     *
     * @return the built instance of type And
     */
    public And build() {
        And and = new AndImpl();
        and.setIdentifier(this.identifier);
        and.setLogicalGrouping(this.logicalGrouping);

        return and;
    }
}
