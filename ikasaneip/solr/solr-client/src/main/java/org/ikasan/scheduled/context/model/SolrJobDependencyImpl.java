package org.ikasan.scheduled.context.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.context.model.JobDependency;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;

public class SolrJobDependencyImpl implements JobDependency {
    private String jobIdentifier;
    private LogicalGrouping logicalGrouping;

    @Override
    public String getJobIdentifier() {
        return jobIdentifier;
    }

    @Override
    public void setJobIdentifier(String jobIdentifier) {
        this.jobIdentifier = jobIdentifier;
    }

    @Override
    public LogicalGrouping getLogicalGrouping() {
        return logicalGrouping;
    }

    @Override
    public void setLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
