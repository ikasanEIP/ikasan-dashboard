package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.ikasan.spec.scheduled.event.service.ContextViewUpdateEventRemoteBroadcastListener;

import java.util.List;

public class ContextViewUpdateEventRemoteBroadcastListenerImpl
    implements ContextViewUpdateEventRemoteBroadcastListener {

    private final List<ClusterEventService> clusterEventServices;

    public ContextViewUpdateEventRemoteBroadcastListenerImpl(List<ClusterEventService> clusterEventServices) {
        this.clusterEventServices = clusterEventServices;
    }

    @Override
    public void receiveBroadcast(String message) {
        for (ClusterEventService service : clusterEventServices) {
            service.broadcastContextViewUpdate(message);
        }
    }
}
