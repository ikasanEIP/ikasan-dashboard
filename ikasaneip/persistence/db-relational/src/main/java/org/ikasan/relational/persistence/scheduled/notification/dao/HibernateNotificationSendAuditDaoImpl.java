package org.ikasan.relational.persistence.scheduled.notification.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.ikasan.relational.persistence.scheduled.notification.model.HibernateNotificationSendAuditRecord;
import org.ikasan.spec.scheduled.notification.dao.NotificationSendAuditDao;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAuditRecord;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate implementation of NotificationSendAuditDao for PostgreSQL persistence.
 *
 * This DAO provides operations for notification send audit records including:
 * - Save/update operations (upsert based on composite ID)
 * - Find operations by composite key
 * - Audit trail of notification send attempts
 */
public class HibernateNotificationSendAuditDaoImpl implements NotificationSendAuditDao<NotificationSendAuditRecord> {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Saves or updates a notification send audit record.
     *
     * Uses merge for upsert behavior - if a record with the same ID exists,
     * it will be updated; otherwise, a new record is created.
     *
     * @param notificationSendAuditRecord the record to save
     * @throws IllegalArgumentException if record is null or not a Hibernate type
     */
    @Override
    @Transactional
    public void save(NotificationSendAuditRecord notificationSendAuditRecord) {
        if (notificationSendAuditRecord == null) {
            throw new IllegalArgumentException("notificationSendAuditRecord cannot be null!");
        }
        if (!(notificationSendAuditRecord instanceof HibernateNotificationSendAuditRecord)) {
            throw new IllegalArgumentException(
                "notificationSendAuditRecord must be an instance of HibernateNotificationSendAuditRecord!");
        }

        HibernateNotificationSendAuditRecord hibernateRecord =
            (HibernateNotificationSendAuditRecord) notificationSendAuditRecord;

        entityManager.merge(hibernateRecord);
        entityManager.flush();
    }

    /**
     * Retrieves a notification send audit record by composite key.
     *
     * @param contextInstanceId the context instance ID
     * @param contextName the context name
     * @param jobName the job name
     * @param monitorType the monitor type
     * @param notifierType the notifier type
     * @return the matching record, or null if not found
     */
    @Override
    public NotificationSendAuditRecord find(String contextInstanceId, String contextName, String jobName,
                                           String monitorType, String notifierType) {
        String id = generateId(contextInstanceId, contextName, jobName, monitorType, notifierType);
        return entityManager.find(HibernateNotificationSendAuditRecord.class, id);
    }

    /**
     * Helper method to delete all records (for testing purposes).
     */
    @Transactional
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM HibernateNotificationSendAuditRecord").executeUpdate();
        entityManager.flush();
    }

    /**
     * Helper method to delete a specific record (for testing purposes).
     */
    @Transactional
    public void delete(String contextInstanceId, String contextName, String jobName,
                      String monitorType, String notifierType) {
        String id = generateId(contextInstanceId, contextName, jobName, monitorType, notifierType);
        HibernateNotificationSendAuditRecord record = entityManager.find(HibernateNotificationSendAuditRecord.class, id);
        if (record != null) {
            entityManager.remove(record);
            entityManager.flush();
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
}
