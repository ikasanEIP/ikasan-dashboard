package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventRemoteBroadcastListener;

import java.util.List;

/**
 * Implementation of remote broadcast listener for job lock cache events.
 * Forwards received broadcasts to all configured cluster event services.
 *
 * @author Ikasan Development Team
 */
public class JobLockCacheEventRemoteBroadcastListenerImpl
    implements JobLockCacheEventRemoteBroadcastListener {

    private final List<ClusterEventService> clusterEventServices;

    /**
     * Constructor
     *
     * @param clusterEventServices list of cluster event services to broadcast to
     */
    public JobLockCacheEventRemoteBroadcastListenerImpl(List<ClusterEventService> clusterEventServices) {
        this.clusterEventServices = clusterEventServices;
    }

    /**
     * Receives a job lock cache broadcast and forwards it to all cluster event services.
     *
     * @param event the job lock cache event to broadcast
     */
    @Override
    public void receiveBroadcast(JobLockCacheEvent event) {
        for (ClusterEventService service : clusterEventServices) {
            service.broadcastJobLockCache(event);
        }
    }
}
