package org.ikasan.job.orchestration.rest.dashboard;

import org.ikasan.bigqueue.IBigQueue;
import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.status.ContextInstanceStatus;
import org.ikasan.job.orchestration.rest.client.ContextMachineRestServiceImpl;
import org.ikasan.spec.bigqueue.message.BigQueueMessage;
import org.ikasan.spec.bigqueue.service.exception.BigQueueNotFoundException;
import org.ikasan.spec.scheduled.core.listener.ContextInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInitiationEventRaisedListener;
import org.ikasan.spec.scheduled.core.listener.SchedulerJobInstanceStateChangeEventListener;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.DryRunParameters;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceDlqEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.InternalEventDrivenJobInstance;
import org.ikasan.spec.scheduled.instance.service.exception.SchedulerJobInstanceInitialisationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A {@link ContextMachine} implementation that proxies operations to the leader cluster node via
 * REST. Used by {@link ContextMachineCache}'s fallback mechanism when a context instance is not
 * present in the local cache, meaning it lives on another node.
 * The leader's URL is resolved on each call via the supplied {@link Supplier}, which reads from
 * ZooKeeper, avoiding the N-1 wasted calls of a fan-out approach. If the targeted leader returns
 * no result (e.g. during a leadership transition), the call is retried up to
 * {@code leaderLookupRetries} times with {@code leaderLookupRetryIntervalMs} between attempts.
 * Lifecycle / infrastructure methods that must remain on the owning node throw
 * {@link UnsupportedOperationException}.
 */
public class ContextMachineRestImpl implements ContextMachine {

    private static final Logger LOG = LoggerFactory.getLogger(ContextMachineRestImpl.class);

    private final String contextInstanceId;
    private final Supplier<String> leaderUrlSupplier;
    private final Map<String, ContextMachineRestServiceImpl> peersByUrl;
    private final int leaderLookupRetries;
    private final long leaderLookupRetryIntervalMs;

    public ContextMachineRestImpl(String contextInstanceId,
                                  Supplier<String> leaderUrlSupplier,
                                  Map<String, ContextMachineRestServiceImpl> peersByUrl,
                                  int leaderLookupRetries,
                                  long leaderLookupRetryIntervalMs) {
        this.contextInstanceId = contextInstanceId;
        this.leaderUrlSupplier = leaderUrlSupplier;
        this.peersByUrl = peersByUrl;
        this.leaderLookupRetries = leaderLookupRetries;
        this.leaderLookupRetryIntervalMs = leaderLookupRetryIntervalMs;
    }

    // ── Routing helpers ─────────────────────────────────────────────────────────────────────────

    private ContextMachineRestServiceImpl resolveLeader() {
        String url = leaderUrlSupplier.get();
        if (url == null) return null;
        return peersByUrl.get(url.toLowerCase());
    }

    /**
     * Calls {@code op} against the current leader, retrying up to {@code leaderLookupRetries}
     * times (with a sleep between each) when the leader is unreachable or returns false.
     * Logs a warning after all attempts are exhausted.
     */
    private void doWrite(Function<ContextMachineRestServiceImpl, Boolean> op, String opName) {
        for (int attempt = 0; attempt <= leaderLookupRetries; attempt++) {
            ContextMachineRestServiceImpl svc = resolveLeader();
            if (svc != null) {
                try {
                    if (op.apply(svc)) return;
                } catch (ResourceAccessException e) {
                    // network failure — fall through to retry
                }
            }
            if (attempt < leaderLookupRetries) {
                LOG.warn("{}: leader unavailable for contextInstanceId [{}] — retry {}/{}",
                    opName, contextInstanceId, attempt + 1, leaderLookupRetries);
                sleep();
            }
        }
        LOG.warn("{}: no leader handled contextInstanceId [{}] after {} attempt(s)",
            opName, contextInstanceId, leaderLookupRetries + 1);
    }

