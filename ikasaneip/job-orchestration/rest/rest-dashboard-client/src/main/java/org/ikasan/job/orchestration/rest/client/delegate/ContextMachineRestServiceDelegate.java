package org.ikasan.job.orchestration.rest.client.delegate;

import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.rest.client.ClusterPeerRestServiceImpl;
import org.ikasan.rest.client.dto.BigQueueMessageDto;
import org.ikasan.spec.bigqueue.message.BigQueueMessage;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceStatus;
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
public class ContextMachineRestServiceDelegate extends ClusterPeerRestServiceImpl {

    public static final String CONTEXT_MACHINE_BASE_URL = "/rest/contextMachine";
    public static final String HOLD_ALL_JOBS_PATH    = "/holdAllJobs";
    public static final String RELEASE_ALL_JOBS_PATH = "/releaseAllJobs";
    public static final String SKIP_ALL_JOBS_PATH      = "/skipAllJobs";
    public static final String DISABLE_ALL_JOBS_PATH      = "/disableAllJobs";
    public static final String REINSTATE_ALL_DISABLED_JOBS_PATH      = "/reinstateAllDisabledJobs";
    public static final String ACKNOWLEDGE_ERROR_PATH      = "/acknowledgeError";
    public static final String BROADCAST_LOCAL_EVENT_PATH  = "/broadcastLocalEvent";
    public static final String HOLD_PATH    = "/hold";
    public static final String RELEASE_PATH = "/release";
    public static final String RESET_PATH   = "/reset";
    public static final String SKIP_PATH    = "/skip";
    public static final String DISABLE_PATH    = "/disable";
    public static final String REINSTATE_DISABLED_PATH    = "/reinstateDisabled";
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

    /**
     * Constructs a new instance of {@code ContextMachineRestServiceDelegate}.
     *
     * @param baseUrl the base URL for the REST service.
     * @param environment the environment from which configuration properties can be retrieved.
     * @param factory the HTTP request factory used to create and configure HTTP requests.
     */
    public ContextMachineRestServiceDelegate(String baseUrl, Environment environment,
                                             HttpComponentsClientHttpRequestFactory factory) {
        super(baseUrl, environment, factory);
    }

    /**
     * Places all jobs within a specified context instance and its child context into a hold state.
     * This operation prevents the jobs from being executed until they are explicitly released.
     *
     * @param contextInstanceId the identifier of the context instance containing the jobs to be held
     * @param childContextName the name of the child context where the jobs should be held
     * @return {@code true} if the operation was successful, {@code false} otherwise
     */
    public boolean holdAllJobs(String contextInstanceId, String childContextName) {
        return invoke(instancePath(contextInstanceId, HOLD_ALL_JOBS_PATH),
            new JobActionBody(null, childContextName));
    }

    /**
     * Releases all jobs for a specific context instance and an optional child context.
     *
     * @param contextInstanceId the unique identifier of the context instance for which all jobs
     *                          need to be released
     * @param childContextName  the name of the child context within the specified context
     *                          instance; may be null if no child context is specified
     * @return true if the*/
    public boolean releaseAllJobs(String contextInstanceId, String childContextName) {
        return invoke(instancePath(contextInstanceId, RELEASE_ALL_JOBS_PATH),
            new JobActionBody(null, childContextName));
    }

    /**
     * Skips all jobs within the specified context and child context based on the provided skip flag.
     *
     * @param contextInstanceId the unique identifier of the context instance where the jobs reside
     * @param childContextName the name of the child context within the specified context instance
     * @param skipFlag a flag indicating whether jobs should be skipped (true) or unskipped (false)
     * @return true if the operation is successfully handled by the remote node, false otherwise
     */
    public boolean skipAllJobs(String contextInstanceId, String childContextName, boolean skipFlag) {
        return invoke(instancePath(contextInstanceId, SKIP_ALL_JOBS_PATH),
            new JobActionBody(null, childContextName, skipFlag));
    }

    /**
     * Places a specific job in a hold state within the specified context instance.
     *
     * @param contextInstanceId the unique identifier of the context instance where the job resides
     * @param jobIdentifier the unique identifier of the job to be held
     * @param childContextName the name of the child*/
    public boolean holdJob(String contextInstanceId, String jobIdentifier, String childContextName) {
        return invoke(instancePath(contextInstanceId, HOLD_PATH),
            new JobActionBody(jobIdentifier, childContextName));
    }

