package org.ikasan.job.orchestration.model.event;

import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;

public class JobLockCacheEventImpl implements JobLockCacheEvent {

    private String jobIdentifier;
    private String contextName;
    private EventType eventType;

    public JobLockCacheEventImpl(String jobIdentifier, String contextName, EventType eventType) {
        this.jobIdentifier = jobIdentifier;
        this.contextName = contextName;
        this.eventType = eventType;
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
        return "JobLockCacheEventImpl{" +
            "jobIdentifier='" + jobIdentifier + '\'' +
            ", contextName='" + contextName + '\'' +
            ", eventType=" + eventType +
            '}';
    }
}
