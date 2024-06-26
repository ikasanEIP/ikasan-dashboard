package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.builder.context.ContextBuilderException;
import org.ikasan.job.orchestration.model.job.SchedulerJobImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobLockParticipantImpl;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;

import java.util.ArrayList;
import java.util.List;

public class SchedulerJobLockParticipantBuilder  {
    protected int lockCount = 1;

    protected String agentName;
    protected String jobName;
    protected String contextName;
    protected List<String> childContextNames;
    protected String description;
    protected String startupControlType = "AUTOMATIC";

    /**
     * Builder class for creating SchedulerJobLockParticipant objects.
     */
    public SchedulerJobLockParticipantBuilder() {
    }

    /**
     * Sets the agent name for the SchedulerJobLockParticipantBuilder.
     *
     * @param agentName the agent name to set
     * @return the SchedulerJobLockParticipantBuilder instance
     */
    public SchedulerJobLockParticipantBuilder withAgentName(String agentName) {
        this.agentName = agentName;

        return this;
    }

    /**
     * Sets the job name for the SchedulerJobLockParticipantBuilder.
     *
     * @param jobName the name of the job
     * @return the SchedulerJobLockParticipantBuilder instance
     */
    public SchedulerJobLockParticipantBuilder withJobName(String jobName) {
        this.jobName = jobName;

        return this;
    }

    /**
     * Sets the context name for the SchedulerJobLockParticipantBuilder.
     *
     * @param contextName the context name to set
     * @return the updated SchedulerJobLockParticipantBuilder
     */
    public SchedulerJobLockParticipantBuilder withContextName(String contextName) {
        this.contextName = contextName;

        return this;
    }

    /**
     * Adds a child context ID to the SchedulerJobLockParticipantBuilder.
     *
     * @param childContextId the ID of the child context to be added
     * @return the SchedulerJobLockParticipantBuilder instance
     */
    public SchedulerJobLockParticipantBuilder addChildContextId(String childContextId) {
        if(this.childContextNames == null) {
            this.childContextNames = new ArrayList<>();
        }

        this.childContextNames.add(childContextId);

        return this;
    }

    /**
     * Sets the description for the SchedulerJobLockParticipantBuilder.
     *
     * @param description the description of the SchedulerJobLockParticipantBuilder
     * @return the SchedulerJobLockParticipantBuilder object
     */
    public SchedulerJobLockParticipantBuilder withDescription(String description) {
        this.description = description;

        return this;
    }

    /**
     * Sets the lock count for the SchedulerJobLockParticipantBuilder.
     *
     * @param lockCount the lock count to be set
     * @return the instance of SchedulerJobLockParticipantBuilder
     */
    public SchedulerJobLockParticipantBuilder withLockCount(int lockCount) {
        this.lockCount = lockCount;

        return this;
    }

    /**
     * Builds a SchedulerJobLockParticipant object with the specified agent name, job name, and other optional properties.
     * Throws a ContextBuilderException if the agent name or job name is null.
     *
     * @return the built SchedulerJobLockParticipant object
     * @throws ContextBuilderException if the agent name or job name is null
     */
    public SchedulerJobLockParticipant build() {
        if(this.agentName == null || this.jobName == null) {
            throw new ContextBuilderException("Both agent name and job name must not be null!");
        }

        SchedulerJobLockParticipant schedulerJob = new SchedulerJobLockParticipantImpl();
        schedulerJob.setIdentifier(this.agentName+"-"+this.jobName);
        schedulerJob.setAgentName(this.agentName);
        schedulerJob.setJobName(this.jobName);
        schedulerJob.setJobDescription(this.description);
        schedulerJob.setStartupControlType(this.startupControlType);
        schedulerJob.setLockCount(this.lockCount);

        return schedulerJob;
    }
}
