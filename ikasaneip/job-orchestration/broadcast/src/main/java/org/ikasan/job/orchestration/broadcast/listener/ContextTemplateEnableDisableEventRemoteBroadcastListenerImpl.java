package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.ikasan.spec.scheduled.event.service.ContextTemplateEnableDisableEventRemoteBroadcastListener;

import java.util.List;

/**
 * Implementation of remote broadcast listener for context template enable/disable events.
 * Forwards received broadcasts to all configured cluster event services.
 *
 * @author Ikasan Development Team
 */
public class ContextTemplateEnableDisableEventRemoteBroadcastListenerImpl
    implements ContextTemplateEnableDisableEventRemoteBroadcastListener {

    private final List<ClusterEventService> clusterEventServices;

    /**
     * Constructor
     *
     * @param clusterEventServices list of cluster event services to broadcast to
     */
    public ContextTemplateEnableDisableEventRemoteBroadcastListenerImpl(List<ClusterEventService> clusterEventServices) {
        this.clusterEventServices = clusterEventServices;
    }

    /**
     * Receives a context template enable/disable broadcast and forwards it to all cluster event services.
     *
     * @param contextTemplate the context template to broadcast
     */
    @Override
    public void receiveBroadcast(ContextTemplate contextTemplate) {
        for (ClusterEventService service : clusterEventServices) {
            service.broadcastContextTemplateEnableDisable(contextTemplate);
        }
    }
}
