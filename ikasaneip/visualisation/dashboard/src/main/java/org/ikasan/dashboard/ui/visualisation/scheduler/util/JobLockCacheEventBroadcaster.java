package org.ikasan.dashboard.ui.visualisation.scheduler.util;

import com.vaadin.flow.shared.Registration;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;

import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class JobLockCacheEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor();

    static LinkedList<Consumer<JobLockCacheEvent>> listeners = new LinkedList<>();

    public static synchronized Registration register(Consumer<JobLockCacheEvent> listener) {
        listeners.add(listener);

        return () -> {
            synchronized (JobLockCacheEventBroadcaster.class) {
                listeners.remove(listener);
            }
        };
    }

    public static synchronized void broadcast(JobLockCacheEvent message) {
        for (Consumer<JobLockCacheEvent> listener : listeners) {
            executor.execute(() -> listener.accept(message));
        }
    }
}
