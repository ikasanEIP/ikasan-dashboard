package org.ikasan.job.orchestration.broadcast;

import java.util.concurrent.ThreadFactory;

public class BroadcasterThreadFactory implements ThreadFactory {
    private static long counter = 0;
    private final String prefix;

    public BroadcasterThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, prefix + "-" + counter++);
    }

}
