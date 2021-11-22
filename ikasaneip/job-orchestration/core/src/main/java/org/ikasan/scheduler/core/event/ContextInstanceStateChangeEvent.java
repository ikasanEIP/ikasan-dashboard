package org.ikasan.scheduler.core.event;

import org.ikasan.scheduler.core.model.instance.ContextInstance;
import org.ikasan.scheduler.core.spec.InstanceStatus;

public class ContextInstanceStateChangeEvent extends StateChangeEvent {
    private ContextInstance contextInstance;

    public ContextInstanceStateChangeEvent(ContextInstance contextInstance, InstanceStatus previousStatus, InstanceStatus newStatus) {
        super(previousStatus, newStatus);
        this.contextInstance = contextInstance;
    }

    public ContextInstance getContextInstance() {
        return contextInstance;
    }
}
