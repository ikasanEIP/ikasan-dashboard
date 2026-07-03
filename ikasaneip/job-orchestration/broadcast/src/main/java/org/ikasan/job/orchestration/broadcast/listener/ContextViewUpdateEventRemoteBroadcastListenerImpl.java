package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.event.service.ClusterEventBroadcastChannel;
import org.ikasan.spec.scheduled.event.service.ContextViewUpdateEventRemoteBroadcastListener;

import java.util.List;

/**
 * Implementation of a remote broadcast listener for context view update events.
 * Forwards received broadcasts to all configured cluster peers via their dedicated
 * {@link ClusterEventBroadcastChannel} execution lanes.
 *
 * @author Ikasan Development Team
 */
public class ContextViewUpdateEventRemoteBroadcastListenerImpl
    implements ContextViewUpdateEventRemoteBroadcastListener {

    private final List<ClusterEventBroadcastChannel> channels;

    public ContextViewUpdateEventRemoteBroadcastListenerImpl(List<ClusterEventBroadcastChannel> channels) {
        this.channels = channels;
    }

    @Override
    public void receiveBroadcast(String message) {
        for (ClusterEventBroadcastChannel channel : channels) {
            channel.submit(() -> channel.service().broadcastContextViewUpdate(message));
        }
    }
}
