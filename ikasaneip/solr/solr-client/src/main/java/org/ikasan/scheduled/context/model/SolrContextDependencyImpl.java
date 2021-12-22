package org.ikasan.scheduled.context.model;

import org.ikasan.spec.scheduled.context.model.ContextDependency;
import org.ikasan.spec.scheduled.context.model.LogicalGrouping;

import java.util.List;

public class SolrContextDependencyImpl implements ContextDependency {
    private String contextIdentifier;
    private String contextDependencyName;
    private LogicalGrouping logicalGrouping;
    private List<ContextDependency> contextDependencies;

    public String getContextIdentifier() {
        return contextIdentifier;
    }

    public void setContextIdentifier(String contextIdentifier) {
        this.contextIdentifier = contextIdentifier;
    }

    public String getContextDependencyName() {
        return contextDependencyName;
    }

    public void setContextDependencyName(String contextDependencyName) {
        this.contextDependencyName = contextDependencyName;
    }

    public LogicalGrouping getLogicalGrouping() {
        return logicalGrouping;
    }

    public void setLogicalGrouping(LogicalGrouping logicalGrouping) {
        this.logicalGrouping = logicalGrouping;
    }

    public List<ContextDependency> getContextDependencies() {
        return contextDependencies;
    }

    public void setContextDependencies(List<ContextDependency> contextDependencies) {
        this.contextDependencies = contextDependencies;
    }
}
