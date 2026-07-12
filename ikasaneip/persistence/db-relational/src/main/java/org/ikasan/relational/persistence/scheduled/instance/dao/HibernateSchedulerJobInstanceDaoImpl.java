package org.ikasan.relational.persistence.scheduled.instance.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.instance.model.HibernateSchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.dao.SchedulerJobInstanceDao;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceAggregateJobStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Hibernate/PostgreSQL implementation of SchedulerJobInstanceDao.
 *
 * Manages persistence operations for scheduler job instance records using JPA Criteria API.
 */
public class HibernateSchedulerJobInstanceDaoImpl implements SchedulerJobInstanceDao {

    private static final Logger logger = LoggerFactory.getLogger(HibernateSchedulerJobInstanceDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Constructor for dependency injection
     */
    public HibernateSchedulerJobInstanceDaoImpl() {
    }

    /**
     * Constructor with EntityManager for testing
     */
    public HibernateSchedulerJobInstanceDaoImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public SchedulerJobInstanceRecord findById(String id) {
        logger.debug("Finding SchedulerJobInstanceRecord by id: {}", id);
        return entityManager.find(HibernateSchedulerJobInstanceRecord.class, id);
    }

    @Override
    @Transactional
    public void save(SchedulerJobInstanceRecord schedulerJobInstanceRecord) {
        logger.debug("Saving SchedulerJobInstanceRecord: {}", schedulerJobInstanceRecord.getId());

        if (!(schedulerJobInstanceRecord instanceof HibernateSchedulerJobInstanceRecord)) {
            throw new IllegalArgumentException(
                "SchedulerJobInstanceRecord must be an instance of HibernateSchedulerJobInstanceRecord");
        }

        HibernateSchedulerJobInstanceRecord hibernateRecord =
            (HibernateSchedulerJobInstanceRecord) schedulerJobInstanceRecord;

        // Set timestamps if new
        if (hibernateRecord.getTimestamp() == 0) {
            hibernateRecord.setTimestamp(System.currentTimeMillis());
        }
        hibernateRecord.setModifiedTimestamp(System.currentTimeMillis());

        // Merge (will insert or update)
        entityManager.merge(hibernateRecord);

        logger.debug("Successfully saved SchedulerJobInstanceRecord: {}", hibernateRecord.getId());
    }

    @Override
    @Transactional
    public void save(List<SchedulerJobInstanceRecord> scheduledContextInstanceRecord) {
        scheduledContextInstanceRecord.forEach(this::save);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextInstanceId(
            String contextInstanceId, int limit, int offset, String sortField, String sortDirection) {
        logger.debug("Finding SchedulerJobInstanceRecords by contextInstanceId: {}", contextInstanceId);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateSchedulerJobInstanceRecord> countRoot =
            countQuery.from(HibernateSchedulerJobInstanceRecord.class);
        countQuery.where(cb.equal(countRoot.get("contextInstanceId"), contextInstanceId));
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateSchedulerJobInstanceRecord> query =
            cb.createQuery(HibernateSchedulerJobInstanceRecord.class);
        Root<HibernateSchedulerJobInstanceRecord> root =
            query.from(HibernateSchedulerJobInstanceRecord.class);
        query.where(cb.equal(root.get("contextInstanceId"), contextInstanceId));

        // Apply sorting
        applySorting(cb, root, query, sortField, sortDirection);

        query.select(root);

        TypedQuery<HibernateSchedulerJobInstanceRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateSchedulerJobInstanceRecord> results = typedQuery.getResultList();

        logger.debug("Found {} SchedulerJobInstanceRecords out of {} total", results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextName(
            String contextName, int limit, int offset, String sortField, String sortDirection) {
        logger.debug("Finding SchedulerJobInstanceRecords by contextName: {}", contextName);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateSchedulerJobInstanceRecord> countRoot =
            countQuery.from(HibernateSchedulerJobInstanceRecord.class);
        countQuery.where(cb.equal(countRoot.get("contextName"), contextName));
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateSchedulerJobInstanceRecord> query =
            cb.createQuery(HibernateSchedulerJobInstanceRecord.class);
        Root<HibernateSchedulerJobInstanceRecord> root =
            query.from(HibernateSchedulerJobInstanceRecord.class);
        query.where(cb.equal(root.get("contextName"), contextName));

        // Apply sorting
        applySorting(cb, root, query, sortField, sortDirection);

        query.select(root);

        TypedQuery<HibernateSchedulerJobInstanceRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateSchedulerJobInstanceRecord> results = typedQuery.getResultList();

        logger.debug("Found {} SchedulerJobInstanceRecords out of {} total", results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean doesJobPlanInstanceContainRepeatingJobs(String contextInstanceId) {
        logger.debug("Checking if job plan instance contains repeating jobs: {}", contextInstanceId);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<HibernateSchedulerJobInstanceRecord> root = query.from(HibernateSchedulerJobInstanceRecord.class);

        // This would need to check for specific job types that are repeating
        // For now, return false as a placeholder - implementation depends on job type logic
        query.select(cb.count(root));
        query.where(cb.equal(root.get("contextInstanceId"), contextInstanceId));

        Long count = entityManager.createQuery(query).getSingleResult();
        return count != null && count > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<SchedulerJobInstanceRecord> getScheduledContextInstancesByFilter(
            SchedulerJobInstanceSearchFilter filter, int limit, int offset,
            String sortField, String sortDirection) {
        logger.debug("Finding SchedulerJobInstanceRecords by filter");

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateSchedulerJobInstanceRecord> countRoot =
            countQuery.from(HibernateSchedulerJobInstanceRecord.class);
        List<Predicate> countPredicates = buildFilterPredicates(cb, countRoot, filter);

        if (!countPredicates.isEmpty()) {
            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateSchedulerJobInstanceRecord> query =
            cb.createQuery(HibernateSchedulerJobInstanceRecord.class);
        Root<HibernateSchedulerJobInstanceRecord> root =
            query.from(HibernateSchedulerJobInstanceRecord.class);

        List<Predicate> dataPredicates = buildFilterPredicates(cb, root, filter);

        if (!dataPredicates.isEmpty()) {
            query.where(cb.and(dataPredicates.toArray(new Predicate[0])));
        }

        // Apply sorting
        applySorting(cb, root, query, sortField, sortDirection);

        query.select(root);

        TypedQuery<HibernateSchedulerJobInstanceRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateSchedulerJobInstanceRecord> results = typedQuery.getResultList();

        logger.debug("Found {} SchedulerJobInstanceRecords out of {} total matching filter",
            results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextInstanceAggregateJobStatus> getJobStatusCountForContextInstances(
            List<String> contextInstanceIds) {
        logger.debug("Getting job status counts for context instances");
        // This is a complex aggregation query that would need to be implemented based on requirements
        // Returning empty list as placeholder
        return new ArrayList<>();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContextInstanceAggregateJobStatus> getJobStatusCountForContextInstancesConsiderNonTargetedDuplication(
            List<String> contextInstanceIds) {
        logger.debug("Getting job status counts for context instances considering duplication");
        // This is a complex aggregation query that would need to be implemented based on requirements
        // Returning empty list as placeholder
        return new ArrayList<>();
    }

    @Override
    @Transactional
    public void deleteSchedulerJobInstances(String contextInstanceId) {
        logger.debug("Deleting all scheduler job instances for context instance: {}", contextInstanceId);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaDelete<HibernateSchedulerJobInstanceRecord> delete =
            cb.createCriteriaDelete(HibernateSchedulerJobInstanceRecord.class);
        Root<HibernateSchedulerJobInstanceRecord> root = delete.from(HibernateSchedulerJobInstanceRecord.class);

        delete.where(cb.equal(root.get("contextInstanceId"), contextInstanceId));

        int deletedCount = entityManager.createQuery(delete).executeUpdate();

        logger.debug("Deleted {} scheduler job instances", deletedCount);
    }

    /**
     * Build predicates for filter-based searching
     */
    private List<Predicate> buildFilterPredicates(CriteriaBuilder cb,
                                                  Root<HibernateSchedulerJobInstanceRecord> root,
                                                  SchedulerJobInstanceSearchFilter filter) {
        List<Predicate> predicates = new ArrayList<>();

        if (filter == null) {
            return predicates;
        }

        // Job name
        if (filter.getJobName() != null && !filter.getJobName().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("jobName")),
                "%" + filter.getJobName().toLowerCase() + "%"));
        }

        // Display name filter
        if (filter.getDisplayNameFilter() != null && !filter.getDisplayNameFilter().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("displayName")),
                "%" + filter.getDisplayNameFilter().toLowerCase() + "%"));
        }

        // Job type
        if (filter.getJobType() != null && !filter.getJobType().isEmpty()) {
            predicates.add(cb.equal(root.get("type"), filter.getJobType()));
        }

        // Context name
        if (filter.getContextName() != null && !filter.getContextName().isEmpty()) {
            predicates.add(cb.equal(root.get("contextName"), filter.getContextName()));
        }

        // Context instance ID
        if (filter.getContextInstanceId() != null && !filter.getContextInstanceId().isEmpty()) {
            predicates.add(cb.equal(root.get("contextInstanceId"), filter.getContextInstanceId()));
        }

        // Child context name
        if (filter.getChildContextName() != null && !filter.getChildContextName().isEmpty()) {
            predicates.add(cb.equal(root.get("childContextName"), filter.getChildContextName()));
        }

        // Status
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            predicates.add(cb.equal(root.get("status"), filter.getStatus()));
        }

        // Target residing context only
        if (filter.isTargetResidingContextOnly() != null) {
            predicates.add(cb.equal(root.get("targetResidingContextOnly"),
                filter.isTargetResidingContextOnly()));
        }

        // Participates in lock
        if (filter.isParticipatesInLock() != null) {
            predicates.add(cb.equal(root.get("participatesInLock"), filter.isParticipatesInLock()));
        }

        // Start time window
        if (filter.getStartTimeWindowStart() > 0 && filter.getStartTimeWindowEnd() > 0) {
            predicates.add(cb.between(root.get("startTime"),
                filter.getStartTimeWindowStart(), filter.getStartTimeWindowEnd()));
        }

        // End time window
        if (filter.getEndTimeWindowStart() > 0 && filter.getEndTimeWindowEnd() > 0) {
            predicates.add(cb.between(root.get("endTime"),
                filter.getEndTimeWindowStart(), filter.getEndTimeWindowEnd()));
        }

        return predicates;
    }

    /**
     * Apply sorting to the query
     */
    private void applySorting(CriteriaBuilder cb, Root<HibernateSchedulerJobInstanceRecord> root,
                             CriteriaQuery<HibernateSchedulerJobInstanceRecord> query,
                             String sortField, String sortDirection) {
        if (sortField != null && !sortField.isEmpty()) {
            Order order = "ASCENDING".equalsIgnoreCase(sortDirection)
                ? cb.asc(root.get(sortField))
                : cb.desc(root.get(sortField));
            query.orderBy(order);
        } else {
            // Default sort by modified timestamp descending
            query.orderBy(cb.desc(root.get("modifiedTimestamp")));
        }
    }
}
