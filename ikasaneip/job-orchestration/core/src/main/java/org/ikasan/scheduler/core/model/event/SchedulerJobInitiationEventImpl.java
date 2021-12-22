package org.ikasan.scheduler.core.model.event;

import org.ikasan.scheduler.core.model.instance.ContextParameterInstanceImpl;
import org.ikasan.scheduler.core.model.job.InternalEventDrivenJobImpl;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;

import java.util.List;

public class SchedulerJobInitiationEventImpl implements SchedulerJobInitiationEvent<ContextParameterInstanceImpl, InternalEventDrivenJobImpl,
    DryRunParametersImpl> {
    private String agentName;
    private String jobName;
    private InternalEventDrivenJobImpl internalEventDrivenJob;
    private String contextId;
    private String contextInstanceId;
    private List<ContextParameterInstanceImpl> contextParameters;
    private boolean dryRun = false;
    private DryRunParametersImpl dryRunParameters;
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
    public String getJobName() {
        return jobName;
    }

    @Override
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public InternalEventDrivenJobImpl getInternalEventDrivenJob() {
        return internalEventDrivenJob;
    }

    @Override
    public void setInternalEventDrivenJob(InternalEventDrivenJobImpl internalEventDrivenJob) {
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
    public String toString() {
        final StringBuffer sb = new StringBuffer("SchedulerJobInitiationEventImpl{");
        sb.append("agentName='").append(agentName).append('\'');
        sb.append(", jobName='").append(jobName).append('\'');
        sb.append(", internalEventDrivenJob=").append(internalEventDrivenJob);
        sb.append(", contextId='").append(contextId).append('\'');
        sb.append(", contextInstanceId='").append(contextInstanceId).append('\'');
        sb.append(", contextParameters=").append(contextParameters);
        sb.append(", dryRun=").append(dryRun);
        sb.append(", dryRunParameters=").append(dryRunParameters);
        sb.append(", skipped=").append(skipped);
        sb.append('}');
        return sb.toString();
    }
}
