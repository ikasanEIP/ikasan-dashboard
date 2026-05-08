package org.ikasan.relational.persistence.scheduled.job.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.job.model.HibernateContextStartJobRecord;
import org.ikasan.spec.scheduled.job.dao.ContextStartJobDao;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Hibernate/PostgreSQL implementation of ContextStartJobDao.
 *
 * Manages persistence operations for context start job records using JPA Criteria API.
 */
public class HibernateContextStartJobDaoImpl implements ContextStartJobDao<HibernateContextStartJobRecord> {

    private static final Logger logger = LoggerFactory.getLogger(HibernateContextStartJobDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Constructor for dependency injection
     */
    public HibernateContextStartJobDaoImpl() {
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateContextStartJobRecord> findAll(int limit, int offset) {
        logger.debug("Finding all ContextStartJobRecords with limit={}, offset={}", limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateContextStartJobRecord> countRoot = countQuery.from(HibernateContextStartJobRecord.class);
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateContextStartJobRecord> query = cb.createQuery(HibernateContextStartJobRecord.class);
        Root<HibernateContextStartJobRecord> root = query.from(HibernateContextStartJobRecord.class);
        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateContextStartJobRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateContextStartJobRecord> results = typedQuery.getResultList();

        logger.debug("Found {} ContextStartJobRecords out of {} total", results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateContextStartJobRecord> findByContext(String contextId, int limit, int offset) {
        logger.debug("Finding ContextStartJobRecords by contextId: {} with limit={}, offset={}",
            contextId, limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateContextStartJobRecord> countRoot = countQuery.from(HibernateContextStartJobRecord.class);

        if (contextId != null && !contextId.isEmpty()) {
            countQuery.where(cb.equal(countRoot.get("contextName"), contextId));
        }

        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateContextStartJobRecord> query = cb.createQuery(HibernateContextStartJobRecord.class);
        Root<HibernateContextStartJobRecord> root = query.from(HibernateContextStartJobRecord.class);

        if (contextId != null && !contextId.isEmpty()) {
            query.where(cb.equal(root.get("contextName"), contextId));
        }

        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateContextStartJobRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateContextStartJobRecord> results = typedQuery.getResultList();

        logger.debug("Found {} ContextStartJobRecords out of {} total for context: {}",
            results.size(), totalCount, contextId);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public HibernateContextStartJobRecord findById(String id) {
        logger.debug("Finding ContextStartJobRecord by id: {}", id);
        return entityManager.find(HibernateContextStartJobRecord.class, id);
    }

    @Override
    @Transactional
    public void save(HibernateContextStartJobRecord record) {
        if (record == null) {
            throw new IllegalArgumentException(
                "ContextStartJobRecord cannot be null!");
        }

        logger.debug("Saving ContextStartJobRecord: {}", record.getId());

        HibernateContextStartJobRecord hibernateRecord = record;

        // Set timestamps if new
        if (hibernateRecord.getTimestamp() == 0) {
            hibernateRecord.setTimestamp(System.currentTimeMillis());
        }
        hibernateRecord.setModifiedTimestamp(System.currentTimeMillis());

        // Merge (will insert or update)
        entityManager.merge(hibernateRecord);

        logger.debug("Successfully saved ContextStartJobRecord: {}", hibernateRecord.getId());
    }

    @Override
    @Transactional
    public void save(List<HibernateContextStartJobRecord> records) {
        records.forEach(this::save);
    }
}
