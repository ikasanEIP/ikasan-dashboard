package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;

import java.util.List;

/**
 * Implementation of remote broadcast listener for context instance saved events.
 * Forwards received broadcasts to all configured cluster event services.
 *
 * @author Ikasan Development Team
 */
public class ContextInstanceSavedEventRemoteBroadcastListenerImpl
    implements ContextInstanceSavedEventRemoteBroadcastListener {

    private final List<ClusterEventService> clusterEventServices;

    /**
     * Constructor
     *
     * @param clusterEventServices list of cluster event services to broadcast to
     */
    public ContextInstanceSavedEventRemoteBroadcastListenerImpl(List<ClusterEventService> clusterEventServices) {
        this.clusterEventServices = clusterEventServices;
    }

    /**
     * Receives a context instance saved broadcast and forwards it to all cluster event services.
     *
     * @param contextInstance the context instance to broadcast
     */
    @Override
    public void receiveBroadcast(ContextInstance contextInstance) {
        for (ClusterEventService service : clusterEventServices) {
            service.broadcastContextInstanceSaved(contextInstance);
        }
    }
}
