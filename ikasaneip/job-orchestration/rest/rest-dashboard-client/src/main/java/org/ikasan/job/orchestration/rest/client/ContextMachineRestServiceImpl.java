package org.ikasan.job.orchestration.rest.client;

import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.rest.client.dto.BigQueueMessageDto;
import org.ikasan.job.orchestration.model.status.ContextInstanceStatus;
import org.ikasan.spec.bigqueue.message.BigQueueMessage;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.springframework.core.env.Environment;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.util.List;

/**
 * REST client for ContextMachine operations on a remote cluster node.
 * Each instance targets a single peer node URL.
 *
 * Methods return true when the remote node handled the request (HTTP 2xx),
 * false when the context instance was not found on that node (HTTP 404) or
 * a network error occurred.
 */
public class ContextMachineRestServiceImpl extends ClusterPeerRestServiceImpl {

    public static final String CONTEXT_MACHINE_BASE_URL = "/rest/contextMachine";
    public static final String HOLD_ALL_JOBS_PATH    = "/holdAllJobs";
    public static final String RELEASE_ALL_JOBS_PATH = "/releaseAllJobs";
    public static final String SKIP_ALL_JOBS_PATH      = "/skipAllJobs";
    public static final String ACKNOWLEDGE_ERROR_PATH      = "/acknowledgeError";
    public static final String BROADCAST_LOCAL_EVENT_PATH  = "/broadcastLocalEvent";
    public static final String HOLD_PATH    = "/hold";
    public static final String RELEASE_PATH = "/release";
    public static final String RESET_PATH   = "/reset";
    public static final String SKIP_PATH    = "/skip";
    public static final String DISABLE_QUARTZ_JOBS_PATH         = "/disableQuartzJobs";
    public static final String ENABLE_QUARTZ_JOBS_PATH          = "/enableQuartzJobs";
    public static final String RUN_UNTIL_MANUALLY_ENDED_PATH    = "/runUntilManuallyEnded";
    public static final String RELEASE_QUEUED_JOBS_PATH         = "/releaseQueuedJobs";
    public static final String KILL_RUNNING_JOBS_PATH           = "/killRunningJobs";
    public static final String SAVE_CONTEXT_PATH                = "/saveContext";
    public static final String UPDATE_CONTEXT_PARAMETERS_PATH   = "/updateContextParameters";
    public static final String DRY_RUN_PARAMETERS_PATH          = "/dryRunParameters";
    public static final String BROADCAST_GLOBAL_EVENTS_PATH     = "/broadcastGlobalEvents";
    public static final String PUBLISH_JOB_INITIATION_PATH      = "/publishJobInitiationEvent";
    public static final String ADD_QUEUED_INITIATION_PATH       = "/addQueuedInitiationEvent";
    public static final String RESUBMIT_DLQ_PATH                = "/resubmitDlq";
    public static final String GET_CONTEXT_PATH                 = "/context";
    public static final String GET_CONTEXT_STATUS_PATH          = "/contextStatus";
    public static final String GET_JOB_STATUS_PATH              = "/jobStatus";
    public static final String GET_INSTANCE_STATUS_PATH         = "/instanceStatus";
    public static final String IS_DRY_RUN_PATH                  = "/isDryRun";
    public static final String GET_DLQ_MESSAGES_PATH             = "/dlqMessages";

    public ContextMachineRestServiceImpl(String baseUrl, Environment environment,
                                         HttpComponentsClientHttpRequestFactory factory) {
        super(baseUrl, environment, factory);
    }

    public boolean holdAllJobs(String contextInstanceId, String childContextName) {
        return invoke(instancePath(contextInstanceId, HOLD_ALL_JOBS_PATH),
            new JobActionBody(null, childContextName));
    }

    public boolean releaseAllJobs(String contextInstanceId, String childContextName) {
        return invoke(instancePath(contextInstanceId, RELEASE_ALL_JOBS_PATH),
            new JobActionBody(null, childContextName));
    }

    public boolean skipAllJobs(String contextInstanceId, String childContextName, boolean skipFlag) {
        return invoke(instancePath(contextInstanceId, SKIP_ALL_JOBS_PATH),
            new JobActionBody(null, childContextName, skipFlag));
    }

