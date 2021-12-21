package org.ikasan.scheduled.job.model;

import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;

public class SolrFileEventDrivenJobImpl extends SolrQuartzScheduleDrivenJobImpl implements FileEventDrivenJob {

    private String filePath;

    @Override
    public String getFilePath() {
        return this.filePath;
    }

    @Override
    public void setFilePath(String path) {
        this.filePath = path;
    }
}
