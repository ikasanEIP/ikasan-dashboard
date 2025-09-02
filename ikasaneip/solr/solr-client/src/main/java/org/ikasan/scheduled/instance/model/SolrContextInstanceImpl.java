package org.ikasan.scheduled.instance.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.scheduled.context.model.SolrContextImpl;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.*;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class SolrContextInstanceImpl extends SolrContextImpl<ContextInstance, ContextParameterInstance, SchedulerJobInstance, JobLockInstance>
    implements StatefulEntity, ContextInstance {
    private ObjectMapper objectMapper = ConcurrentObjectMapperFactory.newInstance();
    private String id;
    private long createdDateTime;
    private long updatedDateTime;
    private long startTime = 0L;
    private long endTime = 0L;
    private long projectedEndTime = 0L;
    private boolean isRunContextUntilManuallyEnded;
    private String timezone;
    private InstanceStatus status;
    private Map<String, SchedulerJobInitiationEvent> heldJobs;

    private boolean containsRepeatingJobs = false;

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
    public long getProjectedEndTime() {
        return projectedEndTime;
    }

    @Override
    public void setProjectedEndTime(long projectedEndTime) {
        this.projectedEndTime = projectedEndTime;
    }

    @Override
    public boolean isRunContextUntilManuallyEnded() {
        return isRunContextUntilManuallyEnded;
    }

    @Override
    public void setRunContextUntilManuallyEnded(boolean runContextUntilManuallyEnded) {
        isRunContextUntilManuallyEnded = runContextUntilManuallyEnded;
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
    public Map<String, SchedulerJobInitiationEvent> getHeldJobs() {
        return heldJobs;
    }

    @Override
    public void setHeldJobs(Map<String, SchedulerJobInitiationEvent> heldJobs) {
        this.heldJobs = heldJobs;
    }

    @Override
    public boolean isContainsRepeatingJobs() {
        return containsRepeatingJobs;
    }

    @Override
    public void setContainsRepeatingJobs(boolean containsRepeatingJobs) {
        this.containsRepeatingJobs = containsRepeatingJobs;
    }

    @Override
    public String toString() {
        try {
            return this.objectMapper.writeValueAsString(this);
        }
        catch (JsonProcessingException e) {
            return String.format("Could not resolve context instance as string. Context Name[%s], Context Instance Id[%s]", this.name, this.id);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SolrContextInstanceImpl that = (SolrContextInstanceImpl) o;
        return Objects.equals(this.id, that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.getName());
    }
}
