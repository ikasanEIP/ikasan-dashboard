package org.ikasan.job.orchestration.model.instance;

import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;

import java.util.ArrayList;
import java.util.List;

public class InternalEventDrivenJobInstanceImpl extends SchedulerJobInstanceImpl implements InternalEventDrivenJobInstance {
    private List<String> successfulReturnCodes;
    private String workingDirectory;
    private String commandLine;
    private long minExecutionTime;
    private long maxExecutionTime;
    private List<ContextParameter> contextParameters = new ArrayList<>();;
    private List<Integer> daysOfWeekToRun;

    @Override
    public List<String> getSuccessfulReturnCodes() {
        return successfulReturnCodes;
    }

    @Override
    public void setSuccessfulReturnCodes(List<String> successfulReturnCodes) {
        this.successfulReturnCodes = successfulReturnCodes;
    }

    @Override
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    @Override
    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public String getCommandLine() {
        return commandLine;
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

    @Override
    public List<Integer> getDaysOfWeekToRun() {
        return daysOfWeekToRun;
    }

    @Override
    public void setDaysOfWeekToRun(List<Integer> daysOfWeekToRun) {
        this.daysOfWeekToRun = daysOfWeekToRun;
    }
}
