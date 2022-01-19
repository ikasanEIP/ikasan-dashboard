package org.ikasan.scheduler.core.model.event;

import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

public class ContextInstanceStateChangeEventImpl extends StateChangeEventImpl implements ContextInstanceStateChangeEvent {
    private ContextInstance contextInstance;

    public ContextInstanceStateChangeEventImpl(ContextInstance contextInstance, InstanceStatus previousStatus, InstanceStatus newStatus) {
        super(previousStatus, newStatus);
        this.contextInstance = contextInstance;
    }

    public ContextInstance getContextInstance() {
        return contextInstance;
    }
}
