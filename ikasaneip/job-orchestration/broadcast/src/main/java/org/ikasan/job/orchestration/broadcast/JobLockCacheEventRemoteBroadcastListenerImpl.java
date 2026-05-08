package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventRemoteBroadcastListener;

import java.util.List;

public class JobLockCacheEventRemoteBroadcastListenerImpl
    implements JobLockCacheEventRemoteBroadcastListener {

    private final List<ClusterEventService> clusterEventServices;

    public JobLockCacheEventRemoteBroadcastListenerImpl(List<ClusterEventService> clusterEventServices) {
        this.clusterEventServices = clusterEventServices;
    }

    @Override
    public void receiveBroadcast(JobLockCacheEvent event) {
        for (ClusterEventService service : clusterEventServices) {
            service.broadcastJobLockCache(event);
        }
    }
}
