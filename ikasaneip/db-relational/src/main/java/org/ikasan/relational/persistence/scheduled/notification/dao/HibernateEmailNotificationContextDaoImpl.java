package org.ikasan.relational.persistence.scheduled.notification.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.notification.model.HibernateEmailNotificationContextRecord;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationContextDao;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContextRecord;
import org.ikasan.spec.search.SearchResults;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Hibernate implementation of EmailNotificationContextDao for PostgreSQL persistence.
 *
 * This DAO provides CRUD operations for email notification context records including:
 * - Save/update operations (upsert based on contextName)
 * - Find operations with pagination
 * - Context name-based filtering
 * - Deletion by context name
 */
public class HibernateEmailNotificationContextDaoImpl implements EmailNotificationContextDao {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Saves or updates an email notification context record.
     *
     * Uses merge for upsert behavior - if a record with the same contextName exists,
     * it will be updated; otherwise, a new record is created.
     *
     * @param emailNotificationContextRecord the record to save
     * @throws IllegalArgumentException if record is null or not a Hibernate type
     */
    @Override
    @Transactional
    public void save(EmailNotificationContextRecord emailNotificationContextRecord) {
        if (emailNotificationContextRecord == null) {
            throw new IllegalArgumentException("emailNotificationContextRecord cannot be null!");
        }
        if (!(emailNotificationContextRecord instanceof HibernateEmailNotificationContextRecord)) {
            throw new IllegalArgumentException(
                "emailNotificationContextRecord must be an instance of HibernateEmailNotificationContextRecord!");
        }

        HibernateEmailNotificationContextRecord hibernateRecord =
            (HibernateEmailNotificationContextRecord) emailNotificationContextRecord;

        entityManager.merge(hibernateRecord);
        entityManager.flush();
    }

    /**
     * Retrieves all email notification context records with pagination.
     *
     * @param limit the maximum number of records to return (0 or negative means no limit)
     * @param offset the number of records to skip
     * @return search results containing records and total count
     */
    @Override
    public SearchResults<EmailNotificationContextRecord> findAll(int limit, int offset) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateEmailNotificationContextRecord> countRoot = countQuery.from(HibernateEmailNotificationContextRecord.class);
        countQuery.select(cb.count(countRoot));
        long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Results query
        CriteriaQuery<HibernateEmailNotificationContextRecord> query = cb.createQuery(HibernateEmailNotificationContextRecord.class);
        Root<HibernateEmailNotificationContextRecord> root = query.from(HibernateEmailNotificationContextRecord.class);
        query.select(root);
        query.orderBy(cb.asc(root.get("contextName")));

        TypedQuery<HibernateEmailNotificationContextRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateEmailNotificationContextRecord> results = typedQuery.getResultList();

        return new SearchResultsImpl<>(
            results.stream()
                .map(EmailNotificationContextRecord.class::cast)
                .toList(),
            totalCount,
            0L
        );
    }

    /**
     * Retrieves email notification context records by context name with pagination.
     *
     * Since contextName is the primary key, this will return at most one result.
     *
     * @param contextName the context name to search for
     * @param limit the maximum number of records to return
     * @param offset the number of records to skip
     * @return search results containing matching records and total count
     */
    @Override
    public SearchResults<EmailNotificationContextRecord> findByContextName(String contextName, int limit, int offset) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateEmailNotificationContextRecord> countRoot = countQuery.from(HibernateEmailNotificationContextRecord.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(cb.equal(countRoot.get("contextName"), contextName));
        long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Results query
        CriteriaQuery<HibernateEmailNotificationContextRecord> query = cb.createQuery(HibernateEmailNotificationContextRecord.class);
        Root<HibernateEmailNotificationContextRecord> root = query.from(HibernateEmailNotificationContextRecord.class);
        query.select(root);
        query.where(cb.equal(root.get("contextName"), contextName));

        TypedQuery<HibernateEmailNotificationContextRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateEmailNotificationContextRecord> results = typedQuery.getResultList();

        return new SearchResultsImpl<>(
            results.stream()
                .map(EmailNotificationContextRecord.class::cast)
                .toList(),
            totalCount,
            0L
        );
    }

    /**
     * Deletes email notification context record(s) by context name.
     *
     * @param contextName the context name of the record(s) to delete
     */
    @Override
    @Transactional
    public void deleteByContextName(String contextName) {
        entityManager.createQuery(
            "DELETE FROM HibernateEmailNotificationContextRecord r WHERE r.contextName = :contextName")
            .setParameter("contextName", contextName)
            .executeUpdate();
        entityManager.flush();
    }
}