    /**
     * Releases a specific job from its hold state using the provided context information.
     *
     * @param contextInstanceId the unique identifier of the context instance where the job resides
     * @param jobIdentifier the unique identifier of the job to be released
     * @param childContextName the name of the child context associated with the job
     * @return true if the job is successfully released, otherwise false
     */
    public boolean releaseJob(String contextInstanceId, String jobIdentifier, String childContextName) {
        return invoke(instancePath(contextInstanceId, RELEASE_PATH),
            new JobActionBody(jobIdentifier, childContextName));
    }

    /**
     * Resets a job with the specified identifiers and context information.
     *
     * @param contextInstanceId the identifier of the context instance in which the job resides
     * @param jobIdentifier the unique identifier of the job to be reset
     * @param childContextName the name of the child context associated with the job
     * @return true if the job reset was successful, false otherwise
     */
    public boolean resetJob(String contextInstanceId, String jobIdentifier, String childContextName) {
        return invoke(instancePath(contextInstanceId, RESET_PATH),
            new JobActionBody(jobIdentifier, childContextName));
    }

    /**
     * Skips the execution of a specific job within a context or its child context, based on the provided flag.
     *
     * @param contextInstanceId the unique identifier of the context instance within which the job is defined.
     * @param jobIdentifier the unique identifier of the job to be skipped.
     * @param childContextName the name of the child context in which the job resides.
     * @param skipFlag a flag indicating whether to skip the job (true) or not (false).
     * @return a boolean indicating whether the request to skip the job was successfully handled*/
    public boolean skipJob(String contextInstanceId, String jobIdentifier, String childContextName, boolean skipFlag) {
        return invoke(instancePath(contextInstanceId, SKIP_PATH),
            new JobActionBody(jobIdentifier, childContextName, skipFlag));
    }

    /**
     * Disables all jobs associated with the specified context instance and child context.
     *
     * @param contextInstanceId the unique identifier of the context instance whose jobs are to be disabled
     * @param childContextName the name of the child context for which the jobs are to be disabled
     * @return true if the jobs were successfully disabled, false otherwise
     */
    public boolean disableJobs(String contextInstanceId, String childContextName) {
        return invoke(instancePath(contextInstanceId, DISABLE_ALL_JOBS_PATH),
            new JobActionBody(null, childContextName));
    }

    /**
     * Disables a specific job within a given context instance.
     *
     * @param contextInstanceId the unique identifier of the context instance where the job exists
     * @param jobIdentifier the unique identifier of the job to be disabled
     * @param childContextName the name of the child context associated with the job
     * @return true if the disable action was successfully processed by the remote node, false otherwise
     */
    public boolean disableJob(String contextInstanceId, String jobIdentifier, String childContextName) {
        return invoke(instancePath(contextInstanceId, DISABLE_PATH),
            new JobActionBody(jobIdentifier, childContextName));
    }

    /**
     * Reinstates all disabled jobs for a specific context instance and child context name.
     *
     * @param contextInstanceId the unique identifier of the context instance.
     * @param childContextName the name of the child context within the specified context instance.
     * @return {@code true} if the operation was successful; {@code false*/
    public boolean reinstateDisabledJobs(String contextInstanceId, String childContextName) {
        return invoke(instancePath(contextInstanceId, REINSTATE_ALL_DISABLED_JOBS_PATH),
            new JobActionBody(null, childContextName));
    }

    /**
     * Enables a specific job within the given context instance and child context.
     *
     * @param contextInstanceId the unique identifier of the context instance where the job resides
     * @param jobIdentifier the identifier of the job to be enabled
     * @param childContextName the name of the child context containing the job
     * @return {@code true} if the job was successfully enabled; {@code false} otherwise
     */
    public boolean enableJob(String contextInstanceId, String jobIdentifier, String childContextName) {
        return invoke(instancePath(contextInstanceId, REINSTATE_DISABLED_PATH),
            new JobActionBody(jobIdentifier, childContextName));
    }

    /**
     * Attempts to acknowledge an error identified by the given identifier within a specific context instance.
     *
     * @param contextInstanceId the unique identifier of the context instance where the error resides
     * @param identifier the identifier of the specific error to be acknowledged
     * @param targetResidingContextOnly a flag indicating whether to restrict the acknowledgment to the residing context only
     * @param childContextName the name of a single child context to consider for acknowledgment
     * @param childContextNames a list of additional child context names to include in the acknowledgment operation
     * @return true if the acknowledgment is successful, false otherwise
     */
    public boolean acknowledgeError(String contextInstanceId, String identifier,
                                    boolean targetResidingContextOnly, String childContextName,
                                    List<String> childContextNames) {
        return invoke(instancePath(contextInstanceId, ACKNOWLEDGE_ERROR_PATH),
            new AcknowledgeBody(identifier, targetResidingContextOnly, childContextName, childContextNames));
    }

