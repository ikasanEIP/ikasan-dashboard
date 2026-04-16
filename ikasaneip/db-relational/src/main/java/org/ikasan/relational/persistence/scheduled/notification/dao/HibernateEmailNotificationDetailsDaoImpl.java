package org.ikasan.relational.persistence.scheduled.notification.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.notification.model.HibernateEmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationDetailsDao;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import org.ikasan.spec.search.SearchResults;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Hibernate implementation of EmailNotificationDetailsDao for PostgreSQL persistence.
 *
 * This DAO provides CRUD operations for email notification details records including:
 * - Save/update operations (upsert based on composite ID)
 * - Find operations with pagination
 * - Context name-based filtering
 * - Job name and monitor type lookups
 * - Deletion by context name or composite key
 */
public class HibernateEmailNotificationDetailsDaoImpl implements EmailNotificationDetailsDao {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Saves or updates an email notification details record.
     *
     * Uses merge for upsert behavior - if a record with the same ID exists,
     * it will be updated; otherwise, a new record is created.
     *
     * @param emailNotificationDetailsRecord the record to save
     * @throws IllegalArgumentException if record is null or not a Hibernate type
     */
    @Override
    @Transactional
    public void save(EmailNotificationDetailsRecord emailNotificationDetailsRecord) {
        if (emailNotificationDetailsRecord == null) {
            throw new IllegalArgumentException("emailNotificationDetailsRecord cannot be null!");
        }
        if (!(emailNotificationDetailsRecord instanceof HibernateEmailNotificationDetailsRecord)) {
            throw new IllegalArgumentException(
                "emailNotificationDetailsRecord must be an instance of HibernateEmailNotificationDetailsRecord!");
        }

        HibernateEmailNotificationDetailsRecord hibernateRecord =
            (HibernateEmailNotificationDetailsRecord) emailNotificationDetailsRecord;

        entityManager.merge(hibernateRecord);
        entityManager.flush();
    }

    @Override
    @Transactional
    public void save(List<EmailNotificationDetailsRecord> emailNotificationDetailsRecords) {
        emailNotificationDetailsRecords.forEach(this::save);
    }

    /**
     * Retrieves all email notification details records with pagination.
     *
     * @param limit the maximum number of records to return (0 or negative means no limit)
     * @param offset the number of records to skip
     * @return search results containing records and total count
     */
    @Override
    public SearchResults<EmailNotificationDetailsRecord> findAll(int limit, int offset) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateEmailNotificationDetailsRecord> countRoot = countQuery.from(HibernateEmailNotificationDetailsRecord.class);
        countQuery.select(cb.count(countRoot));
        long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Results query
        CriteriaQuery<HibernateEmailNotificationDetailsRecord> query = cb.createQuery(HibernateEmailNotificationDetailsRecord.class);
        Root<HibernateEmailNotificationDetailsRecord> root = query.from(HibernateEmailNotificationDetailsRecord.class);
        query.select(root);
        query.orderBy(cb.asc(root.get("id")));

        TypedQuery<HibernateEmailNotificationDetailsRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateEmailNotificationDetailsRecord> results = typedQuery.getResultList();

        return new SearchResultsImpl<>(
            results.stream()
                .map(EmailNotificationDetailsRecord.class::cast)
                .toList(),
            totalCount,
            0L
        );
    }

    /**
     * Retrieves email notification details records by context name with pagination.
     *
     * @param contextName the context name to search for
     * @param limit the maximum number of records to return
     * @param offset the number of records to skip
     * @return search results containing matching records and total count
     */
    @Override
    public SearchResults<EmailNotificationDetailsRecord> findByContextName(String contextName, int limit, int offset) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateEmailNotificationDetailsRecord> countRoot = countQuery.from(HibernateEmailNotificationDetailsRecord.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(cb.equal(countRoot.get("contextName"), contextName));
        long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Results query
        CriteriaQuery<HibernateEmailNotificationDetailsRecord> query = cb.createQuery(HibernateEmailNotificationDetailsRecord.class);
        Root<HibernateEmailNotificationDetailsRecord> root = query.from(HibernateEmailNotificationDetailsRecord.class);
        query.select(root);
        query.where(cb.equal(root.get("contextName"), contextName));
        query.orderBy(cb.asc(root.get("id")));

        TypedQuery<HibernateEmailNotificationDetailsRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateEmailNotificationDetailsRecord> results = typedQuery.getResultList();

        return new SearchResultsImpl<>(
            results.stream()
                .map(EmailNotificationDetailsRecord.class::cast)
                .toList(),
            totalCount,
            0L
        );
    }

    /**
     * Retrieves a specific email notification details record by job name and monitor type.
     *
     * @param jobName the job name
     * @param childContextName the child context name
     * @param monitorType the monitor type
     * @return the matching record, or null if not found
     */
    @Override
    public EmailNotificationDetailsRecord findByJobNameAndMonitorType(String jobName, String childContextName, String monitorType) {
        String id = generateId(jobName, childContextName, monitorType);
        return entityManager.find(HibernateEmailNotificationDetailsRecord.class, id);
    }

    /**
     * Deletes email notification details record(s) by context name.
     *
     * @param contextName the context name of the record(s) to delete
     */
    @Override
    @Transactional
    public void deleteByContextName(String contextName) {
        entityManager.createQuery(
            "DELETE FROM HibernateEmailNotificationDetailsRecord r WHERE r.contextName = :contextName")
            .setParameter("contextName", contextName)
            .executeUpdate();
        entityManager.flush();
    }

    /**
     * Deletes a specific email notification details record by job name and monitor type.
     *
     * @param jobName the job name
     * @param childContextName the child context name
     * @param monitorType the monitor type
     */
    @Override
    @Transactional
    public void deleteByJobNameAndMonitorType(String jobName, String childContextName, String monitorType) {
        String id = generateId(jobName, childContextName, monitorType);
        HibernateEmailNotificationDetailsRecord record = entityManager.find(HibernateEmailNotificationDetailsRecord.class, id);
        if (record != null) {
            entityManager.remove(record);
            entityManager.flush();
        }
    }

    /**
     * Helper method to delete all records (for testing purposes).
     */
    @Transactional
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM HibernateEmailNotificationDetailsRecord").executeUpdate();
        entityManager.flush();
    }

    /**
     * Generates ID from jobName, childContextName, and monitorType.
     * Format: jobName_childContextName_monitorType
     */
    private static String generateId(String jobName, String childContextName, String monitorType) {
        return jobName + "_" + childContextName + "_" + monitorType;
    }
}
