package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.job.orchestration.broadcast.ClusterEventBroadcastChannel;
import org.ikasan.spec.scheduled.event.service.NewSchedulerJobEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;

import java.util.List;

/**
 * Implementation of a remote broadcast listener for new scheduler job events.
 * Forwards received broadcasts to all configured cluster peers via their dedicated
 * {@link ClusterEventBroadcastChannel} execution lanes.
 *
 * @author Ikasan Development Team
 */
public class NewSchedulerJobEventRemoteBroadcastListenerImpl
    implements NewSchedulerJobEventRemoteBroadcastListener {

    private final List<ClusterEventBroadcastChannel> channels;

    public NewSchedulerJobEventRemoteBroadcastListenerImpl(List<ClusterEventBroadcastChannel> channels) {
        this.channels = channels;
    }

    @Override
    public void receiveBroadcast(SchedulerJob schedulerJob) {
        for (ClusterEventBroadcastChannel channel : channels) {
            channel.submit(() -> channel.service().broadcastNewSchedulerJob(schedulerJob));
        }
    }
}
