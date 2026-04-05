package org.ikasan.job.orchestration.broadcast;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;

public interface ContextTemplateEnableDisableEventBroadcastListener {

    /**
     * Called when ContextTemplate is saved.
     *
     * @param contextTemplate
     */
    void receiveBroadcast(ContextTemplate contextTemplate);
}
