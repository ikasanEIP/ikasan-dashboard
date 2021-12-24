package org.ikasan.scheduler.core.model.context;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.ContextDependency;
import org.ikasan.spec.scheduled.context.model.JobDependency;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContextImpl<CONTEXT extends Context, CONTEXT_PARAM, JOB extends SchedulerJob> implements Context<CONTEXT, CONTEXT_PARAM, JOB> {
    protected String name;
    protected List<JobDependency> jobDependencies;
    protected List<CONTEXT> contexts;
    protected List<ContextDependency> contextDependencies;
    protected List<CONTEXT_PARAM> contextParameters;
    protected List<JOB> scheduledJobs;
    protected String timeWindowStart;
    protected String timeWindowEnd;

    @JsonIgnore
    protected Map<String, JOB> scheduledJobsMap = new HashMap<>();
    @JsonIgnore
    protected Map<String, CONTEXT> contextsMap = new HashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<CONTEXT_PARAM> getContextParameters() {
        return contextParameters;
    }

    public void setContextParameters(List<CONTEXT_PARAM> contextParameters) {
        this.contextParameters = contextParameters;
    }

    public List<JOB> getScheduledJobs() {
        return scheduledJobs;
    }

    public void setScheduledJobs(List<JOB> scheduledJobs) {
        this.scheduledJobs = scheduledJobs;
        if(scheduledJobs != null) {
            this.scheduledJobsMap = this.scheduledJobs.stream()
                .collect(Collectors.toMap(item -> item.getIdentifier()   , item -> item));
        }
    }

    public List<JobDependency> getJobDependencies() {
        return jobDependencies;
    }

    public void setJobDependencies(List<JobDependency> jobDependencies) {
        this.jobDependencies = jobDependencies;
    }

    public List<CONTEXT> getContexts() {
        return contexts;
    }

    public void setContexts(List<CONTEXT> contexts) {
        this.contexts = contexts;
        if(this.contexts != null) {
            this.contextsMap = this.contexts.stream()
                .collect(Collectors.toMap(item -> item.getName(), item -> item));
        }
    }

    public List<ContextDependency> getContextDependencies() {
        return contextDependencies;
    }

    public void setContextDependencies(List<ContextDependency> contextDependencies) {
        this.contextDependencies = contextDependencies;
    }

    public Map<String, JOB> getScheduledJobsMap() {
        return scheduledJobsMap;
    }

    public Map<String, CONTEXT> getContextsMap() {
        return contextsMap;
    }

    public String getTimeWindowStart() {
        return timeWindowStart;
    }

    public void setTimeWindowStart(String timeWindowStart) {
        this.timeWindowStart = timeWindowStart;
    }

    public String getTimeWindowEnd() {
        return timeWindowEnd;
    }

    public void setTimeWindowEnd(String timeWindowEnd) {
        this.timeWindowEnd = timeWindowEnd;
    }
}
