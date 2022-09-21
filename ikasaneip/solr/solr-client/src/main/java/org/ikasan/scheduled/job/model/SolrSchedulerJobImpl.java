package org.ikasan.scheduled.job.model;


import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolrSchedulerJobImpl implements SchedulerJob {
    protected String jobIdentifier;
    protected String agentName;
    protected String jobName;
    protected String jobDescription;
    protected String contextName;
    protected List<String> childContextNames;
    protected String startupControlType = "AUTOMATIC";
    protected boolean skip = false;
    protected Map<String, Boolean> skippedContexts = new HashMap<>();
    protected Map<String, Boolean> heldContexts = new HashMap<>();

    @Override
    public String getContextName() {
        return this.contextName;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public List<String> getChildContextNames() {
        return childContextNames;
    }

    public void setChildContextNames(List<String> childContextNames) {
        this.childContextNames = childContextNames;
    }

    @Override
    public String getIdentifier() {
        return this.jobIdentifier;
    }

    @Override
    public void setIdentifier(String jobIdentifier) {
        this.jobIdentifier = jobIdentifier;
    }

    @Override
    public String getAgentName() {
        return this.agentName;
    }

    @Override
    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    @Override
    public String getJobName() {
        return this.jobName;
    }

    @Override
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public String getJobDescription() {
        return this.jobDescription;
    }

    @Override
    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    @Override
    public String getStartupControlType() {
        return startupControlType;
    }

    @Override
    public void setStartupControlType(String startupControlType) {
        this.startupControlType = startupControlType;
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    @Override
    public Map<String, Boolean> getSkippedContexts() {
        return skippedContexts;
    }

    @Override
    public void setSkippedContexts(Map<String, Boolean> skippedContexts) {
        this.skippedContexts = skippedContexts;
    }

    @Override
    public Map<String, Boolean> getHeldContexts() {
        return heldContexts;
    }

    @Override
    public void setHeldContexts(Map<String, Boolean> heldContexts) {
        this.heldContexts = heldContexts;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("SchedulerJobImpl{");
        sb.append("jobIdentifier='").append(jobIdentifier).append('\'');
        sb.append(", agentName='").append(agentName).append('\'');
        sb.append(", jobName='").append(jobName).append('\'');
        sb.append(", contextId='").append(contextName).append('\'');
        if(childContextNames != null) {
            sb.append(", childContextNames=[ ");
            childContextNames.forEach(id -> sb.append("[").append(id).append("] "));
        }
        else {
            sb.append(", childContextNames='").append(this.childContextNames).append('\'');
        }
        sb.append("], jobDescription='").append(this.jobDescription).append('\'');
        sb.append(", startupControlType='").append(startupControlType).append('\'');
        if(this.skippedContexts != null) {
            sb.append(", skippedContexts=[ ");
            this.skippedContexts.entrySet()
                .forEach(id -> sb.append("[").append(id.getKey()).append(", ").append(id.getValue()).append("] "));
        }
        else {
            sb.append(", skippedContexts='").append(this.skippedContexts).append('\'');
        }
        if(this.heldContexts != null) {
            sb.append(", heldContexts=[ ");
            this.heldContexts.entrySet()
                .forEach(id -> sb.append("[").append(id.getKey()).append(", ").append(id.getValue()).append("] "));
        }
        else {
            sb.append(", heldContexts='").append(this.heldContexts).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }
}
