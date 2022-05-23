package org.ikasan.scheduled.context.model;

import org.ikasan.spec.scheduled.context.model.ScheduledContextSearchFilter;

public class ScheduledContextSearchFilterImpl implements ScheduledContextSearchFilter {
    private String contextName;

    public String getContextName() {
        return contextName;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }
}
