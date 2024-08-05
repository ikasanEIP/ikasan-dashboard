package org.ikasan.dashboard.ui.visualisation.scheduler.util;

import org.ikasan.dashboard.ui.util.VaadinThreadFactory;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcastListener;
import org.ikasan.spec.scheduled.job.model.JobConstants;

import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SchedulerJobStateChangeEventBroadcaster {
    static Executor executor = Executors.newFixedThreadPool(10, new VaadinThreadFactory("SchedulerJobStateChangeEventBroadcaster"));

    private static WeakHashMap<SchedulerJobStateChangeEventBroadcastListener, Object> listeners =
        new WeakHashMap<>();

    public static synchronized void register(SchedulerJobStateChangeEventBroadcastListener listener) {
        listeners.put(listener, null);
    }

    public static synchronized void unregister(SchedulerJobStateChangeEventBroadcastListener listener) {
        listeners.remove(listener);
    }

    public static synchronized void broadcast(final SchedulerJobInstanceStateChangeEvent event) {
        // We do not broadcast start and terminal jobs!
        if(event.getSchedulerJobInstance() != null
            && (!event.getSchedulerJobInstance().getAgentName().equals(JobConstants.CONTEXT_TERMINAL_JOB) &&
                !event.getSchedulerJobInstance().getAgentName().equals(JobConstants.CONTEXT_START_JOB))) {
            for (final SchedulerJobStateChangeEventBroadcastListener listener : listeners.keySet()) {
                executor.execute(() -> listener.receiveBroadcast(event));
            }
        }
    }
}
