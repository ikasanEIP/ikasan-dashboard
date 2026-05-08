package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventRemoteBroadcastListener;

import java.util.List;

public class ContextInstanceStateChangeEventRemoteBroadcastListenerImpl
    implements ContextInstanceStateChangeEventRemoteBroadcastListener {

    private final List<ClusterEventService> clusterEventServices;

    public ContextInstanceStateChangeEventRemoteBroadcastListenerImpl(List<ClusterEventService> clusterEventServices) {
        this.clusterEventServices = clusterEventServices;
    }

    @Override
    public void receiveBroadcast(ContextInstanceStateChangeEvent event) {
        for (ClusterEventService service : clusterEventServices) {
            service.broadcastContextInstanceStateChange(event);
        }
    }
}
