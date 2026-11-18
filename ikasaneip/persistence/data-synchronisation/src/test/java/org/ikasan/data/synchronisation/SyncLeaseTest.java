package org.ikasan.data.synchronisation;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SyncLeaseTest {

    private SyncStateStore store;

    @Before
    public void setUp() throws Exception {
        TestEnvironment env = TestEnvironment.get();
        env.reset();
        store = new SyncStateStore(env.mongoTemplate());
    }

    @Test
    public void only_one_instance_holds_the_lease() {
        SyncLease first = new SyncLease(store, "first", 60_000);
        SyncLease second = new SyncLease(store, "second", 60_000);

        assertTrue(first.acquireOrRenew());
        assertFalse(second.acquireOrRenew());
        assertTrue("the holder can renew", first.acquireOrRenew());
        assertFalse(second.acquireOrRenew());
    }

    @Test
    public void another_instance_takes_over_once_the_lease_expires() throws Exception {
        SyncLease first = new SyncLease(store, "first", 300);
        SyncLease second = new SyncLease(store, "second", 300);

        assertTrue(first.acquireOrRenew());
        assertFalse(second.acquireOrRenew());
        Thread.sleep(500);
        assertTrue(second.acquireOrRenew());
        assertFalse("the old holder has lost it", first.acquireOrRenew());
    }

    @Test
    public void a_released_lease_can_be_taken_straight_away() {
        SyncLease first = new SyncLease(store, "first", 60_000);
        SyncLease second = new SyncLease(store, "second", 60_000);

        assertTrue(first.acquireOrRenew());
        first.release();
        assertTrue(second.acquireOrRenew());
    }
}