    public boolean holdJob(String contextInstanceId, String jobIdentifier, String childContextName) {
        return invoke(instancePath(contextInstanceId, HOLD_PATH),
            new JobActionBody(jobIdentifier, childContextName));
    }

    public boolean releaseJob(String contextInstanceId, String jobIdentifier, String childContextName) {
        return invoke(instancePath(contextInstanceId, RELEASE_PATH),
            new JobActionBody(jobIdentifier, childContextName));
    }

    public boolean resetJob(String contextInstanceId, String jobIdentifier, String childContextName) {
        return invoke(instancePath(contextInstanceId, RESET_PATH),
            new JobActionBody(jobIdentifier, childContextName));
    }

    public boolean skipJob(String contextInstanceId, String jobIdentifier, String childContextName, boolean skipFlag) {
        return invoke(instancePath(contextInstanceId, SKIP_PATH),
            new JobActionBody(jobIdentifier, childContextName, skipFlag));
    }

    public boolean acknowledgeError(String contextInstanceId, String identifier,
                                    boolean targetResidingContextOnly, String childContextName,
                                    List<String> childContextNames) {
        return invoke(instancePath(contextInstanceId, ACKNOWLEDGE_ERROR_PATH),
            new AcknowledgeBody(identifier, targetResidingContextOnly, childContextName, childContextNames));
    }

    public boolean broadcastLocalEvent(String contextInstanceId, String agentName, String jobName,
                                       String contextName, String eventContextInstanceId,
                                       List<String> childContextNames) {
        return invoke(instancePath(contextInstanceId, BROADCAST_LOCAL_EVENT_PATH),
            new BroadcastLocalEventBody(agentName, jobName, contextName, eventContextInstanceId, childContextNames));
    }

    // ── No-body write operations ────────────────────────────────────────────────────────────────

    public boolean disableQuartzJobs(String contextInstanceId) {
        return invokeNoBody(instancePath(contextInstanceId, DISABLE_QUARTZ_JOBS_PATH));
    }

    public boolean enableQuartzJobs(String contextInstanceId) {
        return invokeNoBody(instancePath(contextInstanceId, ENABLE_QUARTZ_JOBS_PATH));
    }

    public boolean runUntilManuallyEnded(String contextInstanceId) {
        return invokeNoBody(instancePath(contextInstanceId, RUN_UNTIL_MANUALLY_ENDED_PATH));
    }

    public boolean releaseQueuedJobs(String contextInstanceId) {
        return invokeNoBody(instancePath(contextInstanceId, RELEASE_QUEUED_JOBS_PATH));
    }

    public boolean killRunningJobs(String contextInstanceId) {
        return invokeNoBody(instancePath(contextInstanceId, KILL_RUNNING_JOBS_PATH));
    }

    public boolean saveContext(String contextInstanceId) {
        return invokeNoBody(instancePath(contextInstanceId, SAVE_CONTEXT_PATH));
    }

    public boolean updateContextParameters(String contextInstanceId, List<ContextParameterInstance> contextParameters) {
        return invoke(instancePath(contextInstanceId, UPDATE_CONTEXT_PARAMETERS_PATH), contextParameters);
    }

    // ── Write operations with body ──────────────────────────────────────────────────────────────

    public boolean setDryRunParameters(String contextInstanceId, long minExecutionTimeMillis,
                                       long maxExecutionTimeMillis, long fixedExecutionTimeMillis,
                                       double jobErrorPercentage, boolean error) {
        return invoke(instancePath(contextInstanceId, DRY_RUN_PARAMETERS_PATH),
            new DryRunParametersBody(minExecutionTimeMillis, maxExecutionTimeMillis,
                fixedExecutionTimeMillis, jobErrorPercentage, error));
    }

    public boolean broadcastGlobalEvents(String contextInstanceId, String agentName, String jobName,
                                         String contextName, String eventContextInstanceId,
                                         List<String> childContextNames,
                                         boolean ignoreEnvironmentGroup, boolean forceSending) {
        return invoke(instancePath(contextInstanceId, BROADCAST_GLOBAL_EVENTS_PATH),
            new BroadcastGlobalEventsBody(agentName, jobName, contextName, eventContextInstanceId,
                childContextNames, ignoreEnvironmentGroup, forceSending));
    }

