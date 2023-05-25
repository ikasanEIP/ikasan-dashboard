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

    public SchedulerJobLockParticipantBuilder() {
    }

    /**
     * Set the agent name.
     *
     * @param agentName
     * @return
     */
    public SchedulerJobLockParticipantBuilder withAgentName(String agentName) {
        this.agentName = agentName;

        return this;
    }

    /**
     * Set the job name.
     *
     * @param jobName
     * @return
     */
    public SchedulerJobLockParticipantBuilder withJobName(String jobName) {
        this.jobName = jobName;

        return this;
    }

    /**
     * Set the context name.
     *
     * @param contextName
     * @return
     */
    public SchedulerJobLockParticipantBuilder withContextName(String contextName) {
        this.contextName = contextName;

        return this;
    }

    /**
     * Add a child context id.
     *
     * @param childContextId
     * @return
     */
    public SchedulerJobLockParticipantBuilder addChildContextId(String childContextId) {
        if(this.childContextNames == null) {
            this.childContextNames = new ArrayList<>();
        }

        this.childContextNames.add(childContextId);

        return this;
    }

    /**
     * Set the job description.
     *
     * @param description
     * @return
     */
    public SchedulerJobLockParticipantBuilder withDescription(String description) {
        this.description = description;

        return this;
    }

    /**
     * Set the job startupControlType.
     *
     * @param startupControlType
     * @return
     */
    public SchedulerJobLockParticipantBuilder withStartupControlType(String startupControlType) {
        this.startupControlType = startupControlType;

        return this;
    }

    /**
     * Set the lock count
     *
     * @param lockCount
     * @return
     */
    public SchedulerJobLockParticipantBuilder withLockCount(int lockCount) {
        this.agentName = agentName;

        return this;
    }

    public SchedulerJobLockParticipant build() {
        if(this.agentName == null || this.jobName == null) {
            throw new ContextBuilderException("Both agent name and job name must no be null!");
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
