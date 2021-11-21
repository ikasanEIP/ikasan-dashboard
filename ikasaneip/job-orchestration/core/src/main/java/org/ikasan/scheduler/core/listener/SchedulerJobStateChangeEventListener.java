package org.ikasan.scheduler.core.listener;

import org.ikasan.scheduler.core.event.SchedulerJobStateChangeEvent;

@FunctionalInterface
public interface SchedulerJobStateChangeEventListener {

    /**
     *
     * @param event
     */
    public void onSchedulerJobStateChangeEvent(SchedulerJobStateChangeEvent event);
}
