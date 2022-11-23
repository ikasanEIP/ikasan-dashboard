package org.ikasan.scheduled.context.model;

import org.ikasan.spec.scheduled.context.model.ScheduledContextSearchFilter;

import java.util.List;

public class ScheduledContextSearchFilterImpl implements ScheduledContextSearchFilter {
    private String contextName;
    private List<String> contextNames;

    @Override
    public String getContextName() {
        return contextName;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    @Override
    public List<String> getContextNames() {
        return contextNames;
    }

    @Override
    public void setContextNames(List<String> contextNames) {
        this.contextNames = contextNames;
    }
}
