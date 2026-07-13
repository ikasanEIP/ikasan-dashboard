package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.job.model.JobConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Broadcaster for scheduler job state change events across multiple local listeners (UI widgets) and
 * remote listener (one PeerBroadcastChannel per cluster peer).
 * Uses WeakHashMap to prevent memory leaks from registered local listeners.
 *
 * @author Ikasan Development Team
 */
public class SchedulerJobStateChangeEventBroadcaster {
    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerJobStateChangeEventBroadcaster.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(10, new BroadcasterThreadFactory("SchedulerJobStateChangeEventBroadcaster"));

    private final WeakHashMap<SchedulerJobStateChangeEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private SchedulerJobStateChangeEventRemoteBroadcastListener remoteListener;

    public static volatile SchedulerJobStateChangeEventBroadcaster INSTANCE = new SchedulerJobStateChangeEventBroadcaster();

    /**
     * Private constructor for the SchedulerJobStateChangeEventBroadcaster class.
     *
     * This constructor enforces the singleton design pattern, ensuring that
     * instances of this class cannot be created directly from outside the class.
     * Use the {@code instance()} method to get the singleton instance.
     */
    private SchedulerJobStateChangeEventBroadcaster() {}

    /**
     * Retrieves the singleton instance of the {@code SchedulerJobStateChangeEventBroadcaster}.
     * This method ensures that only one instance of the class exists, in compliance
     * with the singleton design pattern.
     *
     * @return the singleton instance of {@code SchedulerJobStateChangeEventBroadcaster}
     */
    public static SchedulerJobStateChangeEventBroadcaster instance() {
        return INSTANCE;
    }

    /**
     * Registers a local broadcast listener.
     *
     * @param listener the local listener to register
     */
    public synchronized void register(SchedulerJobStateChangeEventLocalBroadcastListener listener) {
        localListeners.put(listener, null);
    }

    /**
     * Unregisters a local broadcast listener.
     *
     * @param listener the local listener to unregister
     */
    public synchronized void unregister(SchedulerJobStateChangeEventLocalBroadcastListener listener) {
        localListeners.remove(listener);
    }

    /**
     * Registers the remote broadcast listener. The remote listener has a list of dashboard nodes so only 1 remote braodcast listener is required.
     *
     * @param listener the remote listener to register
     */
    public synchronized void setRemoteListener(SchedulerJobStateChangeEventRemoteBroadcastListener listener) {
        remoteListener = listener;
    }


    /**
     * Broadcasts a scheduler job state change event to both local and remote listeners.
     * Note: Start and terminal jobs are not broadcast.
     *
     * @param event the state change event to broadcast
     */
    public synchronized void broadcast(final SchedulerJobInstanceStateChangeEvent event) {
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
    public synchronized void remoteBroadcast(final SchedulerJobInstanceStateChangeEvent event) {
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
    public synchronized void localBroadcast(final SchedulerJobInstanceStateChangeEvent event) {
        for (final SchedulerJobStateChangeEventLocalBroadcastListener listener : localListeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(event));
        }
    }

    /**
     * Resets the singleton instance of the {@code SchedulerJobStateChangeEventBroadcaster}.
     *
     * <p>Shut down the current instance's executor and wait for any already queued or in-flight
     * broadcasts to finish, this guarantees no broadcast is dispatched while the reset
     * is still running — or silently lost — once this method returns. Prevents potential
     * non-deamon thread leak.
     * This method is {@code synchronized}, so it cannot run concurrently with
     * {@link #register}, {@link #unregister}, {@link #broadcast}, {@link #remoteBroadcast}
     * or {@link #localBroadcast} on the same instance — no undefined-ordering race
     * between a reset and an in-progress broadcast.
     *
     * <p>A caller that already holds a reference to the old instance (obtained via {@link #instance()}
     * before this call) continues to hold a reference to a now-terminated object. However,
     * since the old instance's executor is shut down, any further
     * {@link #broadcast} or {@link #localBroadcast} call made through a stale reference will
     * throw {@link java.util.concurrent.RejectedExecutionException} rather than quietly
     * succeeding into an orphaned instance nobody observes.
     */
    private synchronized void reset() {
        ExecutorDrainer.shutdownAndAwait(executor, LOGGER);
        INSTANCE = new SchedulerJobStateChangeEventBroadcaster();
    }
}
