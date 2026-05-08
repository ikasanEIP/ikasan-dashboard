package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;

import java.util.List;

public class ContextInstanceSavedEventRemoteBroadcastListenerImpl
    implements ContextInstanceSavedEventRemoteBroadcastListener {

    private final List<ClusterEventService> clusterEventServices;

    public ContextInstanceSavedEventRemoteBroadcastListenerImpl(List<ClusterEventService> clusterEventServices) {
        this.clusterEventServices = clusterEventServices;
    }

    @Override
    public void receiveBroadcast(ContextInstance contextInstance) {
        for (ClusterEventService service : clusterEventServices) {
            service.broadcastContextInstanceSaved(contextInstance);
        }
    }
}
