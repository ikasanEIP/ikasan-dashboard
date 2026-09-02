package org.ikasan.job.orchestration.model.instance;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.job.orchestration.model.job.SchedulerJobImpl;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.instance.model.StatefulEntity;

import java.io.Serializable;

public class SchedulerJobInstanceImpl extends SchedulerJobImpl implements SchedulerJobInstance, StatefulEntity, Serializable {
    private String contextInstanceId;
    private String childContextName;
    private boolean held = false;
    private boolean initiationEventRaised = false;
    private InstanceStatus status;
    private ScheduledProcessEvent scheduledProcessEvent;
    private boolean skip;
    private Boolean errorAcknowledged;

    public SchedulerJobInstanceImpl() {
        status = InstanceStatus.WAITING;
    }

    @Override
    public String getContextInstanceId() {
        return contextInstanceId;
    }

    @Override
    public void setContextInstanceId(String contextInstanceId) {
        this.contextInstanceId = contextInstanceId;
    }

    @Override
    public String getChildContextName() {
        return childContextName;
    }

    @Override
    public void setChildContextName(String childContextName) {
        this.childContextName = childContextName;
    }

    @Override
    public boolean isHeld() {
        return held;
    }

    @Override
    public void setHeld(boolean held) {
        this.held = held;
    }

    @Override
    public boolean isInitiationEventRaised() {
        return initiationEventRaised;
    }

    @Override
    public void setInitiationEventRaised(boolean initiationEventRaised) {
        this.initiationEventRaised = initiationEventRaised;
    }

    @Override
    public InstanceStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(InstanceStatus status) {
        this.status = status;
    }

    @Override
    public ScheduledProcessEvent getScheduledProcessEvent() {
        return scheduledProcessEvent;
    }

    @Override
    public void setScheduledProcessEvent(ScheduledProcessEvent scheduledProcessEvent) {
        this.scheduledProcessEvent = scheduledProcessEvent;
    }

    @Override
    public boolean isSkip() {
        return skip;
    }

    @Override
    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    @Override
    public Boolean isErrorAcknowledged() {
        return errorAcknowledged;
    }

    @Override
    public void setErrorAcknowledged(Boolean errorAcknowledged) {
        this.errorAcknowledged = errorAcknowledged;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (childContextName != null ? childContextName.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
