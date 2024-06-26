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
    private List<Integer> daysOfWeekToRun;
    private boolean targetResidingContextOnly = false;
    boolean participatesInLock;
    boolean repeatable;

    /**
     * Adds a successful return code for the job.
     *
     * @param returnCode the successful return code to be added
     * @return the updated InternalEventDrivenJobBuilder object
     */
    public InternalEventDrivenJobBuilder addSuccessfulReturnCode(String returnCode) {
        if(successfulReturnCodes == null) {
            successfulReturnCodes = new ArrayList<>();
        }
        successfulReturnCodes.add(returnCode);

        return this;
    }

    /**
     * Sets the working directory for the job.
     *
     * @param workingDirectory the path to the working directory
     * @return the InternalEventDrivenJobBuilder instance
     */
    public InternalEventDrivenJobBuilder withWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;

        return this;
    }

    /**
     * Sets the command line for the InternalEventDrivenJobBuilder.
     *
     * @param commandLine the command line to set
     * @return the InternalEventDrivenJobBuilder instance
     */
    public InternalEventDrivenJobBuilder withCommandLine(String commandLine) {
        this.commandLine = commandLine;

        return this;
    }


    /**
     * Sets the minimum execution time for the internal event-driven job.
     *
     * @param minExecutionTime the minimum execution time in milliseconds
     * @return the updated InternalEventDrivenJobBuilder instance
     */
    public InternalEventDrivenJobBuilder withMinExecutionTime(long minExecutionTime) {
        this.minExecutionTime = minExecutionTime;

        return this;
    }

    /**
     * Sets the maximum execution time for the internal event-driven job.
     *
     * @param maxExecutionTime the maximum execution time in milliseconds
     * @return the InternalEventDrivenJobBuilder instance
     */
    public InternalEventDrivenJobBuilder withMaxExecutionTime(long maxExecutionTime) {
        this.maxExecutionTime = maxExecutionTime;

        return this;
    }

    /**
     * Sets the flag to indicate whether the target of the job should reside in the context only.
     *
     * @param targetResidingContextOnly the flag to indicate whether the target should reside in the context only
     * @return the InternalEventDrivenJobBuilder instance
     */
    public InternalEventDrivenJobBuilder withTargetResidingContextOnly(boolean targetResidingContextOnly) {
        this.targetResidingContextOnly = targetResidingContextOnly;

        return this;
    }

    /**
     * Sets whether the job participates in a lock.
     *
     * @param participatesInLock true if the job participates in a lock, false otherwise.
     * @return the updated InternalEventDrivenJobBuilder instance.
     */
    public InternalEventDrivenJobBuilder withParticipatesInLock(boolean participatesInLock) {
        this.participatesInLock = participatesInLock;

        return this;
    }

    /**
     * Sets whether the job is repeatable.
     *
     * @param repeatable true if the job is repeatable, false otherwise
     * @return the updated InternalEventDrivenJobBuilder object
     */
    public InternalEventDrivenJobBuilder withJobRepeatable(boolean repeatable) {
        this.repeatable = repeatable;

        return this;
    }

    /**
     * Adds a context parameter to the internal event-driven job builder.
     *
     * @param contextParameter the context parameter to be added
     * @return the updated InternalEventDrivenJobBuilder object
     */
    public InternalEventDrivenJobBuilder addContextParameter(ContextParameter contextParameter) {
        if(this.contextParameters == null) {
            this.contextParameters = new ArrayList<>();
        }

        this.contextParameters.add(contextParameter);

        return this;
    }

    /**
     * Adds a day of the week to the list of days to run the job on.
     *
     * @param dayOfWeekToRun The day of the week to add to the list. Use the constants defined in Calendar class, for example Calendar.MONDAY.
     * @return The InternalEventDrivenJobBuilder instance.
     */
    public InternalEventDrivenJobBuilder addDayOfWeekToRun(Integer dayOfWeekToRun) {
        if(this.daysOfWeekToRun == null) {
            this.daysOfWeekToRun = new ArrayList<>();
        }

        this.daysOfWeekToRun.add(dayOfWeekToRun);

        return this;
    }


    /**
     * Retrieves an instance of {@link ContextParameterBuilder}.
     *
     * @return an instance of {@link ContextParameterBuilder}
     */
    public ContextParameterBuilder getContextParameterBuilder() {
        return new ContextParameterBuilder();
    }

    /**
     * Builds an instance of InternalEventDrivenJob.
     *
     * @return the built InternalEventDrivenJob instance.
     */
    public InternalEventDrivenJob build() {
        InternalEventDrivenJob internalEventDrivenJob = new InternalEventDrivenJobImpl();

        internalEventDrivenJob.setIdentifier(this.agentName+"-"+this.jobName);
        internalEventDrivenJob.setAgentName(this.agentName);
        internalEventDrivenJob.setJobName(this.jobName);
        internalEventDrivenJob.setDisplayName(this.displayName);
        internalEventDrivenJob.setJobDescription(this.description);
        internalEventDrivenJob.setSuccessfulReturnCodes(this.successfulReturnCodes);
        internalEventDrivenJob.setContextParameters(this.contextParameters);
        internalEventDrivenJob.setCommandLine(this.commandLine);
        internalEventDrivenJob.setWorkingDirectory(this.workingDirectory);
        internalEventDrivenJob.setMinExecutionTime(this.minExecutionTime);
        internalEventDrivenJob.setMaxExecutionTime(this.maxExecutionTime);
        internalEventDrivenJob.setStartupControlType(super.startupControlType);
        internalEventDrivenJob.setContextName(super.contextName);
        internalEventDrivenJob.setChildContextNames(super.childContextNames);
        internalEventDrivenJob.setDaysOfWeekToRun(this.daysOfWeekToRun);
        internalEventDrivenJob.setTargetResidingContextOnly(this.targetResidingContextOnly);
        internalEventDrivenJob.setParticipatesInLock(this.participatesInLock);
        internalEventDrivenJob.setJobRepeatable(repeatable);

        return internalEventDrivenJob;
    }
}
