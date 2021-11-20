package org.ikasan.scheduler.core.model.context;

public abstract class LogicalOperator {
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
