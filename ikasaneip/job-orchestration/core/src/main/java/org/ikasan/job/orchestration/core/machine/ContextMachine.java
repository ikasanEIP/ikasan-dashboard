package org.ikasan.job.orchestration.core.machine;

import org.ikasan.bigqueue.IBigQueue;
import org.ikasan.job.orchestration.broadcast.ContextInstanceDlqEventBroadcastListener;
import org.ikasan.job.orchestration.model.status.ContextInstanceStatus;
import org.ikasan.spec.bigqueue.service.exception.BigQueueNotFoundException;
import org.ikasan.spec.scheduled.core.listener.ContextInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInitiationEventRaisedListener;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.DryRunParameters;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.service.exception.SchedulerJobInstanceInitialisationException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Service interface for ContextMachineImpl operations.
 * This interface defines all public operations that can be performed on a ContextMachineImpl,
 * enabling both local and remote (cluster) invocations.
 */
public interface ContextMachine {

    /**
     * Initializes the context machine by setting up inbound, outbound, and dead letter queues.
     * It also sets a default number of attempts and maximum wait time for processing messages.
     *
     * @throws IOException if there are errors during initialization of the queues
     */
    void init() throws IOException;

    /**
     * Registers the current instance to notification monitors for monitoring.
     * This method will start monitoring the specified instance using MonitorManagement.
     */
    void registerToNotificationMonitors();

    /**
     * Unregisters the current instance from the notification monitors.
     * This method logs an info message about stopping the monitoring for the context and instanceId.
     * It then calls MonitorManagement to stop monitoring for the current instance.
     */
    void unregisterToNotificationMonitors();

    /**
     * This method returns the name of the outbound queue based on the context instance ID.
     *
     * @return The outbound queue name in the format "outbound-{contextInstanceId}-queue"
     */
    String getOutboundQueueName();

    /**
     * Returns the inbound queue name for this context instance.
     *
     * @return The inbound queue name in the format "inbound-{contextInstanceId}-queue"
     */
    String getInboundQueueName();

    /**
     * Returns the dead letter queue name for this context instance.
     *
     * @return The dead letter queue name in the format "dlq-{contextInstanceId}-queue"
     */
    String getDeadLetterQueueName();

    /**
     * Returns the inbound BigQueue instance.
     *
     * @return The IBigQueue instance for inbound messages
     */
    IBigQueue getInboundQueue();

    /**
     * Returns the outbound BigQueue instance.
     *
     * @return The IBigQueue instance for outbound messages
     */
    IBigQueue getOutboundQueue();

    /**
     * Returns the dead letter queue BigQueue instance.
     *
     * @return The IBigQueue instance for dead letter queue messages
     */
    IBigQueue getDeadLetterQueue();

    /**
     * Sets the executor wait timeout in seconds.
     * The timeout value must be greater than 0 to be set.
     *
     * @param executorWaitTimeoutSeconds the timeout in seconds to wait for executor shutdown
     */
    void setExecutorWaitTimeoutSeconds(int executorWaitTimeoutSeconds);

    /**
     * Sets the maximum number of retries for blacklisted messages.
     * The value must be greater than 0 to be set.
     *
     * @param blackListedMessageMaxRetries the maximum number of retries for blacklisted messages
     */
    void setBlackListedMessageMaxRetries(int blackListedMessageMaxRetries);

    /**
     * Resets the context instance with the specified parameters.
     *
     * @param holdCommandJobs flag to determine if command jobs should be held
     * @param initiateWithSameParameters flag to determine if the context should be initiated with the same parameters
     * @param contextParameterInstances list of context parameter instances
     * @throws IOException if an I/O error occurs
     * @throws SchedulerJobInstanceInitialisationException if there is an error initializing scheduler job instances
     * @throws BigQueueNotFoundException if the big queue is not found
     */
    void resetContextInstance(boolean holdCommandJobs, boolean initiateWithSameParameters,
                             List<ContextParameterInstance> contextParameterInstances)
        throws IOException, SchedulerJobInstanceInitialisationException, BigQueueNotFoundException;

    /**
     * Propagates the context instance to all configured agents.
     * If the agents map is empty, a warning is logged.
     */
    void propagateContextInstanceToAgents();

