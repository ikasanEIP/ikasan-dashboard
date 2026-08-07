package org.ikasan.job.orchestration.model.instance;

import org.ikasan.job.orchestration.model.context.ContextImpl;
import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.*;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class  ContextInstanceImpl extends ContextImpl<ContextInstance, ContextParameterInstance, SchedulerJobInstance, JobLockInstance>
    implements StatefulEntity, ContextInstance {
    private static JsonMapper objectMapper = ConcurrentObjectMapperFactory.newInstance();
    private String id;
    private long createdDateTime;
    private long updatedDateTime;
    private long startTime;
    private long projectedEndTime;
    private long endTime;
    private boolean isRunContextUntilManuallyEnded;
    private String timezone;
    private InstanceStatus status;
    private Map<String, SchedulerJobInitiationEvent> heldJobs;

    private boolean containsRepeatingJobs = false;

    public ContextInstanceImpl() {
        this.id = UUID.randomUUID().toString();
        status = InstanceStatus.WAITING;
        createdDateTime = System.currentTimeMillis();
        updatedDateTime = System.currentTimeMillis();
        this.heldJobs = new HashMap<>();
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContextInstanceImpl that = (ContextInstanceImpl) o;
        return Objects.equals(super.getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.getName());
    }

    @Override
    public String toString() {
        try {
            return objectMapper.writeValueAsString(this);
        }
        catch (JacksonException e) {
            return String.format("Could not resolve context instance as string. Context Name[%s], Context Instance Id[%s]", this.name, this.id);
        }
    }
}
