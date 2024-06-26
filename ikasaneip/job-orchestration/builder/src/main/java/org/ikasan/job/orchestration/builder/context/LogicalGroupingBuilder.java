package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.model.context.LogicalGroupingImpl;
import org.ikasan.spec.scheduled.context.model.And;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.Not;
import org.ikasan.spec.scheduled.context.model.Or;

import java.util.ArrayList;
import java.util.List;

public class LogicalGroupingBuilder {
    private LogicalGrouping logicalGrouping;
    private List<And> and;
    private List<Or> or;
    private List<Not> not;

    /**
     * A builder class for creating instances of LogicalGrouping.
     * It provides methods to set the logical grouping type and add And, Or, or Not objects to the LogicalGrouping.
     */
    protected LogicalGroupingBuilder() {
    }

    /**
     * Sets the logical grouping for the builder.
     *
     * @param logicalGrouping the logical grouping to set
     * @return the updated LogicalGroupingBuilder
     */
    public LogicalGroupingBuilder withLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;

        return this;
    }

    /**
     * Adds an instance of And to the list of And conditions in the LogicalGroupingBuilder.
     *
     * @param and the And instance to be added
     * @return the updated LogicalGroupingBuilder instance
     */
    public LogicalGroupingBuilder addAnd(And and) {
        if(this.and == null) {
            this.and = new ArrayList<>();
        }

        this.and.add(and);

        return this;
    }

    /**
     * Adds a logical OR condition to the {@link LogicalGroupingBuilder}.
     *
     * @param or the {@link Or} condition to be added
     * @return the updated {@link LogicalGroupingBuilder} instance
     */
    public LogicalGroupingBuilder addOr(Or or) {
        if(this.or == null) {
            this.or = new ArrayList<>();
        }

        this.or.add(or);

        return this;
    }

    /**
     * Adds the specified Not object to the current LogicalGroupingBuilder.
     *
     * @param not the Not object to be added
     * @return the updated LogicalGroupingBuilder
     */
    public LogicalGroupingBuilder addNot(Not not) {
        if(this.not == null) {
            this.not = new ArrayList<>();
        }

        this.not.add(not);

        return this;
    }

    /**
     * Builds a LogicalGrouping object with the specified attributes.
     *
     * @return The constructed LogicalGrouping object.
     */
    public LogicalGrouping build() {
        LogicalGrouping logicalGrouping = new LogicalGroupingImpl();
        logicalGrouping.setAnd(this.and);
        logicalGrouping.setOr(this.or);
        logicalGrouping.setNot(this.not);
        logicalGrouping.setLogicalGrouping(this.logicalGrouping);

        return logicalGrouping;
    }
}
