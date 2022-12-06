package org.ikasan.job.orchestration.model.instance;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.event.model.ContextualisedScheduledProcessEvent;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregate;

import java.util.List;

public class ScheduledContextInstanceAuditAggregateImpl implements ScheduledContextInstanceAuditAggregate {

    private ContextualisedScheduledProcessEvent contextualisedScheduledProcessEvent;
    private List<SchedulerJobInitiationEvent> schedulerJobInitiationEvents;
    private String previousContextInstanceAuditId;
    private String updatedContextInstanceAuditId;

    @Override
    public ContextualisedScheduledProcessEvent getProcessEvent() {
        return this.contextualisedScheduledProcessEvent;
    }

    @Override
    public void setProcessEvent(ContextualisedScheduledProcessEvent contextualisedScheduledProcessEvent) {
        this.contextualisedScheduledProcessEvent = contextualisedScheduledProcessEvent;
    }

    @Override
    public List<SchedulerJobInitiationEvent> getSchedulerJobInitiationEvents() {
        return this.schedulerJobInitiationEvents;
    }

    @Override
    public void setSchedulerJobInitiationEvents(List<SchedulerJobInitiationEvent> schedulerJobInitiationEvents) {
        this.schedulerJobInitiationEvents = schedulerJobInitiationEvents;
    }

    @Override
    public String getPreviousContextInstanceAuditId() {
        return previousContextInstanceAuditId;
    }

    @Override
    public void setPreviousContextInstanceAuditId(String previousContextInstanceAuditId) {
        this.previousContextInstanceAuditId = previousContextInstanceAuditId;
    }

    @Override
    public String getUpdatedContextInstanceAuditId() {
        return updatedContextInstanceAuditId;
    }

    @Override
    public void setUpdatedContextInstanceAuditId(String updatedContextInstanceAuditId) {
        this.updatedContextInstanceAuditId = updatedContextInstanceAuditId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
