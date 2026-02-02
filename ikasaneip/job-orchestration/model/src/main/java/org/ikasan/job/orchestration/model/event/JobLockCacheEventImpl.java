package org.ikasan.job.orchestration.model.event;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;

import java.util.Objects;

public class JobLockCacheEventImpl implements JobLockCacheEvent {

    private String lockName;
    private String jobIdentifier;
    private String contextName;
    private String environment;
    private EventType eventType;

    /**
     * Constructs a JobLockCacheEventImpl with the provided parameters.
     *
     * @param lockName The name of the lock associated with the event.
     * @param jobIdentifier The identifier of the job related to the event.
     * @param contextName The name of the context associated with the event.
     * @param environment The environment in which the event occurs.
     * @param eventType The type of event being represented.
     */
    public JobLockCacheEventImpl(String lockName, String jobIdentifier, String contextName, String environment, EventType eventType) {
        this.lockName = lockName;
        this.jobIdentifier = jobIdentifier;
        this.contextName = contextName;
        if(environment == null) {
            this.environment = JobLockCacheRecord.DEFAULT_ENVIRONMENT;
        }
        else {
            this.environment = environment;
        }
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
    public String getEnvironment() {
        return this.environment;
    }

    @Override
    public EventType getEvent() {
        return eventType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobLockCacheEventImpl that = (JobLockCacheEventImpl) o;
        return Objects.equals(lockName, that.lockName)
            && Objects.equals(jobIdentifier, that.jobIdentifier)
            && Objects.equals(contextName, that.contextName)
            && Objects.equals(environment, that.environment)
            && eventType == that.eventType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lockName, jobIdentifier, contextName, environment, eventType);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
