package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ContextInstanceStateChangeEventRemoteBroadcastListenerImplTest extends RemoteBroadcastListenerTestSupport {

    @Test
    // light test to capture possible copy paste errors
    public void testReceiveBroadcastSubmitsExpectedClusterServiceCall() {
        ContextInstanceStateChangeEvent event = mock(ContextInstanceStateChangeEvent.class);

        new ContextInstanceStateChangeEventRemoteBroadcastListenerImpl(List.of(channel)).receiveBroadcast(event);

        verify(service).broadcastContextInstanceStateChange(event);
    }
}
