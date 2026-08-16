package org.ikasan.mongo.persistence.scheduled.context.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.context.model.ScheduledContextViewRecord;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * MongoDB implementation of ScheduledContextViewRecord.
 * This class represents a scheduled context view stored in MongoDB.
 */
@Document(collection = "scheduledContextView")
public class MongoScheduledContextViewRecordImpl implements ScheduledContextViewRecord {

    @Id
    private String id;

    @Field("parentContextName")
    private String parentContextName;

    @Field("contextName")
    private String contextName;

    @Field("contextView")
    private String contextView;

    @Field("timestamp")
    private long timestamp;

    @Field("modifiedTimestamp")
    private long modifiedTimestamp;

    @Field("modifiedBy")
    private String modifiedBy;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getParentContextName() {
        return parentContextName;
    }

    @Override
    public void setParentContextName(String parentContextName) {
        this.parentContextName = parentContextName;
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
    public String getContextView() {
        return contextView;
    }

    @Override
    public void setContextView(String contextView) {
        this.contextView = contextView;
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
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
