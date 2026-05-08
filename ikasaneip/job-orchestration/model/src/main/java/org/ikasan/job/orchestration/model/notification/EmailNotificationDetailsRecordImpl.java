package org.ikasan.job.orchestration.model.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;

public class EmailNotificationDetailsRecordImpl implements EmailNotificationDetailsRecord {

    private ObjectMapper objectMapper = ConcurrentObjectMapperFactory.newInstance();

    private String id;
    private String jobName;
    private String contextName;
    private String monitorType;
    private String emailNotificationDetails;
    private long timestamp;
    private long modifiedTimestamp;
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
            return objectMapper.readValue(this.emailNotificationDetails, EmailNotificationDetails.class);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException("Could not convert string to entity: " + emailNotificationDetails, e);
        }
    }

    public void setEmailNotificationDetails(EmailNotificationDetails emailNotificationDetails) {
        try {
            this.emailNotificationDetails = objectMapper.writeValueAsString(emailNotificationDetails);
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException("Could not convert entity to string: " + emailNotificationDetails, e);
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
