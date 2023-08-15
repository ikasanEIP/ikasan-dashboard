package org.ikasan.scheduled.job.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolrSchedulerJobImpl implements SchedulerJob {
    protected String jobIdentifier;
    protected String agentName;
    protected String jobName;
    protected String displayName;
    protected String jobDescription;
    protected String contextName;
    protected List<String> childContextNames;
    protected String startupControlType = "AUTOMATIC";
    @JsonIgnore
    protected boolean skip = false;
    protected Map<String, Boolean> skippedContexts = new HashMap<>();
    protected Map<String, Boolean> heldContexts = new HashMap<>();
    protected int ordinal = -1;

    @Override
    public String getContextName() {
        return this.contextName;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public List<String> getChildContextNames() {
        if(childContextNames == null) {
            childContextNames = new ArrayList<>();
        }
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
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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

    @JsonIgnore
    public boolean isSkip() {
        return skip;
    }

    @JsonIgnore
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
    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
