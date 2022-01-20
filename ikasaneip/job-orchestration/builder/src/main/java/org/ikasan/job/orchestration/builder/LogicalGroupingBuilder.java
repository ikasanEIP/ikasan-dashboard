package org.ikasan.job.orchestration.builder;

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

    protected LogicalGroupingBuilder() {
    }

    public LogicalGroupingBuilder withLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;

        return this;
    }

    public LogicalGroupingBuilder addAnd(And and) {
        if(this.and == null) {
            this.and = new ArrayList<>();
        }

        this.and.add(and);

        return this;
    }

    public LogicalGroupingBuilder addOr(Or or) {
        if(this.or == null) {
            this.or = new ArrayList<>();
        }

        this.or.add(or);

        return this;
    }

    public LogicalGroupingBuilder addNot(Not not) {
        if(this.not == null) {
            this.not = new ArrayList<>();
        }

        this.not.add(not);

        return this;
    }

    public LogicalGrouping build() {
        LogicalGrouping logicalGrouping = new LogicalGroupingImpl();
        logicalGrouping.setAnd(this.and);
        logicalGrouping.setOr(this.or);
        logicalGrouping.setNot(this.not);
        logicalGrouping.setLogicalGrouping(this.logicalGrouping);

        return logicalGrouping;
    }
}
