package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ContextTemplateEnableDisableEventRemoteBroadcastListenerImplTest extends RemoteBroadcastListenerTestSupport {

    @Test
    // light test to capture possible copy paste errors
    public void testReceiveBroadcastSubmitsExpectedClusterServiceCall() {
        ContextTemplate contextTemplate = mock(ContextTemplate.class);

        new ContextTemplateEnableDisableEventRemoteBroadcastListenerImpl(List.of(channel)).receiveBroadcast(contextTemplate);

        verify(service).broadcastContextTemplateEnableDisable(contextTemplate);
    }
}
