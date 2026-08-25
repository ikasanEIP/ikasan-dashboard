package org.ikasan.mongo.persistence.scheduled.context.model;
import org.ikasan.mongo.persistence.general.model.MongoConstants;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.context.model.ScheduledContextViewRecord;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * MongoDB implementation of ScheduledContextViewRecord.
 * This class represents a scheduled context view stored in MongoDB.
 */
@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoScheduledContextViewRecordImpl implements ScheduledContextViewRecord {

    @Id
    private String id;

    @Field(EntityFields.TYPE)
    private String type;

    @Field(EntityFields.MODULE_NAME)
    private String parentContextName;

    @Field(EntityFields.FLOW_NAME)
    private String contextName;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String contextView;

    @Field(EntityFields.CREATED_DATE_TIME)
    private long timestamp;

    @Field(EntityFields.UPDATED_DATE_TIME)
    private long modifiedTimestamp;

    @Field(EntityFields.MODIFIED_BY)
    private String modifiedBy;

    @Indexed
    @Field(EntityFields.EXPIRY)
    private long expiry;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
