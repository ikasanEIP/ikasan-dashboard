package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.service.ContextInstanceDlqEventLocalBroadcastListener;
import org.ikasan.spec.scheduled.event.service.ContextInstanceDlqEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Broadcaster for context instance DLQ (Dead Letter Queue) events across multiple local listeners (UI widgets) and
 * remote listener (one PeerBroadcastChannel per cluster peer).
 * Uses WeakHashMap to prevent memory leaks from registered local listeners.
 *
 * @author Ikasan Development Team
 */
public class ContextInstanceDlqEventBroadcaster {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContextInstanceDlqEventBroadcaster.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor(new BroadcasterThreadFactory("ContextInstanceDlqEventBroadcaster"));

    private final WeakHashMap<ContextInstanceDlqEventLocalBroadcastListener, Object> localListeners =
        new WeakHashMap<>();
    private ContextInstanceDlqEventRemoteBroadcastListener remoteListener;

    public static volatile ContextInstanceDlqEventBroadcaster INSTANCE = new ContextInstanceDlqEventBroadcaster();

    /**
     * Private constructor for the ContextInstanceDlqEventBroadcaster class.
     *
     * This constructor enforces the singleton design pattern, ensuring that
     * instances of this class cannot be created directly from outside the class.
     * Use the {@code instance()} method to get the singleton instance.
     */
    private ContextInstanceDlqEventBroadcaster() {}

    /**
     * Retrieves the singleton instance of the {@code ContextInstanceDlqEventBroadcaster}.
     * This method ensures that only one instance of the class exists, in compliance
     * with the singleton design pattern.
     *
     * @return the singleton instance of {@code ContextInstanceDlqEventBroadcaster}
     */
    public static ContextInstanceDlqEventBroadcaster instance() {
        return INSTANCE;
    }

    /**
     * Registers a local broadcast listener.
     *
     * @param listener the local listener to register
     */
    public synchronized void register(ContextInstanceDlqEventLocalBroadcastListener listener) {
        localListeners.put(listener, null);
    }

    /**
     * Unregisters a local broadcast listener.
     *
     * @param listener the local listener to unregister
     */
    public synchronized void unregister(ContextInstanceDlqEventLocalBroadcastListener listener) {
        localListeners.remove(listener);
    }

    /**
     * Registers the remote broadcast listener. The remote listener has a list of dashboard nodes so only 1 remote braodcast listener is required.
     *
     * @param listener the remote listener to register
     */
    public synchronized void setRemoteListener(ContextInstanceDlqEventRemoteBroadcastListener listener) {
        remoteListener = listener;
    }


    /**
     * Broadcasts a context instance DLQ event to both local and remote listeners.
     *
     * @param contextInstance the context instance to broadcast
     */
    public synchronized void broadcast(final ContextInstance contextInstance) {
        localBroadcast(contextInstance);
        remoteBroadcast(contextInstance);
    }

    /**
     * Broadcasts a context instance DLQ event to remote listeners only.
     *
     * @param contextInstance the context instance to broadcast
     */
    public synchronized void remoteBroadcast(final ContextInstance contextInstance) {
        if (remoteListener != null) {
            remoteListener.receiveBroadcast(contextInstance);
        }
    }

    /**
     * Called when receiving an event from another cluster node.
     * Dispatches to local listeners only to prevent broadcast loops.
     *
     * @param contextInstance the context instance to broadcast locally
     */
    public synchronized void localBroadcast(final ContextInstance contextInstance) {
        for (final ContextInstanceDlqEventLocalBroadcastListener listener: localListeners.keySet()) {
            executor.execute(() -> listener.receiveBroadcast(contextInstance));
        }
    }

    /**
     * Resets the singleton instance of the {@code ContextInstanceDlqEventBroadcaster}.
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
        INSTANCE = new ContextInstanceDlqEventBroadcaster();
    }
}