    /**
     * Tears down the context machine by cleaning up all resources including:
     * - Shutting down executors
     * - Closing queues
     * - Removing listeners
     * - Unregistering from notification monitors
     *
     * @throws IOException if an I/O error occurs during teardown
     */
    void teardown() throws IOException;

    /**
     * Sets a listener for events raised when a job is initiated in the scheduler.
     *
     * @param listener the SchedulerJobInitiationEventRaisedListener to be set as the listener
     */
    void setSchedulerJobInitiationEventRaisedListener(SchedulerJobInitiationEventRaisedListener listener);

    /**
     * Receives an event message and enqueues it to the inbound queue if the context machine is not torn down.
     *
     * @param bigQueueMessage the event message to be enqueued
     * @throws IOException if an I/O error occurs
     */
    void eventReceived(String bigQueueMessage) throws IOException;

    /**
     * Raises an event by converting the given ContextualisedScheduledProcessEvent to a JSON string
     * and enqueuing it in the inbound queue.
     *
     * @param contextualisedScheduledProcessEvent the event to be raised
     * @throws IOException if an I/O error occurs
     */
    void raiseEvent(ContextualisedScheduledProcessEvent contextualisedScheduledProcessEvent) throws IOException;

    /**
     * Gets the context status by context name.
     *
     * @param contextName the name of the context
     * @return the InstanceStatus of the context
     */
    InstanceStatus getContextStatus(String contextName);

    /**
     * Gets the status of a specific job within a context.
     *
     * @param contextName the name of the context
     * @param jobIdentifier the identifier of the job
     * @return the InstanceStatus of the job
     */
    InstanceStatus getJobStatus(String contextName, String jobIdentifier);

    /**
     * Gets the full context instance status including all jobs and their states.
     *
     * @return the ContextInstanceStatus
     */
    ContextInstanceStatus getContextInstanceStatus();

    /**
     * Gets the context instance by context name.
     *
     * @param contextName the name of the context
     * @return the ContextInstance
     */
    ContextInstance getContext(String contextName);

    /**
     * Gets the current context instance.
     *
     * @return the ContextInstance
     */
    ContextInstance getContext();

    /**
     * Adds a listener for scheduler job state change events.
     *
     * @param listener the SchedulerJobInstanceStateChangeEventListener to add
     */
    void addSchedulerJobStateChangeEventListener(SchedulerJobInstanceStateChangeEventListener listener);

    /**
     * Removes a scheduler job state change event listener.
     *
     * @param listener the SchedulerJobInstanceStateChangeEventListener to remove
     */
    void removeSchedulerJobStateChangeEventListener(SchedulerJobInstanceStateChangeEventListener listener);

    /**
     * Adds a listener for context instance state change events.
     *
     * @param listener the ContextInstanceStateChangeEventListener to add
     */
    void addContextInstanceStateChangeEventListener(ContextInstanceStateChangeEventListener listener);

    /**
     * Removes a context instance state change event listener.
     *
     * @param listener the ContextInstanceStateChangeEventListener to remove
     */
    void removeContextInstanceStateChangeEventListener(ContextInstanceStateChangeEventListener listener);

    /**
     * Adds a listener for context instance dead letter queue events.
     *
     * @param listener the ContextInstanceDlqEventBroadcastListener to add
     */
    void addContextInstanceDlqEventEventBroadcastListeners(ContextInstanceDlqEventBroadcastListener listener);

    /**
     * Removes a context instance DLQ event broadcast listener.
     *
     * @param listener the ContextInstanceDlqEventBroadcastListener to remove
     */
    void removeContextInstanceDlqEventEventBroadcastListeners(ContextInstanceDlqEventBroadcastListener listener);

    /**
     * Sets the dry run parameters for this context machine.
     *
     * @param dryRunParameters the DryRunParameters to set
     */
    void setDryRunParameters(DryRunParameters dryRunParameters);

    /**
     * Checks if the context machine is in dry run mode.
     *
     * @return true if dry run parameters are set, false otherwise
     */
    boolean isDryRun();

    /**
     * Disables all Quartz-based scheduled jobs in this context.
     */
    void disableQuartzBasedJobs();

    /**
     * Enables all Quartz-based scheduled jobs in this context.
     */
    void enableQuartzBasedJobs();

    /**
     * Runs the context continuously until manually ended.
     */
    void runContextUntilManuallyEnded();

