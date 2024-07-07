package org.ikasan.orchestration.service.context.util;

import java.util.concurrent.ThreadFactory;

public class JobServiceThreadFactory implements ThreadFactory {

    private static long counter = 0;
    private final String prefix;

    /**
     * A custom ThreadFactory implementation for creating threads in the JobService.
     */
    public JobServiceThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, prefix + "-" + counter++);
    }
}

