package org.ikasan.job.orchestration.model.context;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.LogicalOperator;

import java.util.Objects;

public abstract class LogicalOperatorImpl implements LogicalOperator {
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

    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogicalOperatorImpl that = (LogicalOperatorImpl) o;
        return Objects.equals(identifier, that.identifier) && Objects.equals(logicalGrouping, that.logicalGrouping);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, logicalGrouping);
    }
}