    /**
     * Skips a specific job in the context.
     *
     * @param jobIdentifier the identifier of the job to skip
     * @param childContextName the name of the child context
     * @param skipFlag true to skip the job, false to unskip
     */
    void skipJob(String jobIdentifier, String childContextName, boolean skipFlag);

    /**
     * Skips all jobs in a child context.
     *
     * @param childContextName the name of the child context
     * @param skipFlag true to skip all jobs, false to unskip
     */
    void skipJobs(String childContextName, boolean skipFlag);

    /**
     * Holds a specific job, preventing it from executing.
     *
     * @param jobIdentifier the identifier of the job to hold
     * @param childContextName the name of the child context
     */
    void holdJob(String jobIdentifier, String childContextName);

    /**
     * Resets a specific job to its initial state.
     *
     * @param jobIdentifier the identifier of the job to reset
     * @param childContextName the name of the child context
     */
    void resetJob(String jobIdentifier, String childContextName);

    /**
     * Releases a held job, allowing it to execute.
     *
     * @param jobIdentifier the identifier of the job to release
     * @param childContextName the name of the child context
     */
    void releaseJob(String jobIdentifier, String childContextName);

    /**
     * Acknowledges an error on a scheduler job instance, allowing it to proceed.
     *
     * @param schedulerJobInstance the InternalEventDrivenJobInstance with the error
     */
    void acknowledgeSchedulerJobError(InternalEventDrivenJobInstance schedulerJobInstance);

    /**
     * Releases all queued jobs, allowing them to execute.
     */
    void releaseQueuedJobs();

    /**
     * Kills all currently running jobs.
     */
    void killRunningJobs();

    /**
     * Gets the list of scheduler job initiation events that can run for a given contextualized scheduled process event.
     *
     * @param contextualisedScheduledProcessEvent the event to check
     * @return list of SchedulerJobInitiationEvents that can run
     */
    List<SchedulerJobInitiationEvent> getEventsThatCanRun(ContextualisedScheduledProcessEvent contextualisedScheduledProcessEvent);

    /**
     * Checks if this context machine serves a specific agent.
     *
     * @param agentName the name of the agent
     * @return true if the agent is served, false otherwise
     */
    boolean servesAgent(String agentName);

    /**
     * Adds a scheduler job initiation event to the queue.
     *
     * @param event the SchedulerJobInitiationEvent to add
     */
    void addQueuedSchedulerJobInitiationEvent(SchedulerJobInitiationEvent event);

    /**
     * Saves the current context state.
     */
    void saveContext();

    /**
     * Broadcasts global events to all nodes in the environment.
     *
     * @param schedulerJobInitiationEvent the event to broadcast
     * @param ignoreEnvironmentGroup flag to ignore environment group filtering
     * @param forceSending flag to force sending even if conditions aren't met
     * @throws IOException if an I/O error occurs
     */
    void broadcastGlobalEvents(SchedulerJobInitiationEvent schedulerJobInitiationEvent,
                              boolean ignoreEnvironmentGroup, boolean forceSending) throws IOException;

    /**
     * Broadcasts a local event within this node.
     *
     * @param schedulerJobInitiationEvent the event to broadcast
     * @throws IOException if an I/O error occurs
     */
    void broadcastLocalEvent(SchedulerJobInitiationEvent schedulerJobInitiationEvent) throws IOException;

    /**
     * Resubmits a message from the dead letter queue.
     *
     * @param messageId the ID of the message to resubmit
     * @return true if resubmission was successful, false otherwise
     * @throws IOException if an I/O error occurs
     * @throws BigQueueNotFoundException if the big queue is not found
     */
    boolean resubmitMessageFromDeadLetterQueue(String messageId) throws IOException, BigQueueNotFoundException;

    /**
     * Publishes a job initiation event.
     *
     * @param schedulerJobInitiationEvent the event to publish
     * @throws IOException if an I/O error occurs
     */
    void publishJobInitiationEvent(SchedulerJobInitiationEvent schedulerJobInitiationEvent) throws IOException;

    /**
     * Gets the map of internal event driven job instances.
     *
     * @return map of job identifier to InternalEventDrivenJobInstance
     */
    Map<String, InternalEventDrivenJobInstance> getInternalEventDrivenJobInstancesMap();
}
