package org.ikasan.scheduled.notification.model;

import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

public class SolrEmailNotificationDetailsRecord implements EmailNotificationDetailsRecord {

    private static final JsonMapper objectMapper = ScheduledObjectMapperFactory.newInstance();

    @Field(EntityFields.ID)
    private String id;

    @Field(EntityFields.MODULE_NAME)
    private String jobName;

    @Field(EntityFields.COMPONENT_NAME)
    private String contextName;

    @Field(EntityFields.RELATED_EVENT)
    private String monitorType;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String emailNotificationDetails;

    @Field(EntityFields.CREATED_DATE_TIME)
    private long timestamp;

    @Field(EntityFields.UPDATED_DATE_TIME)
    private long modifiedTimestamp;

    @Field(EntityFields.MODIFIED_BY)
    private String modifiedBy;

    public String getId() {
        return this.id;
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

    public long getTimestamp() {
        return this.timestamp;
    }

    public EmailNotificationDetails getEmailNotificationDetails() {
        try {
            return objectMapper.readValue(this.emailNotificationDetails, SolrEmailNotificationDetails.class);
        }
        catch (JacksonException e) {
            throw new SolrEntityConversionException("Could not convert string to entity: " + emailNotificationDetails, e);
        }
    }

    public void setEmailNotificationDetails(EmailNotificationDetails emailNotificationDetails) {
        try {
            this.emailNotificationDetails = objectMapper.writeValueAsString(emailNotificationDetails);
        }
        catch (JacksonException e) {
            throw new SolrEntityConversionException("Could not convert entity to string: " + emailNotificationDetails, e);
        }
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }
}
