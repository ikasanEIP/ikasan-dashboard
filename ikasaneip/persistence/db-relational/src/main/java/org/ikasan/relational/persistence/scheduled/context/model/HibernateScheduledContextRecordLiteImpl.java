package org.ikasan.relational.persistence.scheduled.context.model;

import org.ikasan.spec.scheduled.context.ScheduledContextRecordLite;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Lightweight implementation of ScheduledContextRecordLite for Hibernate persistence.
 * Used for efficient listing and filtering without loading full context templates.
 */
public class HibernateScheduledContextRecordLiteImpl implements ScheduledContextRecordLite {
    private String id;
    private String contextName;
    private String description;
    private long timestamp;
    private long modifiedTimestamp;
    private String modifiedBy;
    private boolean disabled = false;
    private boolean isQuartzScheduleDrivenJobsDisabledForContext = false;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getContextName() {
        return contextName;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    @Override
    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    @Override
    public String getModifiedBy() {
        return modifiedBy;
    }

    @Override
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public boolean isQuartzScheduleDrivenJobsDisabledForContext() {
        return isQuartzScheduleDrivenJobsDisabledForContext;
    }

    @Override
    public void setQuartzScheduleDrivenJobsDisabledForContext(boolean quartzScheduleDrivenJobsDisabledForContext) {
        isQuartzScheduleDrivenJobsDisabledForContext = quartzScheduleDrivenJobsDisabledForContext;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HibernateScheduledContextRecordLiteImpl that = (HibernateScheduledContextRecordLiteImpl) o;
        return timestamp == that.timestamp && modifiedTimestamp == that.modifiedTimestamp
            && disabled == that.disabled
            && isQuartzScheduleDrivenJobsDisabledForContext == that.isQuartzScheduleDrivenJobsDisabledForContext
            && Objects.equals(id, that.id) && Objects.equals(contextName, that.contextName)
            && Objects.equals(description, that.description)
            && Objects.equals(modifiedBy, that.modifiedBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, contextName, description, timestamp, modifiedTimestamp, modifiedBy, disabled
            , isQuartzScheduleDrivenJobsDisabledForContext);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", HibernateScheduledContextRecordLiteImpl.class.getSimpleName() + "[", "]")
            .add("id='" + id + "'")
            .add("contextName='" + contextName + "'")
            .add("description='" + description + "'")
            .add("timestamp=" + timestamp)
            .add("modifiedTimestamp=" + modifiedTimestamp)
            .add("modifiedBy='" + modifiedBy + "'")
            .add("disabled=" + disabled)
            .add("isQuartzScheduleDrivenJobsDisabledForContext=" + isQuartzScheduleDrivenJobsDisabledForContext)
            .toString();
    }
}
