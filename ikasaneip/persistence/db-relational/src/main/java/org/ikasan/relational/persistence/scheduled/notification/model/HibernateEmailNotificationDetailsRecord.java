package org.ikasan.relational.persistence.scheduled.notification.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.ikasan.relational.persistence.scheduled.ScheduledConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;

/**
 * Hibernate/JPA entity for EmailNotificationDetailsRecord with PostgreSQL JSONB storage.
 *
 * This entity stores detailed email notification configuration per job/monitor type:
 * - Composite ID based on jobName, childContextName, and monitorType
 * - EmailNotificationDetails stored as JSONB for efficient querying
 * - Timestamp tracking for creation and modification
 * - Actor tracking for modification history
 */
@Entity
@Table(name = "email_notification_details")
public class HibernateEmailNotificationDetailsRecord implements EmailNotificationDetailsRecord {

    @Transient
    private final ObjectMapper objectMapper = ScheduledConcurrentObjectMapperFactory.newInstance();

    @Id
    @Column(name = "id", nullable = false, length = 767)
    private String id;

    @Column(name = "job_name", nullable = false, length = 255)
    private String jobName;

    @Column(name = "context_name", nullable = false, length = 255)
    private String contextName;

    @Column(name = "monitor_type", nullable = false, length = 50)
    private String monitorType;

    @Column(name = "email_notification_details", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String emailNotificationDetailsJson;

    @Transient
    private EmailNotificationDetails emailNotificationDetails;

    @Column(name = "timestamp", nullable = false)
    private long timestamp;

    @Column(name = "modified_timestamp", nullable = false)
    private long modifiedTimestamp;

    @Column(name = "modified_by", length = 255)
    private String modifiedBy;

    /**
     * Default constructor for JPA.
     */
    public HibernateEmailNotificationDetailsRecord() {
    }

    /**
     * Lifecycle callback to serialize emailNotificationDetails to JSON before persist/update.
     */
    protected void serializeEmailNotificationDetails() {
        if (this.emailNotificationDetails != null) {
            try {
                this.emailNotificationDetailsJson = objectMapper.writeValueAsString(this.emailNotificationDetails);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize EmailNotificationDetails to JSON", e);
            }

            // Sync indexed fields from emailNotificationDetails
            this.jobName = this.emailNotificationDetails.getJobName();
            this.contextName = this.emailNotificationDetails.getContextName();
            this.monitorType = this.emailNotificationDetails.getMonitorType();

            // Generate ID if not set
            if (this.id == null) {
                this.id = generateId(
                    this.emailNotificationDetails.getJobName(),
                    this.emailNotificationDetails.getChildContextName(),
                    this.emailNotificationDetails.getMonitorType()
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
     * Lifecycle callback to deserialize JSON to emailNotificationDetails after load.
     */
    @PostLoad
    protected void deserializeEmailNotificationDetails() {
        if (this.emailNotificationDetailsJson != null) {
            try {
                this.emailNotificationDetails = objectMapper.readValue(
                    this.emailNotificationDetailsJson,
                    HibernateEmailNotificationDetails.class
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize EmailNotificationDetails from JSON", e);
            }
        }
    }

    /**
     * Generates ID from jobName, childContextName, and monitorType.
     * Format: jobName_childContextName_monitorType
     */
    private static String generateId(String jobName, String childContextName, String monitorType) {
        return jobName + "_" + childContextName + "_" + monitorType;
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
        return emailNotificationDetails;
    }

    @Override
    public void setEmailNotificationDetails(EmailNotificationDetails emailNotificationDetails) {
        this.emailNotificationDetails = emailNotificationDetails;
        this.serializeEmailNotificationDetails();
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