    public boolean publishJobInitiationEvent(String contextInstanceId, String agentName, String jobName,
                                              String contextName, String eventContextInstanceId,
                                              List<String> childContextNames) {
        return invoke(instancePath(contextInstanceId, PUBLISH_JOB_INITIATION_PATH),
            new BroadcastLocalEventBody(agentName, jobName, contextName, eventContextInstanceId, childContextNames));
    }

    public boolean addQueuedInitiationEvent(String contextInstanceId, String agentName, String jobName,
                                             String contextName, String eventContextInstanceId,
                                             List<String> childContextNames) {
        return invoke(instancePath(contextInstanceId, ADD_QUEUED_INITIATION_PATH),
            new BroadcastLocalEventBody(agentName, jobName, contextName, eventContextInstanceId, childContextNames));
    }

    public boolean resubmitDlq(String contextInstanceId, String messageId) {
        return invoke(instancePath(contextInstanceId, RESUBMIT_DLQ_PATH),
            new MessageIdBody(messageId));
    }

    // ── Read operations ─────────────────────────────────────────────────────────────────────────

    public ContextInstance getContextFromPeer(String contextInstanceId) {
        return get(instancePath(contextInstanceId, GET_CONTEXT_PATH), ContextInstanceImpl.class);
    }

    public ContextInstance getChildContextFromPeer(String contextInstanceId, String contextName) {
        return get(instancePath(contextInstanceId, GET_CONTEXT_PATH + "/" + contextName), ContextInstanceImpl.class);
    }

    public InstanceStatus getContextStatusFromPeer(String contextInstanceId, String contextName) {
        return get(instancePath(contextInstanceId, GET_CONTEXT_STATUS_PATH + "/" + contextName), InstanceStatus.class);
    }

    public InstanceStatus getJobStatusFromPeer(String contextInstanceId, String contextName, String jobIdentifier) {
        return get(instancePath(contextInstanceId, GET_JOB_STATUS_PATH + "/" + contextName + "/" + jobIdentifier), InstanceStatus.class);
    }

    public ContextInstanceStatus getInstanceStatusFromPeer(String contextInstanceId) {
        return get(instancePath(contextInstanceId, GET_INSTANCE_STATUS_PATH), ContextInstanceStatus.class);
    }

    public Boolean isDryRunOnPeer(String contextInstanceId) {
        return get(instancePath(contextInstanceId, IS_DRY_RUN_PATH), Boolean.class);
    }

    public List<BigQueueMessage> getDlqMessagesFromPeer(String contextInstanceId) {
        return getList(instancePath(contextInstanceId, GET_DLQ_MESSAGES_PATH), BigQueueMessageDto.class);
    }

    public boolean deleteDlqMessage(String contextInstanceId, String messageId) {
        return invokeDelete(instancePath(contextInstanceId, GET_DLQ_MESSAGES_PATH + "/" + messageId));
    }

    public boolean deleteAllDlqMessages(String contextInstanceId) {
        return invokeDelete(instancePath(contextInstanceId, GET_DLQ_MESSAGES_PATH));
    }

    private String instancePath(String contextInstanceId, String action) {
        return CONTEXT_MACHINE_BASE_URL + "/" + contextInstanceId + action;
    }

    /** Inline DTO matching ContextMachineJobActionDto on the server side. */
    @SuppressWarnings("unused")
    private static class JobActionBody {
        private final String jobIdentifier;
        private final String childContextName;
        private final boolean skipFlag;

        JobActionBody(String jobIdentifier, String childContextName) {
            this(jobIdentifier, childContextName, false);
        }

        JobActionBody(String jobIdentifier, String childContextName, boolean skipFlag) {
            this.jobIdentifier = jobIdentifier;
            this.childContextName = childContextName;
            this.skipFlag = skipFlag;
        }

        public String getJobIdentifier() { return jobIdentifier; }
        public String getChildContextName() { return childContextName; }
        public boolean isSkipFlag() { return skipFlag; }
    }

    /** Inline DTO matching AcknowledgeJobDto on the server side. */
    @SuppressWarnings("unused")
    private static class AcknowledgeBody {
        private final String identifier;
        private final boolean targetResidingContextOnly;
        private final String childContextName;
        private final List<String> childContextNames;

