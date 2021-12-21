package org.ikasan.scheduled.job.model;

import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;

import java.util.List;

public class SolrInternalEventDrivenJobImpl extends SolrSchedulerJobImpl implements InternalEventDrivenJob {

    private List<String> successfulReturnCodes;
    private String workingDirectory;
    private String commandLine;

    @Override
    public List<String> getSuccessfulReturnCodes() {
        return this.successfulReturnCodes;
    }

    @Override
    public void setSuccessfulReturnCodes(List<String> successfulReturnCodes) {
        this.successfulReturnCodes = successfulReturnCodes;
    }

    @Override
    public String getWorkingDirectory() {
        return this.workingDirectory;
    }

    @Override
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public String getCommandLine() {
        return this.commandLine;
    }

    @Override
    public void setCommandLine(String commandLine) {
        this.commandLine = commandLine;
    }

}
