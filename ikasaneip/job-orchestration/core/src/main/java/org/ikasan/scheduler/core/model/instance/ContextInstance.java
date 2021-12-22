package org.ikasan.scheduler.core.model.instance;

import org.ikasan.scheduler.core.model.context.ContextImpl;
import org.ikasan.spec.scheduled.context.model.ContextParameterInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.StatefulEntity;


public class ContextInstance extends ContextImpl<ContextInstance, ContextParameterInstance, SchedulerJobInstance> implements StatefulEntity {
    private String id;
    private long createdDateTime;
    private long updatedDateTime;
    private long startTime;
    private long endTime;
    private String timezone;
    private InstanceStatus status;

    public ContextInstance() {
        status = InstanceStatus.WAITING;
        createdDateTime = System.currentTimeMillis();
        updatedDateTime = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(long createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    public long getUpdatedDateTime() {
        return updatedDateTime;
    }

    public void setUpdatedDateTime(long updatedDateTime) {
        this.updatedDateTime = updatedDateTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public InstanceStatus getStatus() {
        return status;
    }

    public void setStatus(InstanceStatus status) {
        if(status!= null)this.status = status;
    }
}
