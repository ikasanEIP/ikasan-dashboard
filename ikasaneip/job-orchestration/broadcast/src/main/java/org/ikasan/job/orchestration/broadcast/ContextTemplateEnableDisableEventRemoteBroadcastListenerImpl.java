package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.ikasan.spec.scheduled.event.service.ContextTemplateEnableDisableEventRemoteBroadcastListener;

import java.util.List;

public class ContextTemplateEnableDisableEventRemoteBroadcastListenerImpl
    implements ContextTemplateEnableDisableEventRemoteBroadcastListener {

    private final List<ClusterEventService> clusterEventServices;

    public ContextTemplateEnableDisableEventRemoteBroadcastListenerImpl(List<ClusterEventService> clusterEventServices) {
        this.clusterEventServices = clusterEventServices;
    }

    @Override
    public void receiveBroadcast(ContextTemplate contextTemplate) {
        for (ClusterEventService service : clusterEventServices) {
            service.broadcastContextTemplateEnableDisable(contextTemplate);
        }
    }
}
