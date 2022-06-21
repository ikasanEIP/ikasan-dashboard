package org.ikasan.job.orchestration.rest.client.dto;

import org.ikasan.job.orchestration.model.event.DryRunParametersImpl;
import org.ikasan.job.orchestration.model.instance.ContextParameterInstanceImpl;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;

import java.util.List;

public class SchedulerJobInitiationEventDto implements SchedulerJobInitiationEvent<ContextParameterInstanceImpl, InternalEventDrivenJobInstance, DryRunParametersImpl> {
    private String agentName;
    private String agentUrl;
    private String jobName;
    private InternalEventDrivenJobInstance internalEventDrivenJob;
    private String contextId;
    private List<String> childContextIds;
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
}