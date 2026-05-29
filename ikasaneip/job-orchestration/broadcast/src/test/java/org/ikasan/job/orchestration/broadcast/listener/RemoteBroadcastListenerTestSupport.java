package org.ikasan.job.orchestration.broadcast.listener;

import org.ikasan.job.orchestration.broadcast.ClusterEventBroadcastChannel;
import org.ikasan.spec.scheduled.event.service.ClusterEventService;

import static org.mockito.Mockito.mock;

abstract class RemoteBroadcastListenerTestSupport {

    protected final ClusterEventService service = mock(ClusterEventService.class);
    protected final ClusterEventBroadcastChannel channel = new TestBroadcastChannel(service);

    // Test helper to broadcast immediately on the calling thread, bypassing any asynchronous execution lane.
    private static class TestBroadcastChannel implements ClusterEventBroadcastChannel {
        private final ClusterEventService service;

        private TestBroadcastChannel(ClusterEventService service) {
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
