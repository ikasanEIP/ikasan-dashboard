package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.ikasan.spec.scheduled.event.service.NewSchedulerJobEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.List;

/**
 * Implementation of remote broadcast listener for new scheduler job events.
 * Forwards received broadcasts to all configured cluster event services.
 *
 * @author Ikasan Development Team
 */
public class NewSchedulerJobEventRemoteBroadcastListenerImpl
    implements NewSchedulerJobEventRemoteBroadcastListener {

    private final List<ClusterEventService> clusterEventServices;

    /**
     * Constructor
     *
     * @param clusterEventServices list of cluster event services to broadcast to
     */
    public NewSchedulerJobEventRemoteBroadcastListenerImpl(List<ClusterEventService> clusterEventServices) {
        this.clusterEventServices = clusterEventServices;
    }

    /**
     * Receives a new scheduler job broadcast and forwards it to all cluster event services.
     *
     * @param schedulerJob the scheduler job to broadcast
     */
    @Override
    public void receiveBroadcast(SchedulerJob schedulerJob) {
        for (ClusterEventService service : clusterEventServices) {
            service.broadcastNewSchedulerJob(schedulerJob);
        }
    }
}
