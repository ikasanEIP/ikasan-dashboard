package org.ikasan.scheduled.notification.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAudit;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAuditRecord;
import org.ikasan.spec.solr.SolrDaoBase;

public class SolrNotificationSendAuditRecord implements NotificationSendAuditRecord {

    private static ObjectMapper objectMapper = new ObjectMapper();

    @Field(SolrDaoBase.ID)
    private String id;

    @Field(SolrDaoBase.PAYLOAD_CONTENT)
    private String notificationSendAudit;

    @Field(SolrDaoBase.CREATED_DATE_TIME)
    private long timestamp;

    @Field(SolrDaoBase.UPDATED_DATE_TIME)
    private long modifiedTimestamp;

    @Field(SolrDaoBase.MODIFIED_BY)
    private String modifiedBy;

    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public NotificationSendAudit getNotificationSendAudit() {
        try {
            return objectMapper.readValue(this.notificationSendAudit, SolrNotificationSendAudit.class);
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert string to entity: " + notificationSendAudit, e);
        }
    }

    public void setNotificationSendAudit(NotificationSendAudit notificationSendAudit) {
        try {
            this.notificationSendAudit = objectMapper.writeValueAsString(notificationSendAudit);
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert entity to string: " + notificationSendAudit, e);
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
