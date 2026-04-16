package org.ikasan.job.orchestration.broadcast;

import org.junit.Assert;
import org.junit.Test;

public class BroadcasterThreadFactoryTest {

    @Test
    public void testNewThread_createsThreadWithCorrectPrefix() {
        String prefix = "TestPrefix";
        BroadcasterThreadFactory factory = new BroadcasterThreadFactory(prefix);
        Runnable runnable = () -> {};

        Thread thread = factory.newThread(runnable);

        Assert.assertNotNull(thread);
        Assert.assertTrue(thread.getName().startsWith(prefix + "-"));
    }

    @Test
    public void testNewThread_incrementsCounter() {
        String prefix = "TestPrefix";
        BroadcasterThreadFactory factory = new BroadcasterThreadFactory(prefix);
        Runnable runnable = () -> {};

        Thread thread1 = factory.newThread(runnable);
        Thread thread2 = factory.newThread(runnable);

        Assert.assertNotNull(thread1);
        Assert.assertNotNull(thread2);
        Assert.assertNotEquals(thread1.getName(), thread2.getName());
    }

    @Test
    public void testNewThread_withNullRunnable() {
        String prefix = "TestPrefix";
        BroadcasterThreadFactory factory = new BroadcasterThreadFactory(prefix);

        Thread thread = factory.newThread(null);

        Assert.assertNotNull(thread);
        Assert.assertTrue(thread.getName().startsWith(prefix + "-"));
    }

    @Test
    public void testNewThread_withEmptyPrefix() {
        String prefix = "";
        BroadcasterThreadFactory factory = new BroadcasterThreadFactory(prefix);
        Runnable runnable = () -> {};

        Thread thread = factory.newThread(runnable);

        Assert.assertNotNull(thread);
        Assert.assertTrue(thread.getName().startsWith("-"));
    }
}
