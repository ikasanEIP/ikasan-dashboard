package org.ikasan.scheduled.job.model;

import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;

public class SolrFileEventDrivenJobImpl extends SolrSchedulerJobImpl implements FileEventDrivenJob {
    @Override
    public String getCronExpression() {
        return null;
    }

    @Override
    public void setCronExpression(String cronExpression) {

    }

    @Override
    public String getFilePath() {
        return null;
    }

    @Override
    public void setFilePath(String path) {

    }
}
