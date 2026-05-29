package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.job.orchestration.broadcast.ClusterEventBroadcastChannel;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventRemoteBroadcastListener;

import java.util.List;

/**
 * Implementation of a remote broadcast listener for scheduler job state change events.
 * Forwards received broadcasts to all configured cluster peers via their dedicated
 * {@link ClusterEventBroadcastChannel} execution lanes.
 *
 * @author Ikasan Development Team
 */
public class SchedulerJobStateChangeEventRemoteBroadcastListenerImpl
    implements SchedulerJobStateChangeEventRemoteBroadcastListener {

    private final List<ClusterEventBroadcastChannel> channels;

    public SchedulerJobStateChangeEventRemoteBroadcastListenerImpl(List<ClusterEventBroadcastChannel> channels) {
        this.channels = channels;
    }

    @Override
    public void receiveBroadcast(SchedulerJobInstanceStateChangeEvent event) {
        for (ClusterEventBroadcastChannel channel : channels) {
            channel.submit(() -> channel.service().broadcastSchedulerJobStateChange(event));
        }
    }
}
