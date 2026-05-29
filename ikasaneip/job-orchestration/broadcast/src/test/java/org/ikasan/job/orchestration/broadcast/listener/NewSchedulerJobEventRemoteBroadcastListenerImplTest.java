package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class NewSchedulerJobEventRemoteBroadcastListenerImplTest extends RemoteBroadcastListenerTestSupport {

    @Test
    // light test to capture possible copy paste errors
    public void testReceiveBroadcastSubmitsExpectedClusterServiceCall() {
        SchedulerJob schedulerJob = mock(SchedulerJob.class);

        new NewSchedulerJobEventRemoteBroadcastListenerImpl(List.of(channel)).receiveBroadcast(schedulerJob);

        verify(service).broadcastNewSchedulerJob(schedulerJob);
    }
}
