package org.ikasan.job.orchestration.model.event;

import org.ikasan.spec.scheduled.event.model.ContextInstanceStateChangeEvent;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

public class ContextInstanceStateChangeEventImpl extends StateChangeEventImpl implements ContextInstanceStateChangeEvent {
    private ContextInstance contextInstance;
    private String contextInstanceId;

    public ContextInstanceStateChangeEventImpl() {
    }

    public ContextInstanceStateChangeEventImpl(String contextInstanceId, ContextInstance contextInstance
        , InstanceStatus previousStatus, InstanceStatus newStatus) {
        super(previousStatus, newStatus);
        this.contextInstanceId = contextInstanceId;
        this.contextInstance = contextInstance;
    }

    @Override
    public String getContextInstanceId() {
        return contextInstanceId;
    }

    public void setContextInstanceId(String contextInstanceId) {
        this.contextInstanceId = contextInstanceId;
    }

    public ContextInstance getContextInstance() {
        return contextInstance;
    }

    public void setContextInstance(ContextInstance contextInstance) {
        this.contextInstance = contextInstance;
    }
}
