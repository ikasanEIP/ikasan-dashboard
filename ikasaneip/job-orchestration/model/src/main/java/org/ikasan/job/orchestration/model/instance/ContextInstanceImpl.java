package org.ikasan.job.orchestration.model.instance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.SerializationUtils;
import org.ikasan.job.orchestration.model.context.ContextImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.*;

import java.util.*;

public class  ContextInstanceImpl extends ContextImpl<ContextInstance, ContextParameterInstance, SchedulerJobInstance, JobLockInstance>
    implements StatefulEntity, ContextInstance {
    private ObjectMapper objectMapper = ObjectMapperFactory.newInstance();
    private String id;
    private long createdDateTime;
    private long updatedDateTime;
    private long startTime;
    private long endTime;
    private String timezone;
    private InstanceStatus status;
    private Map<String, SchedulerJobInitiationEvent> heldJobs;

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
}
