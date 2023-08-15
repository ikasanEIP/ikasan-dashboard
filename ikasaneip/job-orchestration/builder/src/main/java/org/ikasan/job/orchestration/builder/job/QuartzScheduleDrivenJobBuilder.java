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
     * Set the agent name.
     *
     * @param agentName
     * @return
     */
    public QuartzScheduleDrivenJobBuilder withAgentName(String agentName) {
        this.agentName = agentName;

        return this;
    }

    /**
     * Set the job name.
     *
     * @param jobName
     * @return
     */
    public QuartzScheduleDrivenJobBuilder withJobName(String jobName) {
        this.jobName = jobName;

        return this;
    }

    /**
     * Set the context name.
     *
     * @param contextName
     * @return
     */
    public QuartzScheduleDrivenJobBuilder withContextName(String contextName) {
        this.contextName = contextName;

        return this;
    }

    /**
     * Add a child context id.
     *
     * @param childContextId
     * @return
     */
    public QuartzScheduleDrivenJobBuilder addChildContextId(String childContextId) {
        if(this.childContextNames == null) {
            this.childContextNames = new ArrayList<>();
        }

        this.childContextNames.add(childContextId);

        return this;
    }

    /**
     * Set the job description.
     *
     * @param description
     * @return
     */
    public QuartzScheduleDrivenJobBuilder withDescription(String description) {
        this.description = description;

        return this;
    }

    /**
     * Set the job startupControlType.
     *
     * @param startupControlType
     * @return
     */
    public QuartzScheduleDrivenJobBuilder withStartupControlType(String startupControlType) {
        this.startupControlType = startupControlType;

        return this;
    }

    public QuartzScheduleDrivenJobBuilder withCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;

        return this;
    }

    public QuartzScheduleDrivenJobBuilder withJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;

        return this;
    }

    public QuartzScheduleDrivenJobBuilder withTimeZone(String timeZone) {
        this.timeZone = timeZone;

        return this;
    }

    public QuartzScheduleDrivenJobBuilder withIgnoreMisfire(boolean ignoreMisfire) {
        this.ignoreMisfire = ignoreMisfire;

        return this;
    }

    public QuartzScheduleDrivenJobBuilder withMaxEagerCallbacks(int maxEagerCallbacks) {
        this.maxEagerCallbacks = maxEagerCallbacks;

        return this;
    }

    public QuartzScheduleDrivenJobBuilder withEager(boolean eager) {
        this.eager = eager;

        return this;
    }

    public QuartzScheduleDrivenJobBuilder withPassthroughProperties(Map<String, String> passthroughProperties) {
        this.passthroughProperties = passthroughProperties;

        return this;
    }

    public QuartzScheduleDrivenJobBuilder withPersistentRecovery(boolean persistentRecovery) {
        this.persistentRecovery = persistentRecovery;

        return this;
    }

    public QuartzScheduleDrivenJobBuilder withRecoveryTolerance(long recoveryTolerance) {
        this.recoveryTolerance = recoveryTolerance;

        return this;
    }

    public QuartzScheduleDrivenJobBuilder withBlackoutWindowCronExpression(String blackoutWindowCronExpression) {
        this.blackoutWindowCronExpressions.add(blackoutWindowCronExpression);
        return this;
    }

    public QuartzScheduleDrivenJobBuilder withBlackoutDateTimeWindow(long windowStart, long windowEnd) {
        this.blackoutWindowDateTimeRanges.put(String.valueOf(windowStart), String.valueOf(windowEnd));
        return this;
    }

    public QuartzScheduleDrivenJobBuilder withDisplayName(String displayName) {
        this.displayName = displayName;

        return this;
    }

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