    /**
     * Calls { op} against the current leader and retries on false or network errors.
     */
    private boolean doBooleanWrite(Function<ContextMachineRestServiceImpl, Boolean> op, String opName) {
        for (int attempt = 0; attempt <= leaderLookupRetries; attempt++) {
            ContextMachineRestServiceImpl svc = resolveLeader();
            if (svc != null) {
                try {
                    if (op.apply(svc)) {
                        return true;
                    }
                } catch (ResourceAccessException e) {
                    // network failure - fall through to retry
                }
            }
            if (attempt < leaderLookupRetries) {
                LOG.warn("{}: leader unavailable for contextInstanceId [{}] — retry {}/{}",
                    opName, contextInstanceId, attempt + 1, leaderLookupRetries);
                sleep();
            }
        }
        LOG.warn("{}: no leader handled contextInstanceId [{}] after {} attempt(s)",
            opName, contextInstanceId, leaderLookupRetries + 1);
        return false;
    }

    /**
     * Reads a value from the current leader, retrying up to {@code leaderLookupRetries} times
     * when the leader is unreachable or returns null. Returns {@code fallback} if all attempts
     * are exhausted.
     */
    private <T> T doRead(Function<ContextMachineRestServiceImpl, T> op, T fallback, String opName) {
        for (int attempt = 0; attempt <= leaderLookupRetries; attempt++) {
            ContextMachineRestServiceImpl svc = resolveLeader();
            if (svc != null) {
                try {
                    // Any non-exceptional return (including null) is a definitive server answer.
                    return op.apply(svc);
                } catch (ResourceAccessException e) {
                    if (attempt < leaderLookupRetries) {
                        LOG.warn("{}: network error for contextInstanceId [{}] — retry {}/{}: {}",
                            opName, contextInstanceId, attempt + 1, leaderLookupRetries, e.getMessage());
                        sleep();
                    }
                }
            } else if (attempt < leaderLookupRetries) {
                LOG.warn("{}: leader unavailable for contextInstanceId [{}] — retry {}/{}",
                    opName, contextInstanceId, attempt + 1, leaderLookupRetries);
                sleep();
            }
        }
        LOG.warn("{}: no leader returned result for contextInstanceId [{}] after {} attempt(s)",
            opName, contextInstanceId, leaderLookupRetries + 1);
        return fallback;
    }

