package org.ikasan.notification.factory;

import java.util.concurrent.ThreadFactory;

public class NotificationThreadFactory implements ThreadFactory {

    private static long counter = 0;
    private final String prefix;

    public NotificationThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, prefix + "-" + counter++);
    }
}
