package org.ikasan.dashboard.ui.scheduler.util;

import com.vaadin.flow.shared.Registration;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;

import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ContextInstanceSavedEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor();

    static LinkedList<Consumer<ContextInstance>> listeners = new LinkedList<>();

    public static synchronized Registration register(Consumer<ContextInstance> listener) {
        listeners.add(listener);

        return () -> {
            synchronized (ContextInstanceSavedEventBroadcaster.class) {
                listeners.remove(listener);
            }
        };
    }

    public static synchronized void broadcast(ContextInstance contextInstance) {
        for (Consumer<ContextInstance> listener : listeners) {
            executor.execute(() -> listener.accept(contextInstance));
        }
    }
}
