package org.ikasan.scheduled.instance.model;

import org.ikasan.scheduled.context.model.SolrContextImpl;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.*;

import java.util.Map;
import java.util.UUID;

public class SolrContextInstanceImpl extends SolrContextImpl<ContextInstance, ContextParameterInstance, SchedulerJobInstance>
    implements StatefulEntity, ContextInstance {
    private String id;
    private long createdDateTime;
    private long updatedDateTime;
    private long startTime;
    private long endTime;
    private String timezone;
    private InstanceStatus status;
    private Map<String, String> lockHolders;
    private Map<String, SchedulerJobInitiationEvent> heldJobs;

    public SolrContextInstanceImpl() {
        status = InstanceStatus.WAITING;
        createdDateTime = System.currentTimeMillis();
        updatedDateTime = System.currentTimeMillis();
        this.id = UUID.randomUUID().toString();
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

    @Override
    public Map<String, SchedulerJobInitiationEvent> getHeldJobs() {
        return heldJobs;
    }

    @Override
    public void setHeldJobs(Map<String, SchedulerJobInitiationEvent> heldJobs) {
        this.heldJobs = heldJobs;
    }
}
