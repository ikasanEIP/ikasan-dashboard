package org.ikasan.mongo.persistence.scheduled.notification.model;

import org.ikasan.job.orchestration.exception.EntityConversionException;
import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAudit;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAuditRecord;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Document(collection = "notificationSendAuditRecord")
public class MongoNotificationSendAuditRecordImpl implements NotificationSendAuditRecord {

    private static final JsonMapper OBJECT_MAPPER = ConcurrentObjectMapperFactory.newInstance();

    @Id
    private String id;

    @Field("notificationSendAudit")
    private String notificationSendAudit;

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
}
