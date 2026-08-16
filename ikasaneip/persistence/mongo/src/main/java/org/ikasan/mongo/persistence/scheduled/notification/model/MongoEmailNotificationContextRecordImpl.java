package org.ikasan.mongo.persistence.scheduled.notification.model;

import org.ikasan.job.orchestration.exception.EntityConversionException;
import org.ikasan.job.orchestration.model.notification.EmailNotificationContextImpl;
import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContextRecord;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Document(collection = "emailNotificationContextRecord")
public class MongoEmailNotificationContextRecordImpl implements EmailNotificationContextRecord {

    private static final JsonMapper OBJECT_MAPPER = ConcurrentObjectMapperFactory.newInstance();

    @Id
    private String id;

    @Field("contextName")
    private String contextName;

    @Field("emailNotificationContext")
    private String emailNotificationContext;

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
    public String getContextName() {
        return contextName;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    @Override
    public EmailNotificationContext getEmailNotificationContext() {
        try {
            return OBJECT_MAPPER.readValue(emailNotificationContext, EmailNotificationContextImpl.class);
        } catch (JacksonException e) {
            throw new EntityConversionException("Could not convert string to entity: " + emailNotificationContext, e);
        }
    }

    @Override
    public void setEmailNotificationContext(EmailNotificationContext emailNotificationContext) {
        try {
            this.emailNotificationContext = OBJECT_MAPPER.writeValueAsString(emailNotificationContext);
        } catch (JacksonException e) {
            throw new EntityConversionException("Could not convert entity to string: " + emailNotificationContext, e);
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
