package org.ikasan.job.orchestration.model.notification;

import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContextRecord;

public class EmailNotificationContextRecordImpl implements EmailNotificationContextRecord {

    private String id;
    private String contextName;
    private EmailNotificationContext emailNotificationContext;
    private long timestamp;
    private long modifiedTimestamp;
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
        return emailNotificationContext;
    }

    @Override
    public void setEmailNotificationContext(EmailNotificationContext emailNotificationContext) {
        this.emailNotificationContext = emailNotificationContext;
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
