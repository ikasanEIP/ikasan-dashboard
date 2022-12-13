package org.ikasan.dashboard.ui.scheduler.listener;

import org.ikasan.spec.scheduled.context.model.Context;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;

public interface ContextOpenedListener {

    /**
     * Called when a new context is opened!
     *
     * @param context
     */
    public void contextOpened(Context context);
}
