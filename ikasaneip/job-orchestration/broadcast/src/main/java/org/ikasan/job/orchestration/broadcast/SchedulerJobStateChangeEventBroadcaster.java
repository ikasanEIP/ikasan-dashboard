package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.job.model.JobConstants;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Broadcaster for scheduler job state change events across multiple local listeners (UI widgets) and
 * remote listener (one PeerBroadcastChannel per cluster peer).
 * Uses WeakHashMap to prevent memory leaks from registered local listeners.
 *
 * @author Ikasan Development Team
 */
public class SchedulerJobStateChangeEventBroadcaster {
    static Executor executor = Executors.newFixedThreadPool(10, new BroadcasterThreadFactory("SchedulerJobStateChangeEventBroadcaster"));

    private static final WeakHashMap<SchedulerJobStateChangeEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private static SchedulerJobStateChangeEventRemoteBroadcastListener remoteListener;

    /**
     * Registers a local broadcast listener.
     *
     * @param listener the local listener to register
     */
    public static synchronized void register(SchedulerJobStateChangeEventLocalBroadcastListener listener) {
        localListeners.put(listener, null);
    }

    /**
     * Unregisters a local broadcast listener.
     *
     * @param listener the local listener to unregister
     */
    public static synchronized void unregister(SchedulerJobStateChangeEventLocalBroadcastListener listener) {
        localListeners.remove(listener);
    }

    /**
     * Registers the remote broadcast listener. The remote listener has a list of dashboard nodes so only 1 remote braodcast listener is required.
     *
     * @param listener the remote listener to register
     */
    public static synchronized void setRemoteListener(SchedulerJobStateChangeEventRemoteBroadcastListener listener) {
        remoteListener = listener;
    }


    /**
     * Broadcasts a scheduler job state change event to both local and remote listeners.
     * Note: Start and terminal jobs are not broadcast.
     *
     * @param event the state change event to broadcast
     */
    public static synchronized void broadcast(final SchedulerJobInstanceStateChangeEvent event) {
        // We do not broadcast start and terminal jobs!
        if(event.getSchedulerJobInstance() != null
            && (!event.getSchedulerJobInstance().getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB) &&
                !event.getSchedulerJobInstance().getAgentName().equals(JobConstants.CONTEXT_START_JOB))) {
            localBroadcast(event);
            remoteBroadcast(event);
        }
    }

    /**
     * Broadcasts a scheduler job state change event to remote listeners only.
     *
     * @param event the state change event to broadcast
     */
    public static synchronized void remoteBroadcast(final SchedulerJobInstanceStateChangeEvent event) {
        if (remoteListener != null) {
            remoteListener.receiveBroadcast(event);
        }
    }

    /**
     * Called when receiving a state change event from another cluster node.
     * Dispatches to local listeners only to prevent broadcast loops.
     *
     * @param event the state change event to broadcast locally
     */
    public static synchronized void localBroadcast(final SchedulerJobInstanceStateChangeEvent event) {
        for (final SchedulerJobStateChangeEventLocalBroadcastListener listener : localListeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(event));
        }
    }
}
