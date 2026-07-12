package org.ikasan.relational.persistence.scheduled.instance.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.instance.model.HibernateScheduledContextInstanceAuditAggregateRecord;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditAggregateDao;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateRecord;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hibernate/PostgreSQL implementation of ScheduledContextInstanceAuditAggregateDao.
 *
 * Manages persistence operations for scheduled context instance audit aggregate records
 * using JPA Criteria API.
 */
public class HibernateScheduledContextInstanceAuditAggregateDaoImpl implements ScheduledContextInstanceAuditAggregateDao {

    private static final Logger logger = LoggerFactory.getLogger(HibernateScheduledContextInstanceAuditAggregateDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Constructor for dependency injection
     */
    public HibernateScheduledContextInstanceAuditAggregateDaoImpl() {
    }

    /**
     * Constructor with EntityManager for testing
     */
    public HibernateScheduledContextInstanceAuditAggregateDaoImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void save(ScheduledContextInstanceAuditAggregateRecord scheduledContextInstanceAuditAggregateRecord) {
        logger.debug("Saving ScheduledContextInstanceAuditAggregateRecord: {}",
            scheduledContextInstanceAuditAggregateRecord.getId());

        if (!(scheduledContextInstanceAuditAggregateRecord instanceof HibernateScheduledContextInstanceAuditAggregateRecord)) {
            throw new IllegalArgumentException(
                "ScheduledContextInstanceAuditAggregateRecord must be an instance of HibernateScheduledContextInstanceAuditAggregateRecord");
        }

        HibernateScheduledContextInstanceAuditAggregateRecord hibernateRecord =
            (HibernateScheduledContextInstanceAuditAggregateRecord) scheduledContextInstanceAuditAggregateRecord;

        // Set timestamp if not already set
        if (hibernateRecord.getTimestamp() == 0) {
            hibernateRecord.setTimestamp(System.currentTimeMillis());
        }

        // Merge (will insert or update)
        entityManager.merge(hibernateRecord);

        logger.debug("Successfully saved ScheduledContextInstanceAuditAggregateRecord: {}",
            hibernateRecord.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<ScheduledContextInstanceAuditAggregateRecord> findAll(
            int limit, int offset, String sortField, String sortDirection) {
        logger.debug("Finding all ScheduledContextInstanceAuditAggregateRecords with limit={}, offset={}",
            limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateScheduledContextInstanceAuditAggregateRecord> countRoot =
            countQuery.from(HibernateScheduledContextInstanceAuditAggregateRecord.class);
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateScheduledContextInstanceAuditAggregateRecord> query =
            cb.createQuery(HibernateScheduledContextInstanceAuditAggregateRecord.class);
        Root<HibernateScheduledContextInstanceAuditAggregateRecord> root =
            query.from(HibernateScheduledContextInstanceAuditAggregateRecord.class);

        // Apply sorting
        applySorting(cb, root, query, sortField, sortDirection);

        query.select(root);

        TypedQuery<HibernateScheduledContextInstanceAuditAggregateRecord> typedQuery =
            entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateScheduledContextInstanceAuditAggregateRecord> results = typedQuery.getResultList();

        logger.debug("Found {} ScheduledContextInstanceAuditAggregateRecords out of {} total",
            results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<ScheduledContextInstanceAuditAggregateRecord> findScheduledContextInstanceAuditAggregateRecordsByFilter(
            ScheduledContextInstanceAuditAggregateSearchFilter filter,
            int limit, int offset, String sortField, String sortDirection) {
        logger.debug("Finding ScheduledContextInstanceAuditAggregateRecords by filter");

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateScheduledContextInstanceAuditAggregateRecord> countRoot =
            countQuery.from(HibernateScheduledContextInstanceAuditAggregateRecord.class);
        List<Predicate> countPredicates = buildFilterPredicates(cb, countRoot, filter);

        if (!countPredicates.isEmpty()) {
            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateScheduledContextInstanceAuditAggregateRecord> query =
            cb.createQuery(HibernateScheduledContextInstanceAuditAggregateRecord.class);
        Root<HibernateScheduledContextInstanceAuditAggregateRecord> root =
            query.from(HibernateScheduledContextInstanceAuditAggregateRecord.class);

        List<Predicate> dataPredicates = buildFilterPredicates(cb, root, filter);

        if (!dataPredicates.isEmpty()) {
            query.where(cb.and(dataPredicates.toArray(new Predicate[0])));
        }

        // Apply sorting
        applySorting(cb, root, query, sortField, sortDirection);

        query.select(root);

        TypedQuery<HibernateScheduledContextInstanceAuditAggregateRecord> typedQuery =
            entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateScheduledContextInstanceAuditAggregateRecord> results = typedQuery.getResultList();

        logger.debug("Found {} ScheduledContextInstanceAuditAggregateRecords out of {} total matching filter",
            results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Map<String, Integer>> getRepeatingJobStatusCounts(List<String> contextInstanceIds) {
        if (contextInstanceIds == null || contextInstanceIds.isEmpty()) {
            logger.debug("Getting repeating job status counts - no context instances provided");
            return new HashMap<>();
        }

        logger.debug("Getting repeating job status counts for {} context instances", contextInstanceIds.size());

        // Native query to leverage PostgreSQL JSONB capabilities if needed
        // For now, using standard JPA approach
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<HibernateScheduledContextInstanceAuditAggregateRecord> query =
            cb.createQuery(HibernateScheduledContextInstanceAuditAggregateRecord.class);
        Root<HibernateScheduledContextInstanceAuditAggregateRecord> root =
            query.from(HibernateScheduledContextInstanceAuditAggregateRecord.class);

        // Filter by context instance IDs and repeating jobs only
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(root.get("contextInstanceId").in(contextInstanceIds));
        predicates.add(cb.equal(root.get("repeatingJob"), true));

        query.where(cb.and(predicates.toArray(new Predicate[0])));
        query.select(root);

        List<HibernateScheduledContextInstanceAuditAggregateRecord> records =
            entityManager.createQuery(query).getResultList();

        // Build the result map: contextInstanceId -> (status -> count)
        Map<String, Map<String, Integer>> resultMap = new HashMap<>();

        for (HibernateScheduledContextInstanceAuditAggregateRecord record : records) {
            String contextInstanceId = record.getContextInstanceId();
            String status = record.getStatus();

            if (status == null) {
                continue;
            }

            resultMap.computeIfAbsent(contextInstanceId, k -> new HashMap<>());
            Map<String, Integer> statusCounts = resultMap.get(contextInstanceId);
            statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
        }

        logger.debug("Retrieved status counts for {} context instances", resultMap.size());

        return resultMap;
    }

    /**
     * Build predicates for filter-based searching
     */
    private List<Predicate> buildFilterPredicates(
            CriteriaBuilder cb,
            Root<HibernateScheduledContextInstanceAuditAggregateRecord> root,
            ScheduledContextInstanceAuditAggregateSearchFilter filter) {
        List<Predicate> predicates = new ArrayList<>();

        if (filter == null) {
            return predicates;
        }

        // Context name
        if (filter.getContextName() != null && !filter.getContextName().isEmpty()) {
            predicates.add(cb.equal(root.get("contextName"), filter.getContextName()));
        }

        // Context instance ID
        if (filter.getContextInstanceId() != null && !filter.getContextInstanceId().isEmpty()) {
            predicates.add(cb.equal(root.get("contextInstanceId"), filter.getContextInstanceId()));
        }

        // Scheduled process event name
        if (filter.getScheduledProcessEventName() != null && !filter.getScheduledProcessEventName().isEmpty()) {
            predicates.add(cb.equal(root.get("scheduledProcessEventName"),
                filter.getScheduledProcessEventName()));
        }

        // Raised initiation event name (search in raisedEvents text field)
        if (filter.getRaisedInitiationEventName() != null && !filter.getRaisedInitiationEventName().isEmpty()) {
            predicates.add(cb.like(root.get("raisedEvents"),
                "%" + filter.getRaisedInitiationEventName() + "%"));
        }

        // Status
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            predicates.add(cb.equal(root.get("status"), filter.getStatus()));
        }

        return predicates;
    }

    /**
     * Apply sorting to the query
     */
    private void applySorting(
            CriteriaBuilder cb,
            Root<HibernateScheduledContextInstanceAuditAggregateRecord> root,
            CriteriaQuery<HibernateScheduledContextInstanceAuditAggregateRecord> query,
            String sortField, String sortDirection) {
        if (sortField != null && !sortField.isEmpty()) {
            Order order = "ASCENDING".equalsIgnoreCase(sortDirection)
                ? cb.asc(root.get(sortField))
                : cb.desc(root.get(sortField));
            query.orderBy(order);
        } else {
            // Default sort by timestamp descending
            query.orderBy(cb.desc(root.get("timestamp")));
        }
    }
}
