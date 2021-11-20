package org.ikasan.scheduler.core.spec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.ikasan.scheduler.core.model.context.ContextDependency;
import org.ikasan.scheduler.core.model.context.JobDependency;
import org.ikasan.scheduler.core.model.context.SchedulerJob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Context<CONTEXT extends Context, CONTEXT_PARAM, JOB extends SchedulerJob> {
    protected String name;
    protected List<JobDependency> jobDependencies;
    protected List<CONTEXT> contexts;
    protected List<ContextDependency> contextDependencies;
    protected List<CONTEXT_PARAM> contextParameters;
    protected List<JOB> scheduledJobs;
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
                .collect(Collectors.toMap(item -> item.getIdentifier() , item -> item));
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
}
