package org.ikasan.job.orchestration.core.machine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;

public final class ContextMachineTestHelper {

    public static Map<String, InternalEventDrivenJob> createInternalJobsMap(ContextTemplate contextTemplate) {
        HashMap<String, InternalEventDrivenJob> internalEventDrivenJobs = new HashMap<>();

        if(contextTemplate.getScheduledJobs() != null) {
            contextTemplate.getScheduledJobs().forEach(job -> internalEventDrivenJobs.put(job.getIdentifier(),
                newInternalEventDrivenJob(job.getIdentifier())));
        }

        if (contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()) {
            contextTemplate.getContexts().forEach(template -> addInternalJobs(template, internalEventDrivenJobs));
        }

        return internalEventDrivenJobs;
    }

    private static void addInternalJobs(ContextTemplate contextTemplate, Map<String, InternalEventDrivenJob> internalEventDrivenJobs) {
        if (contextTemplate.getContexts() == null || contextTemplate.getContexts().isEmpty()) {
            contextTemplate.getScheduledJobs().forEach(job -> internalEventDrivenJobs.put(job.getIdentifier(),
                newInternalEventDrivenJob(job.getIdentifier())));
        } else {
            contextTemplate.getContexts().forEach(template -> addInternalJobs(template, internalEventDrivenJobs));
        }
    }

    private static InternalEventDrivenJob newInternalEventDrivenJob(String jobIdentifier) {
        InternalEventDrivenJob job = new InternalEventDrivenJobImpl();
        job.setIdentifier(jobIdentifier);
        job.setChildContextIds(List.of("PASS_THROUGH"));

        return job;
    }

}
