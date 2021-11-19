package org.ikasan.scheduler.core.model.context;

public abstract class LogicalOperator {
    protected String identifier;

//    public LogicalOperator(String identifier) {
//        this.identifier = identifier;
//    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
