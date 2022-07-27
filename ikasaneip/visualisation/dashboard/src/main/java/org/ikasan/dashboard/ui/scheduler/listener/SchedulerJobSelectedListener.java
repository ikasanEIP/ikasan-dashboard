package org.ikasan.dashboard.ui.scheduler.listener;

import org.ikasan.spec.scheduled.job.model.SchedulerJob;

public interface SchedulerJobSelectedListener {

    /**
     * Listener for when scheduler jobs are selected.
     *
     * @param schedulerJob
     */
    void jobSelected(SchedulerJob schedulerJob);
}
