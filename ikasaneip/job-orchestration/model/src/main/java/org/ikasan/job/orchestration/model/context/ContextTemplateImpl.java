package org.ikasan.job.orchestration.model.context;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

public class ContextTemplateImpl extends ContextImpl<ContextTemplate, ContextParameter, SchedulerJob, JobLock> implements ContextTemplate {
    private boolean disabled = false;
    private boolean delayAgentSynchronisationUntilNextInstance = false;
    private boolean requiresAgentSynchronisation = false;

    @Override
    public boolean isDelayAgentSynchronisationUntilNextInstance() {
        return delayAgentSynchronisationUntilNextInstance;
    }

    @Override
    public void setDelayAgentSynchronisationUntilNextInstance(boolean delayAgentSynchronisationUntilNextInstance) {
        this.delayAgentSynchronisationUntilNextInstance = delayAgentSynchronisationUntilNextInstance;
    }

    @Override
    public boolean isRequiresAgentSynchronisation() {
        return requiresAgentSynchronisation;
    }

    @Override
    public void setRequiresAgentSynchronisation(boolean requiresAgentSynchronisation) {
        this.requiresAgentSynchronisation = requiresAgentSynchronisation;
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
