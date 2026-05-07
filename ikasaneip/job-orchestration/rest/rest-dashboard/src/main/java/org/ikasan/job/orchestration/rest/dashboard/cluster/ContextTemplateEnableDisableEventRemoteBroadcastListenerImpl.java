package org.ikasan.job.orchestration.rest.dashboard.cluster;

import org.ikasan.spec.scheduled.event.service.ContextTemplateEnableDisableEventRemoteBroadcastListener;
import org.ikasan.job.orchestration.rest.client.ClusterEventRestServiceImpl;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;

import java.util.List;

public class ContextTemplateEnableDisableEventRemoteBroadcastListenerImpl
    implements ContextTemplateEnableDisableEventRemoteBroadcastListener {

    private final ClusterEventRestServiceImpl clusterEventRestService;
    private final List<String> clusterNodeUrls;

    public ContextTemplateEnableDisableEventRemoteBroadcastListenerImpl(ClusterEventRestServiceImpl clusterEventRestService,
                                                                        List<String> clusterNodeUrls) {
        this.clusterEventRestService = clusterEventRestService;
        this.clusterNodeUrls = clusterNodeUrls;
    }

    @Override
    public void receiveBroadcast(ContextTemplate contextTemplate) {
        for (String nodeUrl : clusterNodeUrls) {
            clusterEventRestService.broadcastContextTemplateEnableDisable(nodeUrl, contextTemplate);
        }
    }
}
