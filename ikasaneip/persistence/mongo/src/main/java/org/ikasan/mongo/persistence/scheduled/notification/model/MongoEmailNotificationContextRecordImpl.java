package org.ikasan.mongo.persistence.scheduled.notification.model;
import org.ikasan.mongo.persistence.general.model.MongoConstants;

import org.ikasan.job.orchestration.exception.EntityConversionException;
import org.ikasan.job.orchestration.model.notification.EmailNotificationContextImpl;
import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContextRecord;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoEmailNotificationContextRecordImpl implements EmailNotificationContextRecord {

    private static final JsonMapper OBJECT_MAPPER = ConcurrentObjectMapperFactory.newInstance();

    @Id
    private String id;

    @Field(EntityFields.COMPONENT_NAME)
    private String contextName;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String emailNotificationContext;

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
