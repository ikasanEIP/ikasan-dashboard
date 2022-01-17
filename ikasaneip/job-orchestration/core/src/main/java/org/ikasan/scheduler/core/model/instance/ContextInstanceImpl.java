package org.ikasan.scheduler.core.model.instance;

import org.ikasan.scheduler.core.model.context.ContextImpl;
import org.ikasan.spec.scheduled.instance.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class  ContextInstanceImpl extends ContextImpl<ContextInstance, ContextParameterInstance, SchedulerJobInstance>
    implements StatefulEntity, ContextInstance {
    private String id;
    private long createdDateTime;
    private long updatedDateTime;
    private long startTime;
    private long endTime;
    private String timezone;
    private InstanceStatus status;
    private Map<String, String> lockHolders;

    public ContextInstanceImpl() {
        this.id = UUID.randomUUID().toString();
        status = InstanceStatus.WAITING;
        createdDateTime = System.currentTimeMillis();
        updatedDateTime = System.currentTimeMillis();
        this.lockHolders = new HashMap<>();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public long getCreatedDateTime() {
        return createdDateTime;
    }

    @Override
    public void setCreatedDateTime(long createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    @Override
    public long getUpdatedDateTime() {
        return updatedDateTime;
    }

    @Override
    public void setUpdatedDateTime(long updatedDateTime) {
        this.updatedDateTime = updatedDateTime;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @Override
    public long getEndTime() {
        return endTime;
    }

    @Override
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @Override
    public String getTimezone() {
        return timezone;
    }

    @Override
    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    @Override
    public InstanceStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(InstanceStatus status) {
        if(status!= null)this.status = status;
    }

    @Override
    public Map<String, String> getLockHolders() {
        return lockHolders;
    }

    @Override
    public void setLockHolders(Map<String, String> lockHolders) {
        this.lockHolders = lockHolders;
    }
}
