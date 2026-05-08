package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.ikasan.spec.scheduled.event.service.NewSchedulerJobEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.List;

public class NewSchedulerJobEventRemoteBroadcastListenerImpl
    implements NewSchedulerJobEventRemoteBroadcastListener {

    private final List<ClusterEventService> clusterEventServices;

    public NewSchedulerJobEventRemoteBroadcastListenerImpl(List<ClusterEventService> clusterEventServices) {
        this.clusterEventServices = clusterEventServices;
    }

    @Override
    public void receiveBroadcast(SchedulerJob schedulerJob) {
        for (ClusterEventService service : clusterEventServices) {
            service.broadcastNewSchedulerJob(schedulerJob);
        }
    }
}
