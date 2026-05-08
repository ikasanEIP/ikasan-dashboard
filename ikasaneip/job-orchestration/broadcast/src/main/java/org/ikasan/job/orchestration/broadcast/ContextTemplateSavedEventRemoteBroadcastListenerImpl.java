package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.ikasan.spec.scheduled.event.service.ContextTemplateSavedEventRemoteBroadcastListener;

import java.util.List;

public class ContextTemplateSavedEventRemoteBroadcastListenerImpl
    implements ContextTemplateSavedEventRemoteBroadcastListener {

    private final List<ClusterEventService> clusterEventServices;

    public ContextTemplateSavedEventRemoteBroadcastListenerImpl(List<ClusterEventService> clusterEventServices) {
        this.clusterEventServices = clusterEventServices;
    }

    @Override
    public void receiveContextTemplateSavedEventBroadcast(ContextTemplate contextTemplate) {
        for (ClusterEventService service : clusterEventServices) {
            service.broadcastContextTemplateSaved(contextTemplate);
        }
    }
}
