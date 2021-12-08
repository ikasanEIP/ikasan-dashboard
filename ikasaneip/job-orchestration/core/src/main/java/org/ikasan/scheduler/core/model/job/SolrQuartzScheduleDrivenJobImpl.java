package org.ikasan.scheduler.core.model.job;

import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;

public class SolrQuartzScheduleDrivenJobImpl extends SchedulerJobImpl implements QuartzScheduleDrivenJob {

    @Override
    public String getCronExpression() {
        return null;
    }

    @Override
    public void setCronExpression(String cronExpression) {

    }
}
