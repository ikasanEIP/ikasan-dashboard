package org.ikasan.scheduled.instance.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.scheduled.job.model.SolrSchedulerJobImpl;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.instance.model.StatefulEntity;

public class SolrSchedulerJobInstanceImpl extends SolrSchedulerJobImpl implements SchedulerJobInstance, StatefulEntity {
    private String contextInstanceId;
    private String childContextName;
    private boolean held = false;
    private boolean initiationEventRaised = false;
    private InstanceStatus status;
    private ScheduledProcessEvent scheduledProcessEvent;

    public SolrSchedulerJobInstanceImpl() {
        status = InstanceStatus.WAITING;
    }

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

    public boolean isHeld() {
        return held;
    }

    public void setHeld(boolean held) {
        this.held = held;
    }

    public boolean isInitiationEventRaised() {
        return initiationEventRaised;
    }

    public void setInitiationEventRaised(boolean initiationEventRaised) {
        this.initiationEventRaised = initiationEventRaised;
    }

    public InstanceStatus getStatus() {
        return status;
    }

    public void setStatus(InstanceStatus status) {
        this.status = status;
    }

    public ScheduledProcessEvent getScheduledProcessEvent() {
        return scheduledProcessEvent;
    }

    public void setScheduledProcessEvent(ScheduledProcessEvent scheduledProcessEvent) {
        this.scheduledProcessEvent = scheduledProcessEvent;
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
