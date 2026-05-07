package org.ikasan.job.orchestration.rest.dashboard.cluster;

import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventRemoteBroadcastListener;
import org.ikasan.job.orchestration.rest.client.ClusterEventRestServiceImpl;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;

import java.util.List;

public class SchedulerJobStateChangeEventRemoteBroadcastListenerImpl
    implements SchedulerJobStateChangeEventRemoteBroadcastListener {

    private final ClusterEventRestServiceImpl clusterEventRestService;
    private final List<String> clusterNodeUrls;

    public SchedulerJobStateChangeEventRemoteBroadcastListenerImpl(ClusterEventRestServiceImpl clusterEventRestService,
                                                                   List<String> clusterNodeUrls) {
        this.clusterEventRestService = clusterEventRestService;
        this.clusterNodeUrls = clusterNodeUrls;
    }

    @Override
    public void receiveBroadcast(SchedulerJobInstanceStateChangeEvent event) {
        for (String nodeUrl : clusterNodeUrls) {
            clusterEventRestService.broadcastSchedulerJobStateChange(nodeUrl, event);
        }
    }
}
