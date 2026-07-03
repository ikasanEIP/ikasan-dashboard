package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.event.service.ClusterEventBroadcastChannel;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.service.ContextTemplateEnableDisableEventRemoteBroadcastListener;

import java.util.List;

/**
 * Implementation of a remote broadcast listener for context template enable/disable events.
 * Forwards received broadcasts to all configured cluster peers via their dedicated
 * {@link ClusterEventBroadcastChannel} execution lanes.
 *
 * @author Ikasan Development Team
 */
public class ContextTemplateEnableDisableEventRemoteBroadcastListenerImpl
    implements ContextTemplateEnableDisableEventRemoteBroadcastListener {

    private final List<ClusterEventBroadcastChannel> channels;

    public ContextTemplateEnableDisableEventRemoteBroadcastListenerImpl(List<ClusterEventBroadcastChannel> channels) {
        this.channels = channels;
    }

    @Override
    public void receiveBroadcast(ContextTemplate contextTemplate) {
        for (ClusterEventBroadcastChannel channel : channels) {
            channel.submit(() -> channel.service().broadcastContextTemplateEnableDisable(contextTemplate));
        }
    }
}
