package org.ikasan.scheduled.context.model;

import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.LogicalOperator;

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
}
