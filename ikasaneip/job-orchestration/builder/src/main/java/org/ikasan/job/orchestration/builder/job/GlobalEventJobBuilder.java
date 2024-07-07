package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.builder.context.ContextBuilderException;
import org.ikasan.job.orchestration.model.job.GlobalEventJobImpl;
import org.ikasan.spec.scheduled.job.model.GlobalEventJob;

import java.util.List;

public class GlobalEventJobBuilder extends SchedulerJobBuilder {


    /**
     * Builds a GlobalEventJob instance with the specified properties.
     *
     * @return a GlobalEventJob instance
     * @throws ContextBuilderException if either agentName or jobName is null
     */
    public GlobalEventJob build() {
        if(this.jobName == null) {
            throw new ContextBuilderException("Job name must no be null!");
        }

        GlobalEventJob globalEventJob = new GlobalEventJobImpl();
        globalEventJob.setJobName(this.jobName);
        globalEventJob.setJobDescription(this.description);
        globalEventJob.setContextName(this.contextName);
        globalEventJob.setDisplayName(super.displayName);
        globalEventJob.setStartupControlType(null);

        return globalEventJob;
    }

}
