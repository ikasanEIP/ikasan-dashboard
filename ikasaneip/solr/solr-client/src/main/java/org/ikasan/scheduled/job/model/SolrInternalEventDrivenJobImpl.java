package org.ikasan.scheduled.job.model;

import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;

import java.util.List;

public class SolrInternalEventDrivenJobImpl extends SolrSchedulerJobImpl implements InternalEventDrivenJob {
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
}
