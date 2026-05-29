package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.job.orchestration.broadcast.ClusterEventBroadcastChannel;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventRemoteBroadcastListener;

import java.util.List;

/**
 * Implementation of a remote broadcast listener for context instance state change events.
 * Forwards received broadcasts to all configured cluster peers via their dedicated
 * {@link ClusterEventBroadcastChannel} execution lanes.
 *
 * @author Ikasan Development Team
 */
public class ContextInstanceStateChangeEventRemoteBroadcastListenerImpl
    implements ContextInstanceStateChangeEventRemoteBroadcastListener {

    private final List<ClusterEventBroadcastChannel> channels;

    public ContextInstanceStateChangeEventRemoteBroadcastListenerImpl(List<ClusterEventBroadcastChannel> channels) {
        this.channels = channels;
    }

    @Override
    public void receiveBroadcast(ContextInstanceStateChangeEvent event) {
        for (ClusterEventBroadcastChannel channel : channels) {
            channel.submit(() -> channel.service().broadcastContextInstanceStateChange(event));
        }
    }
}
