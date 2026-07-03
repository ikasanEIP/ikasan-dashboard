package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.event.service.ClusterEventBroadcastChannel;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.service.ContextTemplateSavedEventRemoteBroadcastListener;

import java.util.List;

/**
 * Implementation of a remote broadcast listener for context template saved events.
 * Forwards received broadcasts to all configured cluster peers via their dedicated
 * {@link ClusterEventBroadcastChannel} execution lanes.
 *
 * @author Ikasan Development Team
 */
public class ContextTemplateSavedEventRemoteBroadcastListenerImpl
    implements ContextTemplateSavedEventRemoteBroadcastListener {

    private final List<ClusterEventBroadcastChannel> channels;

    public ContextTemplateSavedEventRemoteBroadcastListenerImpl(List<ClusterEventBroadcastChannel> channels) {
        this.channels = channels;
    }

    @Override
    public void receiveContextTemplateSavedEventBroadcast(ContextTemplate contextTemplate) {
        for (ClusterEventBroadcastChannel channel : channels) {
            channel.submit(() -> channel.service().broadcastContextTemplateSaved(contextTemplate));
        }
    }
}
