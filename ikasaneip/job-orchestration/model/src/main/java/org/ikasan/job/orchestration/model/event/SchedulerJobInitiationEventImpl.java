package org.ikasan.job.orchestration.model.event;

import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;

import java.util.List;
import java.util.Objects;

public class SchedulerJobInitiationEventImpl implements SchedulerJobInitiationEvent<ContextParameterInstanceImpl, InternalEventDrivenJobInstance, DryRunParametersImpl> {
    private String agentName;
    private String agentUrl;
    private String jobName;
    private InternalEventDrivenJobInstance internalEventDrivenJob;
    private String contextName;
    private List<String> childContextNames;
    private String contextInstanceId;
    private List<ContextParameterInstanceImpl> contextParameters;
    private boolean dryRun = false;
    private DryRunParametersImpl dryRunParameters;
    private boolean skipped = false;
    private ScheduledProcessEvent catalystEvent;

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
    public InternalEventDrivenJobInstance getInternalEventDrivenJob() {
        return internalEventDrivenJob;
    }

    @Override
    public void setInternalEventDrivenJob(InternalEventDrivenJobInstance internalEventDrivenJob) {
        this.internalEventDrivenJob = internalEventDrivenJob;
    }

    @Override
    public String getContextName() {
        return contextName;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    @Override
    public List<String> getChildContextNames() {
        return childContextNames;
    }

    @Override
    public void setChildContextNames(List<String> childContextNames) {
        this.childContextNames = childContextNames;
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
    public void setContextParameters(List<ContextParameterInstanceImpl> contextParameters) {
        this.contextParameters = contextParameters;
    }

    @Override
    public List<ContextParameterInstanceImpl> getContextParameters() {
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
    public void setDryRunParameters(DryRunParametersImpl dryRunParameters) {
        this.dryRunParameters = dryRunParameters;
    }

    @Override
    public DryRunParametersImpl getDryRunParameters() {
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
    public ScheduledProcessEvent getCatalystEvent() {
        return catalystEvent;
    }

    @Override
    public void setCatalystEvent(ScheduledProcessEvent catalystEvent) {
        this.catalystEvent = catalystEvent;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("SchedulerJobInitiationEventImpl{");
        sb.append("agentName='").append(agentName).append('\'');
        sb.append(", agentUrl='").append(agentUrl).append('\'');
        sb.append(", jobName='").append(jobName).append('\'');
        sb.append(", internalEventDrivenJob=").append(internalEventDrivenJob);
        sb.append(", contextId='").append(contextName).append('\'');
        if(childContextNames != null) {
            sb.append(", childContextIds=[ ");
            childContextNames.forEach(id -> sb.append("[").append(id).append("] "));
        }
        else {
            sb.append(", childContextIds='").append(this.childContextNames).append('\'');
        }
        sb.append("], contextInstanceId='").append(contextInstanceId).append('\'');
        sb.append(", contextParameters=").append(contextParameters);
        sb.append(", dryRun=").append(dryRun);
        sb.append(", dryRunParameters=").append(dryRunParameters);
        sb.append(", skipped=").append(skipped);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchedulerJobInitiationEventImpl that = (SchedulerJobInitiationEventImpl) o;
        return dryRun == that.dryRun &&
            skipped == that.skipped &&
            Objects.equals(agentName, that.agentName) &&
            Objects.equals(agentUrl, that.agentUrl) &&
            Objects.equals(jobName, that.jobName) &&
            Objects.equals(internalEventDrivenJob, that.internalEventDrivenJob) &&
            Objects.equals(contextName, that.contextName) &&
            Objects.equals(childContextNames, that.childContextNames) &&
            Objects.equals(contextInstanceId, that.contextInstanceId) &&
            Objects.equals(contextParameters, that.contextParameters) &&
            Objects.equals(dryRunParameters, that.dryRunParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agentName, agentUrl, jobName, internalEventDrivenJob, contextName, childContextNames
            , contextInstanceId, contextParameters, dryRun, dryRunParameters, skipped);
    }
}
