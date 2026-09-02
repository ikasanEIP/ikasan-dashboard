package org.ikasan.job.orchestration.model.notification;

import org.ikasan.spec.scheduled.notification.model.NotificationSendAudit;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAuditRecord;
import tools.jackson.databind.json.JsonMapper;

public class NotificationSendAuditRecordImpl implements NotificationSendAuditRecord {

    private static final JsonMapper objectMapper = JsonMapper.builder().build();

    private String id;
    private String notificationSendAudit;
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

    public long getTimestamp() {
        return this.timestamp;
    }

    public NotificationSendAudit getNotificationSendAudit() {
        return objectMapper.readValue(this.notificationSendAudit, NotificationSendAuditImpl.class);
    }

    public void setNotificationSendAudit(NotificationSendAudit notificationSendAudit) {
        this.notificationSendAudit = objectMapper.writeValueAsString(notificationSendAudit);
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
