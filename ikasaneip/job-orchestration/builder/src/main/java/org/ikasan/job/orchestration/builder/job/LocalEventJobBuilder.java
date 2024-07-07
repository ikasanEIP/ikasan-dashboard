package org.ikasan.job.orchestration.builder.job;

import org.ikasan.job.orchestration.builder.context.ContextBuilderException;
import org.ikasan.job.orchestration.model.job.GlobalEventJobImpl;
import org.ikasan.job.orchestration.model.job.LocalEventJobImpl;
import org.ikasan.spec.scheduled.job.model.GlobalEventJob;
import org.ikasan.spec.scheduled.job.model.LocalEventJob;

public class LocalEventJobBuilder extends SchedulerJobBuilder {


    /**
     * Builds a LocalEventJob object based on the provided parameters.
     *
     * @return the built LocalEventJob object
     * @throws ContextBuilderException if the job name is null
     */
    public LocalEventJob build() {
        if(this.jobName == null) {
            throw new ContextBuilderException("Job name must no be null!");
        }

        LocalEventJob localEventJob = new LocalEventJobImpl();
        localEventJob.setJobName(this.jobName);
        localEventJob.setJobDescription(this.description);
        localEventJob.setContextName(this.contextName);
        localEventJob.setDisplayName(super.displayName);

        return localEventJob;
    }

}
