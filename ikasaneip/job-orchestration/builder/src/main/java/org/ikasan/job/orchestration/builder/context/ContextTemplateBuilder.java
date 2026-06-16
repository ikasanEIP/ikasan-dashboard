package org.ikasan.job.orchestration.builder.context;

import org.ikasan.job.orchestration.builder.job.SchedulerJobBuilder;
import org.ikasan.job.orchestration.builder.job.SchedulerJobLockParticipantBuilder;
import org.ikasan.job.orchestration.builder.util.ContextTemplateUtils;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.spec.scheduled.context.model.*;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.*;

public class ContextTemplateBuilder {
    protected String name;
    protected String description;
    protected String timezone;
    protected List<JobDependency> jobDependencies = new ArrayList<>();
    protected List<ContextTemplate> contexts = new ArrayList<>();
    protected List<ContextDependency> contextDependencies = new ArrayList<>();
    protected List<ContextParameter> contextParameters = new ArrayList<>();
    protected List<SchedulerJob> scheduledJobs = new ArrayList<>();
    protected String timeWindowStartCronExpression;
    protected boolean customWeekDayOfMonth = false;
    private boolean delayAgentSynchronisationUntilNextInstance = false;
    protected long contextTtlMilliseconds;
    protected boolean renderLogicalBoundaries = true;
    protected boolean renderOrLogicalBoundariesOnly = false;
    protected Map<Long, Long> blackoutWindowDateTimeRanges = new HashMap<>();
    protected List<String> blackoutWindowCronExpressions = new ArrayList<>();
    protected List<JobLock> jobLocks = new ArrayList<>();
    protected int treeViewExpandLevel = 1;
    protected boolean ableToRunConcurrently = true;
    private boolean useDisplayName = false;
    private int ordinal = -1;

    /**
     * Sets the name of the context template.
     *
     * @param name the name of the context template
     * @return the updated ContextTemplateBuilder object
     */
    public ContextTemplateBuilder withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the description of the context template builder.
     *
     * @param description the description to set
     * @return the updated ContextTemplateBuilder instance
     */
    public ContextTemplateBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets the timezone for the context template.
     *
     * @param timezone the timezone to set
     * @return the updated ContextTemplateBuilder object
     */
    public ContextTemplateBuilder withTimezone(String timezone) {
        this.timezone = timezone;
        return this;
    }

    /**
     *
     */
    public ContextTemplateBuilder addJobDependency(JobDependency jobDependency) {
        if(this.jobDependencies == null) {
            this.jobDependencies = new ArrayList<>();
        }
        this.jobDependencies.add(jobDependency);
        return this;
    }

    /**
     * Adds a context template to the list of contexts in the ContextTemplateBuilder.
     *
     * @param contextTemplate the context template to be added
     * @return the updated ContextTemplateBuilder object
     */
    public ContextTemplateBuilder addContext(ContextTemplate contextTemplate) {
        if(this.contexts == null) {
            this.contexts = new ArrayList<>();
        }
        this.contexts.add(contextTemplate);
        return this;
    }

    /**
     * Add a context dependency to the ContextTemplateBuilder.
     *
     * @param contextDependency the context dependency to add
     * @return the updated ContextTemplateBuilder object
     */
    public ContextTemplateBuilder addContextDependency(ContextDependency contextDependency) {
        if(this.contextDependencies == null) {
            this.contextDependencies = new ArrayList<>();
        }
        this.contextDependencies.add(contextDependency);
        return this;
    }

    /**
     *
     */
    public ContextTemplateBuilder addContextParameter(ContextParameter contextParameter) {
        if(this.contextParameters == null) {
            this.contextParameters = new ArrayList<>();
        }
        this.contextParameters.add(contextParameter);
        return this;
    }

    /**
     * Adds a scheduler job to the context template builder.
     *
     * @param schedulerJob the scheduler job to add
     * @return the updated ContextTemplateBuilder object
     */
    public ContextTemplateBuilder addSchedulerJob(SchedulerJob schedulerJob) {
        if(this.scheduledJobs == null) {
            this.scheduledJobs = new ArrayList<>();
        }
        if(!scheduledJobs.contains(schedulerJob)) {
            this.scheduledJobs.add(schedulerJob);
        }
        return this;
    }

    /**
     * Adds the given list of job locks to the current ContextTemplateBuilder object.
     *
     * @param jobLocks the list of job locks to add
     * @return the updated ContextTemplateBuilder object
     */
    public ContextTemplateBuilder addJobLocks(List<JobLock> jobLocks) {
        if(this.jobLocks == null) {
            this.jobLocks = new ArrayList<>();
        }

        this.jobLocks.addAll(jobLocks);

        return this;
    }

