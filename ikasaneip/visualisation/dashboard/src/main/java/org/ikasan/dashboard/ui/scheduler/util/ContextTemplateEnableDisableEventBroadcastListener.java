package org.ikasan.dashboard.ui.scheduler.util;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;

public interface ContextTemplateEnableDisableEventBroadcastListener {

    /**
     * Called when ContextTemplate is saved.
     *
     * @param contextTemplate
     */
    void receiveBroadcast(ContextTemplate contextTemplate);
}
