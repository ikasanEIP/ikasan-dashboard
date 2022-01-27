package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.builder.context.ContextParameterBuilder;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.spec.scheduled.context.model.ContextParameter;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;

import java.util.ArrayList;
import java.util.List;

public class InternalEventDrivenJobBuilder extends SchedulerJobBuilder {

    private List<String> successfulReturnCodes;
    private String workingDirectory;
    private String commandLine;
    private long minExecutionTime;
    private long maxExecutionTime;
    private List<ContextParameter> contextParameters = new ArrayList<>();

    public InternalEventDrivenJobBuilder addSuccessfulReturnCode(String returnCode) {
        if(successfulReturnCodes == null) {
            successfulReturnCodes = new ArrayList<>();
        }
        successfulReturnCodes.add(returnCode);

        return this;
    }

    public InternalEventDrivenJobBuilder withWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;

        return this;
    }

    public InternalEventDrivenJobBuilder withCommandLine(String commandLine) {
        this.commandLine = commandLine;

        return this;
    }


    public InternalEventDrivenJobBuilder withMinExecutionTime(long minExecutionTime) {
        this.minExecutionTime = minExecutionTime;

        return this;
    }

    public InternalEventDrivenJobBuilder withMaxExecutionTime(long maxExecutionTime) {
        this.maxExecutionTime = maxExecutionTime;

        return this;
    }

    public InternalEventDrivenJobBuilder addContextParameter(ContextParameter contextParameter) {
        if(this.contextParameters == null) {
            this.contextParameters = new ArrayList<>();
        }

        this.contextParameters.add(contextParameter);

        return this;
    }


    public ContextParameterBuilder getContextParameterBuilder() {
        return new ContextParameterBuilder();
    }

    public InternalEventDrivenJob build() {
        InternalEventDrivenJob internalEventDrivenJob = new InternalEventDrivenJobImpl();

        internalEventDrivenJob.setIdentifier(this.agentName+"-"+this.jobName);
        internalEventDrivenJob.setAgentName(this.agentName);
        internalEventDrivenJob.setJobName(this.jobName);
        internalEventDrivenJob.setJobDescription(this.description);
        internalEventDrivenJob.setSuccessfulReturnCodes(this.successfulReturnCodes);
        internalEventDrivenJob.setContextParameters(this.contextParameters);
        internalEventDrivenJob.setCommandLine(this.commandLine);
        internalEventDrivenJob.setWorkingDirectory(this.workingDirectory);
        internalEventDrivenJob.setMinExecutionTime(this.minExecutionTime);
        internalEventDrivenJob.setMaxExecutionTime(this.maxExecutionTime);
        internalEventDrivenJob.setStartupControlType(super.startupControlType);

        return internalEventDrivenJob;
    }
}
