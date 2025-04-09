package org.ikasan.job.orchestration.core.machine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ikasan.job.orchestration.model.instance.BridgingJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.InternalEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.BridgingJobInstance;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;

public final class ContextMachineTestHelper {

    public static Map<String, InternalEventDrivenJobInstance> createInternalJobsMap(ContextTemplate contextTemplate) {
        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();

        if(contextTemplate.getScheduledJobs() != null) {
            contextTemplate.getScheduledJobs().forEach(job -> {
                if(!job.getAgentName().equals("BRIDGING_JOB")) {
                    internalEventDrivenJobs.put(job.getIdentifier() + "-" + contextTemplate.getName(),
                        newInternalEventDrivenJob(job.getIdentifier(), contextTemplate.getName(), job.getJobName()));
                }
            });
        }

        if (contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()) {
            contextTemplate.getContexts().forEach(template -> addInternalJobs(template, internalEventDrivenJobs));
        }

        return internalEventDrivenJobs;
    }

    public static Map<String, BridgingJobInstance> creatBridgingMap(ContextTemplate contextTemplate) {
        HashMap<String, BridgingJobInstance> bridgingJobInstanceMap = new HashMap<>();

        if(contextTemplate.getScheduledJobs() != null) {
            contextTemplate.getScheduledJobs().forEach(job -> {
                if(job.getAgentName().equals("BRIDGING_JOB")) {
                    bridgingJobInstanceMap.put(job.getIdentifier() + "-" + contextTemplate.getName(),
                        newBridgingJobInstance(job.getIdentifier(), contextTemplate.getName(), job.getJobName()));
                }
            });
        }

        if (contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()) {
            contextTemplate.getContexts().forEach(template -> addBridgingJobs(template, bridgingJobInstanceMap));
        }

        return bridgingJobInstanceMap;
    }

    private static void addInternalJobs(ContextTemplate contextTemplate, Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs) {
        if (contextTemplate.getScheduledJobs() != null && !contextTemplate.getScheduledJobs().isEmpty()) {
            contextTemplate.getScheduledJobs().forEach(job -> {
                if(!job.getAgentName().equals("BRIDGING_JOB")) {
                    internalEventDrivenJobs.put(job.getIdentifier() + "-" + contextTemplate.getName(),
                        newInternalEventDrivenJob(job.getIdentifier(), contextTemplate.getName(), job.getJobName()));
                }
            });
        }

        if (contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()) {
            contextTemplate.getContexts().forEach(template -> addInternalJobs(template, internalEventDrivenJobs));
        }
    }

    private static void addBridgingJobs(ContextTemplate contextTemplate, Map<String, BridgingJobInstance> bridgingJobInstanceMap) {
        if (contextTemplate.getScheduledJobs() != null && !contextTemplate.getScheduledJobs().isEmpty()) {
            contextTemplate.getScheduledJobs().forEach(job -> {
                if(job.getAgentName().equals("BRIDGING_JOB")) {
                    bridgingJobInstanceMap.put(job.getIdentifier() + "-" + contextTemplate.getName(),
                        newBridgingJobInstance(job.getIdentifier(), contextTemplate.getName(), job.getJobName()));
                }
            });
        }

        if (contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()) {
            contextTemplate.getContexts().forEach(template -> addBridgingJobs(template, bridgingJobInstanceMap));
        }
    }

    private static InternalEventDrivenJobInstance newInternalEventDrivenJob(String jobIdentifier, String childContextName, String jobName) {
        InternalEventDrivenJobInstance job = new InternalEventDrivenJobInstanceImpl();
        job.setJobName(jobName);
        job.setIdentifier(jobIdentifier);
        job.setChildContextName(childContextName);
        job.setChildContextNames(List.of(childContextName));
        job.setTargetResidingContextOnly(true);

        return job;
    }

    private static BridgingJobInstance newBridgingJobInstance(String jobIdentifier, String childContextName, String jobName) {
        BridgingJobInstance job = new BridgingJobInstanceImpl();
        job.setJobName(jobName);
        job.setIdentifier(jobIdentifier);
        job.setChildContextName(childContextName);
        job.setChildContextNames(List.of(childContextName));

        return job;
    }

}
