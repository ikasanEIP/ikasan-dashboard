package org.ikasan.job.orchestration.provision.job;

import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;

public class TestSchedulerJobRecord implements SchedulerJobRecord<SchedulerJob> {

    private SchedulerJob schedulerJob;

    public TestSchedulerJobRecord(SchedulerJob schedulerJob) {
        this.schedulerJob = schedulerJob;
    }

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
    public String getContextName() {
        return null;
    }

    @Override
    public SchedulerJob getJob() {
        return this.schedulerJob;
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
    public boolean isParticipatesInLock() {
        return false;
    }

    @Override
    public boolean isTargetResidingContextOnly() {
        return false;
    }
}
