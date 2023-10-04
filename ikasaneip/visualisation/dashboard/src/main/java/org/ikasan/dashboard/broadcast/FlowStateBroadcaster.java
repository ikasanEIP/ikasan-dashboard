package org.ikasan.dashboard.broadcast;

import org.ikasan.dashboard.ui.util.VaadimThreadFactory;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FlowStateBroadcaster
{
    static Executor executor = Executors.newSingleThreadExecutor(new VaadimThreadFactory("FlowStateBroadcaster"));

    private static WeakHashMap<FlowStateBroadcastListener, Object> listeners =
        new WeakHashMap<>();

    public static synchronized void register(FlowStateBroadcastListener listener) {
        listeners.put(listener, null);
    }

    public static synchronized void unregister(FlowStateBroadcastListener listener) {
        listeners.remove(listener);
    }

    public static synchronized void broadcast(final FlowState flowState) {
        for (final FlowStateBroadcastListener listener: listeners.keySet()) {
            executor.execute(() -> listener.receiveFlowStateBroadcast(flowState));
        }
    }
}
