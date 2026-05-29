package org.ikasan.job.orchestration.broadcast.listener;

import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.verify;

public class ContextViewUpdateEventRemoteBroadcastListenerImplTest extends RemoteBroadcastListenerTestSupport {

    @Test
    // light test to capture possible copy paste errors
    public void testReceiveBroadcastSubmitsExpectedClusterServiceCall() {
        String message = "context view updated";

        new ContextViewUpdateEventRemoteBroadcastListenerImpl(List.of(channel)).receiveBroadcast(message);

        verify(service).broadcastContextViewUpdate(message);
    }
}
