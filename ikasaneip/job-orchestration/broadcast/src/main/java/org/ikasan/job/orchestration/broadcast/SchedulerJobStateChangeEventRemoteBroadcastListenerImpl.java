package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventRemoteBroadcastListener;

import java.util.List;

public class SchedulerJobStateChangeEventRemoteBroadcastListenerImpl
    implements SchedulerJobStateChangeEventRemoteBroadcastListener {

    private final List<ClusterEventService> clusterEventServices;

    public SchedulerJobStateChangeEventRemoteBroadcastListenerImpl(List<ClusterEventService> clusterEventServices) {
        this.clusterEventServices = clusterEventServices;
    }

    @Override
    public void receiveBroadcast(SchedulerJobInstanceStateChangeEvent event) {
        for (ClusterEventService service : clusterEventServices) {
            service.broadcastSchedulerJobStateChange(event);
        }
    }
}
