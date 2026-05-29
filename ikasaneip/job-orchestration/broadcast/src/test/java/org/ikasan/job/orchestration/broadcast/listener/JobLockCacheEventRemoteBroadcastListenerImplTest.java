package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JobLockCacheEventRemoteBroadcastListenerImplTest extends RemoteBroadcastListenerTestSupport {

    @Test
    // light test to capture possible copy paste errors
    public void testReceiveBroadcastSubmitsExpectedClusterServiceCall() {
        JobLockCacheEvent event = mock(JobLockCacheEvent.class);

        new JobLockCacheEventRemoteBroadcastListenerImpl(List.of(channel)).receiveBroadcast(event);

        verify(service).broadcastJobLockCache(event);
    }
}
