package org.ikasan.scheduled.context.model;

import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

public class SolrContextTemplateImpl extends SolrContextImpl<ContextTemplate, ContextParameter, SchedulerJob, JobLock> implements ContextTemplate {
    private boolean disabled = false;

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
