package org.ikasan.job.orchestration.builder;

import org.ikasan.job.orchestration.model.job.SchedulerJobImpl;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

public class SchedulerJobBuilder {
    protected String agentName;
    protected String jobName;
    protected String contextId;
    protected String description;

    protected SchedulerJobBuilder() {
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
     * Set the job description.
     *
     * @param description
     * @return
     */
    public SchedulerJobBuilder withDescription(String description) {
        this.description = description;

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

        return schedulerJob;
    }
}
