package org.ikasan.dashboard.ui.scheduler.util;

import com.vaadin.flow.shared.Registration;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class NewSchedulerJobEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor();

    static LinkedList<Consumer<SchedulerJob>> listeners = new LinkedList<>();

    public static synchronized Registration register(Consumer<SchedulerJob> listener) {
        listeners.add(listener);

        return () -> {
            synchronized (NewSchedulerJobEventBroadcaster.class) {
                listeners.remove(listener);
            }
        };
    }

    public static synchronized void broadcast(SchedulerJob schedulerJob) {
        for (Consumer<SchedulerJob> listener : listeners) {
            executor.execute(() -> listener.accept(schedulerJob));
        }
    }
}
