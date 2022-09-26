package org.ikasan.dashboard.ui.visualisation.scheduler.service;

import com.vaadin.flow.shared.Registration;
import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventBroadcaster;

import java.util.function.Consumer;

public class JobLockCacheEventBroadcasterImpl implements JobLockCacheEventBroadcaster<Registration> {

    @Override
    public synchronized Registration register(Consumer<JobLockCacheEvent> listener) {
        return org.ikasan.dashboard.ui.visualisation.scheduler.util.JobLockCacheEventBroadcaster.register(listener);
    }

    @Override
    public synchronized void broadcast(JobLockCacheEvent message) {
        org.ikasan.dashboard.ui.visualisation.scheduler.util.JobLockCacheEventBroadcaster.broadcast(message);
    }
}
