package org.ikasan.dashboard.ui.scheduler.util;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;

public interface ContextTemplateSavedEventBroadcastListener {

    /**
     * Called when ContextTemplate is saved.
     *
     * @param contextTemplate
     */
    void receiveContextTemplateSavedEventBroadcast(ContextTemplate contextTemplate);
}
