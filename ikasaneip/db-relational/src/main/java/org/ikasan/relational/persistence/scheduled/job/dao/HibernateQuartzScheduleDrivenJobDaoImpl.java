package org.ikasan.relational.persistence.scheduled.job.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.job.model.HibernateQuartzScheduleDrivenJobRecord;
import org.ikasan.spec.scheduled.job.dao.QuartzScheduleDrivenJobDao;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Hibernate/PostgreSQL implementation of QuartzScheduleDrivenJobDao.
 *
 * Manages persistence operations for quartz schedule driven job records using JPA Criteria API.
 */
public class HibernateQuartzScheduleDrivenJobDaoImpl implements QuartzScheduleDrivenJobDao<HibernateQuartzScheduleDrivenJobRecord> {

    private static final Logger logger = LoggerFactory.getLogger(HibernateQuartzScheduleDrivenJobDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Constructor for dependency injection
     */
    public HibernateQuartzScheduleDrivenJobDaoImpl() {
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateQuartzScheduleDrivenJobRecord> findAll(int limit, int offset) {
        logger.debug("Finding all QuartzScheduleDrivenJobRecords with limit={}, offset={}", limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateQuartzScheduleDrivenJobRecord> countRoot = countQuery.from(HibernateQuartzScheduleDrivenJobRecord.class);
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateQuartzScheduleDrivenJobRecord> query = cb.createQuery(HibernateQuartzScheduleDrivenJobRecord.class);
        Root<HibernateQuartzScheduleDrivenJobRecord> root = query.from(HibernateQuartzScheduleDrivenJobRecord.class);
        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateQuartzScheduleDrivenJobRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateQuartzScheduleDrivenJobRecord> results = typedQuery.getResultList();

        logger.debug("Found {} QuartzScheduleDrivenJobRecords out of {} total", results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateQuartzScheduleDrivenJobRecord> findByContext(String contextId, int limit, int offset) {
        logger.debug("Finding QuartzScheduleDrivenJobRecords by contextId: {} with limit={}, offset={}",
            contextId, limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateQuartzScheduleDrivenJobRecord> countRoot = countQuery.from(HibernateQuartzScheduleDrivenJobRecord.class);

        if (contextId != null && !contextId.isEmpty()) {
            countQuery.where(cb.equal(countRoot.get("contextName"), contextId));
        }

        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateQuartzScheduleDrivenJobRecord> query = cb.createQuery(HibernateQuartzScheduleDrivenJobRecord.class);
        Root<HibernateQuartzScheduleDrivenJobRecord> root = query.from(HibernateQuartzScheduleDrivenJobRecord.class);

        if (contextId != null && !contextId.isEmpty()) {
            query.where(cb.equal(root.get("contextName"), contextId));
        }

        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateQuartzScheduleDrivenJobRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateQuartzScheduleDrivenJobRecord> results = typedQuery.getResultList();

        logger.debug("Found {} QuartzScheduleDrivenJobRecords out of {} total for context: {}",
            results.size(), totalCount, contextId);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public HibernateQuartzScheduleDrivenJobRecord findById(String id) {
        logger.debug("Finding QuartzScheduleDrivenJobRecord by id: {}", id);
        return entityManager.find(HibernateQuartzScheduleDrivenJobRecord.class, id);
    }

    @Override
    @Transactional
    public void save(HibernateQuartzScheduleDrivenJobRecord record) {
        if (record == null) {
            throw new IllegalArgumentException(
                "QuartzScheduleDrivenJobRecord must not be null!");
        }

        logger.debug("Saving QuartzScheduleDrivenJobRecord: {}", record.getId());

        HibernateQuartzScheduleDrivenJobRecord hibernateRecord = record;

        // Set timestamps if new
        if (hibernateRecord.getTimestamp() == 0) {
            hibernateRecord.setTimestamp(System.currentTimeMillis());
        }
        hibernateRecord.setModifiedTimestamp(System.currentTimeMillis());

        // Merge (will insert or update)
        entityManager.merge(hibernateRecord);

        logger.debug("Successfully saved QuartzScheduleDrivenJobRecord: {}", hibernateRecord.getId());
    }
}
