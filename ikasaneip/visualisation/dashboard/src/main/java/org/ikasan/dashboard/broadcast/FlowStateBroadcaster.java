package org.ikasan.dashboard.broadcast;

import org.ikasan.dashboard.ui.util.VaadinThreadFactory;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FlowStateBroadcaster {
    private final Executor executor = Executors.newSingleThreadExecutor(new VaadinThreadFactory("FlowStateBroadcaster"));

    private final WeakHashMap<FlowStateBroadcastListener, Object> listeners =
        new WeakHashMap<>();

    public static FlowStateBroadcaster INSTANCE = new FlowStateBroadcaster();

    /**
     * Private constructor for the FlowStateBroadcaster class.
     * This constructor enforces the singleton design pattern use the {@code instance()}
     * method to get the singleton instance.
     */
    private FlowStateBroadcaster() {}

    /**
     * Ensures that only one instance of the class exists, in compliance
     * with the singleton design pattern.
     * @return the singleton instance of {@code FlowStateBroadcaster}
     */
    public static FlowStateBroadcaster instance() {
        return INSTANCE;
    }

    public synchronized void register(FlowStateBroadcastListener listener) {
        listeners.put(listener, null);
    }

    public synchronized void unregister(FlowStateBroadcastListener listener) {
        listeners.remove(listener);
    }

    public synchronized void broadcast(final FlowState flowState) {
        for (final FlowStateBroadcastListener listener: listeners.keySet()) {
            executor.execute(() -> listener.receiveFlowStateBroadcast(flowState));
        }
    }

    /**
     * Resets the singleton instance of the {@code FlowStateBroadcaster}.
     *
     * This method creates a new instance of the {@code FlowStateBroadcaster}
     * and assigns it to the {@code INSTANCE} field, effectively clearing any existing
     * listeners or configurations associated with the previous instance.
     *
     * Use this method cautiously as it overrides the previous state of the singleton,
     * which may affect ongoing operations.
     */
    private void reset() {
        INSTANCE = new FlowStateBroadcaster();
    }
}
