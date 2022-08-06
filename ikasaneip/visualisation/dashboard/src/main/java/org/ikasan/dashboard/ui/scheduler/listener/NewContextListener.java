package org.ikasan.dashboard.ui.scheduler.listener;

import org.ikasan.spec.scheduled.context.model.ContextTemplate;

public interface NewContextListener {

    /**
     * Called when a new context is added!
     *
     * @param contextTemplate
     */
    public void newContext(ContextTemplate contextTemplate);
}
