package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventRemoteBroadcastListener;

import java.util.List;

/**
 * Implementation of remote broadcast listener for context instance state change events.
 * Forwards received broadcasts to all configured cluster event services.
 *
 * @author Ikasan Development Team
 */
public class ContextInstanceStateChangeEventRemoteBroadcastListenerImpl
    implements ContextInstanceStateChangeEventRemoteBroadcastListener {

    private final List<ClusterEventService> clusterEventServices;

    /**
     * Constructor
     *
     * @param clusterEventServices list of cluster event services to broadcast to
     */
    public ContextInstanceStateChangeEventRemoteBroadcastListenerImpl(List<ClusterEventService> clusterEventServices) {
        this.clusterEventServices = clusterEventServices;
    }

    /**
     * Receives a context instance state change broadcast and forwards it to all cluster event services.
     *
     * @param event the state change event to broadcast
     */
    @Override
    public void receiveBroadcast(ContextInstanceStateChangeEvent event) {
        for (ClusterEventService service : clusterEventServices) {
            service.broadcastContextInstanceStateChange(event);
        }
    }
}
