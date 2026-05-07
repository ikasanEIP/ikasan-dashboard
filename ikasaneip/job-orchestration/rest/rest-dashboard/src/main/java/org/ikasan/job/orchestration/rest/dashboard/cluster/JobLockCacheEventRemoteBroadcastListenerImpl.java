package org.ikasan.job.orchestration.rest.dashboard.cluster;

import org.ikasan.spec.scheduled.event.service.JobLockCacheEventRemoteBroadcastListener;
import org.ikasan.job.orchestration.rest.client.ClusterEventRestServiceImpl;
import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;

import java.util.List;

public class JobLockCacheEventRemoteBroadcastListenerImpl
    implements JobLockCacheEventRemoteBroadcastListener {

    private final ClusterEventRestServiceImpl clusterEventRestService;
    private final List<String> clusterNodeUrls;

    public JobLockCacheEventRemoteBroadcastListenerImpl(ClusterEventRestServiceImpl clusterEventRestService,
                                                        List<String> clusterNodeUrls) {
        this.clusterEventRestService = clusterEventRestService;
        this.clusterNodeUrls = clusterNodeUrls;
    }

    @Override
    public void receiveBroadcast(JobLockCacheEvent event) {
        for (String nodeUrl : clusterNodeUrls) {
            clusterEventRestService.broadcastJobLockCache(nodeUrl, event);
        }
    }
}
