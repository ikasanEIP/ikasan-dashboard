package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ContextInstanceDlqEventRemoteBroadcastListenerImplTest extends RemoteBroadcastListenerTestSupport {

    @Test
    // light test to capture possible copy paste errors
    public void testReceiveBroadcastSubmitsExpectedClusterServiceCall() {
        ContextInstance contextInstance = mock(ContextInstance.class);

        new ContextInstanceDlqEventRemoteBroadcastListenerImpl(List.of(channel)).receiveBroadcast(contextInstance);

        verify(service).broadcastContextInstanceDlq(contextInstance);
    }
}
