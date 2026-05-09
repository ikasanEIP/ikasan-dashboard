package org.ikasan.job.orchestration.broadcast;

import java.util.concurrent.ThreadFactory;

/**
 * Thread factory for creating named threads used by broadcaster executors.
 * Each thread created is assigned a name with the format: prefix-counter.
 *
 * @author Ikasan Development Team
 */
public class BroadcasterThreadFactory implements ThreadFactory {
    private static long counter = 0;
    private final String prefix;

    /**
     * Constructor
     *
     * @param prefix the prefix to use for thread names
     */
    public BroadcasterThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Creates a new thread with a name derived from the prefix and an incrementing counter.
     *
     * @param r the runnable to be executed by the new thread
     * @return a new thread instance
     */
    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, prefix + "-" + counter++);
    }

}
