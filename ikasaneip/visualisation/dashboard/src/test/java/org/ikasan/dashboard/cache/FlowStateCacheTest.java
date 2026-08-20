package org.ikasan.dashboard.cache;

import org.awaitility.Awaitility;
import org.ikasan.dashboard.broadcast.FlowState;
import org.ikasan.dashboard.broadcast.State;
import org.junit.*;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.*;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

/**
 * Comprehensive unit tests for FlowStateCache throttling functionality.
 *
 * These tests verify that rapid RECOVERING <-> STOPPED <-> RUNNING oscillations are properly throttled
 * to prevent memory issues from excessive broadcasts, while all other state transitions
 * are broadcast immediately.
 */
public class FlowStateCacheTest {

    private FlowStateCache cache;
    private TestCacheStateBroadcastListener listener;

    @Before
    public void setup() throws Exception {
        // Configure Awaitility for more lenient timing on slow CI servers
        Awaitility.setDefaultPollInterval(Duration.ofMillis(10));
        Awaitility.setDefaultTimeout(Duration.ofSeconds(5));

        // Reset singleton instance before each test
        resetSingletonInstance();

        cache = FlowStateCache.instance();

        // Register test listener to capture broadcasts
        listener = new TestCacheStateBroadcastListener();
        CacheStateBroadcaster.register(listener);

        // Set a throttle interval for tests (500ms - longer to be more reliable)
        cache.setThrottleIntervalMs(500);
    }

    @After
    public void teardown() {
        if (listener != null) {
            CacheStateBroadcaster.unregister(listener);
        }
        if (cache != null) {
            cache.teardown();
        }
    }

    /**
     * Reset the FlowStateCache singleton instance using reflection.
     * This allows each test to start with a fresh instance.
     */
    private void resetSingletonInstance() throws Exception {
        Field instance = FlowStateCache.class.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    /**
     * Test that a single state change is broadcast immediately.
     */
    @Test
    public void testSingleStateChangeIsBroadcastImmediately() {
        FlowState state = new FlowState("module1", "flow1", State.RUNNING_STATE);

        cache.put(state);

        // Wait for broadcast using Awaitility
        await().untilAsserted(() -> {
            assertEquals("Should broadcast once", 1, listener.getBroadcastCount());
            assertEquals("Should broadcast the correct state", State.RUNNING_STATE,
                listener.getLastBroadcast().getState());
        });
    }

    /**
     * Test that the same state is not broadcast multiple times (no change).
     */
    @Test
    public void testSameStateIsNotRebroadcast() {
        FlowState state = new FlowState("module1", "flow1", State.RUNNING_STATE);

        cache.put(state);
        cache.put(state); // Same state again
        cache.put(state); // And again

        // Wait for any async broadcasts
        await().untilAsserted(() ->
            assertEquals("Should only broadcast once for unchanged state", 1, listener.getBroadcastCount())
        );
    }

    /**
     * Test that rapid RECOVERING <-> STOPPED oscillations are throttled.
     * This is the key test for the memory issue fix.
     */
    @Test
    public void testRecoverStopOscillationsAreThrottled() throws InterruptedException {
        String moduleName = "module1";
        String flowName = "flow1";

        // Simulate rapid RECOVERING <-> STOPPED oscillations
        cache.put(new FlowState(moduleName, flowName, State.RECOVERING_STATE));
        Thread.sleep(50);
        cache.put(new FlowState(moduleName, flowName, State.STOPPED_STATE));
        Thread.sleep(50);
        cache.put(new FlowState(moduleName, flowName, State.RECOVERING_STATE));
        Thread.sleep(50);
        cache.put(new FlowState(moduleName, flowName, State.STOPPED_STATE));

        // Wait for immediate broadcasts (first 2)
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                assertThat("Should have 2 immediate broadcasts (before throttling starts)",
                    listener.getBroadcastCount(), is(2));
                assertEquals("First broadcast should be RECOVERING", State.RECOVERING_STATE,
                    listener.getBroadcasts().get(0).getState());
                assertEquals("Second broadcast should be STOPPED (starts oscillation detection)", State.STOPPED_STATE,
                    listener.getBroadcasts().get(1).getState());
            });

