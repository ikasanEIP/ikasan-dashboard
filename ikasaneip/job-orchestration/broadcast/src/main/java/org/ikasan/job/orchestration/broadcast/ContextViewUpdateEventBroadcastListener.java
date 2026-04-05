package org.ikasan.job.orchestration.broadcast;

public interface ContextViewUpdateEventBroadcastListener {

    /**
     * Called when Context View is updated.
     *
     * @param message
     */
    void receiveBroadcast(String message);
}
