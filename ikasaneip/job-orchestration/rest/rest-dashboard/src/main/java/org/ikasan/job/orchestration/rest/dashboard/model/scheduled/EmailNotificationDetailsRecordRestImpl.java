package org.ikasan.job.orchestration.rest.dashboard.model.scheduled;

import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;

public class EmailNotificationDetailsRecordRestImpl implements EmailNotificationDetailsRecord {

    private String id;

    private String jobName;

    private String contextName;

    private String monitorType;

    private EmailNotificationDetails emailNotificationDetails;

    private long timestamp;

    private long modifiedTimestamp;

    private String modifiedBy;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getContextName() {
        return contextName;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public String getMonitorType() {
        return monitorType;
    }

    public void setMonitorType(String monitorType) {
        this.monitorType = monitorType;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public EmailNotificationDetails getEmailNotificationDetails() {
       return this.emailNotificationDetails;
    }

    public void setEmailNotificationDetails(EmailNotificationDetails emailNotificationDetails) {
        this.emailNotificationDetails = emailNotificationDetails;
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
