package org.ikasan.mongo.persistence.scheduled.notification.model;

import org.ikasan.job.orchestration.exception.EntityConversionException;
import org.ikasan.job.orchestration.model.notification.EmailNotificationDetailsImpl;
import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Document(collection = "emailNotificationDetailsRecord")
public class MongoEmailNotificationDetailsRecordImpl implements EmailNotificationDetailsRecord {

    private static final JsonMapper OBJECT_MAPPER = ConcurrentObjectMapperFactory.newInstance();

    @Id
    private String id;

    @Field("jobName")
    private String jobName;

    @Field("contextName")
    private String contextName;

    @Field("monitorType")
    private String monitorType;

    @Field("emailNotificationDetails")
    private String emailNotificationDetails;

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
    public String getJobName() {
        return jobName;
    }

    @Override
    public void setJobName(String jobName) {
        this.jobName = jobName;
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
    public String getMonitorType() {
        return monitorType;
    }

    @Override
    public void setMonitorType(String monitorType) {
        this.monitorType = monitorType;
    }

    @Override
    public EmailNotificationDetails getEmailNotificationDetails() {
        try {
            return OBJECT_MAPPER.readValue(emailNotificationDetails, EmailNotificationDetailsImpl.class);
        } catch (JacksonException e) {
            throw new EntityConversionException("Could not convert string to entity: " + emailNotificationDetails, e);
        }
    }

    @Override
    public void setEmailNotificationDetails(EmailNotificationDetails emailNotificationDetails) {
        try {
            this.emailNotificationDetails = OBJECT_MAPPER.writeValueAsString(emailNotificationDetails);
        } catch (JacksonException e) {
            throw new EntityConversionException("Could not convert entity to string: " + emailNotificationDetails, e);
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
