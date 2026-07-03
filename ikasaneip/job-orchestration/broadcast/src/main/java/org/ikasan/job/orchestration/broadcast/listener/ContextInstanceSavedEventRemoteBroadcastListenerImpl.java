package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.event.service.ClusterEventBroadcastChannel;
import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventRemoteBroadcastListener;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;

import java.util.List;

/**
 * Implementation of a remote broadcast listener for context instance saved events.
 * Forwards received broadcasts to all configured cluster peers via their dedicated
 * {@link ClusterEventBroadcastChannel} execution lanes.
 *
 * @author Ikasan Development Team
 */
public class ContextInstanceSavedEventRemoteBroadcastListenerImpl
    implements ContextInstanceSavedEventRemoteBroadcastListener {

    private final List<ClusterEventBroadcastChannel> channels;

    public ContextInstanceSavedEventRemoteBroadcastListenerImpl(List<ClusterEventBroadcastChannel> channels) {
        this.channels = channels;
    }

    @Override
    public void receiveBroadcast(ContextInstance contextInstance) {
        for (ClusterEventBroadcastChannel channel : channels) {
            channel.submit(() -> channel.service().broadcastContextInstanceSaved(contextInstance));
        }
    }
}
