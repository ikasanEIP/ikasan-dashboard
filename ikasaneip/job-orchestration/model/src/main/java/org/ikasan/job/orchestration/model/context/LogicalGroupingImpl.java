package org.ikasan.job.orchestration.model.context;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.job.orchestration.util.serialise.SortedAndListSerializer;
import org.ikasan.job.orchestration.util.serialise.SortedNotListSerializer;
import org.ikasan.job.orchestration.util.serialise.SortedOrListSerializer;
import org.ikasan.spec.scheduled.context.model.And;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.Not;
import org.ikasan.spec.scheduled.context.model.Or;


import java.util.List;
import java.util.Objects;

@JsonPropertyOrder({ "and",
    "or",
    "not",
    "logicalGrouping"})
public class LogicalGroupingImpl implements LogicalGrouping {
    private LogicalGrouping logicalGrouping;
    @JsonSerialize(using = SortedAndListSerializer.class)
    private List<And> and;
    @JsonSerialize(using = SortedOrListSerializer.class)
    private List<Or> or;
    @JsonSerialize(using = SortedNotListSerializer.class)
    private List<Not> not;

    public LogicalGrouping getLogicalGrouping() {
        return logicalGrouping;
    }

    public void setLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;
    }

    public List<And> getAnd() {
        return and;
    }

    public void setAnd(List<And> and) {
        this.and = and;
    }

    public List<Or> getOr() {
        return or;
    }

    public void setOr(List<Or> or) {
        this.or = or;
    }

    public List<Not> getNot() {
        return not;
    }

    public void setNot(List<Not> not) {
        this.not = not;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogicalGroupingImpl that = (LogicalGroupingImpl) o;
        return Objects.equals(logicalGrouping, that.logicalGrouping) && Objects.equals(and, that.and) && Objects.equals(or, that.or) && Objects.equals(not, that.not);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logicalGrouping, and, or, not);
    }
}
