package org.ikasan.scheduler.core.listener;

import org.ikasan.scheduler.core.model.event.ContextInstanceStateChangeEvent;

@FunctionalInterface
public interface ContextInstanceStateChangeEventListener {

    /**
     * Listener interface for ContextInstance state changes.
     *
     * @param event
     */
    public void onContextInstanceStateChangeEvent(ContextInstanceStateChangeEvent event);
}
