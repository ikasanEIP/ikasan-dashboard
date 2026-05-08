package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceDlqEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;

import java.util.List;

public class ContextInstanceDlqEventRemoteBroadcastListenerImpl
    implements ContextInstanceDlqEventRemoteBroadcastListener {

    private final List<ClusterEventService> clusterEventServices;

    public ContextInstanceDlqEventRemoteBroadcastListenerImpl(List<ClusterEventService> clusterEventServices) {
        this.clusterEventServices = clusterEventServices;
    }

    @Override
    public void receiveBroadcast(ContextInstance contextInstance) {
        for (ClusterEventService service : clusterEventServices) {
            service.broadcastContextInstanceDlq(contextInstance);
        }
    }
}
