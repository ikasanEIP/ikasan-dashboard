package org.ikasan.scheduled.job.model;

import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;

public class SolrQuartzScheduleDrivenJobImpl extends SolrSchedulerJobImpl implements QuartzScheduleDrivenJob {
    @Override
    public String getCronExpression() {
        return null;
    }

    @Override
    public void setCronExpression(String cronExpression) {

    }
}
