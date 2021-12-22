package org.ikasan.scheduler.core.model.event;

import org.ikasan.scheduler.core.model.instance.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

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
