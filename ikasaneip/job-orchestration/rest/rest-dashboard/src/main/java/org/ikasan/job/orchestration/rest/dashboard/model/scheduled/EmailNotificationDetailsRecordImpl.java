package org.ikasan.job.orchestration.rest.dashboard.model.scheduled;

import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;

public class EmailNotificationDetailsRecordImpl implements EmailNotificationDetailsRecord {

    private String id;

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
