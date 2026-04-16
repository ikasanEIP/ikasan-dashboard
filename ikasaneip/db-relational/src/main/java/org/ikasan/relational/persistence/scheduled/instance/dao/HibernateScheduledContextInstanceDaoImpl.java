package org.ikasan.relational.persistence.scheduled.instance.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.instance.model.HibernateScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceDao;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceSearchFilter;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Hibernate/PostgreSQL implementation of ScheduledContextInstanceDao.
 *
 * Manages persistence operations for scheduled context instance records using JPA Criteria API.
 */
public class HibernateScheduledContextInstanceDaoImpl implements ScheduledContextInstanceDao {

    private static final Logger logger = LoggerFactory.getLogger(HibernateScheduledContextInstanceDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Constructor for dependency injection
     */
    public HibernateScheduledContextInstanceDaoImpl() {
    }

    /**
     * Constructor with EntityManager for testing
     */
    public HibernateScheduledContextInstanceDaoImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduledContextInstanceRecord findById(String id) {
        logger.debug("Finding ScheduledContextInstanceRecord by id: {}", id);
        return entityManager.find(HibernateScheduledContextInstanceRecord.class, id);
    }

    @Override
    @Transactional
    public void deleteById(String id) {
        logger.debug("Deleting ScheduledContextInstanceRecord by id: {}", id);
        ScheduledContextInstanceRecord record = findById(id);
        if (record != null) {
            entityManager.remove(entityManager.contains(record) ? record : entityManager.merge(record));
            logger.debug("Successfully deleted ScheduledContextInstanceRecord: {}", id);
        } else {
            logger.warn("ScheduledContextInstanceRecord not found for deletion: {}", id);
        }
    }

    @Override
    @Transactional
    public void save(ScheduledContextInstanceRecord scheduledContextInstanceRecord) {
        logger.debug("Saving ScheduledContextInstanceRecord: {}", scheduledContextInstanceRecord.getId());

        if (!(scheduledContextInstanceRecord instanceof HibernateScheduledContextInstanceRecord)) {
            throw new IllegalArgumentException(
                "ScheduledContextInstanceRecord must be an instance of HibernateScheduledContextInstanceRecord");
        }

        HibernateScheduledContextInstanceRecord hibernateRecord =
            (HibernateScheduledContextInstanceRecord) scheduledContextInstanceRecord;

        // Set timestamps if new
        if (hibernateRecord.getTimestamp() == 0) {
            hibernateRecord.setTimestamp(System.currentTimeMillis());
        }
        hibernateRecord.setModifiedTimestamp(System.currentTimeMillis());

        // Merge (will insert or update)
        entityManager.merge(hibernateRecord);

        logger.debug("Successfully saved ScheduledContextInstanceRecord: {}", hibernateRecord.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByStatus(
            List<InstanceStatus> instanceStatuses) {
        return getScheduledContextInstancesByStatus(instanceStatuses, -1, -1);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByStatus(
            List<InstanceStatus> instanceStatuses, int limit, int offset) {
        logger.debug("Finding ScheduledContextInstanceRecords by statuses with limit={}, offset={}", limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateScheduledContextInstanceRecord> countRoot =
            countQuery.from(HibernateScheduledContextInstanceRecord.class);

        if (instanceStatuses != null && !instanceStatuses.isEmpty()) {
            List<String> statusStrings = instanceStatuses.stream()
                .map(InstanceStatus::toString)
                .toList();
            countQuery.where(countRoot.get("status").in(statusStrings));
        }

        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateScheduledContextInstanceRecord> query =
            cb.createQuery(HibernateScheduledContextInstanceRecord.class);
        Root<HibernateScheduledContextInstanceRecord> root =
            query.from(HibernateScheduledContextInstanceRecord.class);

        if (instanceStatuses != null && !instanceStatuses.isEmpty()) {
            List<String> statusStrings = instanceStatuses.stream()
                .map(InstanceStatus::toString)
                .toList();
            query.where(root.get("status").in(statusStrings));
        }

        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateScheduledContextInstanceRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateScheduledContextInstanceRecord> results = typedQuery.getResultList();

        logger.debug("Found {} ScheduledContextInstanceRecords out of {} total", results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByContextName(
            String contextName, int limit, int offset, String sortField, String sortDirection) {
        return getScheduledContextInstancesByContextName(contextName, 0, 0, limit, offset, sortField, sortDirection);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByContextName(
            String contextName, long startTimestamp, long endTimestamp,
            int limit, int offset, String sortField, String sortDirection) {
        logger.debug("Finding ScheduledContextInstanceRecords by contextName: {}", contextName);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Build predicates
        List<Predicate> predicates = new ArrayList<>();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateScheduledContextInstanceRecord> countRoot =
            countQuery.from(HibernateScheduledContextInstanceRecord.class);

        buildContextNamePredicates(cb, countRoot, contextName, startTimestamp, endTimestamp, predicates);

        if (!predicates.isEmpty()) {
            countQuery.where(cb.and(predicates.toArray(new Predicate[0])));
        }
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateScheduledContextInstanceRecord> query =
            cb.createQuery(HibernateScheduledContextInstanceRecord.class);
        Root<HibernateScheduledContextInstanceRecord> root =
            query.from(HibernateScheduledContextInstanceRecord.class);

        predicates.clear();
        buildContextNamePredicates(cb, root, contextName, startTimestamp, endTimestamp, predicates);

        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        // Apply sorting
        applySorting(cb, root, query, sortField, sortDirection);

        query.select(root);

        TypedQuery<HibernateScheduledContextInstanceRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateScheduledContextInstanceRecord> results = typedQuery.getResultList();

        logger.debug("Found {} ScheduledContextInstanceRecords out of {} total", results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByFilter(
            ContextInstanceSearchFilter filter, int limit, int offset, String sortField, String sortDirection) {
        logger.debug("Finding ScheduledContextInstanceRecords by filter");

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateScheduledContextInstanceRecord> countRoot =
            countQuery.from(HibernateScheduledContextInstanceRecord.class);
        List<Predicate> countPredicates = buildFilterPredicates(cb, countRoot, filter);

        if (!countPredicates.isEmpty()) {
            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateScheduledContextInstanceRecord> query =
            cb.createQuery(HibernateScheduledContextInstanceRecord.class);
        Root<HibernateScheduledContextInstanceRecord> root =
            query.from(HibernateScheduledContextInstanceRecord.class);

        List<Predicate> dataPredicates = buildFilterPredicates(cb, root, filter);

        if (!dataPredicates.isEmpty()) {
            query.where(cb.and(dataPredicates.toArray(new Predicate[0])));
        }

        // Apply sorting
        applySorting(cb, root, query, sortField, sortDirection);

        query.select(root);

        TypedQuery<HibernateScheduledContextInstanceRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateScheduledContextInstanceRecord> results = typedQuery.getResultList();

        logger.debug("Found {} ScheduledContextInstanceRecords out of {} total matching filter",
            results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    /**
     * Build predicates for context name filtering
     */
    private void buildContextNamePredicates(CriteriaBuilder cb,
                                           Root<HibernateScheduledContextInstanceRecord> root,
                                           String contextName, long startTimestamp, long endTimestamp,
                                           List<Predicate> predicates) {
        if (contextName != null && !contextName.isEmpty()) {
            predicates.add(cb.equal(root.get("contextName"), contextName));
        }

        if (startTimestamp > 0 && endTimestamp > 0) {
            predicates.add(cb.between(root.get("timestamp"), startTimestamp, endTimestamp));
        }
    }

    /**
     * Build predicates for filter-based searching
     */
    private List<Predicate> buildFilterPredicates(CriteriaBuilder cb,
                                                  Root<HibernateScheduledContextInstanceRecord> root,
                                                  ContextInstanceSearchFilter filter) {
        List<Predicate> predicates = new ArrayList<>();

        if (filter == null) {
            return predicates;
        }

        // Context search filter (using LIKE for partial matching)
        if (filter.getContextSearchFilter() != null && !filter.getContextSearchFilter().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("contextName")),
                "%" + filter.getContextSearchFilter().toLowerCase() + "%"));
        }

        // Context instance names (IN clause)
        if (filter.getContextInstanceNames() != null && !filter.getContextInstanceNames().isEmpty()) {
            predicates.add(root.get("contextName").in(filter.getContextInstanceNames()));
        }

        // Context instance ID
        if (filter.getContextInstanceId() != null && !filter.getContextInstanceId().isEmpty()) {
            predicates.add(cb.equal(root.get("contextInstanceId"), filter.getContextInstanceId()));
        }

        // Status
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            predicates.add(cb.equal(root.get("status"), filter.getStatus()));
        }

        // Created timestamp
        if (filter.getCreatedTimestamp() > 0) {
            predicates.add(cb.equal(root.get("timestamp"), filter.getCreatedTimestamp()));
        }

        // Modified timestamp
        if (filter.getModifiedTimestamp() > 0) {
            predicates.add(cb.equal(root.get("modifiedTimestamp"), filter.getModifiedTimestamp()));
        }

        // Start time
        if (filter.getStartTime() > 0) {
            predicates.add(cb.equal(root.get("startTime"), filter.getStartTime()));
        }

        // Start time range
        if (filter.getStartTimeStart() > 0 && filter.getStartTimeEnd() > 0) {
            predicates.add(cb.between(root.get("startTime"),
                filter.getStartTimeStart(), filter.getStartTimeEnd()));
        }

        // End time
        if (filter.getEndTime() > 0) {
            predicates.add(cb.equal(root.get("endTime"), filter.getEndTime()));
        }

        // End time range
        if (filter.getEndTimeStart() > 0 && filter.getEndTimeEnd() > 0) {
            predicates.add(cb.between(root.get("endTime"),
                filter.getEndTimeStart(), filter.getEndTimeEnd()));
        }

        return predicates;
    }

    /**
     * Apply sorting to the query
     */
    private void applySorting(CriteriaBuilder cb, Root<HibernateScheduledContextInstanceRecord> root,
                             CriteriaQuery<HibernateScheduledContextInstanceRecord> query,
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
