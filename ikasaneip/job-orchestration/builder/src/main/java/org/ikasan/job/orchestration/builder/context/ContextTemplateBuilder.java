package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.builder.job.SchedulerJobBuilder;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.spec.scheduled.context.model.ContextDependency;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobDependency;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContextTemplateBuilder {
    protected String name;
    protected String description;
    protected String timezone;
    protected List<JobDependency> jobDependencies;
    protected List<ContextTemplate> contexts;
    protected List<ContextDependency> contextDependencies;
    protected List<ContextParameter> contextParameters;
    protected List<SchedulerJob> scheduledJobs;
    protected String timeWindowStartCronExpression;
    protected String timeWindowEndCronExpression;
    protected Map<String, List<SchedulerJob>> jobLocks;

    public ContextTemplateBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ContextTemplateBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ContextTemplateBuilder withTimezone(String timezone) {
        this.timezone = timezone;
        return this;
    }

    public ContextTemplateBuilder addJobDependency(JobDependency jobDependency) {
        if(this.jobDependencies == null) {
            this.jobDependencies = new ArrayList<>();
        }
        this.jobDependencies.add(jobDependency);
        return this;
    }

    public ContextTemplateBuilder addContext(ContextTemplate contextTemplate) {
        if(this.contexts == null) {
            this.contexts = new ArrayList<>();
        }
        this.contexts.add(contextTemplate);
        return this;
    }

    public ContextTemplateBuilder addContextDependency(ContextDependency contextDependency) {
        if(this.contextDependencies == null) {
            this.contextDependencies = new ArrayList<>();
        }
        this.contextDependencies.add(contextDependency);
        return this;
    }

    public ContextTemplateBuilder addContextParameter(ContextParameter contextParameter) {
        if(this.contextParameters == null) {
            this.contextParameters = new ArrayList<>();
        }
        this.contextParameters.add(contextParameter);
        return this;
    }

    public ContextTemplateBuilder addSchedulerJob(SchedulerJob schedulerJob) {
        if(this.scheduledJobs == null) {
            this.scheduledJobs = new ArrayList<>();
        }
        if(!scheduledJobs.contains(schedulerJob)) {
            this.scheduledJobs.add(schedulerJob);
        }
        return this;
    }

    public ContextTemplateBuilder addJobLocks(Map<String, List<SchedulerJob>> jobLocks) {
        if(this.jobLocks == null) {
            this.jobLocks = new HashMap<>();
        }

        this.jobLocks.putAll(jobLocks);

        return this;
    }

    public ContextTemplateBuilder withTimeWindowStartCronExpression(String timeWindowStartCronExpression) {
        this.timeWindowStartCronExpression = timeWindowStartCronExpression;
        return this;
    }

    public ContextTemplateBuilder withTimeWindowEndCronExpression(String timeWindowEndCronExpression) {
        this.timeWindowEndCronExpression = timeWindowEndCronExpression;
        return this;
    }

    public SchedulerJobBuilder getSchedulerJobBuilder() {
        return new SchedulerJobBuilder();
    }

    public JobDependencyBuilder getJobDependencyBuilder() {
        return new JobDependencyBuilder();
    }

    public LogicalGroupingBuilder getLogicalGroupingBuilder() {
        return new LogicalGroupingBuilder();
    }

    public JobAndBuilder getJobAndBuilder() {
        return new JobAndBuilder();
    }

    public JobOrBuilder getJobOrBuilder() {
        return new JobOrBuilder();
    }

    public JobNotBuilder getJobNotBuilder() {
        return new JobNotBuilder();
    }

    public ContextAndBuilder getContextAndBuilder() {
        return new ContextAndBuilder();
    }

    public ContextOrBuilder getContextOrBuilder() {
        return new ContextOrBuilder();
    }

    public ContextNotBuilder getContextNotBuilder() {
        return new ContextNotBuilder();
    }

    public ContextParameterBuilder getContextParameterBuilder() {
        return new ContextParameterBuilder();
    }

    public ContextDependencyBuilder getContextDependencyBuilder() {
        return new ContextDependencyBuilder();
    }

    public JobLockBuilder getJobLockBuilder() {
        return new JobLockBuilder();
    }

    public ContextTemplate build() {
        ContextTemplate contextTemplate = new ContextTemplateImpl();
        contextTemplate.setName(this.name);
        contextTemplate.setDescription(this.description);
        contextTemplate.setTimezone(this.timezone);
        contextTemplate.setTimeWindowStart(this.timeWindowStartCronExpression);
        contextTemplate.setTimeWindowEnd(this.timeWindowEndCronExpression);
        contextTemplate.setContextDependencies(this.contextDependencies);
        contextTemplate.setContexts(this.contexts);
        contextTemplate.setContextParameters(this.contextParameters);
        contextTemplate.setJobDependencies(this.jobDependencies);
        contextTemplate.setScheduledJobs(this.scheduledJobs);
        contextTemplate.setJobLocks(this.jobLocks);

        return contextTemplate;
    }
}

