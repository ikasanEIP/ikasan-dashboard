package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventRemoteBroadcastListener;

import java.util.List;

/**
 * Implementation of remote broadcast listener for scheduler job state change events.
 * Forwards received broadcasts to all configured cluster event services.
 *
 * @author Ikasan Development Team
 */
public class SchedulerJobStateChangeEventRemoteBroadcastListenerImpl
    implements SchedulerJobStateChangeEventRemoteBroadcastListener {

    private final List<ClusterEventService> clusterEventServices;

    /**
     * Constructor
     *
     * @param clusterEventServices list of cluster event services to broadcast to
     */
    public SchedulerJobStateChangeEventRemoteBroadcastListenerImpl(List<ClusterEventService> clusterEventServices) {
        this.clusterEventServices = clusterEventServices;
    }

    /**
     * Receives a scheduler job state change broadcast and forwards it to all cluster event services.
     *
     * @param event the state change event to broadcast
     */
    @Override
    public void receiveBroadcast(SchedulerJobInstanceStateChangeEvent event) {
        for (ClusterEventService service : clusterEventServices) {
            service.broadcastSchedulerJobStateChange(event);
        }
    }
}
