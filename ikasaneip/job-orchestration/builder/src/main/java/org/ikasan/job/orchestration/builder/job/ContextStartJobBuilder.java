package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.builder.context.ContextBuilderException;
import org.ikasan.job.orchestration.model.job.ContextStartJobImpl;
import org.ikasan.job.orchestration.model.job.GlobalEventJobImpl;
import org.ikasan.spec.scheduled.job.model.ContextStartJob;
import org.ikasan.spec.scheduled.job.model.GlobalEventJob;

public class ContextStartJobBuilder extends SchedulerJobBuilder {
    /**
     * Builds a ContextStartJob object based on the provided parameters.
     *
     * @return The ContextStartJob object built based on the provided parameters.
     * @throws ContextBuilderException if either agent name or job name is null.
     */
    public ContextStartJob build() {
        if(this.agentName == null || this.jobName == null) {
            throw new ContextBuilderException("Both agent name and job name must no be null!");
        }

        ContextStartJob contextStartJob = new ContextStartJobImpl();
        contextStartJob.setJobName(this.jobName);
        contextStartJob.setJobDescription(this.description);
        contextStartJob.setStartupControlType(this.startupControlType);
        contextStartJob.setContextName(this.contextName);
        contextStartJob.setChildContextNames(super.childContextNames);
        contextStartJob.setDisplayName(super.displayName);

        return contextStartJob;
    }

}
