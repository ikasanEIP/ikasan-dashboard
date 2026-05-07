package org.ikasan.job.orchestration.rest.dashboard.cluster;

import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventRemoteBroadcastListener;
import org.ikasan.job.orchestration.rest.client.ClusterEventRestServiceImpl;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;

import java.util.List;

public class ContextInstanceStateChangeEventRemoteBroadcastListenerImpl
    implements ContextInstanceStateChangeEventRemoteBroadcastListener {

    private final ClusterEventRestServiceImpl clusterEventRestService;
    private final List<String> clusterNodeUrls;

    public ContextInstanceStateChangeEventRemoteBroadcastListenerImpl(ClusterEventRestServiceImpl clusterEventRestService,
                                                                      List<String> clusterNodeUrls) {
        this.clusterEventRestService = clusterEventRestService;
        this.clusterNodeUrls = clusterNodeUrls;
    }

    @Override
    public void receiveBroadcast(ContextInstanceStateChangeEvent event) {
        for (String nodeUrl : clusterNodeUrls) {
            clusterEventRestService.broadcastContextInstanceStateChange(nodeUrl, event);
        }
    }
}
