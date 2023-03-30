package org.ikasan.dashboard.ui.scheduler.util;

import org.ikasan.spec.scheduled.job.model.SchedulerJob;

public interface NewSchedulerJobEventBroadcastListener {

    /**
     * Called when new Scheduler Job is created.
     *
     * @param schedulerJob
     */
    void receiveBroadcast(SchedulerJob schedulerJob);
}
