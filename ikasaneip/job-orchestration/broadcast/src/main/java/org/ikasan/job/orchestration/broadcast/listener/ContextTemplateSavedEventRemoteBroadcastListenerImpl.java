package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.ikasan.spec.scheduled.event.service.ContextTemplateSavedEventRemoteBroadcastListener;

import java.util.List;

/**
 * Implementation of remote broadcast listener for context template saved events.
 * Forwards received broadcasts to all configured cluster event services.
 *
 * @author Ikasan Development Team
 */
public class ContextTemplateSavedEventRemoteBroadcastListenerImpl
    implements ContextTemplateSavedEventRemoteBroadcastListener {

    private final List<ClusterEventService> clusterEventServices;

    /**
     * Constructor
     *
     * @param clusterEventServices list of cluster event services to broadcast to
     */
    public ContextTemplateSavedEventRemoteBroadcastListenerImpl(List<ClusterEventService> clusterEventServices) {
        this.clusterEventServices = clusterEventServices;
    }

    /**
     * Receives a context template saved broadcast and forwards it to all cluster event services.
     *
     * @param contextTemplate the context template to broadcast
     */
    @Override
    public void receiveContextTemplateSavedEventBroadcast(ContextTemplate contextTemplate) {
        for (ClusterEventService service : clusterEventServices) {
            service.broadcastContextTemplateSaved(contextTemplate);
        }
    }
}
