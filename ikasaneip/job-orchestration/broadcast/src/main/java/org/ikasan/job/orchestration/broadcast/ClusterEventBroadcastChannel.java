package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.service.ClusterEventService;

/**
 * NOTE - This iterface has been added into Ikasan5 at org.ikasan.spec.scheduled.event.service.ClusterEventBroadcastChannel
 *   once merged to 5 snapshot this version will be removed.
 * Delivery channel for broadcasting cluster events to one remote dashboard node.
 *
 * @author Ikasan Development Team
 */
public interface ClusterEventBroadcastChannel {

    /**
     * Submits a broadcast action to this remote node's delivery lane.
     *
     * @param task broadcast action to execute
     */
    void submit(Runnable task);

    /**
     * Returns the service used to publish events to this remote node.
     *
     * @return cluster event service
     */
    ClusterEventService service();

    /**
     * Releases channel resources.
     */
    void shutdown();
}
