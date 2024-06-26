package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.OrImpl;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.Or;

public class ContextOrBuilder {
    protected String identifier;
    protected LogicalGrouping logicalGrouping;

    /**
     * This class represents a builder for creating instances of the ContextOrBuilder class.
     * It provides methods to set the identifier and logical grouping of the ContextOrBuilder.
     */
    protected ContextOrBuilder() {
    }

    /**
     * Sets the identifier of the ContextOrBuilder.
     *
     * @param identifier the identifier to be set
     * @return the ContextOrBuilder object with the updated identifier
     */
    public ContextOrBuilder withIdentifier(String identifier) {
        this.identifier = identifier;

        return this;
    }

    /**
     * Sets the logical grouping of the ContextOrBuilder.
     *
     * @param logicalGrouping the logical grouping to be set
     * @return the ContextOrBuilder object with the updated logical grouping
     */
    public ContextOrBuilder withLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;

        return this;
    }

    /**
     * Builds an instance of Or with the provided identifier and logical grouping.
     *
     * @return the built Or object
     */
    public Or build() {
        Or or = new OrImpl();
        or.setIdentifier(this.identifier);
        or.setLogicalGrouping(this.logicalGrouping);

        return or;
    }
}
