package org.ikasan.scheduler.core.listener;

import org.ikasan.scheduler.core.event.SchedulerJobInstanceStateChangeEvent;

@FunctionalInterface
public interface SchedulerJobInstanceStateChangeEventListener {

    /**
     *
     * @param event
     */
    public void onSchedulerJobInstanceStateChangeEvent(SchedulerJobInstanceStateChangeEvent event);
}
