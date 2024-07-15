package org.ikasan.dashboard.ui.util;

import java.util.concurrent.ThreadFactory;

public class VaadinThreadFactory implements ThreadFactory {
    private static long counter = 0;
    private final String prefix;

    public VaadinThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, prefix + "-" + counter++);
    }

}
