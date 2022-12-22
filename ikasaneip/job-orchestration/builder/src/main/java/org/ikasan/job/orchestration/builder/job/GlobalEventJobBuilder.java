package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.builder.context.ContextBuilderException;
import org.ikasan.job.orchestration.model.job.GlobalEventJobImpl;
import org.ikasan.spec.scheduled.job.model.GlobalEventJob;

public class GlobalEventJobBuilder extends SchedulerJobBuilder {

    public GlobalEventJob build() {
        if(this.agentName == null || this.jobName == null) {
            throw new ContextBuilderException("Both agent name and job name must no be null!");
        }

        GlobalEventJob globalEventJob = new GlobalEventJobImpl();
        globalEventJob.setIdentifier(this.agentName+"-"+this.jobName);
        globalEventJob.setAgentName(this.agentName);
        globalEventJob.setJobName(this.jobName);
        globalEventJob.setJobDescription(this.description);
        globalEventJob.setStartupControlType(this.startupControlType);
        globalEventJob.setContextName(this.contextName);
        globalEventJob.setContextName(this.contextName);
        globalEventJob.setChildContextNames(super.childContextNames);

        return globalEventJob;
    }

}
