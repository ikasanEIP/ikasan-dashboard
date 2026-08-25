package org.ikasan.mongo.persistence.scheduled.notification.model;
import org.ikasan.mongo.persistence.general.model.MongoConstants;

import org.ikasan.job.orchestration.exception.EntityConversionException;
import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAudit;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAuditRecord;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoNotificationSendAuditRecordImpl implements NotificationSendAuditRecord {

    private static final JsonMapper OBJECT_MAPPER = ConcurrentObjectMapperFactory.newInstance();

    @Id
    private String id;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String notificationSendAudit;

    @Field(EntityFields.CREATED_DATE_TIME)
    private long timestamp;

    @Field(EntityFields.UPDATED_DATE_TIME)
    private long modifiedTimestamp;

    @Field(EntityFields.MODIFIED_BY)
    private String modifiedBy;

    @Indexed
    @Field(EntityFields.TYPE)
    private String type;

    @Indexed
    @Field(EntityFields.EXPIRY)
    private long expiry;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public NotificationSendAudit getNotificationSendAudit() {
        try {
            return OBJECT_MAPPER.readValue(notificationSendAudit, MongoNotificationSendAuditImpl.class);
        } catch (JacksonException e) {
            throw new EntityConversionException("Could not convert string to entity: " + notificationSendAudit, e);
        }
    }

    @Override
    public void setNotificationSendAudit(NotificationSendAudit notificationSendAudit) {
        try {
            this.notificationSendAudit = OBJECT_MAPPER.writeValueAsString(notificationSendAudit);
        } catch (JacksonException e) {
            throw new EntityConversionException("Could not convert entity to string: " + notificationSendAudit, e);
        }
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }
}