        AcknowledgeBody(String identifier, boolean targetResidingContextOnly,
                        String childContextName, List<String> childContextNames) {
            this.identifier = identifier;
            this.targetResidingContextOnly = targetResidingContextOnly;
            this.childContextName = childContextName;
            this.childContextNames = childContextNames;
        }

        public String getIdentifier() { return identifier; }
        public boolean isTargetResidingContextOnly() { return targetResidingContextOnly; }
        public String getChildContextName() { return childContextName; }
        public List<String> getChildContextNames() { return childContextNames; }
    }

    /** Inline DTO matching BroadcastLocalEventDto on the server side. */
    @SuppressWarnings("unused")
    private static class BroadcastLocalEventBody {
        private final String agentName;
        private final String jobName;
        private final String contextName;
        private final String contextInstanceId;
        private final List<String> childContextNames;

        BroadcastLocalEventBody(String agentName, String jobName, String contextName,
                                String contextInstanceId, List<String> childContextNames) {
            this.agentName = agentName;
            this.jobName = jobName;
            this.contextName = contextName;
            this.contextInstanceId = contextInstanceId;
            this.childContextNames = childContextNames;
        }

        public String getAgentName() { return agentName; }
        public String getJobName() { return jobName; }
        public String getContextName() { return contextName; }
        public String getContextInstanceId() { return contextInstanceId; }
        public List<String> getChildContextNames() { return childContextNames; }
    }

    @SuppressWarnings("unused")
    private static class DryRunParametersBody {
        private final long minExecutionTimeMillis;
        private final long maxExecutionTimeMillis;
        private final long fixedExecutionTimeMillis;
        private final double jobErrorPercentage;
        private final boolean error;

        DryRunParametersBody(long minExecutionTimeMillis, long maxExecutionTimeMillis,
                             long fixedExecutionTimeMillis, double jobErrorPercentage, boolean error) {
            this.minExecutionTimeMillis = minExecutionTimeMillis;
            this.maxExecutionTimeMillis = maxExecutionTimeMillis;
            this.fixedExecutionTimeMillis = fixedExecutionTimeMillis;
            this.jobErrorPercentage = jobErrorPercentage;
            this.error = error;
        }

        public long getMinExecutionTimeMillis() { return minExecutionTimeMillis; }
        public long getMaxExecutionTimeMillis() { return maxExecutionTimeMillis; }
        public long getFixedExecutionTimeMillis() { return fixedExecutionTimeMillis; }
        public double getJobErrorPercentage() { return jobErrorPercentage; }
        public boolean isError() { return error; }
    }

    @SuppressWarnings("unused")
    private static class BroadcastGlobalEventsBody {
        private final String agentName;
        private final String jobName;
        private final String contextName;
        private final String contextInstanceId;
        private final List<String> childContextNames;
        private final boolean ignoreEnvironmentGroup;
        private final boolean forceSending;

        BroadcastGlobalEventsBody(String agentName, String jobName, String contextName,
                                   String contextInstanceId, List<String> childContextNames,
                                   boolean ignoreEnvironmentGroup, boolean forceSending) {
            this.agentName = agentName;
            this.jobName = jobName;
            this.contextName = contextName;
            this.contextInstanceId = contextInstanceId;
            this.childContextNames = childContextNames;
            this.ignoreEnvironmentGroup = ignoreEnvironmentGroup;
            this.forceSending = forceSending;
        }

        public String getAgentName() { return agentName; }
        public String getJobName() { return jobName; }
        public String getContextName() { return contextName; }
        public String getContextInstanceId() { return contextInstanceId; }
        public List<String> getChildContextNames() { return childContextNames; }
        public boolean isIgnoreEnvironmentGroup() { return ignoreEnvironmentGroup; }
        public boolean isForceSending() { return forceSending; }
    }

    @SuppressWarnings("unused")
    private static class MessageIdBody {
        private final String messageId;
        MessageIdBody(String messageId) { this.messageId = messageId; }
        public String getMessageId() { return messageId; }
    }

    @SuppressWarnings("unused")
    private static class ContextParametersBody {
        private final List<ContextParameterInstance> contextParameters;
        ContextParametersBody(List<ContextParameterInstance> contextParameters) {
            this.contextParameters = contextParameters;
        }
        public List<ContextParameterInstance> getContextParameters() { return contextParameters; }
    }
}