    /**
     * Broadcasts a local event to a specific context instance and its associated child contexts.
     *
     * @param contextInstanceId the identifier of the context instance where the event is being broadcasted.
     * @param agentName the name of the agent associated with this event.
     * @param jobName the name of the job associated with this event.
     * @param contextName the name of the context where the event is triggered.
     * @param eventContextInstanceId the identifier of the context instance related to the event.
     * @param childContextNames a list of child context names to which the event is also propagated.
     * @return {@code true} if the event was successfully broadcasted; {@code false} otherwise.
     */
    public boolean broadcastLocalEvent(String contextInstanceId, String agentName, String jobName,
                                       String contextName, String eventContextInstanceId,
                                       List<String> childContextNames) {
        return invoke(instancePath(contextInstanceId, BROADCAST_LOCAL_EVENT_PATH),
            new BroadcastLocalEventBody(agentName, jobName, contextName, eventContextInstanceId, childContextNames));
    }

    // ── No-body write operations ────────────────────────────────────────────────────────────────


    /**
     * Disables Quartz jobs for the specified context instance.
     *
     * @param contextInstanceId the unique identifier of the context instance for which Quartz jobs should be disabled
     * @return true if the operation was successful, false otherwise
     */
    public boolean disableQuartzJobs(String contextInstanceId) {
        return invokeNoBody(instancePath(contextInstanceId, DISABLE_QUARTZ_JOBS_PATH));
    }

    /**
     * Enables Quartz jobs for a given context instance.
     *
     * @param contextInstanceId the ID of the context instance for which Quartz jobs should be enabled.
     * @return {@code true} if the Quartz jobs were successfully enabled (HTTP 2xx), {@code false} otherwise.
     */
    public boolean enableQuartzJobs(String contextInstanceId) {
        return invokeNoBody(instancePath(contextInstanceId, ENABLE_QUARTZ_JOBS_PATH));
    }

    /**
     * Executes a process or operation identified by the given context instance ID
     * and continues running until it is manually ended.
     *
     * @param contextInstanceId the unique identifier of the context instance to be executed
     * @return true if the process was successfully initiated to run until manually ended, false otherwise
     */
    public boolean runUntilManuallyEnded(String contextInstanceId) {
        return invokeNoBody(instancePath(contextInstanceId, RUN_UNTIL_MANUALLY_ENDED_PATH));
    }

    /**
     * Releases all queued jobs for the specified context instance.
     *
     * @param contextInstanceId the identifier of the context instance for which
     *                          queued jobs should be released.
     * @return true if the operation was successful (HTTP 2xx), false otherwise.
     */
    public boolean releaseQueuedJobs(String contextInstanceId) {
        return invokeNoBody(instancePath(contextInstanceId, RELEASE_QUEUED_JOBS_PATH));
    }

    /**
     * Terminates all currently running jobs associated with the specified context instance ID.
     *
     * @param contextInstanceId the unique identifier of the context instance for which running jobs are to be terminated
     * @return {@code true} if the jobs were successfully terminated, {@code false} otherwise
     */
    public boolean killRunningJobs(String contextInstanceId) {
        return invokeNoBody(instancePath(contextInstanceId, KILL_RUNNING_JOBS_PATH));
    }

    /**
     * Saves the given context by invoking the appropriate service operation.
     *
     * @param contextInstanceId the unique identifier of the context instance to be saved
     * @return true if the context was successfully saved, false otherwise
     */
    public boolean saveContext(String contextInstanceId) {
        return invokeNoBody(instancePath(contextInstanceId, SAVE_CONTEXT_PATH));
    }

    /**
     * Updates the parameters of a specific context instance.
     *
     * @param contextInstanceId the unique identifier of the context instance whose parameters need to be updated
     * @param contextParameters a list of context parameter instances to be updated for the specified context instance
     * @return true if the update operation was successful, false otherwise
     */
    public boolean updateContextParameters(String contextInstanceId, List<ContextParameterInstance> contextParameters) {
        return invoke(instancePath(contextInstanceId, UPDATE_CONTEXT_PARAMETERS_PATH), contextParameters);
    }

    // ── Write operations with body ──────────────────────────────────────────────────────────────

