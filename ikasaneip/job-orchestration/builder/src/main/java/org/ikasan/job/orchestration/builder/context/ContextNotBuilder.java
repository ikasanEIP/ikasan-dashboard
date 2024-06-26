package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.NotImpl;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.Not;

public class ContextNotBuilder {
    protected String identifier;
    protected LogicalGrouping logicalGrouping;

    /**
     * This class represents a builder for creating an instance of the Not interface, which negates a logical condition.
     * It allows setting the identifier and logicalGrouping properties of the resulting Not object.
     */
    protected ContextNotBuilder() {
    }

    /**
     * Sets the identifier for the ContextNotBuilder.
     *
     * @param identifier The identifier value to set.
     * @return The ContextNotBuilder object.
     */
    public ContextNotBuilder withIdentifier(String identifier) {
        this.identifier = identifier;

        return this;
    }

    /**
     * Sets the logical grouping for the ContextNotBuilder.
     *
     * @param logicalGrouping The logical grouping value to set.
     * @return The ContextNotBuilder object.
     */
    public ContextNotBuilder withLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;

        return this;
    }

    /**
     * Builds an instance of the Not interface with the specified identifier and logical grouping.
     *
     * @return The built instance of the Not interface.
     */
    public Not build() {
        Not not = new NotImpl();
        not.setIdentifier(this.identifier);
        not.setLogicalGrouping(this.logicalGrouping);

        return not;
    }
}
