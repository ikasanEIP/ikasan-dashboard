package org.ikasan.scheduled.instance.model;

import org.ikasan.spec.scheduled.instance.model.ContextStartJobInstance;
import org.ikasan.spec.scheduled.job.model.JobConstants;

import java.util.Objects;

public class SolrContextStartJobInstanceImpl extends SolrSchedulerJobInstanceImpl implements ContextStartJobInstance {

    public SolrContextStartJobInstanceImpl() {
        super();
        super.agentName = JobConstants.CONTEXT_START_JOB;
    }

    public final String getAgentName() {
        return agentName;
    }

    public final void setAgentName(String agentName) {
        // nothing to do
    }

    @Override
    public String getIdentifier() {
        return agentName + "-" + getJobName();
    }

    @Override
    public void setIdentifier(String jobIdentifier) {
        // nothing to do
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextStartJobInstance)) return false;
        if (!super.equals(o)) return false;
        ContextStartJobInstance that = (ContextStartJobInstance) o;
        return Objects.equals(super.jobName, that.getJobName())
            && Objects.equals(super.contextName, that.getContextName())
            && Objects.equals(super.getChildContextName(), that.getChildContextName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.jobName, super.contextName, super.getChildContextName());
    }
}
