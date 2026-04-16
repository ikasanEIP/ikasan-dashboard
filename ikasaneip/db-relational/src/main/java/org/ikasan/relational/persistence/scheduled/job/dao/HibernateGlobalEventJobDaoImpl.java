package org.ikasan.relational.persistence.scheduled.job.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.job.model.HibernateGlobalEventJobRecord;
import org.ikasan.spec.scheduled.job.dao.GlobalEventJobDao;
import org.ikasan.spec.scheduled.job.model.GlobalEventJob;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Hibernate/PostgreSQL implementation of GlobalEventJobDao.
 *
 * Manages persistence operations for global event job records using JPA Criteria API.
 */
public class HibernateGlobalEventJobDaoImpl implements GlobalEventJobDao<HibernateGlobalEventJobRecord> {

    private static final Logger logger = LoggerFactory.getLogger(HibernateGlobalEventJobDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Constructor for dependency injection
     */
    public HibernateGlobalEventJobDaoImpl() {
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateGlobalEventJobRecord> findAll(int limit, int offset) {
        logger.debug("Finding all GlobalEventJobRecords with limit={}, offset={}", limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateGlobalEventJobRecord> countRoot = countQuery.from(HibernateGlobalEventJobRecord.class);
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateGlobalEventJobRecord> query = cb.createQuery(HibernateGlobalEventJobRecord.class);
        Root<HibernateGlobalEventJobRecord> root = query.from(HibernateGlobalEventJobRecord.class);
        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateGlobalEventJobRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateGlobalEventJobRecord> results = typedQuery.getResultList();

        logger.debug("Found {} GlobalEventJobRecords out of {} total", results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateGlobalEventJobRecord> findByContext(String contextId, int limit, int offset) {
        logger.debug("Finding GlobalEventJobRecords by contextId: {} with limit={}, offset={}",
            contextId, limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateGlobalEventJobRecord> countRoot = countQuery.from(HibernateGlobalEventJobRecord.class);

        if (contextId != null && !contextId.isEmpty()) {
            countQuery.where(cb.equal(countRoot.get("contextName"), contextId));
        }

        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateGlobalEventJobRecord> query = cb.createQuery(HibernateGlobalEventJobRecord.class);
        Root<HibernateGlobalEventJobRecord> root = query.from(HibernateGlobalEventJobRecord.class);

        if (contextId != null && !contextId.isEmpty()) {
            query.where(cb.equal(root.get("contextName"), contextId));
        }

        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateGlobalEventJobRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateGlobalEventJobRecord> results = typedQuery.getResultList();

        logger.debug("Found {} GlobalEventJobRecords out of {} total for context: {}",
            results.size(), totalCount, contextId);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public HibernateGlobalEventJobRecord findById(String id) {
        logger.debug("Finding GlobalEventJobRecord by id: {}", id);
        return entityManager.find(HibernateGlobalEventJobRecord.class, id);
    }

    @Override
    @Transactional
    public void save(HibernateGlobalEventJobRecord record) {
        if (record == null) {
            throw new IllegalArgumentException(
                "GlobalEventJobRecord must non be null!");
        }

        logger.debug("Saving GlobalEventJobRecord: {}", record.getId());

        // Set timestamps if new
        if (record.getTimestamp() == 0) {
            record.setTimestamp(System.currentTimeMillis());
        }
        record.setModifiedTimestamp(System.currentTimeMillis());

        // Merge (will insert or update)
        entityManager.merge(record);

        logger.debug("Successfully saved GlobalEventJobRecord: {}", record.getId());
    }

    @Override
    @Transactional
    public void skip(HibernateGlobalEventJobRecord jobRecord, List<String> childContextNames, String actor) {
        logger.debug("Skipping GlobalEventJobRecord: {} for childContexts: {} by actor: {}",
            jobRecord.getId(), childContextNames, actor);
        GlobalEventJob globalEventJob = jobRecord.getGlobalEventJob();

        if (globalEventJob != null) {
            Map<String, Boolean> skippedContexts = globalEventJob.getSkippedContexts();

            if (childContextNames != null && !childContextNames.isEmpty()) {
                // Skip in specific child contexts
                for (String childContextName : childContextNames) {
                    skippedContexts.put(childContextName, true);
                }
            } else {
                // Skip in all contexts - use the parent context name
                skippedContexts.put(jobRecord.getContextName(), true);
            }

            jobRecord.setModifiedBy(actor);
            jobRecord.setGlobalEventJob(globalEventJob);
            save(jobRecord);

            logger.debug("Successfully skipped GlobalEventJobRecord: {}", jobRecord.getId());
        } else {
            logger.warn("Cannot skip GlobalEventJobRecord: {} - GlobalEventJob is null", jobRecord.getId());
        }
    }

    @Override
    @Transactional
    public void enable(HibernateGlobalEventJobRecord jobRecord, String actor) {
        logger.debug("Enabling GlobalEventJobRecord: {} by actor: {}", jobRecord.getId(), actor);

        GlobalEventJob globalEventJob = jobRecord.getGlobalEventJob();
        if (globalEventJob != null) {
            Map<String, Boolean> skippedContexts = globalEventJob.getSkippedContexts();

            // Clear all skipped contexts to enable the job
            skippedContexts.clear();

            jobRecord.setModifiedBy(actor);
            jobRecord.setGlobalEventJob(globalEventJob);
            save(jobRecord);

            logger.debug("Successfully enabled GlobalEventJobRecord: {}", jobRecord.getId());
        } else {
            logger.warn("Cannot enable GlobalEventJobRecord: {} - GlobalEventJob is null", jobRecord.getId());
        }
    }
}