    /**
     * Configures dry-run parameters for a specific context instance by sending a request to the
     * corresponding REST API. These parameters can influence the execution behavior and simulate
     * execution times and error rates for jobs within the context.
     *
     * @param contextInstanceId the unique identifier of the context instance for which the dry-run
     *                          parameters are being set
     * @param minExecutionTimeMillis the minimum execution time in milliseconds for simulated jobs
     * @param maxExecutionTimeMillis the maximum execution time in milliseconds for simulated jobs
     * @param fixedExecutionTimeMillis the fixed execution time in milliseconds for simulated jobs,
     *                                 when applicable
     * @param jobErrorPercentage the percentage of simulated jobs that should fail during execution
     * @param error a flag indicating whether an error condition should be simulated
     * @return true if the request to configure the dry-run parameters was successful, false otherwise
     */
    public boolean setDryRunParameters(String contextInstanceId, long minExecutionTimeMillis,
                                       long maxExecutionTimeMillis, long fixedExecutionTimeMillis,
                                       double jobErrorPercentage, boolean error) {
        return invoke(instancePath(contextInstanceId, DRY_RUN_PARAMETERS_PATH),
            new DryRunParametersBody(minExecutionTimeMillis, maxExecutionTimeMillis,
                fixedExecutionTimeMillis, jobErrorPercentage, error));
    }

    /**
     * Broadcasts global events to the specified context instance and its child contexts.
     *
     * @param contextInstanceId the identifier of the context instance where the event should be sent.
     * @param agentName the name of the agent associated with the event.
     * @param jobName the name of the job related to the event.
     * @param contextName the name of the context for which the event is relevant.
     * @param eventContextInstanceId the identifier of the event's associated context instance.
     * @param childContextNames the list of child context names where the event should be propagated.
     * @param ignoreEnvironmentGroup a flag indicating whether the environment group should be ignored.
     * @param forceSending a flag indicating whether to force the event*/
    public boolean broadcastGlobalEvents(String contextInstanceId, String agentName, String jobName,
                                         String contextName, String eventContextInstanceId,
                                         List<String> childContextNames,
                                         boolean ignoreEnvironmentGroup, boolean forceSending) {
        return invoke(instancePath(contextInstanceId, BROADCAST_GLOBAL_EVENTS_PATH),
            new BroadcastGlobalEventsBody(agentName, jobName, contextName, eventContextInstanceId,
                childContextNames, ignoreEnvironmentGroup, forceSending));
    }

    /**
     * Publishes a job initiation event to a specified context instance path.
     *
     * @param contextInstanceId        The unique identifier of the context instance where the event will be published.
     * @param agentName                The name of the agent responsible for initiating the job.
     * @param jobName                  The name of the job being initiated.
     * @param contextName              The context name associated with the initiation event.
     * @param eventContextInstanceId   The unique identifier of the event-specific context instance.
     * @param childContextNames        A list of child context names related to the job initiation event.
     * @return                         A boolean indicating whether the job initiation event was successfully published.
     */
    public boolean publishJobInitiationEvent(String contextInstanceId, String agentName, String jobName,
                                              String contextName, String eventContextInstanceId,
                                              List<String> childContextNames) {
        return invoke(instancePath(contextInstanceId, PUBLISH_JOB_INITIATION_PATH),
            new BroadcastLocalEventBody(agentName, jobName, contextName, eventContextInstanceId, childContextNames));
    }

    /**
     * Adds a queued initiation event for a specific context instance.
     *
     * @param contextInstanceId The identifier of the context instance where the event should be added.
     * @param agentName The name of the agent responsible for this initiation event.
     * @param jobName The name of the job associated with this event.
     * @param contextName The name of the context where the event originates.
     * @param eventContextInstanceId The identifier of the event's context instance.
     * @param childContextNames A list of child context names associated with this event.
     * @return A boolean indicating whether the event was successfully added to the queue.
     */
    public boolean addQueuedInitiationEvent(String contextInstanceId, String agentName, String jobName,
                                             String contextName, String eventContextInstanceId,
                                             List<String> childContextNames) {
        return invoke(instancePath(contextInstanceId, ADD_QUEUED_INITIATION_PATH),
            new BroadcastLocalEventBody(agentName, jobName, contextName, eventContextInstanceId, childContextNames));
    }

    /**
     * Resubmits a message from the Dead Letter Queue (DLQ) for processing.
     *
     * @param contextInstanceId the identifier of the context instance associated with the DLQ message
     * @param messageId the unique identifier of the message to be resubmitted
     * @return true if the message was successfully resubmitted, false otherwise
     */
    public boolean resubmitDlq(String contextInstanceId, String messageId) {
        return invoke(instancePath(contextInstanceId, RESUBMIT_DLQ_PATH),
            new MessageIdBody(messageId));
    }

