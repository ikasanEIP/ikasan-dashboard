package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceDlqEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;

import java.util.List;

/**
 * Implementation of remote broadcast listener for context instance DLQ events.
 * Forwards received broadcasts to all configured cluster event services.
 *
 * @author Ikasan Development Team
 */
public class ContextInstanceDlqEventRemoteBroadcastListenerImpl
    implements ContextInstanceDlqEventRemoteBroadcastListener {

    private final List<ClusterEventService> clusterEventServices;

    /**
     * Constructor
     *
     * @param clusterEventServices list of cluster event services to broadcast to
     */
    public ContextInstanceDlqEventRemoteBroadcastListenerImpl(List<ClusterEventService> clusterEventServices) {
        this.clusterEventServices = clusterEventServices;
    }

    /**
     * Receives a context instance DLQ broadcast and forwards it to all cluster event services.
     *
     * @param contextInstance the context instance to broadcast
     */
    @Override
    public void receiveBroadcast(ContextInstance contextInstance) {
        for (ClusterEventService service : clusterEventServices) {
            service.broadcastContextInstanceDlq(contextInstance);
        }
    }
}
