package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SchedulerJobStateChangeEventRemoteBroadcastListenerImplTest extends RemoteBroadcastListenerTestSupport {

    @Test
    // light test to capture possible copy paste errors
    public void testReceiveBroadcastSubmitsExpectedClusterServiceCall() {
        SchedulerJobInstanceStateChangeEvent event = mock(SchedulerJobInstanceStateChangeEvent.class);

        new SchedulerJobStateChangeEventRemoteBroadcastListenerImpl(List.of(channel)).receiveBroadcast(event);

        verify(service).broadcastSchedulerJobStateChange(event);
    }
}
