package org.ikasan.job.orchestration.context.util;

import java.util.concurrent.ThreadFactory;

public class JobThreadFactory implements ThreadFactory {

    private static long counter = 0;
    private final String prefix;

    public JobThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, prefix + "-" + counter++);
    }
}
