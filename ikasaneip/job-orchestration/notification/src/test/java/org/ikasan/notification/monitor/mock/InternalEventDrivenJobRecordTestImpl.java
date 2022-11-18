package org.ikasan.notification.monitor.mock;

import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;

public class InternalEventDrivenJobRecordTestImpl implements InternalEventDrivenJobRecord {

    private long min;
    private long max;

    public InternalEventDrivenJobRecordTestImpl(long min, long max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getAgentName() {
        return null;
    }

    @Override
    public void setAgentName(String agentName) {

    }

    @Override
    public String getJobName() {
        return "job-1";
    }

    @Override
    public void setJobName(String jobName) {

    }

    @Override
    public String getContextName() {
        return null;
    }

    @Override
    public void setContextName(String contextName) {

    }

    @Override
    public InternalEventDrivenJob getInternalEventDrivenJob() {
        InternalEventDrivenJob job = new InternalEventDrivenJobImpl();
        job.setMinExecutionTime(min);
        job.setMaxExecutionTime(max);
        job.setJobName("job-1");
        return job;
    }

    @Override
    public void setInternalEventDrivenJob(InternalEventDrivenJob internalEventDrivenJob) {

    }

    @Override
    public long getTimestamp() {
        return 0;
    }

    @Override
    public void setTimestamp(long timestamp) {

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

    @Override
    public boolean isHeld() {
        return false;
    }

    @Override
    public void setHeld(boolean held) {

    }

    @Override
    public boolean isSkipped() {
        return false;
    }

    @Override
    public void setSkipped(boolean skipped) {

    }

    @Override
    public void setTargetResidingContextOnly(boolean targetResidingContextOnly) {

    }

    @Override
    public boolean isTargetResidingContextOnly() {
        return false;
    }

    @Override
    public void setParticipatesInLock(boolean participatesInLock) {

    }

    @Override
    public boolean isParticipatesInLock() {
        return false;
    }
}
