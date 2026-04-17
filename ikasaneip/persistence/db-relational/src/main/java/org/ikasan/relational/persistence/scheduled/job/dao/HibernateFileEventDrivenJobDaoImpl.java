package org.ikasan.relational.persistence.scheduled.job.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.job.model.HibernateFileEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.dao.FileEventDrivenJobDao;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Hibernate/PostgreSQL implementation of FileEventDrivenJobDao.
 *
 * Manages persistence operations for file event driven job records using JPA Criteria API.
 */
public class HibernateFileEventDrivenJobDaoImpl implements FileEventDrivenJobDao<HibernateFileEventDrivenJobRecord> {

    private static final Logger logger = LoggerFactory.getLogger(HibernateFileEventDrivenJobDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Constructor for dependency injection
     */
    public HibernateFileEventDrivenJobDaoImpl() {
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateFileEventDrivenJobRecord> findAll(int limit, int offset) {
        logger.debug("Finding all FileEventDrivenJobRecords with limit={}, offset={}", limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateFileEventDrivenJobRecord> countRoot = countQuery.from(HibernateFileEventDrivenJobRecord.class);
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateFileEventDrivenJobRecord> query = cb.createQuery(HibernateFileEventDrivenJobRecord.class);
        Root<HibernateFileEventDrivenJobRecord> root = query.from(HibernateFileEventDrivenJobRecord.class);
        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateFileEventDrivenJobRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateFileEventDrivenJobRecord> results = typedQuery.getResultList();

        logger.debug("Found {} FileEventDrivenJobRecords out of {} total", results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateFileEventDrivenJobRecord> findByContext(String contextId, int limit, int offset) {
        logger.debug("Finding FileEventDrivenJobRecords by contextId: {} with limit={}, offset={}",
            contextId, limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateFileEventDrivenJobRecord> countRoot = countQuery.from(HibernateFileEventDrivenJobRecord.class);

        if (contextId != null && !contextId.isEmpty()) {
            countQuery.where(cb.equal(countRoot.get("contextName"), contextId));
        }

        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateFileEventDrivenJobRecord> query = cb.createQuery(HibernateFileEventDrivenJobRecord.class);
        Root<HibernateFileEventDrivenJobRecord> root = query.from(HibernateFileEventDrivenJobRecord.class);

        if (contextId != null && !contextId.isEmpty()) {
            query.where(cb.equal(root.get("contextName"), contextId));
        }

        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateFileEventDrivenJobRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateFileEventDrivenJobRecord> results = typedQuery.getResultList();

        logger.debug("Found {} FileEventDrivenJobRecords out of {} total for context: {}",
            results.size(), totalCount, contextId);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public HibernateFileEventDrivenJobRecord findById(String id) {
        logger.debug("Finding FileEventDrivenJobRecord by id: {}", id);
        return entityManager.find(HibernateFileEventDrivenJobRecord.class, id);
    }

    @Override
    @Transactional
    public void save(HibernateFileEventDrivenJobRecord record) {
        if (record == null) {
            throw new IllegalArgumentException(
                "FileEventDrivenJobRecord must not be null");
        }

        logger.debug("Saving FileEventDrivenJobRecord: {}", record.getId());

        if (record.getTimestamp() == 0) {
            record.setTimestamp(System.currentTimeMillis());
        }
        record.setModifiedTimestamp(System.currentTimeMillis());

        // Merge (will insert or update)
        entityManager.merge(record);

        logger.debug("Successfully saved FileEventDrivenJobRecord: {}", record.getId());
    }
}
