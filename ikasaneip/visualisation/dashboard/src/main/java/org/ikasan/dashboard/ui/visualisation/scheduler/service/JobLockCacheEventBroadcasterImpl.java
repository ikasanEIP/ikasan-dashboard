package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventBroadcastListener;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventBroadcaster;

public class JobLockCacheEventBroadcasterImpl implements JobLockCacheEventBroadcaster {

    @Override
    public void register(JobLockCacheEventBroadcastListener listener) {
        org.ikasan.job.orchestration.broadcast.JobLockCacheEventBroadcaster.register(listener);
    }

    @Override
    public synchronized void broadcast(JobLockCacheEvent message) {
        org.ikasan.job.orchestration.broadcast.JobLockCacheEventBroadcaster.broadcast(message);
    }
}
