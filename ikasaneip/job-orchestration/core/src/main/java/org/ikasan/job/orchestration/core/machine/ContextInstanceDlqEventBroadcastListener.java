package org.ikasan.job.orchestration.core.machine;

import org.ikasan.spec.scheduled.instance.model.ContextInstance;

public interface ContextInstanceDlqEventBroadcastListener {

    /**
     * Receives a broadcast event with the given context instance.
     *
     * @param contextInstance the context instance associated with the broadcast event
     */
    void receiveBroadcast(ContextInstance contextInstance);
}
