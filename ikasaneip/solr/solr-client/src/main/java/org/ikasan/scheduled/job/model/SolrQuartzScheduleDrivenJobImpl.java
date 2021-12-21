package org.ikasan.scheduled.job.model;

import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;

public class SolrQuartzScheduleDrivenJobImpl extends SolrSchedulerJobImpl implements QuartzScheduleDrivenJob {

    private String jobGroup;
    private String cronExpression;
    private String timeZone;

    @Override
    public String getJobGroup() {
        return this.jobGroup;
    }

    @Override
    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    @Override
    public String getCronExpression() {
        return this.cronExpression;
    }

    @Override
    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    @Override
    public String getTimeZone() {
        return this.timeZone;
    }

    @Override
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }
}
