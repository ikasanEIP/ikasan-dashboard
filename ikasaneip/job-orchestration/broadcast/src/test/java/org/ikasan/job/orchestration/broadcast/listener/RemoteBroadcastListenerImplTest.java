package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.job.orchestration.broadcast.ClusterEventBroadcastChannel;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RemoteBroadcastListenerImplTest {

    private final ClusterEventService service = mock(ClusterEventService.class);
    private final ClusterEventBroadcastChannel channel = new ImmediateBroadcastChannel(service);

    @Test
    public void testSchedulerJobStateChangeListenerSubmitsExpectedClusterServiceCall() {
        SchedulerJobInstanceStateChangeEvent event = mock(SchedulerJobInstanceStateChangeEvent.class);

        new SchedulerJobStateChangeEventRemoteBroadcastListenerImpl(List.of(channel)).receiveBroadcast(event);

        verify(service).broadcastSchedulerJobStateChange(event);
    }

    @Test
    public void testJobLockCacheListenerSubmitsExpectedClusterServiceCall() {
        JobLockCacheEvent event = mock(JobLockCacheEvent.class);

        new JobLockCacheEventRemoteBroadcastListenerImpl(List.of(channel)).receiveBroadcast(event);

        verify(service).broadcastJobLockCache(event);
    }

    @Test
    public void testContextInstanceStateChangeListenerSubmitsExpectedClusterServiceCall() {
        ContextInstanceStateChangeEvent event = mock(ContextInstanceStateChangeEvent.class);

        new ContextInstanceStateChangeEventRemoteBroadcastListenerImpl(List.of(channel)).receiveBroadcast(event);

        verify(service).broadcastContextInstanceStateChange(event);
    }

    @Test
    public void testContextViewUpdateListenerSubmitsExpectedClusterServiceCall() {
        String message = "context view updated";

        new ContextViewUpdateEventRemoteBroadcastListenerImpl(List.of(channel)).receiveBroadcast(message);

        verify(service).broadcastContextViewUpdate(message);
    }

    @Test
    public void testNewSchedulerJobListenerSubmitsExpectedClusterServiceCall() {
        SchedulerJob schedulerJob = mock(SchedulerJob.class);

        new NewSchedulerJobEventRemoteBroadcastListenerImpl(List.of(channel)).receiveBroadcast(schedulerJob);

        verify(service).broadcastNewSchedulerJob(schedulerJob);
    }

    @Test
    public void testContextInstanceSavedListenerSubmitsExpectedClusterServiceCall() {
        ContextInstance contextInstance = mock(ContextInstance.class);

        new ContextInstanceSavedEventRemoteBroadcastListenerImpl(List.of(channel)).receiveBroadcast(contextInstance);

        verify(service).broadcastContextInstanceSaved(contextInstance);
    }

    @Test
    public void testContextTemplateSavedListenerSubmitsExpectedClusterServiceCall() {
        ContextTemplate contextTemplate = mock(ContextTemplate.class);

        new ContextTemplateSavedEventRemoteBroadcastListenerImpl(List.of(channel))
            .receiveContextTemplateSavedEventBroadcast(contextTemplate);

        verify(service).broadcastContextTemplateSaved(contextTemplate);
    }

    @Test
    public void testContextTemplateEnableDisableListenerSubmitsExpectedClusterServiceCall() {
        ContextTemplate contextTemplate = mock(ContextTemplate.class);

        new ContextTemplateEnableDisableEventRemoteBroadcastListenerImpl(List.of(channel)).receiveBroadcast(contextTemplate);

        verify(service).broadcastContextTemplateEnableDisable(contextTemplate);
    }

    @Test
    public void testContextInstanceDlqListenerSubmitsExpectedClusterServiceCall() {
        ContextInstance contextInstance = mock(ContextInstance.class);

        new ContextInstanceDlqEventRemoteBroadcastListenerImpl(List.of(channel)).receiveBroadcast(contextInstance);

        verify(service).broadcastContextInstanceDlq(contextInstance);
    }

    private static class ImmediateBroadcastChannel implements ClusterEventBroadcastChannel {
        private final ClusterEventService service;

        private ImmediateBroadcastChannel(ClusterEventService service) {
            this.service = service;
        }

        @Override
        public void submit(Runnable task) {
            task.run();
        }

        @Override
        public ClusterEventService service() {
            return service;
        }

        @Override
        public void shutdown() {
            // Nothing to release.
        }
    }
}
