package org.ikasan.dashboard.ui.scheduler.util;

import com.vaadin.flow.shared.Registration;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;

import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ContextTemplateSavedEventBroadcaster {
    static Executor executor = Executors.newSingleThreadExecutor();

    static LinkedList<Consumer<ContextTemplate>> listeners = new LinkedList<>();

    public static synchronized Registration register(Consumer<ContextTemplate> listener) {
        listeners.add(listener);

        return () -> {
            synchronized (ContextTemplateSavedEventBroadcaster.class) {
                listeners.remove(listener);
            }
        };
    }

    public static synchronized void broadcast(ContextTemplate contextTemplate) {
        for (Consumer<ContextTemplate> listener : listeners) {
            executor.execute(() -> listener.accept(contextTemplate));
        }
    }
}
