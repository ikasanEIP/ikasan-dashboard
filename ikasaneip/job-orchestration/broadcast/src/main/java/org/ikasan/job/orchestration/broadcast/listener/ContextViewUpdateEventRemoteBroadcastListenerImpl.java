package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.ikasan.spec.scheduled.event.service.ContextViewUpdateEventRemoteBroadcastListener;

import java.util.List;

/**
 * Implementation of remote broadcast listener for context view update events.
 * Forwards received broadcasts to all configured cluster event services.
 *
 * @author Ikasan Development Team
 */
public class ContextViewUpdateEventRemoteBroadcastListenerImpl
    implements ContextViewUpdateEventRemoteBroadcastListener {

    private final List<ClusterEventService> clusterEventServices;

    /**
     * Constructor
     *
     * @param clusterEventServices list of cluster event services to broadcast to
     */
    public ContextViewUpdateEventRemoteBroadcastListenerImpl(List<ClusterEventService> clusterEventServices) {
        this.clusterEventServices = clusterEventServices;
    }

    /**
     * Receives a context view update broadcast and forwards it to all cluster event services.
     *
     * @param message the message to broadcast
     */
    @Override
    public void receiveBroadcast(String message) {
        for (ClusterEventService service : clusterEventServices) {
            service.broadcastContextViewUpdate(message);
        }
    }
}
