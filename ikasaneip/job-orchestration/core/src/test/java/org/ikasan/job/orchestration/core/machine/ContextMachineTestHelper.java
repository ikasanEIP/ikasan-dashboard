package org.ikasan.job.orchestration.core.machine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ikasan.job.orchestration.model.instance.InternalEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;

public final class ContextMachineTestHelper {

    public static Map<String, InternalEventDrivenJobInstance> createInternalJobsMap(ContextTemplate contextTemplate) {
        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();

        if(contextTemplate.getScheduledJobs() != null) {
            contextTemplate.getScheduledJobs().forEach(job -> internalEventDrivenJobs.put(job.getIdentifier() + "-" + contextTemplate.getName(),
                newInternalEventDrivenJob(job.getIdentifier(), contextTemplate.getName())));
        }

        if (contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()) {
            contextTemplate.getContexts().forEach(template -> addInternalJobs(template, internalEventDrivenJobs));
        }

        return internalEventDrivenJobs;
    }

    private static void addInternalJobs(ContextTemplate contextTemplate, Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs) {
        if (contextTemplate.getScheduledJobs() != null && !contextTemplate.getScheduledJobs().isEmpty()) {
            contextTemplate.getScheduledJobs().forEach(job -> internalEventDrivenJobs.put(job.getIdentifier() + "-" + contextTemplate.getName(),
                newInternalEventDrivenJob(job.getIdentifier(), contextTemplate.getName())));
        }

        if (contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()) {
            contextTemplate.getContexts().forEach(template -> addInternalJobs(template, internalEventDrivenJobs));
        }
    }

    private static InternalEventDrivenJobInstance newInternalEventDrivenJob(String jobIdentifier, String childContextName) {
        InternalEventDrivenJobInstance job = new InternalEventDrivenJobInstanceImpl();
        job.setIdentifier(jobIdentifier);
        job.setChildContextName(childContextName);
        job.setTargetResidingContextOnly(true);

        return job;
    }

}
