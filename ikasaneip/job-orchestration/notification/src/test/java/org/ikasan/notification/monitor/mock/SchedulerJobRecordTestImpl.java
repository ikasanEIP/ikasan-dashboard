package org.ikasan.notification.monitor.mock;

import org.ikasan.job.orchestration.model.job.FileEventDrivenJobImpl;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;

import java.util.Arrays;

public class SchedulerJobRecordTestImpl implements SchedulerJobRecord {
    
    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getDisplayName() {
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
    public String getContextName() {
        return null;
    }

    @Override
    public SchedulerJob getJob() {
        FileEventDrivenJob fileEventDrivenJob = new FileEventDrivenJobImpl();
        fileEventDrivenJob.setCronExpression("0 0/1 05-23 ? * MON-SUN *");
        fileEventDrivenJob.setChildContextNames(Arrays.asList("context-instance-1"));
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
    public String getModifiedBy() {
        return null;
    }

    @Override
    public boolean isHeld() {
        return false;
    }

    @Override
    public boolean isSkipped() {
        return false;
    }

    @Override
    public boolean isTargetResidingContextOnly() {
        return false;
    }

    @Override
    public boolean isParticipatesInLock() {
        return false;
    }
}
