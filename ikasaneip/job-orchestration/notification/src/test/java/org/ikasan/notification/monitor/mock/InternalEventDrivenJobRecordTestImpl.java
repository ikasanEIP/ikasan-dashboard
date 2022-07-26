package org.ikasan.notification.monitor.mock;

import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;

public class InternalEventDrivenJobRecordTestImpl implements InternalEventDrivenJobRecord {
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
    public String getContextId() {
        return null;
    }

    @Override
    public void setContextId(String contextId) {

    }

    @Override
    public InternalEventDrivenJob getInternalEventDrivenJob() {
        InternalEventDrivenJob job = new InternalEventDrivenJobImpl();
        job.setMinExecutionTime(200);
        job.setMaxExecutionTime(25000);
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
}