        // Wait for throttle interval to pass and delayed broadcast to execute
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                assertThat("Should have 3 broadcasts total (2 immediate + 1 delayed)",
                    listener.getBroadcastCount(), is(3));
                assertEquals("Final broadcast should be STOPPED (latest state)", State.STOPPED_STATE,
                    listener.getLastBroadcast().getState());
            });
    }

    /**
     * Test that state changes for different flows don't interfere with each other.
     */
    @Test
    public void testDifferentFlowsAreIndependentlyThrottled() throws InterruptedException {
        // Change state for flow1 with RECOVERING <-> STOPPED oscillation (throttled)
        cache.put(new FlowState("module1", "flow1", State.RECOVERING_STATE));
        Thread.sleep(50);
        cache.put(new FlowState("module1", "flow1", State.STOPPED_STATE));
        Thread.sleep(50);
        cache.put(new FlowState("module1", "flow1", State.RECOVERING_STATE)); // Now throttled

        // Change state for flow2 with RECOVERING <-> STOPPED oscillation (throttled)
        cache.put(new FlowState("module1", "flow2", State.RECOVERING_STATE));
        Thread.sleep(50);
        cache.put(new FlowState("module1", "flow2", State.STOPPED_STATE));
        Thread.sleep(50);
        cache.put(new FlowState("module1", "flow2", State.RECOVERING_STATE)); // Now throttled

        // Wait for immediate broadcasts (4 total)
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() ->
                assertThat("Should have 4 immediate broadcasts (2 per flow before throttling)",
                    listener.getBroadcastCount(), is(4))
            );

        // Wait for delayed broadcasts (2 more)
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() ->
                assertThat("Should have 6 broadcasts total (4 immediate + 2 delayed)",
                    listener.getBroadcastCount(), is(6))
            );
    }

    /**
     * Test that non-oscillating state changes are broadcast immediately without throttling.
     */
    @Test
    public void testNonOscillatingStatesAreBroadcastImmediately() throws InterruptedException {
        String moduleName = "module1";
        String flowName = "flow1";

        // Rapid state changes that are NOT RECOVERING <-> STOPPED oscillations
        cache.put(new FlowState(moduleName, flowName, State.RUNNING_STATE));
        Thread.sleep(50);
        cache.put(new FlowState(moduleName, flowName, State.PAUSED_STATE));
        Thread.sleep(50);
        cache.put(new FlowState(moduleName, flowName, State.RUNNING_STATE));
        Thread.sleep(50);
        cache.put(new FlowState(moduleName, flowName, State.STOPPED_IN_ERROR_STATE));

        // All should be broadcast immediately because they are not oscillations
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() ->
                assertThat("All non-oscillating state changes should be broadcast immediately",
                    listener.getBroadcastCount(), is(4))
            );
    }

    /**
     * Test that RECOVERING <-> STOPPED oscillations are throttled but then switching
     * to a different state broadcasts immediately.
     */
    @Test
    public void testOscillationFollowedByDifferentStateIsBroadcast() throws InterruptedException {
        String moduleName = "module1";
        String flowName = "flow1";

        // Start with RECOVERING <-> STOPPED oscillations (throttled)
        cache.put(new FlowState(moduleName, flowName, State.RECOVERING_STATE));
        Thread.sleep(20);
        cache.put(new FlowState(moduleName, flowName, State.STOPPED_STATE));
        Thread.sleep(20);
        cache.put(new FlowState(moduleName, flowName, State.RECOVERING_STATE));
        Thread.sleep(20);

        // Now switch to a different state (should broadcast immediately)
        cache.put(new FlowState(moduleName, flowName, State.RUNNING_STATE));

        // Wait for broadcasts using Awaitility
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                assertThat("Should have 3 broadcasts", listener.getBroadcastCount(), is(3));
                assertEquals("Last broadcast should be RUNNING", State.RUNNING_STATE,
                    listener.getLastBroadcast().getState());
            });
    }

    /**
     * Test that STOPPED -> RECOVERING -> STOPPED (starting with STOPPED) is also throttled.
     */
    @Test
    public void testStoppedToRecoveringOscillationIsThrottled() throws InterruptedException {
        String moduleName = "module1";
        String flowName = "flow1";

        // Start with STOPPED instead of RECOVERING
        cache.put(new FlowState(moduleName, flowName, State.STOPPED_STATE));
        Thread.sleep(20);
        cache.put(new FlowState(moduleName, flowName, State.RECOVERING_STATE));
        Thread.sleep(20);
        cache.put(new FlowState(moduleName, flowName, State.STOPPED_STATE));
        Thread.sleep(20);
        cache.put(new FlowState(moduleName, flowName, State.RECOVERING_STATE));

        // Wait for immediate broadcasts
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() ->
                assertThat("Should have 2 immediate broadcasts (before throttling)",
                    listener.getBroadcastCount(), is(2))
            );

        // Wait for delayed broadcast
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                assertThat("Should have 3 broadcasts total (2 immediate + 1 delayed)",
                    listener.getBroadcastCount(), is(3));
                assertEquals("Final broadcast should be RECOVERING", State.RECOVERING_STATE,
                    listener.getLastBroadcast().getState());
            });
    }

    /**
     * Test that pending broadcasts are cancelled when a new oscillating state change occurs.
     */
    @Test
    public void testPendingBroadcastIsCancelledOnNewStateChange() throws Exception {
        String moduleName = "module1";
        String flowName = "flow1";

        // First two states - broadcast immediately (RECOVERING, then STOPPED starts oscillation)
        cache.put(new FlowState(moduleName, flowName, State.RECOVERING_STATE));
        Thread.sleep(20);
        cache.put(new FlowState(moduleName, flowName, State.STOPPED_STATE));
        Thread.sleep(20);

        // Third state change - RECOVERING (now throttled, scheduled for later)
        cache.put(new FlowState(moduleName, flowName, State.RECOVERING_STATE));
        Thread.sleep(10); // Short wait - broadcast should still be pending

        // Get pending broadcasts map using reflection
        Field pendingBroadcastsField = FlowStateCache.class.getDeclaredField("pendingBroadcasts");
        pendingBroadcastsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, ScheduledFuture<?>> pendingBroadcasts =
            (ConcurrentHashMap<String, ScheduledFuture<?>>) pendingBroadcastsField.get(cache);

        String key = moduleName + flowName;
        assertTrue("Should have a pending broadcast", pendingBroadcasts.containsKey(key));
        ScheduledFuture<?> firstScheduled = pendingBroadcasts.get(key);

        // Fourth state change - STOPPED again (oscillation, should cancel previous)
        cache.put(new FlowState(moduleName, flowName, State.STOPPED_STATE));
        Thread.sleep(10);

        assertTrue("First scheduled broadcast should be cancelled", firstScheduled.isCancelled());

        ScheduledFuture<?> secondScheduled = pendingBroadcasts.get(key);
        assertNotNull("Should have a new pending broadcast", secondScheduled);
        assertNotSame("Should be a different scheduled future", firstScheduled, secondScheduled);

        // Wait for the final delayed broadcast using Awaitility
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() ->
                assertEquals("Should broadcast STOPPED as final state", State.STOPPED_STATE,
                    listener.getLastBroadcast().getState())
            );
    }

    /**
     * Test custom throttle interval configuration for oscillations.
     */
    @Test
    public void testCustomThrottleInterval() throws InterruptedException {
        // Set custom throttle interval of 200ms
        cache.setThrottleIntervalMs(200);

        // Use RECOVERING <-> STOPPED oscillation to trigger throttling
        cache.put(new FlowState("module1", "flow1", State.RECOVERING_STATE));
        Thread.sleep(50);

        cache.put(new FlowState("module1", "flow1", State.STOPPED_STATE));
        Thread.sleep(50);

        // Third state - now throttling kicks in
        cache.put(new FlowState("module1", "flow1", State.RECOVERING_STATE));

        // Wait for immediate broadcasts
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() ->
                assertThat("Should have 2 immediate broadcasts (before throttling)",
                    listener.getBroadcastCount(), is(2))
            );

        // Wait for throttle interval to pass and delayed broadcast
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() ->
                assertThat("Should have 3 broadcasts after throttle interval passes (2 immediate + 1 delayed)",
                    listener.getBroadcastCount(), is(3))
            );
    }

    /**
     * Test that invalid throttle intervals are rejected.
     */
    @Test
    public void testInvalidThrottleIntervalIsRejected() throws Exception {
        // Get initial throttle interval
        Field throttleField = FlowStateCache.class.getDeclaredField("throttleIntervalMs");
        throttleField.setAccessible(true);
        long initialThrottle = (Long) throttleField.get(cache);

        // Try to set invalid throttle interval
        cache.setThrottleIntervalMs(0);

        long currentThrottle = (Long) throttleField.get(cache);
        assertEquals("Throttle interval should not change for invalid value",
            initialThrottle, currentThrottle);

        // Try negative value
        cache.setThrottleIntervalMs(-100);

        currentThrottle = (Long) throttleField.get(cache);
        assertEquals("Throttle interval should not change for negative value",
            initialThrottle, currentThrottle);
    }

    /**
     * Test that oscillation detection window works correctly - states outside the window are not throttled.
     */
    @Test
    public void testOscillationWindowPreventsThrottlingOfDistantStates() throws InterruptedException {
        String moduleName = "module1";
        String flowName = "flow1";

        // Set a short oscillation window (200ms)
        cache.setOscillationWindowMs(200);

        // First transition: RECOVERING -> STOPPED (within window, starts oscillation detection)
        cache.put(new FlowState(moduleName, flowName, State.RECOVERING_STATE));
        Thread.sleep(50);
        cache.put(new FlowState(moduleName, flowName, State.STOPPED_STATE));
        Thread.sleep(50);

        // Wait longer than the oscillation window
        Thread.sleep(250); // Total 350ms > 200ms window

        // Third state change - RECOVERING (outside window, should NOT be throttled)
        cache.put(new FlowState(moduleName, flowName, State.RECOVERING_STATE));

        // Should have 3 broadcasts (all immediate, no throttling because outside window)
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() ->
                assertThat("Should have 3 immediate broadcasts - states outside oscillation window",
                    listener.getBroadcastCount(), is(3))
            );
    }

    /**
     * Test that oscillation window can be configured.
     */
    @Test
    public void testOscillationWindowConfiguration() throws Exception {
        // Get oscillationWindowMs field using reflection
        Field oscillationWindowField = FlowStateCache.class.getDeclaredField("oscillationWindowMs");
        oscillationWindowField.setAccessible(true);
        long initialWindow = (Long) oscillationWindowField.get(cache);

        // Set new oscillation window
        cache.setOscillationWindowMs(3000);
        long newWindow = (Long) oscillationWindowField.get(cache);
        assertEquals("Oscillation window should be updated", 3000, newWindow);

        // Try invalid values
        cache.setOscillationWindowMs(0);
        long currentWindow = (Long) oscillationWindowField.get(cache);
        assertEquals("Oscillation window should not change for invalid value", 3000, currentWindow);

        cache.setOscillationWindowMs(-100);
        currentWindow = (Long) oscillationWindowField.get(cache);
        assertEquals("Oscillation window should not change for negative value", 3000, currentWindow);
    }

    /**
     * Test that extremely rapid RECOVERING <-> STOPPED oscillations (stress test) are properly throttled.
     */
    @Test
    public void testExtremelyRapidOscillations() throws InterruptedException {
        String moduleName = "module1";
        String flowName = "flow1";

        // Simulate 20 rapid RECOVERING <-> STOPPED oscillations within 50ms
        for (int i = 0; i < 10; i++) {
            cache.put(new FlowState(moduleName, flowName, State.RECOVERING_STATE));
            cache.put(new FlowState(moduleName, flowName, State.STOPPED_STATE));
            Thread.sleep(2); // Very short delay between changes
        }

        // Wait for immediate broadcasts
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() ->
                assertThat("Should have 2 immediate broadcasts before throttling kicks in",
                    listener.getBroadcastCount(), is(2))
            );

        // Wait for delayed broadcast
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                assertThat("Should have exactly 3 broadcasts despite 20 oscillations (2 immediate + 1 delayed)",
                    listener.getBroadcastCount(), is(3));
                assertEquals("Final state should be STOPPED", State.STOPPED_STATE,
                    listener.getLastBroadcast().getState());
            });
    }

    /**
     * Test that cache still contains the latest state even when broadcast is throttled.
     */
    @Test
    public void testCacheContainsLatestStateDuringThrottling() throws InterruptedException {
        String moduleName = "module1";
        String flowName = "flow1";

        // Use RECOVERING <-> STOPPED oscillation to trigger throttling
        cache.put(new FlowState(moduleName, flowName, State.RECOVERING_STATE));
        Thread.sleep(20);

        cache.put(new FlowState(moduleName, flowName, State.STOPPED_STATE));
        Thread.sleep(20);

        cache.put(new FlowState(moduleName, flowName, State.RECOVERING_STATE));

        // Cache should immediately contain the latest state
        assertTrue("Cache should contain the flow", cache.contains(moduleName, flowName));

        // Note: We can't directly get the state from cache without accessing private fields,
        // but we verify the broadcast behavior shows the latest state
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() ->
                assertEquals("Last broadcast should have the final state", State.RECOVERING_STATE,
                    listener.getLastBroadcast().getState())
            );
    }

    /**
     * Test teardown properly cleans up resources.
     */
    @Test
    public void testTeardownCleansUpResources() throws Exception {
        // Use RECOVERING <-> STOPPED oscillation to create tracking state
        cache.put(new FlowState("module1", "flow1", State.RECOVERING_STATE));
        cache.put(new FlowState("module1", "flow1", State.STOPPED_STATE));

        // Wait for broadcasts to be processed
        await().atMost(Duration.ofSeconds(1))
            .untilAsserted(() ->
                assertThat("Should have broadcasts", listener.getBroadcastCount(), greaterThan(0))
            );

        // Get references to internal state
        Field lastStateField = FlowStateCache.class.getDeclaredField("lastState");
        lastStateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, FlowState> lastState =
            (ConcurrentHashMap<String, FlowState>) lastStateField.get(cache);

        Field lastBroadcastTimeField = FlowStateCache.class.getDeclaredField("lastBroadcastTime");
        lastBroadcastTimeField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Long> lastBroadcastTime =
            (ConcurrentHashMap<String, Long>) lastBroadcastTimeField.get(cache);

        Field lastStateTimeField = FlowStateCache.class.getDeclaredField("lastStateTime");
        lastStateTimeField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Long> lastStateTime =
            (ConcurrentHashMap<String, Long>) lastStateTimeField.get(cache);

        Field inOscillationField = FlowStateCache.class.getDeclaredField("inOscillation");
        inOscillationField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Boolean> inOscillation =
            (ConcurrentHashMap<String, Boolean>) inOscillationField.get(cache);

        Field pendingBroadcastsField = FlowStateCache.class.getDeclaredField("pendingBroadcasts");
        pendingBroadcastsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, ScheduledFuture<?>> pendingBroadcasts =
            (ConcurrentHashMap<String, ScheduledFuture<?>>) pendingBroadcastsField.get(cache);

        assertFalse("Should have lastState entries before teardown", lastState.isEmpty());

        cache.teardown();

        assertTrue("lastState should be cleared after teardown", lastState.isEmpty());
        assertTrue("lastStateTime should be cleared after teardown", lastStateTime.isEmpty());
        assertTrue("inOscillation should be cleared after teardown", inOscillation.isEmpty());
        assertTrue("lastBroadcastTime should be cleared after teardown", lastBroadcastTime.isEmpty());
        assertTrue("pendingBroadcasts should be cleared after teardown", pendingBroadcasts.isEmpty());
    }

    /**
     * Test that rapid RUNNING <-> STOPPED oscillations are throttled.
     */
    @Test
    public void testRunningStoppedOscillationsAreThrottled() throws InterruptedException {
        String moduleName = "module1";
        String flowName = "flow1";

        // Simulate rapid RUNNING <-> STOPPED oscillations
        cache.put(new FlowState(moduleName, flowName, State.RUNNING_STATE));
        Thread.sleep(50);
        cache.put(new FlowState(moduleName, flowName, State.STOPPED_STATE));
        Thread.sleep(50);
        cache.put(new FlowState(moduleName, flowName, State.RUNNING_STATE));
        Thread.sleep(50);
        cache.put(new FlowState(moduleName, flowName, State.STOPPED_STATE));

        // Wait for immediate broadcasts (first 2)
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                assertThat("Should have 2 immediate broadcasts (before throttling starts)",
                    listener.getBroadcastCount(), is(2));
                assertEquals("First broadcast should be RUNNING", State.RUNNING_STATE,
                    listener.getBroadcasts().get(0).getState());
                assertEquals("Second broadcast should be STOPPED (starts oscillation detection)", State.STOPPED_STATE,
                    listener.getBroadcasts().get(1).getState());
            });

        // Wait for throttle interval to pass and delayed broadcast to execute
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                assertThat("Should have 3 broadcasts total (2 immediate + 1 delayed)",
                    listener.getBroadcastCount(), is(3));
                assertEquals("Final broadcast should be STOPPED (latest state)", State.STOPPED_STATE,
                    listener.getLastBroadcast().getState());
            });
    }

    /**
     * Test that rapid RUNNING <-> RECOVERING oscillations are throttled.
     */
    @Test
    public void testRunningRecoveringOscillationsAreThrottled() throws InterruptedException {
        String moduleName = "module1";
        String flowName = "flow1";

        // Simulate rapid RUNNING <-> RECOVERING oscillations
        cache.put(new FlowState(moduleName, flowName, State.RUNNING_STATE));
        Thread.sleep(50);
        cache.put(new FlowState(moduleName, flowName, State.RECOVERING_STATE));
        Thread.sleep(50);
        cache.put(new FlowState(moduleName, flowName, State.RUNNING_STATE));
        Thread.sleep(50);
        cache.put(new FlowState(moduleName, flowName, State.RECOVERING_STATE));

        // Wait for immediate broadcasts (first 2)
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                assertThat("Should have 2 immediate broadcasts (before throttling starts)",
                    listener.getBroadcastCount(), is(2));
                assertEquals("First broadcast should be RUNNING", State.RUNNING_STATE,
                    listener.getBroadcasts().get(0).getState());
                assertEquals("Second broadcast should be RECOVERING (starts oscillation detection)", State.RECOVERING_STATE,
                    listener.getBroadcasts().get(1).getState());
            });

        // Wait for throttle interval to pass and delayed broadcast to execute
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                assertThat("Should have 3 broadcasts total (2 immediate + 1 delayed)",
                    listener.getBroadcastCount(), is(3));
                assertEquals("Final broadcast should be RECOVERING (latest state)", State.RECOVERING_STATE,
                    listener.getLastBroadcast().getState());
            });
    }

    /**
     * Test that rapid three-way oscillations (RUNNING <-> RECOVERING <-> STOPPED) are throttled.
     */
    @Test
    public void testThreeWayOscillationsAreThrottled() throws InterruptedException {
        String moduleName = "module1";
        String flowName = "flow1";

        // Simulate rapid three-way oscillations
        cache.put(new FlowState(moduleName, flowName, State.RUNNING_STATE));
        Thread.sleep(50);
        cache.put(new FlowState(moduleName, flowName, State.RECOVERING_STATE));
        Thread.sleep(50);
        cache.put(new FlowState(moduleName, flowName, State.STOPPED_STATE));
        Thread.sleep(50);
        cache.put(new FlowState(moduleName, flowName, State.RUNNING_STATE));
        Thread.sleep(50);
        cache.put(new FlowState(moduleName, flowName, State.RECOVERING_STATE));

        // Wait for immediate broadcasts (first 2)
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                assertThat("Should have 2 immediate broadcasts (before throttling starts)",
                    listener.getBroadcastCount(), is(2));
            });

        // Wait for throttle interval to pass and delayed broadcast to execute
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                assertThat("Should have 3 broadcasts total (2 immediate + 1 delayed)",
                    listener.getBroadcastCount(), is(3));
                assertEquals("Final broadcast should be RECOVERING (latest state)", State.RECOVERING_STATE,
                    listener.getLastBroadcast().getState());
            });
    }

    /**
     * Test that RUNNING state transitions to non-oscillating states are broadcast immediately.
     */
    @Test
    public void testRunningToNonOscillatingStateIsBroadcastImmediately() throws InterruptedException {
        String moduleName = "module1";
        String flowName = "flow1";

        // Start with RUNNING <-> STOPPED oscillations (throttled)
        cache.put(new FlowState(moduleName, flowName, State.RUNNING_STATE));
        Thread.sleep(20);
        cache.put(new FlowState(moduleName, flowName, State.STOPPED_STATE));
        Thread.sleep(20);
        cache.put(new FlowState(moduleName, flowName, State.RUNNING_STATE));
        Thread.sleep(20);

        // Now switch to a non-oscillating state (should broadcast immediately)
        cache.put(new FlowState(moduleName, flowName, State.PAUSED_STATE));

        // Wait for broadcasts using Awaitility
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                assertThat("Should have 3 broadcasts", listener.getBroadcastCount(), is(3));
                assertEquals("Last broadcast should be PAUSED", State.PAUSED_STATE,
                    listener.getLastBroadcast().getState());
            });
    }

    /**
     * Test that oscillations involving RUNNING state clear when transition to non-oscillating state.
     */
    @Test
    public void testRunningOscillationClearsOnNonOscillatingState() throws InterruptedException {
        String moduleName = "module1";
        String flowName = "flow1";

        // Create RUNNING <-> RECOVERING oscillation
        cache.put(new FlowState(moduleName, flowName, State.RUNNING_STATE));
        Thread.sleep(20);
        cache.put(new FlowState(moduleName, flowName, State.RECOVERING_STATE));
        Thread.sleep(20);
        cache.put(new FlowState(moduleName, flowName, State.RUNNING_STATE));

        // Transition to PAUSED (non-oscillating)
        Thread.sleep(20);
        cache.put(new FlowState(moduleName, flowName, State.PAUSED_STATE));

        // Wait for broadcasts
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                assertThat("Should have 3 immediate broadcasts", listener.getBroadcastCount(), is(3));
            });

        // Now return to RECOVERING - should NOT be throttled because oscillation was cleared
        Thread.sleep(20);
        cache.put(new FlowState(moduleName, flowName, State.RECOVERING_STATE));

        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                assertThat("Should have 4 broadcasts (oscillation was cleared)",
                    listener.getBroadcastCount(), is(4));
                assertEquals("Last broadcast should be RECOVERING", State.RECOVERING_STATE,
                    listener.getLastBroadcast().getState());
            });
    }

    /**
     * Test extremely rapid RUNNING <-> STOPPED oscillations (stress test).
     */
    @Test
    public void testExtremelyRapidRunningStoppedOscillations() throws InterruptedException {
        String moduleName = "module1";
        String flowName = "flow1";

        // Simulate 20 rapid RUNNING <-> STOPPED oscillations within 50ms
        for (int i = 0; i < 10; i++) {
            cache.put(new FlowState(moduleName, flowName, State.RUNNING_STATE));
            cache.put(new FlowState(moduleName, flowName, State.STOPPED_STATE));
            Thread.sleep(2); // Very short delay between changes
        }

        // Wait for immediate broadcasts
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() ->
                assertThat("Should have 2 immediate broadcasts before throttling kicks in",
                    listener.getBroadcastCount(), is(2))
            );

        // Wait for delayed broadcast
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() -> {
                assertThat("Should have exactly 3 broadcasts despite 20 oscillations (2 immediate + 1 delayed)",
                    listener.getBroadcastCount(), is(3));
                assertEquals("Final state should be STOPPED", State.STOPPED_STATE,
                    listener.getLastBroadcast().getState());
            });
    }

    /**
     * Test concurrent updates from multiple threads with oscillating states.
     */
    @Test
    public void testConcurrentOscillatingStateUpdates() throws InterruptedException {
        final String moduleName = "module1";
        final String flowName = "flow1";
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger errorCount = new AtomicInteger(0);

        // Create multiple threads that all update the same flow with oscillating states
        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            new Thread(() -> {
                try {
                    // Use RECOVERING <-> STOPPED oscillation
                    State state = (threadNum % 2 == 0) ? State.RECOVERING_STATE : State.STOPPED_STATE;
                    cache.put(new FlowState(moduleName, flowName, state));
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        assertTrue("All threads should complete", latch.await(5, TimeUnit.SECONDS));
        assertEquals("Should have no errors from concurrent updates", 0, errorCount.get());

        // Wait for broadcasts to complete and verify throttling using Awaitility
        await().atMost(Duration.ofSeconds(2))
            .untilAsserted(() ->
                assertThat("Should have throttled concurrent oscillating broadcasts",
                    listener.getBroadcastCount(), lessThan(threadCount))
            );
    }

    /**
     * Helper class to capture broadcast events for testing.
     */
    private static class TestCacheStateBroadcastListener implements CacheStateBroadcastListener {
        private final List<FlowState> broadcasts = new ArrayList<>();
        private final Object lock = new Object();

        @Override
        public void receiveCacheStateBroadcast(FlowState flowState) {
            synchronized (lock) {
                broadcasts.add(flowState);
            }
        }

        public int getBroadcastCount() {
            synchronized (lock) {
                return broadcasts.size();
            }
        }

        public FlowState getLastBroadcast() {
            synchronized (lock) {
                return broadcasts.isEmpty() ? null : broadcasts.get(broadcasts.size() - 1);
            }
        }

        public List<FlowState> getBroadcasts() {
            synchronized (lock) {
                return new ArrayList<>(broadcasts);
            }
        }
    }
}
