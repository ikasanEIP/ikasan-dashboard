package org.ikasan.job.orchestration.rest.dashboard.cluster;

import org.ikasan.spec.scheduled.event.service.ContextViewUpdateEventRemoteBroadcastListener;
import org.ikasan.job.orchestration.rest.client.ClusterEventRestServiceImpl;

import java.util.List;

public class ContextViewUpdateEventRemoteBroadcastListenerImpl
    implements ContextViewUpdateEventRemoteBroadcastListener {

    private final ClusterEventRestServiceImpl clusterEventRestService;
    private final List<String> clusterNodeUrls;

    public ContextViewUpdateEventRemoteBroadcastListenerImpl(ClusterEventRestServiceImpl clusterEventRestService,
                                                             List<String> clusterNodeUrls) {
        this.clusterEventRestService = clusterEventRestService;
        this.clusterNodeUrls = clusterNodeUrls;
    }

    @Override
    public void receiveBroadcast(String message) {
        for (String nodeUrl : clusterNodeUrls) {
            clusterEventRestService.broadcastContextViewUpdate(nodeUrl, message);
        }
    }
}
