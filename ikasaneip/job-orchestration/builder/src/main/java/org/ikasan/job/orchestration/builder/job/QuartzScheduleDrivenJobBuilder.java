package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.builder.context.ContextTemplateBuilder;
import org.ikasan.job.orchestration.model.job.QuartzScheduleDrivenJobImpl;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuartzScheduleDrivenJobBuilder {
    protected String agentName;
    protected String jobName;
    protected String displayName;
    protected String contextName;
    protected List<String> childContextNames;
    protected String description;
    protected String startupControlType = "AUTOMATIC";
    protected String cronExpression;
    protected String jobGroup;
    protected String timeZone;
    protected boolean ignoreMisfire = true;
    protected boolean eager = false;
    protected int maxEagerCallbacks;
    protected Map<String,String> passthroughProperties;
    protected boolean persistentRecovery = true;
    protected long recoveryTolerance = 30 * 60 * 1000;
    protected Map<String, String> blackoutWindowDateTimeRanges = new HashMap<>();
    protected List<String> blackoutWindowCronExpressions = new ArrayList<>();

    /**
     * Sets the agent name for the QuartzScheduleDrivenJobBuilder.
     *
     * @param agentName The name of the agent.
     * @return The QuartzScheduleDrivenJobBuilder instance.
     */
    public QuartzScheduleDrivenJobBuilder withAgentName(String agentName) {
        this.agentName = agentName;

        return this;
    }

    /**
     * Sets the job name for the Quartz schedule-driven job.
     *
     * @param jobName the job name to set
     * @return the updated QuartzScheduleDrivenJobBuilder instance
     */
    public QuartzScheduleDrivenJobBuilder withJobName(String jobName) {
        this.jobName = jobName;

        return this;
    }

    /**
     * Sets the context name for the Quartz schedule driven job builder.
     *
     * @param contextName the context name to set
     * @return the QuartzScheduleDrivenJobBuilder instance
     */
    public QuartzScheduleDrivenJobBuilder withContextName(String contextName) {
        this.contextName = contextName;

        return this;
    }

    /**
     * Adds a child context ID to the QuartzScheduleDrivenJobBuilder.
     *
     * @param childContextId the ID of the child context to add
     * @return the updated QuartzScheduleDrivenJobBuilder
     */
    public QuartzScheduleDrivenJobBuilder addChildContextId(String childContextId) {
        if(this.childContextNames == null) {
            this.childContextNames = new ArrayList<>();
        }

        this.childContextNames.add(childContextId);

        return this;
    }

    /**
     * Sets the description for the QuartzScheduleDrivenJobBuilder instance.
     *
     * @param description the description of the job
     * @return the QuartzScheduleDrivenJobBuilder instance
     */
    public QuartzScheduleDrivenJobBuilder withDescription(String description) {
        this.description = description;

        return this;
    }

    /**
     * Sets the startup control type for the QuartzScheduleDrivenJobBuilder.
     *
     * @param startupControlType the startup control type to set
     * @return the updated QuartzScheduleDrivenJobBuilder
     */
    public QuartzScheduleDrivenJobBuilder withStartupControlType(String startupControlType) {
        this.startupControlType = startupControlType;

        return this;
    }

    /**
     * Sets the cron expression for the schedule-driven job.
     *
     * @param cronExpression the cron expression to set
     * @return the QuartzScheduleDrivenJobBuilder instance
     */
    public QuartzScheduleDrivenJobBuilder withCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;

        return this;
    }

    /**
     * Sets the job group of the QuartzScheduleDrivenJobBuilder.
     *
     * @param jobGroup The job group to set.
     * @return The QuartzScheduleDrivenJobBuilder instance.
     */
    public QuartzScheduleDrivenJobBuilder withJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;

        return this;
    }

    /**
     * Sets the time zone for the QuartzScheduleDrivenJobBuilder.
     *
     * @param timeZone the time zone to set for the job builder
     * @return the QuartzScheduleDrivenJobBuilder with the updated time zone
     */
    public QuartzScheduleDrivenJobBuilder withTimeZone(String timeZone) {
        this.timeZone = timeZone;

        return this;
    }

    /**
     * Sets whether to ignore misfires when scheduling the job.
     *
     * @param ignoreMisfire {@code true} to ignore misfires, {@code false} otherwise
     * @return the updated {@link QuartzScheduleDrivenJobBuilder} instance
     */
    public QuartzScheduleDrivenJobBuilder withIgnoreMisfire(boolean ignoreMisfire) {
        this.ignoreMisfire = ignoreMisfire;

        return this;
    }

    /**
     * Sets the maximum number of eager callbacks for the Quartz schedule-driven job.
     *
     * @param maxEagerCallbacks The maximum number of eager callbacks.
     * @return The QuartzScheduleDrivenJobBuilder instance.
     */
    public QuartzScheduleDrivenJobBuilder withMaxEagerCallbacks(int maxEagerCallbacks) {
        this.maxEagerCallbacks = maxEagerCallbacks;

        return this;
    }

    /**
     * Sets the eager flag for the QuartzScheduleDrivenJobBuilder.
     *
     * @param eager the value of the eager flag
     * @return the updated QuartzScheduleDrivenJobBuilder instance
     */
    public QuartzScheduleDrivenJobBuilder withEager(boolean eager) {
        this.eager = eager;

        return this;
    }

    /**
     * Sets the passthrough properties for the Quartz Schedule Driven job.
     *
     * @param passthroughProperties a map containing the passthrough properties to be set
     * @return the current instance of QuartzScheduleDrivenJobBuilder
     */
    public QuartzScheduleDrivenJobBuilder withPassthroughProperties(Map<String, String> passthroughProperties) {
        this.passthroughProperties = passthroughProperties;

        return this;
    }

    /**
     * Sets the value for persistent recovery in the QuartzScheduleDrivenJobBuilder.
     * If persistent recovery is enabled, the job will be persisted and can be recovered upon restart.
     *
     * @param persistentRecovery true to enable persistent recovery, false otherwise
     * @return the QuartzScheduleDrivenJobBuilder instance
     */
    public QuartzScheduleDrivenJobBuilder withPersistentRecovery(boolean persistentRecovery) {
        this.persistentRecovery = persistentRecovery;

        return this;
    }

    /**
     * Sets the recovery tolerance for the job. The recovery tolerance specifies the maximum
     * allowable time (in milliseconds) for a job to recover after a failure. If the recovery
     * time exceeds the tolerance, the job will be considered failed.
     *
     * @param recoveryTolerance the recovery tolerance in milliseconds
     * @return the QuartzScheduleDrivenJobBuilder instance
     */
    public QuartzScheduleDrivenJobBuilder withRecoveryTolerance(long recoveryTolerance) {
        this.recoveryTolerance = recoveryTolerance;

        return this;
    }

    /**
     * Adds a blackout window cron expression to the QuartzScheduleDrivenJobBuilder.
     *
     * @param blackoutWindowCronExpression the cron expression representing the blackout window
     * @return the QuartzScheduleDrivenJobBuilder with the blackout window cron expression added
     */
    public QuartzScheduleDrivenJobBuilder withBlackoutWindowCronExpression(String blackoutWindowCronExpression) {
        this.blackoutWindowCronExpressions.add(blackoutWindowCronExpression);
        return this;
    }

    /**
     * Sets the blackout date time window for the Quartz schedule driven job builder.
     *
     * @param windowStart the start time of the blackout window in milliseconds
     * @param windowEnd the end time of the blackout window in milliseconds
     * @return the Quartz schedule driven job builder with the updated blackout date time window
     */
    public QuartzScheduleDrivenJobBuilder withBlackoutDateTimeWindow(long windowStart, long windowEnd) {
        this.blackoutWindowDateTimeRanges.put(String.valueOf(windowStart), String.valueOf(windowEnd));
        return this;
    }

    /**
     * Sets the display name of the Quartz schedule driven job.
     *
     * @param displayName the display name of the job
     * @return the QuartzScheduleDrivenJobBuilder instance with the display name set
     */
    public QuartzScheduleDrivenJobBuilder withDisplayName(String displayName) {
        this.displayName = displayName;

        return this;
    }

    /**
     * Builds a QuartzScheduleDrivenJob instance with the configured properties.
     *
     * @return a QuartzScheduleDrivenJob instance
     */
    public QuartzScheduleDrivenJob build() {
        QuartzScheduleDrivenJob quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
        quartzScheduleDrivenJob.setAgentName(this.agentName);
        quartzScheduleDrivenJob.setIdentifier(this.agentName+"-"+this.jobName);
        quartzScheduleDrivenJob.setJobDescription(this.description);
        quartzScheduleDrivenJob.setJobName(this.jobName);
        quartzScheduleDrivenJob.setContextName(this.contextName);
        quartzScheduleDrivenJob.setChildContextNames(this.childContextNames);
        quartzScheduleDrivenJob.setStartupControlType(this.startupControlType);
        quartzScheduleDrivenJob.setCronExpression(this.cronExpression);
        quartzScheduleDrivenJob.setTimeZone(this.timeZone);
        quartzScheduleDrivenJob.setJobGroup(this.jobGroup);
        quartzScheduleDrivenJob.setIgnoreMisfire(this.ignoreMisfire);
        quartzScheduleDrivenJob.setMaxEagerCallbacks(this.maxEagerCallbacks);
        quartzScheduleDrivenJob.setEager(this.eager);
        quartzScheduleDrivenJob.setPassthroughProperties(this.passthroughProperties);
        quartzScheduleDrivenJob.setPersistentRecovery(this.persistentRecovery);
        quartzScheduleDrivenJob.setRecoveryTolerance(this.recoveryTolerance);
        quartzScheduleDrivenJob.setBlackoutWindowCronExpressions(this.blackoutWindowCronExpressions);
        quartzScheduleDrivenJob.setBlackoutWindowDateTimeRanges(this.blackoutWindowDateTimeRanges);
        quartzScheduleDrivenJob.setDisplayName(this.displayName);

        return quartzScheduleDrivenJob;
    }
}
