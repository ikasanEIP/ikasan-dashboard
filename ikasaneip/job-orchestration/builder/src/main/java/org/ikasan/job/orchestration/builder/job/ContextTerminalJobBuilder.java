package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.builder.context.ContextBuilderException;
import org.ikasan.job.orchestration.model.job.ContextStartJobImpl;
import org.ikasan.job.orchestration.model.job.ContextTerminalJobImpl;
import org.ikasan.spec.scheduled.job.model.ContextStartJob;
import org.ikasan.spec.scheduled.job.model.ContextTerminalJob;

public class ContextTerminalJobBuilder extends SchedulerJobBuilder {
    /**
     * Builds a ContextTerminalJob object based on the provided configuration.
     *
     * @return a ContextTerminalJob object
     * @throws ContextBuilderException if both agent name and job name are null
     */
    public ContextTerminalJob build() {
        if(this.agentName == null || this.jobName == null) {
            throw new ContextBuilderException("Both agent name and job name must no be null!");
        }

        ContextTerminalJob contextTerminalJob = new ContextTerminalJobImpl();
        contextTerminalJob.setJobName(this.jobName);
        contextTerminalJob.setJobDescription(this.description);
        contextTerminalJob.setStartupControlType(this.startupControlType);
        contextTerminalJob.setContextName(this.contextName);
        contextTerminalJob.setChildContextNames(super.childContextNames);
        contextTerminalJob.setDisplayName(super.displayName);

        return contextTerminalJob;
    }

}
