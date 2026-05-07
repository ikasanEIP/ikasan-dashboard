package org.ikasan.job.orchestration.rest.dashboard.cluster;

import org.ikasan.spec.scheduled.event.service.ContextInstanceDlqEventRemoteBroadcastListener;
import org.ikasan.job.orchestration.rest.client.ClusterEventRestServiceImpl;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;

import java.util.List;

public class ContextInstanceDlqEventRemoteBroadcastListenerImpl
    implements ContextInstanceDlqEventRemoteBroadcastListener {

    private final ClusterEventRestServiceImpl clusterEventRestService;
    private final List<String> clusterNodeUrls;

    public ContextInstanceDlqEventRemoteBroadcastListenerImpl(ClusterEventRestServiceImpl clusterEventRestService,
                                                              List<String> clusterNodeUrls) {
        this.clusterEventRestService = clusterEventRestService;
        this.clusterNodeUrls = clusterNodeUrls;
    }

    @Override
    public void receiveBroadcast(ContextInstance contextInstance) {
        for (String nodeUrl : clusterNodeUrls) {
            clusterEventRestService.broadcastContextInstanceDlq(nodeUrl, contextInstance);
        }
    }
}
