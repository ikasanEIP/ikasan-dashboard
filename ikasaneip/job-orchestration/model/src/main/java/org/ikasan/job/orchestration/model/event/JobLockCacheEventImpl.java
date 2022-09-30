package org.ikasan.job.orchestration.model.event;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;

public class JobLockCacheEventImpl implements JobLockCacheEvent {

    private String lockName;
    private String jobIdentifier;
    private String contextName;
    private EventType eventType;

    public JobLockCacheEventImpl(String lockName, String jobIdentifier, String contextName, EventType eventType) {
        this.lockName = lockName;
        this.jobIdentifier = jobIdentifier;
        this.contextName = contextName;
        this.eventType = eventType;
    }

    @Override
    public String getLockName() {
        return null;
    }

    @Override
    public String getJobIdentifier() {
        return this.jobIdentifier;
    }

    @Override
    public String getContextName() {
        return this.contextName;
    }

    @Override
    public EventType getEvent() {
        return eventType;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
