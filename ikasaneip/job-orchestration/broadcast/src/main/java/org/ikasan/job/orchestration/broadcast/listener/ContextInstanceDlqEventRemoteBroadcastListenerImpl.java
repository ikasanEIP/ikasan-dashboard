package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.job.orchestration.broadcast.ClusterEventBroadcastChannel;
import org.ikasan.spec.scheduled.event.service.ContextInstanceDlqEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;

import java.util.List;

/**
 * Implementation of a remote broadcast listener for context instance DLQ events.
 * Forwards received broadcasts to all configured cluster peers via their dedicated
 * {@link ClusterEventBroadcastChannel} execution lanes.
 *
 * @author Ikasan Development Team
 */
public class ContextInstanceDlqEventRemoteBroadcastListenerImpl
    implements ContextInstanceDlqEventRemoteBroadcastListener {

    private final List<ClusterEventBroadcastChannel> channels;

    public ContextInstanceDlqEventRemoteBroadcastListenerImpl(List<ClusterEventBroadcastChannel> channels) {
        this.channels = channels;
    }

    @Override
    public void receiveBroadcast(ContextInstance contextInstance) {
        for (ClusterEventBroadcastChannel channel : channels) {
            channel.submit(() -> channel.service().broadcastContextInstanceDlq(contextInstance));
        }
    }
}