    /**
     * Sets the cron expression for the start of the time window in the context template builder.
     *
     * @param timeWindowStartCronExpression the cron expression for the start of the time window
     * @return the updated ContextTemplateBuilder instance
     */
    public ContextTemplateBuilder withTimeWindowStartCronExpression(String timeWindowStartCronExpression) {
        this.timeWindowStartCronExpression = timeWindowStartCronExpression;
        return this;
    }

    public ContextTemplateBuilder withCustomWeekDayOfMonth(boolean customWeekDayOfMonth) {
        this.customWeekDayOfMonth = customWeekDayOfMonth;
        return this;
    }

    public ContextTemplateBuilder withRenderLogicalBoundaries(boolean renderLogicalBoundaries) {
        this.renderLogicalBoundaries = renderLogicalBoundaries;
        return this;
    }

    public ContextTemplateBuilder withRenderOrLogicalBoundariesOnly(boolean renderOrLogicalBoundariesOnly) {
        this.renderOrLogicalBoundariesOnly = renderOrLogicalBoundariesOnly;
        return this;
    }

    /**
     * Sets whether to delay agent synchronisation until the next instance for the context template.
     *
     * @param delayAgentSynchronisationUntilNextInstance true to delay agent synchronisation until next instance, false otherwise
     * @return the updated ContextTemplateBuilder object
     */
    public ContextTemplateBuilder withDelayAgentSynchronisationUntilNextInstance
    (boolean delayAgentSynchronisationUntilNextInstance) {
        this.delayAgentSynchronisationUntilNextInstance = delayAgentSynchronisationUntilNextInstance;
        return this;
    }

    /**
     * Sets the time-to-live (TTL) of the context template in milliseconds.
     * This determines how long the context template will persist before being automatically expired.
     *
     * @param contextTtlMilliseconds the time-to-live (TTL) of the context template in milliseconds
     * @return the updated ContextTemplateBuilder object
     */
    public ContextTemplateBuilder withContextTtlMilliseconds(long contextTtlMilliseconds) {
        this.contextTtlMilliseconds = contextTtlMilliseconds;
        return this;
    }

    /**
     * Adds a blackout window cron expression to the ContextTemplateBuilder.
     * Blackout windows are time intervals during which the context template should not run.
     *
     * @param blackoutWindowCronExpression the cron expression specifying the blackout window
     * @return the updated ContextTemplateBuilder object
     */
    public ContextTemplateBuilder withBlackoutWindowCronExpression(String blackoutWindowCronExpression) {
        this.blackoutWindowCronExpressions.add(blackoutWindowCronExpression);
        return this;
    }

    /**
     * Sets a blackout date time window for the context template.
     *
     * @param windowStart the start time of the blackout window in milliseconds
     * @param windowEnd the end time of the blackout window in milliseconds
     * @return the updated ContextTemplateBuilder object
     */
    public ContextTemplateBuilder withBlackoutDateTimeWindow(long windowStart, long windowEnd) {
        this.blackoutWindowDateTimeRanges.put(windowStart, windowEnd);
        return this;
    }

    /**
     * Sets the expand level of the tree view in the context template builder.
     *
     * @param treeViewExpandLevel the expand level of the tree view
     * @return the updated ContextTemplateBuilder object
     */
    public ContextTemplateBuilder withTreeViewExpandLevel(int treeViewExpandLevel) {
        this.treeViewExpandLevel = treeViewExpandLevel;
        return this;
    }

    /**
     * Sets the ability to run the context template concurrently.
     *
     * @param ableToRunConcurrently boolean value indicating whether the context template can run concurrently
     * @return the updated ContextTemplateBuilder object
     */
    public ContextTemplateBuilder isAbleToRunConcurrently(boolean ableToRunConcurrently) {
        this.ableToRunConcurrently = ableToRunConcurrently;
        return this;
    }

    /**
     * Sets whether to use display name.
     *
     * @param useDisplayName true to use display name, false otherwise
     * @return the updated ContextTemplateBuilder object
     */
    public ContextTemplateBuilder useDisplayName(boolean useDisplayName) {
        this.useDisplayName = useDisplayName;
        return this;
    }

    /**
     * Sets the ordinal value for the ContextTemplateBuilder object.
     *
     * @param ordinal the ordinal value to set
     * @return the updated ContextTemplateBuilder object
     */
    public ContextTemplateBuilder withOrdinal(int ordinal) {
        this.ordinal = ordinal;
        return this;
    }

