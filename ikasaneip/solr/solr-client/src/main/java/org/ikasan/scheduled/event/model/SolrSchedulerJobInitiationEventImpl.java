package org.ikasan.scheduled.event.model;

import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.ikasan.scheduled.instance.model.SolrContextParameterInstanceImpl;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;

public class SolrSchedulerJobInitiationEventImpl implements SchedulerJobInitiationEvent<SolrContextParameterInstanceImpl, InternalEventDrivenJob, SolrDryRunParameters> {
    private String agentName;
    private String agentUrl;
    private String jobName;
    private InternalEventDrivenJob internalEventDrivenJob;
    private String contextId;
    private List<String> childContextIds;
    private String contextInstanceId;
    private List<SolrContextParameterInstanceImpl> contextParameters;
    private boolean dryRun = false;
    private SolrDryRunParameters dryRunParameters;
    private boolean skipped = false;

    @Override
    public String getAgentName() {
        return agentName;
    }

    @Override
    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    @Override
    public String getAgentUrl() {
        return agentUrl;
    }

    @Override
    public void setAgentUrl(String agentUrl) {
        this.agentUrl = agentUrl;
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    @Override
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public InternalEventDrivenJob getInternalEventDrivenJob() {
        return internalEventDrivenJob;
    }

    @Override
    public void setInternalEventDrivenJob(InternalEventDrivenJob internalEventDrivenJob) {
        this.internalEventDrivenJob = internalEventDrivenJob;
    }

    @Override
    public String getContextId() {
        return contextId;
    }

    @Override
    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    @Override
    public List<String> getChildContextIds() {
        return childContextIds;
    }

    @Override
    public void setChildContextIds(List<String> childContextIds) {
        this.childContextIds = childContextIds;
    }

    @Override
    public String getContextInstanceId() {
        return contextInstanceId;
    }

    @Override
    public void setContextInstanceId(String contextInstanceId) {
        this.contextInstanceId = contextInstanceId;
    }

    @Override
    public void setContextParameters(List<SolrContextParameterInstanceImpl> contextParameters) {
        this.contextParameters = contextParameters;
    }

    @Override
    public List<SolrContextParameterInstanceImpl> getContextParameters() {
        return this.contextParameters;
    }

    @Override
    public boolean isDryRun() {
        return dryRun;
    }

    @Override
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    @Override
    public void setDryRunParameters(SolrDryRunParameters dryRunParameters) {
        this.dryRunParameters = dryRunParameters;
    }

    @Override
    public SolrDryRunParameters getDryRunParameters() {
        return this.dryRunParameters;
    }

    @Override
    public void setSkipped(boolean skipped) {
        this.skipped = skipped;
    }

    @Override
    public boolean isSkipped() {
        return skipped;
    }

    @Override
    public boolean equals(Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
