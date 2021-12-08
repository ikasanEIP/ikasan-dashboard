package org.ikasan.scheduler.core.model.job;

import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;

import java.util.List;

public class InternalEventDrivenJobImpl extends SchedulerJobImpl implements InternalEventDrivenJob {
    @Override
    public List<String> getSuccessfulReturnCodes() {
        return null;
    }

    @Override
    public void setSuccessfulReturnCodes(List<String> successfulReturnCodes) {

    }

    @Override
    public long getSecondsToWaitForProcessStart() {
        return 0;
    }

    @Override
    public void setSecondsToWaitForProcessStart(long secondsToWaitForProcessStart) {

    }

    @Override
    public String getWorkingDirectory() {
        return null;
    }

    @Override
    public void setWorkingDirectory(String workingDirectory) {

    }

    @Override
    public String getCommandLine() {
        return null;
    }

    @Override
    public void setCommandLine(String commandLine) {

    }

    @Override
    public String getStdErr() {
        return null;
    }

    @Override
    public void setStdErr(String stdErr) {

    }

    @Override
    public String getStdOut() {
        return null;
    }

    @Override
    public void setStdOut(String stdOut) {

    }

    @Override
    public boolean isRetryOnFail() {
        return false;
    }

    @Override
    public void setRetryOnFail(boolean retryOnFail) {

    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("InternalEventDrivenJobImpl{");
        sb.append("jobIdentifier='").append(jobIdentifier).append('\'');
        sb.append(", agentName='").append(agentName).append('\'');
        sb.append(", jobName='").append(jobName).append('\'');
        sb.append(", contextId='").append(contextId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
