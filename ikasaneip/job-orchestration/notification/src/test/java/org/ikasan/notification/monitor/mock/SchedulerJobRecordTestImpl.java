package org.ikasan.notification.monitor.mock;

import org.ikasan.scheduled.job.model.SolrFileEventDrivenJobImpl;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;

public class SchedulerJobRecordTestImpl implements SchedulerJobRecord {
    
    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public String getAgentName() {
        return null;
    }

    @Override
    public String getJobName() {
        return null;
    }

    @Override
    public String getContextId() {
        return null;
    }

    @Override
    public SchedulerJob getJob() {
        FileEventDrivenJob fileEventDrivenJob = new SolrFileEventDrivenJobImpl();
        fileEventDrivenJob.setCronExpression("0 0/1 08-23 ? * MON-SUN *");
        return fileEventDrivenJob;
    }

    @Override
    public long getTimestamp() {
        return 0;
    }

    @Override
    public long getModifiedTimestamp() {
        return 0;
    }

    @Override
    public void setModifiedTimestamp(long timestamp) {

    }

    @Override
    public String getModifiedBy() {
        return null;
    }

    @Override
    public void setModifiedBy(String modifiedBy) {

    }
}