    /**
     * Returns a new instance of SchedulerJobBuilder.
     *
     * @return a new instance of SchedulerJobBuilder
     */
    public SchedulerJobBuilder getSchedulerJobBuilder() {
        return new SchedulerJobBuilder();
    }

    /**
     * Returns a new instance of SchedulerJobLockParticipantBuilder.
     *
     * @return a new instance of SchedulerJobLockParticipantBuilder
     */
    public SchedulerJobLockParticipantBuilder getSchedulerJobLockParticipantBuilder() {
        return new SchedulerJobLockParticipantBuilder();
    }

    /**
     *
     */
    public JobDependencyBuilder getJobDependencyBuilder() {
        return new JobDependencyBuilder();
    }

    /**
     * Returns a new instance of LogicalGroupingBuilder.
     *
     * @return a new instance of LogicalGroupingBuilder
     */
    public LogicalGroupingBuilder getLogicalGroupingBuilder() {
        return new LogicalGroupingBuilder();
    }

    /**
     *
     */
    public JobAndBuilder getJobAndBuilder() {
        return new JobAndBuilder();
    }

    /**
     * Returns a JobOrBuilder object.
     *
     * @return the JobOrBuilder object
     */
    public JobOrBuilder getJobOrBuilder() {
        return new JobOrBuilder();
    }

    /**
     * Retrieves a JobNotBuilder object.
     *
     * @return a new instance of JobNotBuilder
     */
    public JobNotBuilder getJobNotBuilder() {
        return new JobNotBuilder();
    }

    /**
     * Retrieves a new instance of ContextAndBuilder.
     *
     * @return a new instance of ContextAndBuilder
     */
    public ContextAndBuilder getContextAndBuilder() {
        return new ContextAndBuilder();
    }

    /**
     * Retrieves the ContextOrBuilder object.
     *
     * @return the ContextOrBuilder object
     */
    public ContextOrBuilder getContextOrBuilder() {
        return new ContextOrBuilder();
    }

    /**
     * Retrieves a new instance of the ContextNotBuilder class.
     *
     * @return A new instance of the ContextNotBuilder class.
     */
    public ContextNotBuilder getContextNotBuilder() {
        return new ContextNotBuilder();
    }

    /**
     * Returns a new instance of ContextParameterBuilder.
     *
     * @return a new instance of ContextParameterBuilder
     */
    public ContextParameterBuilder getContextParameterBuilder() {
        return new ContextParameterBuilder();
    }

    /**
     *
     */
    public ContextDependencyBuilder getContextDependencyBuilder() {
        return new ContextDependencyBuilder();
    }

    /**
     * Retrieves a JobLockBuilder object.
     *
     * @return a JobLockBuilder object
     */
    public JobLockBuilder getJobLockBuilder() {
        return new JobLockBuilder();
    }

    /**
     * Builds a ContextTemplate object with the provided values.
     *
     * @return the built ContextTemplate object
     */
    public ContextTemplate build() {
        ContextTemplate contextTemplate = new ContextTemplateImpl();
        contextTemplate.setName(this.name);
        contextTemplate.setDescription(this.description);
        contextTemplate.setTimezone(this.timezone);
        contextTemplate.setTimeWindowStart(this.timeWindowStartCronExpression);
        contextTemplate.setCustomWeekDayOfMonth(this.customWeekDayOfMonth);
        contextTemplate.setContextTtlMilliseconds(this.contextTtlMilliseconds);
        contextTemplate.setContextDependencies(this.contextDependencies);
        contextTemplate.setContexts(ContextTemplateUtils.setOrdinalsInContextTemplates(this.contexts));
        contextTemplate.setContextParameters(this.contextParameters);
        contextTemplate.setJobDependencies(this.jobDependencies);
        contextTemplate.setScheduledJobs(this.scheduledJobs);
        contextTemplate.setJobLocks(this.jobLocks);
        contextTemplate.setBlackoutWindowCronExpressions(this.blackoutWindowCronExpressions);
        contextTemplate.setBlackoutWindowDateTimeRanges(this.blackoutWindowDateTimeRanges);
        contextTemplate.setTreeViewExpandLevel(this.treeViewExpandLevel);
        contextTemplate.setAbleToRunConcurrently(this.ableToRunConcurrently);
        contextTemplate.setUseDisplayName(this.useDisplayName);
        contextTemplate.setOrdinal(this.ordinal);
        contextTemplate.setDelayAgentSynchronisationUntilNextInstance(this.delayAgentSynchronisationUntilNextInstance);
        contextTemplate.setRenderLogicalBoundaries(this.renderLogicalBoundaries);
        contextTemplate.setRenderOrLogicalBoundariesOnly(this.renderOrLogicalBoundariesOnly);
        return contextTemplate;
    }

}

