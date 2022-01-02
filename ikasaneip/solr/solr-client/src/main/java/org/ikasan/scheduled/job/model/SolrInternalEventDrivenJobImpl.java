package org.ikasan.scheduled.job.model;

import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;

import java.util.List;

public class SolrInternalEventDrivenJobImpl extends SolrSchedulerJobImpl implements InternalEventDrivenJob {

    private List<String> successfulReturnCodes;
    private String workingDirectory;
    private String commandLine;
    private long minExecutionTime;
    private long maxExecutionTime;
    private List<ContextParameter> contextParameters;

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

    @Override
    public long getMinExecutionTime() {
        return minExecutionTime;
    }

    @Override
    public void setMinExecutionTime(long minExecutionTime) {
        this.minExecutionTime = minExecutionTime;
    }

    @Override
    public long getMaxExecutionTime() {
        return maxExecutionTime;
    }

    @Override
    public void setMaxExecutionTime(long maxExecutionTime) {
        this.maxExecutionTime = maxExecutionTime;
    }

    @Override
    public List<ContextParameter> getContextParameters() {
        return contextParameters;
    }

    @Override
    public void setContextParameters(List<ContextParameter> contextParameters) {
        this.contextParameters = contextParameters;
    }
}
