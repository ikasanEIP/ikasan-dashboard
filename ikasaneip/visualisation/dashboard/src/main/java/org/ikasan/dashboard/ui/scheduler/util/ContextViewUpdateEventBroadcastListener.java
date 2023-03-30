package org.ikasan.dashboard.ui.scheduler.util;

public interface ContextViewUpdateEventBroadcastListener {

    /**
     * Called when Context View is updated.
     *
     * @param message
     */
    void receiveBroadcast(String message);
}
