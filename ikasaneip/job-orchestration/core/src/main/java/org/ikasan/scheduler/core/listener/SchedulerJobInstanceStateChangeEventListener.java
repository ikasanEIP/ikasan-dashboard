package org.ikasan.scheduler.core.listener;

import org.ikasan.scheduler.core.model.event.SchedulerJobInstanceStateChangeEvent;

@FunctionalInterface
public interface SchedulerJobInstanceStateChangeEventListener {

    /**
     * Listener interface for SchedulerJobInstance state changes.
     *
     * @param event
     */
    public void onSchedulerJobInstanceStateChangeEvent(SchedulerJobInstanceStateChangeEvent event);
}
