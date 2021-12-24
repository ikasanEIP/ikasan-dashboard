package org.ikasan.scheduler.core.model.event;

import org.ikasan.scheduler.core.model.instance.ContextInstanceImpl;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

public class ContextInstanceStateChangeEvent extends StateChangeEvent {
    private ContextInstanceImpl contextInstance;

    public ContextInstanceStateChangeEvent(ContextInstanceImpl contextInstance, InstanceStatus previousStatus, InstanceStatus newStatus) {
        super(previousStatus, newStatus);
        this.contextInstance = contextInstance;
    }

    public ContextInstanceImpl getContextInstance() {
        return contextInstance;
    }
}
