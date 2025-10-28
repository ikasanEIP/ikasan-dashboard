package org.ikasan.scheduled.context.model;

import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.LogicalOperator;

import java.util.Objects;

public abstract class SolrLogicalOperatorImpl implements LogicalOperator {
    protected String identifier;
    protected LogicalGrouping logicalGrouping;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public LogicalGrouping getLogicalGrouping() {
        return logicalGrouping;
    }

    public void setLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SolrLogicalOperatorImpl that = (SolrLogicalOperatorImpl) o;
        return Objects.equals(identifier, that.identifier) && Objects.equals(logicalGrouping, that.logicalGrouping);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, logicalGrouping);
    }
}
