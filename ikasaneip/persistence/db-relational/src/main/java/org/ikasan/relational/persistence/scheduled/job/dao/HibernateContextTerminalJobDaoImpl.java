package org.ikasan.relational.persistence.scheduled.job.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.job.model.HibernateContextTerminalJobRecord;
import org.ikasan.spec.scheduled.job.dao.ContextTerminalJobDao;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Hibernate/PostgreSQL implementation of ContextTerminalJobDao.
 *
 * Manages persistence operations for context terminal job records using JPA Criteria API.
 */
public class HibernateContextTerminalJobDaoImpl implements ContextTerminalJobDao<HibernateContextTerminalJobRecord> {

    private static final Logger logger = LoggerFactory.getLogger(HibernateContextTerminalJobDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Constructor for dependency injection
     */
    public HibernateContextTerminalJobDaoImpl() {
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateContextTerminalJobRecord> findAll(int limit, int offset) {
        logger.debug("Finding all ContextTerminalJobRecords with limit={}, offset={}", limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateContextTerminalJobRecord> countRoot = countQuery.from(HibernateContextTerminalJobRecord.class);
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateContextTerminalJobRecord> query = cb.createQuery(HibernateContextTerminalJobRecord.class);
        Root<HibernateContextTerminalJobRecord> root = query.from(HibernateContextTerminalJobRecord.class);
        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateContextTerminalJobRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateContextTerminalJobRecord> results = typedQuery.getResultList();

        logger.debug("Found {} ContextTerminalJobRecords out of {} total", results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateContextTerminalJobRecord> findByContext(String contextId, int limit, int offset) {
        logger.debug("Finding ContextTerminalJobRecords by contextId: {} with limit={}, offset={}",
            contextId, limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateContextTerminalJobRecord> countRoot = countQuery.from(HibernateContextTerminalJobRecord.class);

        if (contextId != null && !contextId.isEmpty()) {
            countQuery.where(cb.equal(countRoot.get("contextName"), contextId));
        }

        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateContextTerminalJobRecord> query = cb.createQuery(HibernateContextTerminalJobRecord.class);
        Root<HibernateContextTerminalJobRecord> root = query.from(HibernateContextTerminalJobRecord.class);

        if (contextId != null && !contextId.isEmpty()) {
            query.where(cb.equal(root.get("contextName"), contextId));
        }

        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateContextTerminalJobRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateContextTerminalJobRecord> results = typedQuery.getResultList();

        logger.debug("Found {} ContextTerminalJobRecords out of {} total for context: {}",
            results.size(), totalCount, contextId);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public HibernateContextTerminalJobRecord findById(String id) {
        logger.debug("Finding ContextTerminalJobRecord by id: {}", id);
        return entityManager.find(HibernateContextTerminalJobRecord.class, id);
    }

    @Override
    @Transactional
    public void save(HibernateContextTerminalJobRecord record) {
        if (record == null) {
            throw new IllegalArgumentException(
                "ContextTerminalJobRecord cannot be null!");
        }

        logger.debug("Saving ContextTerminalJobRecord: {}", record.getId());

        HibernateContextTerminalJobRecord hibernateRecord = record;

        // Set timestamps if new
        if (hibernateRecord.getTimestamp() == 0) {
            hibernateRecord.setTimestamp(System.currentTimeMillis());
        }
        hibernateRecord.setModifiedTimestamp(System.currentTimeMillis());

        // Merge (will insert or update)
        entityManager.merge(hibernateRecord);

        logger.debug("Successfully saved ContextTerminalJobRecord: {}", hibernateRecord.getId());
    }
}
