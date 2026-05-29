package org.ikasan.job.orchestration.broadcast;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.ikasan.spec.scheduled.event.service.ClusterEventService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;

public class PeerBroadcastChannelTest {

    private static final int THRESHOLD = 3;
    private static final long OPEN_DURATION_MS = 60_000L; // won't expire during a test
    private static final long OPEN_DURATION_ZERO = 0L;    // expires immediately — for half-open tests

    private ListAppender<ILoggingEvent> logAppender;
    private Logger classLogger;
    private final List<PeerBroadcastChannel> channels = new ArrayList<>();

    @Before
    public void setUp() {
        classLogger = (Logger) LoggerFactory.getLogger(PeerBroadcastChannel.class);
        classLogger.setLevel(Level.DEBUG);
        logAppender = new ListAppender<>();
        logAppender.start();
        classLogger.addAppender(logAppender);
    }

    @After
    public void tearDown() {
        channels.forEach(PeerBroadcastChannel::shutdown);
        classLogger.detachAppender(logAppender);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private PeerBroadcastChannel createChannel(int threshold, long openDurationMs) {
        ClusterEventService service = mock(ClusterEventService.class);
        PeerBroadcastChannel channel = new PeerBroadcastChannel(service, 100, "http://peer:8080", threshold, openDurationMs);
        channels.add(channel);
        return channel;
    }

    private PeerBroadcastChannel createChannelWithService(ClusterEventService service, int queueCapacity,
                                                          int threshold, long openDurationMs) {
        PeerBroadcastChannel channel = new PeerBroadcastChannel(service, queueCapacity, "http://peer:8080",
            threshold, openDurationMs);
        channels.add(channel);
        return channel;
    }

    /** Polls until the executor's completed-task count reaches {@code target}, or the timeout expires. */
    private void awaitCompletedTasks(PeerBroadcastChannel channel, long target, long timeoutMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (channel.getExecutor().getCompletedTaskCount() < target) {
            Assert.assertTrue("Timed out waiting for " + target + " completed tasks",
                System.currentTimeMillis() < deadline);
            Thread.sleep(10);
        }
    }

    /** Submits {@code count} failing tasks and awaits their completion. */
    private void submitFailures(PeerBroadcastChannel channel, int count) throws InterruptedException {
        long startCount = channel.getExecutor().getCompletedTaskCount();
        for (int i = 0; i < count; i++) {
            channel.submit(() -> { throw new RuntimeException("simulated network error"); });
        }
        awaitCompletedTasks(channel, startCount + count, 5000);
    }

    /** Opens the circuit by submitting exactly {@code THRESHOLD} failing tasks. */
    private void openCircuit(PeerBroadcastChannel channel) throws InterruptedException {
        submitFailures(channel, THRESHOLD);
    }

    // -------------------------------------------------------------------------
    // Circuit breaker — CLOSED state
    // -------------------------------------------------------------------------

    @Test
    public void testCircuitOpensAfterThresholdConsecutiveFailures() throws InterruptedException {
        PeerBroadcastChannel channel = createChannel(THRESHOLD, OPEN_DURATION_MS);

        openCircuit(channel);

        AtomicBoolean taskRan = new AtomicBoolean(false);
        channel.submit(() -> taskRan.set(true));
        Thread.sleep(100);

        Assert.assertFalse("Task should be dropped when circuit is open", taskRan.get());
    }

    @Test
    public void testCircuitRemainsClosedBelowThreshold() throws InterruptedException {
        PeerBroadcastChannel channel = createChannel(THRESHOLD, OPEN_DURATION_MS);

        submitFailures(channel, THRESHOLD - 1);

        CountDownLatch latch = new CountDownLatch(1);
        channel.submit(latch::countDown);
        Assert.assertTrue("Task should execute when circuit is still below threshold",
            latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    public void testSuccessResetsFailureCounter() throws InterruptedException {
        PeerBroadcastChannel channel = createChannel(THRESHOLD, OPEN_DURATION_MS);

        // Accumulate THRESHOLD-1 failures
        submitFailures(channel, THRESHOLD - 1);

        // A success resets the consecutive-failure counter to 0
        long countAfterFailures = channel.getExecutor().getCompletedTaskCount();
        channel.submit(() -> {}); // no-op success
        awaitCompletedTasks(channel, countAfterFailures + 1, 5000);

        // THRESHOLD-1 more failures — counter is back at 0, so the circuit must remain closed
        submitFailures(channel, THRESHOLD - 1);

        CountDownLatch latch = new CountDownLatch(1);
        channel.submit(latch::countDown);
        Assert.assertTrue("Circuit must remain closed because the success reset the failure counter",
            latch.await(2, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // Circuit breaker — OPEN state
    // -------------------------------------------------------------------------

    @Test
    public void testOpenCircuitDropsTasksImmediately() throws InterruptedException {
        PeerBroadcastChannel channel = createChannel(THRESHOLD, OPEN_DURATION_MS);

        openCircuit(channel);

        long countBeforeDrop = channel.getExecutor().getCompletedTaskCount();
        AtomicBoolean taskRan = new AtomicBoolean(false);
        channel.submit(() -> taskRan.set(true));

        Thread.sleep(100);
        Assert.assertEquals("No new tasks should complete when circuit is open",
            countBeforeDrop, channel.getExecutor().getCompletedTaskCount());
        Assert.assertFalse("Task must not run when circuit is open", taskRan.get());
    }

    // -------------------------------------------------------------------------
    // Circuit breaker — HALF_OPEN state
    // -------------------------------------------------------------------------

    @Test
    public void testHalfOpenAllowsExactlyOneProbeAfterWindow() throws InterruptedException {
        PeerBroadcastChannel channel = createChannel(THRESHOLD, OPEN_DURATION_ZERO);

        openCircuit(channel);

        // openDurationMs=0: the window has already expired.
        // The first submit wins the CAS (OPEN→HALF_OPEN) and is dispatched as the probe.
        // The second submit must see HALF_OPEN and be dropped.
        //
        // The probe is held on a latch so that onSuccess() (which would reset state to CLOSED)
        // cannot run before the second submit is evaluated. Without this guard the executor can
        // complete the probe and transition state back to CLOSED between the two submit() calls,
        // causing the second submit to be accepted instead of dropped.
        CountDownLatch probeLatch = new CountDownLatch(1);
        AtomicInteger probeCount = new AtomicInteger(0);
        long countBeforeProbes = channel.getExecutor().getCompletedTaskCount();

        channel.submit(() -> {                                    // probe — CAS wins (OPEN→HALF_OPEN)
            try { probeLatch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            probeCount.incrementAndGet();
        });
        channel.submit(probeCount::incrementAndGet);             // must be dropped — state is HALF_OPEN

        probeLatch.countDown();                                  // release probe so it can complete
        awaitCompletedTasks(channel, countBeforeProbes + 1, 5000);
        Thread.sleep(50);                                        // confirm no late arrivals

        Assert.assertEquals("Exactly one probe must run; the concurrent submission must be dropped",
            1, probeCount.get());
    }

    @Test
    public void testSuccessfulProbeClosesCircuit() throws InterruptedException {
        PeerBroadcastChannel channel = createChannel(THRESHOLD, OPEN_DURATION_ZERO);

        openCircuit(channel);

        long countAfterOpen = channel.getExecutor().getCompletedTaskCount();
        channel.submit(() -> {}); // probe succeeds — circuit should close
        awaitCompletedTasks(channel, countAfterOpen + 1, 5000);

        CountDownLatch latch = new CountDownLatch(1);
        channel.submit(latch::countDown);
        Assert.assertTrue("Circuit should be closed after a successful probe",
            latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    public void testFailedProbeReopensCircuit() throws InterruptedException {
        PeerBroadcastChannel channel = createChannel(THRESHOLD, OPEN_DURATION_ZERO);

        openCircuit(channel);

        long countAfterOpen = channel.getExecutor().getCompletedTaskCount();
        channel.submit(() -> { throw new RuntimeException("probe network error"); }); // probe fails
        awaitCompletedTasks(channel, countAfterOpen + 1, 5000);

        boolean probeFailedLogged = logAppender.list.stream()
            .anyMatch(e -> e.getLevel() == Level.DEBUG
                && e.getFormattedMessage().contains("circuit remains open"));
        Assert.assertTrue("Expected DEBUG log when the probe fails and the circuit stays open",
            probeFailedLogged);
    }

    // -------------------------------------------------------------------------
    // Queue eviction
    // -------------------------------------------------------------------------

    @Test
    public void testQueueEvictsOldestWhenAtCapacity() throws InterruptedException {
        CountDownLatch blockLatch = new CountDownLatch(1);
        CountDownLatch workerStarted = new CountDownLatch(1);
        List<Integer> delivered = new CopyOnWriteArrayList<>();

        ClusterEventService service = mock(ClusterEventService.class);
        PeerBroadcastChannel channel = createChannelWithService(service, 1, THRESHOLD, OPEN_DURATION_MS);

        // task1 occupies the worker thread while it runs
        channel.submit(() -> {
            workerStarted.countDown();
            try {
                blockLatch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            delivered.add(1);
        });
        Assert.assertTrue("Worker thread did not start", workerStarted.await(5, TimeUnit.SECONDS));

        // task2 fills the single queue slot; task3 triggers the rejection handler
        channel.submit(() -> delivered.add(2)); // task2 — fills the slot
        channel.submit(() -> delivered.add(3)); // task3 — evicts task2, queues itself

        blockLatch.countDown();
        awaitCompletedTasks(channel, 2, 5000); // task1 + task3 complete; task2 was evicted

        Assert.assertTrue("task1 (in-flight) must be delivered", delivered.contains(1));
        Assert.assertFalse("task2 (oldest queued) must have been evicted", delivered.contains(2));
        Assert.assertTrue("task3 (newest) must be delivered", delivered.contains(3));

        boolean warnLogged = logAppender.list.stream()
            .anyMatch(e -> e.getLevel() == Level.WARN && e.getFormattedMessage().contains("evicted"));
        Assert.assertTrue("Expected WARN log when the oldest queued event is evicted", warnLogged);
    }

    // -------------------------------------------------------------------------
    // Multi-channel dispatch
    // -------------------------------------------------------------------------

    @Test
    public void testEachChannelDispatchesSubmittedTasksIndependently() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        PeerBroadcastChannel channel1 = createChannel(THRESHOLD, OPEN_DURATION_MS);
        PeerBroadcastChannel channel2 = createChannel(THRESHOLD, OPEN_DURATION_MS);

        channel1.submit(latch::countDown);
        channel2.submit(latch::countDown);

        Assert.assertTrue("Both channels must independently execute their submitted tasks",
            latch.await(5, TimeUnit.SECONDS));
    }

    // -------------------------------------------------------------------------
    // Lifecycle — shutdown
    // -------------------------------------------------------------------------

    @Test
    public void testShutdownPreventsSubsequentTaskExecution() {
        PeerBroadcastChannel channel = createChannel(THRESHOLD, OPEN_DURATION_MS);
        channel.shutdown();

        AtomicBoolean taskRan = new AtomicBoolean(false);
        try {
            channel.submit(() -> taskRan.set(true));
        } catch (Exception ignored) {
            // RejectedExecutionException is expected after shutdown
        }
        Assert.assertFalse("Task must not run after shutdown", taskRan.get());
    }

    @Test
    public void testShutdownLogsDiscardedTaskCount() throws InterruptedException {
        CountDownLatch blockLatch = new CountDownLatch(1);
        CountDownLatch workerStarted = new CountDownLatch(1);

        PeerBroadcastChannel channel = createChannel(THRESHOLD, OPEN_DURATION_MS);

        // Block the worker thread so queued tasks accumulate
        channel.submit(() -> {
            workerStarted.countDown();
            try {
                blockLatch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Assert.assertTrue("Worker thread did not start", workerStarted.await(5, TimeUnit.SECONDS));

        // Queue two tasks that will be pending when shutdown is called
        channel.submit(() -> {});
        channel.submit(() -> {});

        channel.shutdown();
        blockLatch.countDown(); // harmless — worker is already interrupted

        boolean discardLogged = logAppender.list.stream()
            .anyMatch(e -> e.getLevel() == Level.DEBUG
                && e.getFormattedMessage().contains("discarded"));
        Assert.assertTrue("Expected DEBUG log listing the number of discarded tasks on shutdown",
            discardLogged);
    }

    // -------------------------------------------------------------------------
    // Logging
    // -------------------------------------------------------------------------

    @Test
    public void testWarnLoggedWhenCircuitOpens() throws InterruptedException {
        PeerBroadcastChannel channel = createChannel(THRESHOLD, OPEN_DURATION_MS);

        openCircuit(channel);

        boolean warnLogged = logAppender.list.stream()
            .anyMatch(e -> e.getLevel() == Level.WARN
                && e.getFormattedMessage().contains("Circuit opened"));
        Assert.assertTrue("Expected WARN log when the circuit opens after threshold failures", warnLogged);
    }

    @Test
    public void testInfoLoggedWhenCircuitCloses() throws InterruptedException {
        PeerBroadcastChannel channel = createChannel(THRESHOLD, OPEN_DURATION_ZERO);

        openCircuit(channel);

        long countAfterOpen = channel.getExecutor().getCompletedTaskCount();
        channel.submit(() -> {}); // successful probe closes the circuit
        awaitCompletedTasks(channel, countAfterOpen + 1, 5000);

        boolean infoLogged = logAppender.list.stream()
            .anyMatch(e -> e.getLevel() == Level.INFO
                && e.getFormattedMessage().contains("Circuit closed"));
        Assert.assertTrue("Expected INFO log when the circuit closes after a successful probe", infoLogged);
    }
}
