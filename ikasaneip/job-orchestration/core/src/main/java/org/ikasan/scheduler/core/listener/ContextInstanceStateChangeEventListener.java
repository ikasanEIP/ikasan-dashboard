package org.ikasan.scheduler.core.listener;

import org.ikasan.scheduler.core.event.ContextInstanceStateChangeEvent;

@FunctionalInterface
public interface ContextInstanceStateChangeEventListener {

    /**
     *
     * @param event
     */
    public void onContextInstanceStateChangeEvent(ContextInstanceStateChangeEvent event);
}