    private void sleep() {
        try {
            Thread.sleep(leaderLookupRetryIntervalMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ── Local/proxy identity ─────────────────────────────────────────────────────────────────────

    @Override
    public boolean isLocal() {
        return false;
    }

    // ── Supported proxy operations ───────────────────────────────────────────────────────────────

    @Override
    public void holdJobs(String childContextName) {
        doWrite(svc -> svc.holdAllJobs(contextInstanceId, childContextName), "holdJobs");
    }

    @Override
    public void releaseJobs(String childContextName) {
        doWrite(svc -> svc.releaseAllJobs(contextInstanceId, childContextName), "releaseJobs");
    }

    @Override
    public void holdJob(String jobIdentifier, String childContextName) {
        doWrite(svc -> svc.holdJob(contextInstanceId, jobIdentifier, childContextName), "holdJob");
    }

    @Override
    public void releaseJob(String jobIdentifier, String childContextName) {
        doWrite(svc -> svc.releaseJob(contextInstanceId, jobIdentifier, childContextName), "releaseJob");
    }

    @Override
    public void resetJob(String jobIdentifier, String childContextName) {
        doWrite(svc -> svc.resetJob(contextInstanceId, jobIdentifier, childContextName), "resetJob");
    }

    @Override
    public void skipJob(String jobIdentifier, String childContextName, boolean skipFlag) {
        doWrite(svc -> svc.skipJob(contextInstanceId, jobIdentifier, childContextName, skipFlag), "skipJob");
    }

    @Override
    public void skipJobs(String childContextName, boolean skipFlag) {
        doWrite(svc -> svc.skipAllJobs(contextInstanceId, childContextName, skipFlag), "skipJobs");
    }

    @Override
    public InstanceStatus getContextStatus(String contextName) {
        return doRead(svc -> svc.getContextStatusFromPeer(contextInstanceId, contextName), null, "getContextStatus");
    }

    @Override
    public InstanceStatus getJobStatus(String contextName, String jobIdentifier) {
        return doRead(svc -> svc.getJobStatusFromPeer(contextInstanceId, contextName, jobIdentifier), null, "getJobStatus");
    }

    @Override
    public ContextInstanceStatus getContextInstanceStatus() {
        return doRead(svc -> svc.getInstanceStatusFromPeer(contextInstanceId), null, "getContextInstanceStatus");
    }

    @Override
    public ContextInstance getContext(String contextName) {
        return doRead(svc -> svc.getChildContextFromPeer(contextInstanceId, contextName), null, "getContext(contextName)");
    }

    @Override
    public ContextInstance getContext() {
        return doRead(svc -> svc.getContextFromPeer(contextInstanceId), null, "getContext");
    }

    @Override
    public void updateContextParameters(List<ContextParameterInstance> contextParameterInstances) {
        doWrite(svc -> svc.updateContextParameters(contextInstanceId, contextParameterInstances), "updateContextParameters");
    }

    @Override
    public void setDryRunParameters(DryRunParameters dryRunParameters) {
        doWrite(svc -> svc.setDryRunParameters(contextInstanceId,
            dryRunParameters.getMinExecutionTimeMillis(),
            dryRunParameters.getMaxExecutionTimeMillis(),
            dryRunParameters.getFixedExecutionTimeMillis(),
            dryRunParameters.getJobErrorPercentage(),
            dryRunParameters.isError()), "setDryRunParameters");
    }

    @Override
    public boolean isDryRun() {
        Boolean result = doRead(svc -> svc.isDryRunOnPeer(contextInstanceId), null, "isDryRun");
        return result != null && result;
    }

    @Override
    public void disableQuartzBasedJobs() {
        doWrite(svc -> svc.disableQuartzJobs(contextInstanceId), "disableQuartzBasedJobs");
    }

    @Override
    public void enableQuartzBasedJobs() {
        doWrite(svc -> svc.enableQuartzJobs(contextInstanceId), "enableQuartzBasedJobs");
    }

    @Override
    public void runContextUntilManuallyEnded() {
        doWrite(svc -> svc.runUntilManuallyEnded(contextInstanceId), "runContextUntilManuallyEnded");
    }

    @Override
    public void acknowledgeSchedulerJobError(InternalEventDrivenJobInstance schedulerJobInstance) {
        doWrite(svc -> svc.acknowledgeError(contextInstanceId,
            schedulerJobInstance.getIdentifier(),
            schedulerJobInstance.isTargetResidingContextOnly(),
            schedulerJobInstance.getChildContextName(),
            schedulerJobInstance.getChildContextNames()), "acknowledgeSchedulerJobError");
    }

    @Override
    public void releaseQueuedJobs() {
        doWrite(svc -> svc.releaseQueuedJobs(contextInstanceId), "releaseQueuedJobs");
    }

    @Override
    public void killRunningJobs() {
        doWrite(svc -> svc.killRunningJobs(contextInstanceId), "killRunningJobs");
    }

    @Override
    public void addQueuedSchedulerJobInitiationEvent(SchedulerJobInitiationEvent event) {
        doWrite(svc -> svc.addQueuedInitiationEvent(contextInstanceId,
            event.getAgentName(), event.getJobName(), event.getContextName(),
            event.getContextInstanceId(), event.getChildContextNames()), "addQueuedSchedulerJobInitiationEvent");
    }

    @Override
    public void saveContext() {
        doWrite(svc -> svc.saveContext(contextInstanceId), "saveContext");
    }

    @Override
    public void broadcastGlobalEvents(SchedulerJobInitiationEvent event, boolean ignoreEnvironmentGroup,
                                      boolean forceSending) throws IOException {
        doWrite(svc -> svc.broadcastGlobalEvents(contextInstanceId,
            event.getAgentName(), event.getJobName(), event.getContextName(),
            event.getContextInstanceId(), event.getChildContextNames(),
            ignoreEnvironmentGroup, forceSending), "broadcastGlobalEvents");
    }

    @Override
    public void broadcastLocalEvent(SchedulerJobInitiationEvent event) throws IOException {
        doWrite(svc -> svc.broadcastLocalEvent(contextInstanceId,
            event.getAgentName(), event.getJobName(), event.getContextName(),
            event.getContextInstanceId(), event.getChildContextNames()), "broadcastLocalEvent");
    }

    @Override
    public boolean resubmitMessageFromDeadLetterQueue(String messageId)
            throws IOException, BigQueueNotFoundException {
        return doBooleanWrite(svc -> svc.resubmitDlq(contextInstanceId, messageId), "resubmitMessageFromDeadLetterQueue");
    }

    @Override
    public List<BigQueueMessage> getDlqMessages() {
        List<BigQueueMessage> result = doRead(
            svc -> svc.getDlqMessagesFromPeer(contextInstanceId), null, "getDlqMessages");
        return result != null ? result : new ArrayList<>();
    }

    @Override
    public boolean deleteDlqMessage(String messageId) throws IOException, BigQueueNotFoundException {
        return doBooleanWrite(svc -> svc.deleteDlqMessage(contextInstanceId, messageId), "deleteDlqMessage");
    }

    @Override
    public void deleteAllDlqMessages() throws IOException, BigQueueNotFoundException {
        doWrite(svc -> svc.deleteAllDlqMessages(contextInstanceId), "deleteAllDlqMessages");
    }

    @Override
    public void publishJobInitiationEvent(SchedulerJobInitiationEvent event) throws IOException {
        doWrite(svc -> svc.publishJobInitiationEvent(contextInstanceId,
            event.getAgentName(), event.getJobName(), event.getContextName(),
            event.getContextInstanceId(), event.getChildContextNames()), "publishJobInitiationEvent");
    }

    // ── Unsupported lifecycle / infrastructure methods ───────────────────────────────────────────

    @Override public void init() throws IOException {
        throw new UnsupportedOperationException("init is not supported by the REST proxy");
    }

    @Override public void registerToNotificationMonitors() {
        throw new UnsupportedOperationException("registerToNotificationMonitors is not supported by the REST proxy");
    }

    @Override public void unregisterToNotificationMonitors() {
        throw new UnsupportedOperationException("unregisterToNotificationMonitors is not supported by the REST proxy");
    }

    @Override public String getOutboundQueueName() {
        throw new UnsupportedOperationException("getOutboundQueueName is not supported by the REST proxy");
    }

    @Override public String getInboundQueueName() {
        throw new UnsupportedOperationException("getInboundQueueName is not supported by the REST proxy");
    }

    @Override public String getDeadLetterQueueName() {
        throw new UnsupportedOperationException("getDeadLetterQueueName is not supported by the REST proxy");
    }

    @Override public IBigQueue getInboundQueue() {
        throw new UnsupportedOperationException("getInboundQueue is not supported by the REST proxy");
    }

    @Override public IBigQueue getOutboundQueue() {
        throw new UnsupportedOperationException("getOutboundQueue is not supported by the REST proxy");
    }

    @Override public IBigQueue getDeadLetterQueue() {
        throw new UnsupportedOperationException("getDeadLetterQueue is not supported by the REST proxy");
    }

    @Override public void setExecutorWaitTimeoutSeconds(int executorWaitTimeoutSeconds) {
        throw new UnsupportedOperationException("setExecutorWaitTimeoutSeconds is not supported by the REST proxy");
    }

    @Override public void setBlackListedMessageMaxRetries(int blackListedMessageMaxRetries) {
        throw new UnsupportedOperationException("setBlackListedMessageMaxRetries is not supported by the REST proxy");
    }

    @Override public void setErrorRetrySleepInterval(long errorRetrySleepInterval) {
        throw new UnsupportedOperationException("setErrorRetrySleepInterval is not supported by the REST proxy");
    }

    @Override public void resetContextInstance(boolean holdCommandJobs, boolean initiateWithSameParameters,
                                               List<ContextParameterInstance> contextParameterInstances)
            throws IOException, SchedulerJobInstanceInitialisationException, BigQueueNotFoundException {
        throw new UnsupportedOperationException("resetContextInstance is not supported by the REST proxy");
    }

    @Override public void propagateContextInstanceToAgents() {
        throw new UnsupportedOperationException("propagateContextInstanceToAgents is not supported by the REST proxy");
    }

    @Override public void teardown() throws IOException {
        throw new UnsupportedOperationException("teardown is not supported by the REST proxy");
    }

    @Override public void setSchedulerJobInitiationEventRaisedListener(SchedulerJobInitiationEventRaisedListener listener) {
        throw new UnsupportedOperationException("setSchedulerJobInitiationEventRaisedListener is not supported by the REST proxy");
    }

    @Override public void eventReceived(String bigQueueMessage) throws IOException {
        throw new UnsupportedOperationException("eventReceived is not supported by the REST proxy");
    }

    @Override public void raiseEvent(ContextualisedScheduledProcessEvent event) throws IOException {
        throw new UnsupportedOperationException("raiseEvent is not supported by the REST proxy");
    }

    @Override public void addSchedulerJobStateChangeEventListener(SchedulerJobInstanceStateChangeEventListener listener) {
        throw new UnsupportedOperationException("addSchedulerJobStateChangeEventListener is not supported by the REST proxy");
    }

    @Override public void removeSchedulerJobStateChangeEventListener(SchedulerJobInstanceStateChangeEventListener listener) {
        throw new UnsupportedOperationException("removeSchedulerJobStateChangeEventListener is not supported by the REST proxy");
    }

    @Override public void addContextInstanceStateChangeEventListener(ContextInstanceStateChangeEventListener listener) {
        throw new UnsupportedOperationException("addContextInstanceStateChangeEventListener is not supported by the REST proxy");
    }

    @Override public void removeContextInstanceStateChangeEventListener(ContextInstanceStateChangeEventListener listener) {
        throw new UnsupportedOperationException("removeContextInstanceStateChangeEventListener is not supported by the REST proxy");
    }

    @Override public void addContextInstanceDlqEventEventBroadcastListeners(ContextInstanceDlqEventLocalBroadcastListener listener) {
        throw new UnsupportedOperationException("addContextInstanceDlqEventEventBroadcastListeners is not supported by the REST proxy");
    }

    @Override public void removeContextInstanceDlqEventEventBroadcastListeners(ContextInstanceDlqEventLocalBroadcastListener listener) {
        throw new UnsupportedOperationException("removeContextInstanceDlqEventEventBroadcastListeners is not supported by the REST proxy");
    }

    @Override public List<SchedulerJobInitiationEvent> getEventsThatCanRun(ContextualisedScheduledProcessEvent event) {
        throw new UnsupportedOperationException("getEventsThatCanRun is not supported by the REST proxy");
    }

    @Override public boolean servesAgent(String agentName) {
        throw new UnsupportedOperationException("servesAgent is not supported by the REST proxy");
    }

    @Override public Map<String, InternalEventDrivenJobInstance> getInternalEventDrivenJobInstancesMap() {
        throw new UnsupportedOperationException("getInternalEventDrivenJobInstancesMap is not supported by the REST proxy");
    }

    @Override public void setPublishRaiseEventsAfterJobPlanInstanceFlush(boolean publishRaiseEventsAfterJobPlanInstanceFlush) {
        throw new UnsupportedOperationException("setPublishRaiseEventsAfterJobPlanInstanceFlush is not supported by the REST proxy");
    }
}
