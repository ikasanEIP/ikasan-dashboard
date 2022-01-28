package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.model.job.QuartzScheduleDrivenJobImpl;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;

import java.util.Map;

public class QuartzScheduleDrivenJobBuilder extends SchedulerJobBuilder {
    protected String cronExpression;
    protected String jobGroup;
    protected String timeZone;
    protected boolean ignoreMisfire = true;
    protected boolean eager = false;
    protected int maxEagerCallbacks;
    protected Map<String,String> passthroughProperties;
    protected boolean persistentRecovery = true;
    protected long recoveryTolerance = 30 * 60 * 1000;

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

    public QuartzScheduleDrivenJob build() {
        QuartzScheduleDrivenJob quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
        quartzScheduleDrivenJob.setAgentName(super.agentName);
        quartzScheduleDrivenJob.setIdentifier(super.agentName+"-"+super.jobName);
        quartzScheduleDrivenJob.setJobDescription(super.description);
        quartzScheduleDrivenJob.setJobName(super.jobName);
        quartzScheduleDrivenJob.setContextId(super.contextId);
        quartzScheduleDrivenJob.setStartupControlType(super.startupControlType);
        quartzScheduleDrivenJob.setCronExpression(this.cronExpression);
        quartzScheduleDrivenJob.setTimeZone(this.timeZone);
        quartzScheduleDrivenJob.setJobGroup(this.jobGroup);
        quartzScheduleDrivenJob.setIgnoreMisfire(this.ignoreMisfire);
        quartzScheduleDrivenJob.setMaxEagerCallbacks(this.maxEagerCallbacks);
        quartzScheduleDrivenJob.setEager(this.eager);
        quartzScheduleDrivenJob.setPassthroughProperties(this.passthroughProperties);
        quartzScheduleDrivenJob.setPersistentRecovery(this.persistentRecovery);
        quartzScheduleDrivenJob.setRecoveryTolerance(this.recoveryTolerance);

        return quartzScheduleDrivenJob;
    }
}
