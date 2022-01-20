package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.model.job.QuartzScheduleDrivenJobImpl;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;

public class QuartzScheduleDrivenJobBuilder extends SchedulerJobBuilder {
    protected String cronExpression;
    protected String jobGroup;
    protected String timeZone;

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

    public QuartzScheduleDrivenJob build() {
        QuartzScheduleDrivenJob quartzScheduleDrivenJob = new QuartzScheduleDrivenJobImpl();
        quartzScheduleDrivenJob.setAgentName(super.agentName);
        quartzScheduleDrivenJob.setIdentifier(super.agentName+"-"+super.jobName);
        quartzScheduleDrivenJob.setJobDescription(super.description);
        quartzScheduleDrivenJob.setJobName(super.jobName);
        quartzScheduleDrivenJob.setContextId(super.contextId);
        quartzScheduleDrivenJob.setCronExpression(this.cronExpression);
        quartzScheduleDrivenJob.setTimeZone(this.timeZone);
        quartzScheduleDrivenJob.setJobGroup(this.jobGroup);

        return quartzScheduleDrivenJob;
    }
}
