package org.ikasan.relational.persistence.scheduled.notification.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.ikasan.relational.persistence.scheduled.ScheduledConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContextRecord;

/**
 * Hibernate/JPA entity for EmailNotificationContextRecord with PostgreSQL JSONB storage.
 *
 * This entity stores email notification context configuration with:
 * - contextName as primary key (one notification config per context)
 * - EmailNotificationContext stored as JSONB for efficient querying and storage
 * - Timestamp tracking for creation and modification
 * - Actor tracking for modification history
 */
@Entity
@Table(name = "email_notification_context")
public class HibernateEmailNotificationContextRecord implements EmailNotificationContextRecord {

    @Transient
    private final ObjectMapper objectMapper = ScheduledConcurrentObjectMapperFactory.newInstance();

    @Id
    @Column(name = "context_name", nullable = false, length = 255)
    private String contextName;

    @Column(name = "email_notification_context", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String emailNotificationContextJson;

    @Transient
    private EmailNotificationContext emailNotificationContext;

    @Column(name = "timestamp", nullable = false)
    private long timestamp;

    @Column(name = "modified_timestamp", nullable = false)
    private long modifiedTimestamp;

    @Column(name = "modified_by", length = 255)
    private String modifiedBy;

    /**
     * Default constructor for JPA.
     */
    public HibernateEmailNotificationContextRecord() {
    }

    /**
     * Lifecycle callback to serialize emailNotificationContext to JSON before persist/update.
     */
    protected void serializeEmailNotificationContext() {
        if (this.emailNotificationContext != null) {
            try {
                this.emailNotificationContextJson = objectMapper.writeValueAsString(this.emailNotificationContext);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize EmailNotificationContext to JSON", e);
            }
        }

        // Set timestamps
        long now = System.currentTimeMillis();
        if (this.timestamp == 0) {
            this.timestamp = now;
        }
        this.modifiedTimestamp = now;

        // Sync contextName from emailNotificationContext if available
        if (this.emailNotificationContext != null && this.contextName == null) {
            this.contextName = this.emailNotificationContext.getContextName();
        }
    }

    /**
     * Lifecycle callback to deserialize JSON to emailNotificationContext after load.
     */
    @PostLoad
    protected void deserializeEmailNotificationContext() {
        if (this.emailNotificationContextJson != null) {
            try {
                this.emailNotificationContext = objectMapper.readValue(
                    this.emailNotificationContextJson,
                    EmailNotificationContext.class
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize EmailNotificationContext from JSON", e);
            }
        }
    }

    @Override
    public String getId() {
        return this.contextName;
    }

    @Override
    public void setId(String id) {
        this.contextName = id;
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
        // Sync contextName
        if (emailNotificationContext != null && this.contextName == null) {
            this.contextName = emailNotificationContext.getContextName();
        }
        serializeEmailNotificationContext();
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
