package org.ikasan.relational.persistence.scheduled.job.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.job.model.HibernateBridgingJobRecord;
import org.ikasan.spec.scheduled.job.dao.BridgingJobDao;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Hibernate/PostgreSQL implementation of BridgingJobDao.
 *
 * Manages persistence operations for bridging job records using JPA Criteria API.
 */
public class HibernateBridgingJobDaoImpl implements BridgingJobDao<HibernateBridgingJobRecord> {

    private static final Logger logger = LoggerFactory.getLogger(HibernateBridgingJobDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Constructor for dependency injection
     */
    public HibernateBridgingJobDaoImpl() {
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateBridgingJobRecord> findAll(int limit, int offset) {
        logger.debug("Finding all BridgingJobRecords with limit={}, offset={}", limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateBridgingJobRecord> countRoot = countQuery.from(HibernateBridgingJobRecord.class);
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateBridgingJobRecord> query = cb.createQuery(HibernateBridgingJobRecord.class);
        Root<HibernateBridgingJobRecord> root = query.from(HibernateBridgingJobRecord.class);
        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateBridgingJobRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateBridgingJobRecord> results = typedQuery.getResultList();

        logger.debug("Found {} BridgingJobRecords out of {} total", results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateBridgingJobRecord> findByContext(String contextId, int limit, int offset) {
        logger.debug("Finding BridgingJobRecords by contextId: {} with limit={}, offset={}",
            contextId, limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateBridgingJobRecord> countRoot = countQuery.from(HibernateBridgingJobRecord.class);

        if (contextId != null && !contextId.isEmpty()) {
            countQuery.where(cb.equal(countRoot.get("contextName"), contextId));
        }

        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateBridgingJobRecord> query = cb.createQuery(HibernateBridgingJobRecord.class);
        Root<HibernateBridgingJobRecord> root = query.from(HibernateBridgingJobRecord.class);

        if (contextId != null && !contextId.isEmpty()) {
            query.where(cb.equal(root.get("contextName"), contextId));
        }

        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateBridgingJobRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateBridgingJobRecord> results = typedQuery.getResultList();

        logger.debug("Found {} BridgingJobRecords out of {} total for context: {}",
            results.size(), totalCount, contextId);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public HibernateBridgingJobRecord findById(String id) {
        logger.debug("Finding BridgingJobRecord by id: {}", id);
        return entityManager.find(HibernateBridgingJobRecord.class, id);
    }

    @Override
    @Transactional
    public void save(HibernateBridgingJobRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("BridgingJobRecord cannot be null!");
        }

        logger.debug("Saving BridgingJobRecord: {}", record.getId());

        HibernateBridgingJobRecord hibernateRecord = record;

        // Set timestamps if new
        if (hibernateRecord.getTimestamp() == 0) {
            hibernateRecord.setTimestamp(System.currentTimeMillis());
        }
        hibernateRecord.setModifiedTimestamp(System.currentTimeMillis());

        // Merge (will insert or update)
        entityManager.merge(hibernateRecord);

        logger.debug("Successfully saved BridgingJobRecord: {}", hibernateRecord.getId());
    }

    @Override
    @Transactional
    public void save(List<HibernateBridgingJobRecord> records) {
        records.forEach(this::save);
    }

    @Transactional
    public void delete(HibernateBridgingJobRecord record) {
        entityManager.remove(record);
    }
}
