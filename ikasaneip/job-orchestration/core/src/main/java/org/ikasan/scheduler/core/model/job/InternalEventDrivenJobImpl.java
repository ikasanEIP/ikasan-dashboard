package org.ikasan.scheduler.core.model.job;

import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;

import java.util.List;

public class InternalEventDrivenJobImpl extends SchedulerJobImpl implements InternalEventDrivenJob {

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

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("InternalEventDrivenJobImpl{");
        sb.append("successfulReturnCodes=").append(successfulReturnCodes);
        sb.append(", workingDirectory='").append(workingDirectory).append('\'');
        sb.append(", commandLine='").append(commandLine).append('\'');
        sb.append(", jobIdentifier='").append(jobIdentifier).append('\'');
        sb.append(", agentName='").append(agentName).append('\'');
        sb.append(", jobName='").append(jobName).append('\'');
        sb.append(", contextId='").append(contextId).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