    // ── Read operations ─────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves a ContextInstance object from a peer system based on the provided context instance identifier.
     *
     * @param contextInstanceId the unique identifier of the context instance to be retrieved
     * @return the ContextInstance object corresponding to the specified identifier
     */
    public ContextInstance getContextFromPeer(String contextInstanceId) {
        return get(instancePath(contextInstanceId, GET_CONTEXT_PATH), ContextInstanceImpl.class);
    }

    /**
     * Retrieves a child context instance from a peer using the specified context instance ID and context name.
     *
     * @param contextInstanceId the identifier of the parent context instance from which the child context is retrieved
     * @param contextName the name of the child context to retrieve
     * @return the child context instance corresponding to the specified ID and name
     */
    public ContextInstance getChildContextFromPeer(String contextInstanceId, String contextName) {
        return get(instancePath(contextInstanceId, GET_CONTEXT_PATH + "/" + contextName), ContextInstanceImpl.class);
    }

    /**
     * Retrieves the status of a specific context instance from a peer system.
     *
     * @param contextInstanceId the unique identifier of the context instance.
     * @param contextName the name of the context whose status is being retrieved.
     * @return the status of the specified context instance as an InstanceStatus object.
     */
    public InstanceStatus getContextStatusFromPeer(String contextInstanceId, String contextName) {
        return get(instancePath(contextInstanceId, GET_CONTEXT_STATUS_PATH + "/" + contextName), InstanceStatus.class);
    }

    /**
     * Retrieves the status of a job from a peer system using the specified context and job details.
     *
     * @param contextInstanceId the unique identifier of the context instance.
     * @param contextName the name of the context to which the job belongs.
     * @param jobIdentifier the unique identifier of the job whose status is being requested.
     * @return an InstanceStatus object representing the status of the specified job.
     */
    public InstanceStatus getJobStatusFromPeer(String contextInstanceId, String contextName, String jobIdentifier) {
        return get(instancePath(contextInstanceId, GET_JOB_STATUS_PATH + "/" + contextName + "/" + jobIdentifier), InstanceStatus.class);
    }

    /**
     * Retrieves the status of a specific context instance from a peer service.
     *
     * @param contextInstanceId The unique identifier of the context instance whose status is to be fetched.
     * @return The status of the specified context instance encapsulated in a {@code ContextInstanceStatus} object.
     */
    public ContextInstanceStatus getInstanceStatusFromPeer(String contextInstanceId) {
        return get(instancePath(contextInstanceId, GET_INSTANCE_STATUS_PATH), ContextInstanceStatus.class);
    }

    /**
     * Determines whether the dry run mode is enabled for a specific peer instance.
     *
     * @param contextInstanceId the unique identifier of the context instance to check
     * @return {@code true} if the dry run mode is enabled on the specified peer, {@code false} otherwise
     */
    public Boolean isDryRunOnPeer(String contextInstanceId) {
        return get(instancePath(contextInstanceId, IS_DRY_RUN_PATH), Boolean.class);
    }

    /**
     * Retrieves a list of Dead Letter Queue (DLQ) messages from a peer for the specified context instance ID.
     *
     * @param contextInstanceId the unique identifier of the context instance for which DLQ messages are retrieved.
     * @return a list of {@code BigQueueMessage} objects representing the DLQ messages associated with the given context instance ID.
     */
    public List<BigQueueMessage> getDlqMessagesFromPeer(String contextInstanceId) {
        return getList(instancePath(contextInstanceId, GET_DLQ_MESSAGES_PATH), BigQueueMessageDto.class);
    }

    /**
     * Deletes a dead-letter queue (DLQ) message identified by the specified context instance ID
     * and message ID.
     *
     * @param contextInstanceId the unique identifier of the context instance associated with the DLQ message
     * @param messageId the unique identifier of the DLQ message to be deleted
     * @return true if the deletion was successful, false otherwise
     */
    public boolean deleteDlqMessage(String contextInstanceId, String messageId) {
        return invokeDelete(instancePath(contextInstanceId, GET_DLQ_MESSAGES_PATH + "/" + messageId));
    }

    /**
     * Deletes all Dead Letter Queue (DLQ) messages for the specified context instance.
     *
     * @param contextInstanceId The unique identifier of the context instance
     *                          whose DLQ messages are to be deleted.
     * @return true if the DLQ messages were successfully deleted, false otherwise.
     */
    public boolean deleteAllDlqMessages(String contextInstanceId) {
        return invokeDelete(instancePath(contextInstanceId, GET_DLQ_MESSAGES_PATH));
    }

    /**
     * Constructs a full path for a given context instance and action.
     *
     * @param contextInstanceId the unique identifier of the context instance.
     * @param action the specific action to be appended to the path.
     * @return the constructed full path as a String.
     */
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
