package org.ikasan.job.orchestration.core.machine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ikasan.job.orchestration.model.instance.BridgingJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.InternalEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.LocalEventJobInstanceImpl;
import org.ikasan.job.orchestration.model.job.InternalEventDrivenJobImpl;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.BridgingJobInstance;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.model.LocalEventJobInstance;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.JobConstants;

public final class ContextMachineTestHelper {

    /**
     * Creates a map of InternalEventDrivenJobInstance objects based on the scheduled jobs in the given ContextTemplate.
     *
     * @param contextTemplate The context template containing scheduled jobs and child contexts.
     * @return A map where the key is a concatenated string of job identifier and context name,
     * and the value is an InternalEventDrivenJobInstance object corresponding to the scheduled job.
     */
    public static Map<String, InternalEventDrivenJobInstance> createInternalJobsMap(ContextTemplate contextTemplate) {
        HashMap<String, InternalEventDrivenJobInstance> internalEventDrivenJobs = new HashMap<>();

        if(contextTemplate.getScheduledJobs() != null) {
            contextTemplate.getScheduledJobs().forEach(job -> {
                if(!job.getAgentName().equals(JobConstants.BRIDGING_JOB) && !job.getAgentName().equals(JobConstants.LOCAL_EVENT_JOB)) {
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

    /**
     * Creates a map of BridgingJobInstance objects based on the scheduled jobs in the given ContextTemplate.
     *
     * @param contextTemplate The context template containing scheduled jobs and child contexts.
     * @return A map where the key is a concatenated string of job identifier and context name,
     * and the value is a BridgingJobInstance object corresponding to the scheduled job.
     */
    public static Map<String, BridgingJobInstance> creatBridgingMap(ContextTemplate contextTemplate) {
        HashMap<String, BridgingJobInstance> bridgingJobInstanceMap = new HashMap<>();

        if(contextTemplate.getScheduledJobs() != null) {
            contextTemplate.getScheduledJobs().forEach(job -> {
                if(job.getAgentName().equals(JobConstants.BRIDGING_JOB)) {
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

    /**
     * Creates a map of LocalEventJobInstance objects based on the scheduled jobs in the given ContextTemplate.
     *
     * @param contextTemplate The context template containing scheduled jobs and child contexts.
     * @return A map where the key is a concatenated string of job identifier and context name,
     * and the value is a LocalEventJobInstance object corresponding to the scheduled job.
     */
    public static Map<String, LocalEventJobInstance> creatLocalJobsMap(ContextTemplate contextTemplate) {
        HashMap<String, LocalEventJobInstance> localEventJobInstanceHashMap = new HashMap<>();

        if(contextTemplate.getScheduledJobs() != null) {
            contextTemplate.getScheduledJobs().forEach(job -> {
                if(job.getAgentName().equals(JobConstants.LOCAL_EVENT_JOB)) {
                    localEventJobInstanceHashMap.put(job.getIdentifier() + "-" + contextTemplate.getName(),
                        newLocalEventJobInstance(job.getIdentifier(), contextTemplate.getName(), job.getJobName()));
                }
            });
        }

        if (contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()) {
            contextTemplate.getContexts().forEach(template -> addLocalEventJobsJobs(template, localEventJobInstanceHashMap));
        }

        return localEventJobInstanceHashMap;
    }

    /**
     * Adds InternalEventDrivenJobInstance objects to the provided map based on the scheduled jobs in the given ContextTemplate.
     *
     * @param contextTemplate The context template containing scheduled jobs and child contexts.
     * @param internalEventDrivenJobs The map to store the InternalEventDrivenJobInstance objects.
     */
    private static void addInternalJobs(ContextTemplate contextTemplate, Map<String, InternalEventDrivenJobInstance> internalEventDrivenJobs) {
        if (contextTemplate.getScheduledJobs() != null && !contextTemplate.getScheduledJobs().isEmpty()) {
            contextTemplate.getScheduledJobs().forEach(job -> {
                if(!job.getAgentName().equals(JobConstants.BRIDGING_JOB) && !job.getAgentName().equals(JobConstants.LOCAL_EVENT_JOB)) {
                    internalEventDrivenJobs.put(job.getIdentifier() + "-" + contextTemplate.getName(),
                        newInternalEventDrivenJob(job.getIdentifier(), contextTemplate.getName(), job.getJobName()));
                }
            });
        }

        if (contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()) {
            contextTemplate.getContexts().forEach(template -> addInternalJobs(template, internalEventDrivenJobs));
        }
    }

    /**
     * Adds BridgingJobInstance objects to the provided map based on the scheduled jobs in the given ContextTemplate.
     *
     * @param contextTemplate The context template containing scheduled jobs and child contexts.
     * @param bridgingJobInstanceMap The map to store the BridgingJobInstance objects.
     */
    private static void addBridgingJobs(ContextTemplate contextTemplate, Map<String, BridgingJobInstance> bridgingJobInstanceMap) {
        if (contextTemplate.getScheduledJobs() != null && !contextTemplate.getScheduledJobs().isEmpty()) {
            contextTemplate.getScheduledJobs().forEach(job -> {
                if(job.getAgentName().equals(JobConstants.BRIDGING_JOB)) {
                    bridgingJobInstanceMap.put(job.getIdentifier() + "-" + contextTemplate.getName(),
                        newBridgingJobInstance(job.getIdentifier(), contextTemplate.getName(), job.getJobName()));
                }
            });
        }

        if (contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()) {
            contextTemplate.getContexts().forEach(template -> addBridgingJobs(template, bridgingJobInstanceMap));
        }
    }

    /**
     * Adds LocalEventJobInstance objects to the provided map based on the scheduled jobs in the given ContextTemplate.
     *
     * @param contextTemplate The context template containing scheduled jobs and child contexts.
     * @param localEventJobInstanceMap The map to store the LocalEventJobInstance objects.
     */
    private static void addLocalEventJobsJobs(ContextTemplate contextTemplate, Map<String, LocalEventJobInstance> localEventJobInstanceMap) {
        if (contextTemplate.getScheduledJobs() != null && !contextTemplate.getScheduledJobs().isEmpty()) {
            contextTemplate.getScheduledJobs().forEach(job -> {
                if(job.getAgentName().equals(JobConstants.LOCAL_EVENT_JOB)) {
                    localEventJobInstanceMap.put(job.getIdentifier() + "-" + contextTemplate.getName(),
                        newLocalEventJobInstance(job.getIdentifier(), contextTemplate.getName(), job.getJobName()));
                }
            });
        }

        if (contextTemplate.getContexts() != null && !contextTemplate.getContexts().isEmpty()) {
            contextTemplate.getContexts().forEach(template -> addLocalEventJobsJobs(template, localEventJobInstanceMap));
        }
    }

    /**
     * Creates a new instance of InternalEventDrivenJobInstance with the given job identifier, child context name, and job name.
     *
     * @param jobIdentifier The unique identifier of the job.
     * @param childContextName The name of the child context.
     * @param jobName The name of the job.
     * @return A new instance of InternalEventDrivenJobInstance initialized with the provided parameters.
     */
    private static InternalEventDrivenJobInstance newInternalEventDrivenJob(String jobIdentifier, String childContextName, String jobName) {
        InternalEventDrivenJobInstance job = new InternalEventDrivenJobInstanceImpl();
        job.setJobName(jobName);
        job.setIdentifier(jobIdentifier);
        job.setChildContextName(childContextName);
        job.setChildContextNames(List.of(childContextName));
        job.setTargetResidingContextOnly(true);

        return job;
    }

    /**
     * Creates a new instance of BridgingJobInstance with the given job identifier, child context name, and job name.
     *
     * @param jobIdentifier The unique identifier of the job.
     * @param childContextName The name of the child context.
     * @param jobName The name of the job.
     * @return A new instance of BridgingJobInstance initialized with the provided parameters.
     */
    private static BridgingJobInstance newBridgingJobInstance(String jobIdentifier, String childContextName, String jobName) {
        BridgingJobInstance job = new BridgingJobInstanceImpl();
        job.setJobName(jobName);
        job.setIdentifier(jobIdentifier);
        job.setChildContextName(childContextName);
        job.setChildContextNames(List.of(childContextName));

        return job;
    }

    /**
     * Creates a new instance of BridgingJobInstance with the given job identifier, child context name, and job name.
     *
     * @param jobIdentifier The unique identifier of the job.
     * @param childContextName The name of the child context.
     * @param jobName The name of the job.
     * @return A new instance of BridgingJobInstance initialized with the provided parameters.
     */
    private static LocalEventJobInstance newLocalEventJobInstance(String jobIdentifier, String childContextName, String jobName) {
        LocalEventJobInstance job = new LocalEventJobInstanceImpl();
        job.setJobName(jobName);
        job.setIdentifier(jobIdentifier);
        job.setChildContextName(childContextName);
        job.setChildContextNames(List.of(childContextName));

        return job;
    }

}
