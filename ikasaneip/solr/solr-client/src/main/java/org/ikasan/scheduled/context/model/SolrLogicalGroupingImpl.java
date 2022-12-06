package org.ikasan.scheduled.context.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.context.model.And;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;
import org.ikasan.spec.scheduled.context.model.Not;
import org.ikasan.spec.scheduled.context.model.Or;

import java.util.List;

public class SolrLogicalGroupingImpl implements LogicalGrouping {
    private LogicalGrouping logicalGrouping;
    private List<And> and;
    private List<Or> or;
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
}
