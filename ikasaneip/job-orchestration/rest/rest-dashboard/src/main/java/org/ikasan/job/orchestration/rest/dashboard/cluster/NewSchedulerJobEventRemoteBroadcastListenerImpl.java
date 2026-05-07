package org.ikasan.job.orchestration.rest.dashboard.cluster;

import org.ikasan.spec.scheduled.event.service.NewSchedulerJobEventRemoteBroadcastListener;
import org.ikasan.job.orchestration.rest.client.ClusterEventRestServiceImpl;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.List;

public class NewSchedulerJobEventRemoteBroadcastListenerImpl
    implements NewSchedulerJobEventRemoteBroadcastListener {

    private final ClusterEventRestServiceImpl clusterEventRestService;
    private final List<String> clusterNodeUrls;

    public NewSchedulerJobEventRemoteBroadcastListenerImpl(ClusterEventRestServiceImpl clusterEventRestService,
                                                           List<String> clusterNodeUrls) {
        this.clusterEventRestService = clusterEventRestService;
        this.clusterNodeUrls = clusterNodeUrls;
    }

    @Override
    public void receiveBroadcast(SchedulerJob schedulerJob) {
        for (String nodeUrl : clusterNodeUrls) {
            clusterEventRestService.broadcastNewSchedulerJob(nodeUrl, schedulerJob);
        }
    }
}
