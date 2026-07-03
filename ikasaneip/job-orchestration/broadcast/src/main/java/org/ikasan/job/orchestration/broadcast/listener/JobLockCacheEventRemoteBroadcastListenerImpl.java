package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.event.service.ClusterEventBroadcastChannel;
import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventRemoteBroadcastListener;

import java.util.List;

/**
 * Implementation of a remote broadcast listener for job lock cache events.
 * Forwards received broadcasts to all configured cluster peers via their dedicated
 * {@link ClusterEventBroadcastChannel} execution lanes.
 *
 * @author Ikasan Development Team
 */
public class JobLockCacheEventRemoteBroadcastListenerImpl
    implements JobLockCacheEventRemoteBroadcastListener {

    private final List<ClusterEventBroadcastChannel> channels;

    public JobLockCacheEventRemoteBroadcastListenerImpl(List<ClusterEventBroadcastChannel> channels) {
        this.channels = channels;
    }

    @Override
    public void receiveBroadcast(JobLockCacheEvent event) {
        for (ClusterEventBroadcastChannel channel : channels) {
            channel.submit(() -> channel.service().broadcastJobLockCache(event));
        }
    }
}
