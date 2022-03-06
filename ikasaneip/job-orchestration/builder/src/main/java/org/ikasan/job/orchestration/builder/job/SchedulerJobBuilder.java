package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.builder.context.ContextBuilderException;
import org.ikasan.job.orchestration.model.job.SchedulerJobImpl;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.ArrayList;
import java.util.List;

public class SchedulerJobBuilder {
    protected String agentName;
    protected String jobName;
    protected String contextId;
    protected List<String> childContextIds;
    protected String description;
    protected String startupControlType = "AUTOMATIC";

    public SchedulerJobBuilder() {
    }

    /**
     * Set the agent name.
     *
     * @param agentName
     * @return
     */
    public SchedulerJobBuilder withAgentName(String agentName) {
        this.agentName = agentName;

        return this;
    }

    /**
     * Set the job name.
     *
     * @param jobName
     * @return
     */
    public SchedulerJobBuilder withJobName(String jobName) {
        this.jobName = jobName;

        return this;
    }

    /**
     * Set the context id.
     *
     * @param contextId
     * @return
     */
    public SchedulerJobBuilder withContextId(String contextId) {
        this.contextId = contextId;

        return this;
    }

    /**
     * Add a child context id.
     *
     * @param childContextId
     * @return
     */
    public SchedulerJobBuilder addChildContextId(String childContextId) {
        if(this.childContextIds == null) {
            this.childContextIds = new ArrayList<>();
        }

        this.childContextIds.add(childContextId);

        return this;
    }

    /**
     * Set the job description.
     *
     * @param description
     * @return
     */
    public SchedulerJobBuilder withDescription(String description) {
        this.description = description;

        return this;
    }

    /**
     * Set the job startupControlType.
     *
     * @param startupControlType
     * @return
     */
    public SchedulerJobBuilder withStartupControlType(String startupControlType) {
        this.startupControlType = startupControlType;

        return this;
    }

    public SchedulerJob build() {
        if(this.agentName == null || this.jobName == null) {
            throw new ContextBuilderException("Both agent name and job name must no be null!");
        }

        SchedulerJob schedulerJob = new SchedulerJobImpl();
        schedulerJob.setIdentifier(this.agentName+"-"+this.jobName);
        schedulerJob.setAgentName(this.agentName);
        schedulerJob.setJobName(this.jobName);
        schedulerJob.setJobDescription(this.description);
        schedulerJob.setStartupControlType(this.startupControlType);

        return schedulerJob;
    }
}
