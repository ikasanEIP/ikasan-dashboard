package org.ikasan.scheduler.core.model.job;

import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;

public class FileEventDrivenJobImpl extends SchedulerJobImpl implements FileEventDrivenJob {

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
