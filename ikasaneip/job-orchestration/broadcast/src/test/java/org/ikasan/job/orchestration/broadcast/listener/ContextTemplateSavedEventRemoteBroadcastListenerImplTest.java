package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ContextTemplateSavedEventRemoteBroadcastListenerImplTest extends RemoteBroadcastListenerTestSupport {

    @Test
    // light test to capture possible copy paste errors
    public void testReceiveContextTemplateSavedEventBroadcastSubmitsExpectedClusterServiceCall() {
        ContextTemplate contextTemplate = mock(ContextTemplate.class);

        new ContextTemplateSavedEventRemoteBroadcastListenerImpl(List.of(channel))
            .receiveContextTemplateSavedEventBroadcast(contextTemplate);

        verify(service).broadcastContextTemplateSaved(contextTemplate);
    }
}
