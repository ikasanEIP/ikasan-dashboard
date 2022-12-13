package org.ikasan.dashboard.ui.scheduler.listener;

import org.ikasan.spec.scheduled.context.model.Context;

public interface ContextSelectedListener {

    /**
     * Called when a new context is opened!
     *
     * @param contextName
     */
    public void contextSelected(String contextName);
}
