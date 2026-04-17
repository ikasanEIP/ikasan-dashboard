package org.ikasan.relational.persistence.scheduled.job.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.job.model.HibernateInternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.dao.InternalEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Hibernate/PostgreSQL implementation of InternalEventDrivenJobDao.
 *
 * Manages persistence operations for internal event driven job records using JPA Criteria API.
 */
public class HibernateInternalEventDrivenJobDaoImpl implements InternalEventDrivenJobDao<HibernateInternalEventDrivenJobRecord> {

    private static final Logger logger = LoggerFactory.getLogger(HibernateInternalEventDrivenJobDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Constructor for dependency injection
     */
    public HibernateInternalEventDrivenJobDaoImpl() {
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateInternalEventDrivenJobRecord> findAll(int limit, int offset) {
        logger.debug("Finding all InternalEventDrivenJobRecords with limit={}, offset={}", limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateInternalEventDrivenJobRecord> countRoot = countQuery.from(HibernateInternalEventDrivenJobRecord.class);
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateInternalEventDrivenJobRecord> query = cb.createQuery(HibernateInternalEventDrivenJobRecord.class);
        Root<HibernateInternalEventDrivenJobRecord> root = query.from(HibernateInternalEventDrivenJobRecord.class);
        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateInternalEventDrivenJobRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateInternalEventDrivenJobRecord> results = typedQuery.getResultList();

        logger.debug("Found {} InternalEventDrivenJobRecords out of {} total", results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateInternalEventDrivenJobRecord> findByContext(String contextId, int limit, int offset) {
        logger.debug("Finding InternalEventDrivenJobRecords by contextId: {} with limit={}, offset={}",
            contextId, limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateInternalEventDrivenJobRecord> countRoot = countQuery.from(HibernateInternalEventDrivenJobRecord.class);

        if (contextId != null && !contextId.isEmpty()) {
            countQuery.where(cb.equal(countRoot.get("contextName"), contextId));
        }

        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateInternalEventDrivenJobRecord> query = cb.createQuery(HibernateInternalEventDrivenJobRecord.class);
        Root<HibernateInternalEventDrivenJobRecord> root = query.from(HibernateInternalEventDrivenJobRecord.class);

        if (contextId != null && !contextId.isEmpty()) {
            query.where(cb.equal(root.get("contextName"), contextId));
        }

        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateInternalEventDrivenJobRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateInternalEventDrivenJobRecord> results = typedQuery.getResultList();

        logger.debug("Found {} InternalEventDrivenJobRecords out of {} total for context: {}",
            results.size(), totalCount, contextId);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public HibernateInternalEventDrivenJobRecord findById(String id) {
        logger.debug("Finding InternalEventDrivenJobRecord by id: {}", id);
        return entityManager.find(HibernateInternalEventDrivenJobRecord.class, id);
    }

    @Override
    @Transactional
    public void save(HibernateInternalEventDrivenJobRecord record) {
        if (record == null) {
            throw new IllegalArgumentException(
                "InternalEventDrivenJobRecord must non be null!");
        }

        logger.debug("Saving InternalEventDrivenJobRecord: {}", record.getId());

        HibernateInternalEventDrivenJobRecord hibernateRecord = record;

        // Set timestamps if new
        if (hibernateRecord.getTimestamp() == 0) {
            hibernateRecord.setTimestamp(System.currentTimeMillis());
        }
        hibernateRecord.setModifiedTimestamp(System.currentTimeMillis());

        // Merge (will insert or update)
        entityManager.merge(hibernateRecord);

        logger.debug("Successfully saved InternalEventDrivenJobRecord: {}", hibernateRecord.getId());
    }

    @Override
    @Transactional
    public void skip(HibernateInternalEventDrivenJobRecord jobRecord, List<String> childContextNames, String actor) {
        logger.debug("Skipping InternalEventDrivenJobRecord: {} for childContexts: {} by actor: {}",
            jobRecord.getId(), childContextNames, actor);

        if (jobRecord.getInternalEventDrivenJob() != null) {
            InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();
            internalEventDrivenJob.setSkippedContexts(new HashMap<>());

            if (internalEventDrivenJob.isTargetResidingContextOnly() && childContextNames != null) {
                childContextNames.forEach(name ->
                    internalEventDrivenJob.getSkippedContexts().put(name, true));
            } else if (internalEventDrivenJob.getChildContextNames() != null) {
                internalEventDrivenJob.getChildContextNames().forEach(name ->
                    internalEventDrivenJob.getSkippedContexts().put(name, true));
            }

            jobRecord.setSkipped(true);
            jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
            jobRecord.setModifiedBy(actor);

            save(jobRecord);

            logger.debug("Successfully skipped InternalEventDrivenJobRecord: {}", jobRecord.getId());
        } else {
            logger.warn("Cannot skip InternalEventDrivenJobRecord: {} - InternalEventDrivenJob is null", jobRecord.getId());
        }
    }

    @Override
    @Transactional
    public void hold(HibernateInternalEventDrivenJobRecord jobRecord, List<String> childContextNames, String actor) {
        logger.debug("Holding InternalEventDrivenJobRecord: {} for childContexts: {} by actor: {}",
            jobRecord.getId(), childContextNames, actor);

        if (jobRecord.getInternalEventDrivenJob() != null) {
            InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();
            internalEventDrivenJob.setHeldContexts(new HashMap<>());

            if (internalEventDrivenJob.isTargetResidingContextOnly() && childContextNames != null) {
                childContextNames.forEach(name ->
                    internalEventDrivenJob.getHeldContexts().put(name, true));
            } else if (internalEventDrivenJob.getChildContextNames() != null) {
                internalEventDrivenJob.getChildContextNames().forEach(name ->
                    internalEventDrivenJob.getHeldContexts().put(name, true));
            }

            jobRecord.setHeld(true);
            jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
            jobRecord.setModifiedBy(actor);

            save(jobRecord);

            logger.debug("Successfully held InternalEventDrivenJobRecord: {}", jobRecord.getId());
        } else {
            logger.warn("Cannot hold InternalEventDrivenJobRecord: {} - InternalEventDrivenJob is null", jobRecord.getId());
        }
    }

    @Override
    @Transactional
    public void enable(HibernateInternalEventDrivenJobRecord jobRecord, String actor) {
        logger.debug("Enabling InternalEventDrivenJobRecord: {} by actor: {}", jobRecord.getId(), actor);

        if (jobRecord.getInternalEventDrivenJob() != null) {
            InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();
            internalEventDrivenJob.setSkippedContexts(new HashMap<>());

            jobRecord.setSkipped(false);
            jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
            jobRecord.setModifiedBy(actor);

            save(jobRecord);

            logger.debug("Successfully enabled InternalEventDrivenJobRecord: {}", jobRecord.getId());
        } else {
            logger.warn("Cannot enable InternalEventDrivenJobRecord: {} - InternalEventDrivenJob is null", jobRecord.getId());
        }
    }

    @Override
    @Transactional
    public void release(HibernateInternalEventDrivenJobRecord jobRecord, String actor) {
        logger.debug("Releasing InternalEventDrivenJobRecord: {} by actor: {}", jobRecord.getId(), actor);

        if (jobRecord.getInternalEventDrivenJob() != null) {
            InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();
            internalEventDrivenJob.setHeldContexts(new HashMap<>());

            jobRecord.setHeld(false);
            jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
            jobRecord.setModifiedBy(actor);

            save(jobRecord);

            logger.debug("Successfully released InternalEventDrivenJobRecord: {}", jobRecord.getId());
        } else {
            logger.warn("Cannot release InternalEventDrivenJobRecord: {} - InternalEventDrivenJob is null", jobRecord.getId());
        }
    }

    @Override
    @Transactional
    public void releaseAll(List<HibernateInternalEventDrivenJobRecord> jobRecords, String actor) {
        logger.debug("Releasing {} InternalEventDrivenJobRecords by actor: {}", jobRecords.size(), actor);

        jobRecords.forEach(jobRecord -> {
            if (jobRecord.getInternalEventDrivenJob() != null) {
                InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();
                internalEventDrivenJob.setHeldContexts(new HashMap<>());
                jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
                jobRecord.setHeld(false);
                jobRecord.setModifiedBy(actor);
            }
        });

        jobRecords.forEach(this::save);

        logger.debug("Successfully released {} InternalEventDrivenJobRecords", jobRecords.size());
    }

    @Override
    @Transactional
    public void holdAll(List<HibernateInternalEventDrivenJobRecord> jobRecords, String actor) {
        logger.debug("Holding {} InternalEventDrivenJobRecords by actor: {}", jobRecords.size(), actor);

        jobRecords.forEach(jobRecord -> {
            if (jobRecord.getInternalEventDrivenJob() != null) {
                InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();

                if (internalEventDrivenJob.getChildContextNames() != null) {
                    HashMap<String, Boolean> heldContexts = new HashMap<>();
                    internalEventDrivenJob.getChildContextNames()
                        .forEach(name -> heldContexts.put(name, Boolean.TRUE));
                    internalEventDrivenJob.setHeldContexts(heldContexts);
                }

                internalEventDrivenJob.setSkippedContexts(new HashMap<>());
                jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
                jobRecord.setSkipped(false);
                jobRecord.setHeld(true);
                jobRecord.setModifiedBy(actor);
            }
        });

        jobRecords.forEach(this::save);

        logger.debug("Successfully held {} InternalEventDrivenJobRecords", jobRecords.size());
    }

    @Override
    @Transactional
    public void enableAll(List<HibernateInternalEventDrivenJobRecord> jobRecords, String actor) {
        logger.debug("Enabling {} InternalEventDrivenJobRecords by actor: {}", jobRecords.size(), actor);

        jobRecords.forEach(jobRecord -> {
            if (jobRecord.getInternalEventDrivenJob() != null) {
                InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();
                internalEventDrivenJob.setSkippedContexts(new HashMap<>());
                jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
                jobRecord.setSkipped(false);
                jobRecord.setModifiedBy(actor);
            }
        });

        jobRecords.forEach(this::save);

        logger.debug("Successfully enabled {} InternalEventDrivenJobRecords", jobRecords.size());
    }
}
