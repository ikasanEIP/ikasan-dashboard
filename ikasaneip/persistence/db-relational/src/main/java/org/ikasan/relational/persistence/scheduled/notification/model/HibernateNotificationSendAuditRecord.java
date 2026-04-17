package org.ikasan.relational.persistence.scheduled.notification.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.ikasan.relational.persistence.scheduled.ScheduledConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAudit;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAuditRecord;

/**
 * Hibernate/JPA entity for NotificationSendAuditRecord with PostgreSQL JSONB storage.
 *
 * This entity stores audit records of notification send attempts:
 * - Composite ID based on contextInstanceId, contextName, jobName, monitorType, and notifierType
 * - NotificationSendAudit data stored as JSONB
 * - Timestamp tracking for creation and modification
 * - Actor tracking for modification history
 * - Used to track whether notifications were successfully sent for specific contexts
 */
@Entity
@Table(name = "notification_send_audit")
public class HibernateNotificationSendAuditRecord implements NotificationSendAuditRecord {

    @Transient
    private final ObjectMapper objectMapper = ScheduledConcurrentObjectMapperFactory.newInstance();

    @Id
    @Column(name = "id", nullable = false, length = 1024)
    private String id;

    @Column(name = "notification_send_audit", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String notificationSendAuditJson;

    @Transient
    private NotificationSendAudit notificationSendAudit;

    @Column(name = "timestamp", nullable = false)
    private long timestamp;

    @Column(name = "modified_timestamp", nullable = false)
    private long modifiedTimestamp;

    @Column(name = "modified_by", length = 255)
    private String modifiedBy;

    /**
     * Default constructor for JPA.
     */
    public HibernateNotificationSendAuditRecord() {
    }

    /**
     * Lifecycle callback to serialize notificationSendAudit to JSON before persist/update.
     */
    protected void serializeNotificationSendAudit() {
        if (this.notificationSendAudit != null) {
            try {
                this.notificationSendAuditJson = objectMapper.writeValueAsString(this.notificationSendAudit);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize NotificationSendAudit to JSON", e);
            }

            // Generate ID if not set
            if (this.id == null) {
                this.id = generateId(
                    this.notificationSendAudit.getContextInstanceId(),
                    this.notificationSendAudit.getContextName(),
                    this.notificationSendAudit.getJobName(),
                    this.notificationSendAudit.getMonitorType(),
                    this.notificationSendAudit.getNotifierType()
                );
            }
        }

        // Set timestamps
        long now = System.currentTimeMillis();
        if (this.timestamp == 0) {
            this.timestamp = now;
        }
        this.modifiedTimestamp = now;
    }

    /**
     * Lifecycle callback to deserialize JSON to notificationSendAudit after load.
     */
    @PostLoad
    protected void deserializeNotificationSendAudit() {
        if (this.notificationSendAuditJson != null) {
            try {
                this.notificationSendAudit = objectMapper.readValue(
                    this.notificationSendAuditJson,
                    HibernateNotificationSendAudit.class
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize NotificationSendAudit from JSON", e);
            }
        }
    }

    /**
     * Generates ID from contextInstanceId, contextName, jobName, monitorType, and notifierType.
     * Format: contextInstanceId_contextName_jobName_monitorType_notifierType
     */
    private static String generateId(String contextInstanceId, String contextName, String jobName,
                                     String monitorType, String notifierType) {
        StringBuilder sb = new StringBuilder();
        sb.append(contextInstanceId);
        sb.append("_");
        sb.append(contextName);
        sb.append("_");
        sb.append(jobName);
        sb.append("_");
        sb.append(monitorType);
        sb.append("_");
        sb.append(notifierType);
        return sb.toString();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public NotificationSendAudit getNotificationSendAudit() {
        return notificationSendAudit;
    }

    @Override
    public void setNotificationSendAudit(NotificationSendAudit notificationSendAudit) {
        this.notificationSendAudit = notificationSendAudit;
        this.serializeNotificationSendAudit();
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
