package org.ikasan.relational.persistence.scheduled.job.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.dao.SchedulerJobDao;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.SchedulerJobSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Hibernate/PostgreSQL implementation of SchedulerJobDao.
 *
 * This is a polymorphic DAO that can query any type of SchedulerJob.
 * Note: Save operations are not supported - use specific job DAOs instead.
 */
public class HibernateSchedulerJobDaoImpl implements SchedulerJobDao<HibernateSchedulerJobRecord> {

    private static final Logger logger = LoggerFactory.getLogger(HibernateSchedulerJobDaoImpl.class);

    private static final List<String> ALL_JOB_TYPES = Arrays.asList(
        JobConstants.FILE_EVENT_DRIVEN_JOB,
        JobConstants.INTERNAL_EVENT_DRIVEN_JOB,
        JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB,
        JobConstants.GLOBAL_EVENT_JOB,
        JobConstants.CONTEXT_START_JOB,
        JobConstants.CONTEXT_TERMINAL_JOB,
        JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE
    );

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Constructor for dependency injection
     */
    public HibernateSchedulerJobDaoImpl() {
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateSchedulerJobRecord> findAll(int limit, int offset) {
        logger.debug("Finding all SchedulerJobRecords with limit={}, offset={}", limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateSchedulerJobRecord> countRoot = countQuery.from(HibernateSchedulerJobRecord.class);
        countQuery.where(countRoot.get("type").in(ALL_JOB_TYPES));
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateSchedulerJobRecord> query = cb.createQuery(HibernateSchedulerJobRecord.class);
        Root<HibernateSchedulerJobRecord> root = query.from(HibernateSchedulerJobRecord.class);
        query.where(root.get("type").in(ALL_JOB_TYPES));
        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateSchedulerJobRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateSchedulerJobRecord> results = typedQuery.getResultList();

        logger.debug("Found {} SchedulerJobRecords out of {} total", results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateSchedulerJobRecord> findByContext(String contextName, int limit, int offset) {
        logger.debug("Finding SchedulerJobRecords by contextName: {} with limit={}, offset={}",
            contextName, limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateSchedulerJobRecord> countRoot = countQuery.from(HibernateSchedulerJobRecord.class);

        Predicate typePredicate = countRoot.get("type").in(ALL_JOB_TYPES);
        Predicate contextPredicate = cb.or(
            cb.equal(countRoot.get("contextName"), contextName),
            cb.equal(countRoot.get("contextName"), JobConstants.GLOBAL_EVENT)
        );

        countQuery.where(cb.and(typePredicate, contextPredicate));
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateSchedulerJobRecord> query = cb.createQuery(HibernateSchedulerJobRecord.class);
        Root<HibernateSchedulerJobRecord> root = query.from(HibernateSchedulerJobRecord.class);

        Predicate typePredicateData = root.get("type").in(ALL_JOB_TYPES);
        Predicate contextPredicateData = cb.or(
            cb.equal(root.get("contextName"), contextName),
            cb.equal(root.get("contextName"), JobConstants.GLOBAL_EVENT)
        );

        query.where(cb.and(typePredicateData, contextPredicateData));
        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateSchedulerJobRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateSchedulerJobRecord> results = typedQuery.getResultList();

        logger.debug("Found {} SchedulerJobRecords out of {} total for context: {}",
            results.size(), totalCount, contextName);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateSchedulerJobRecord> findByAgent(String agentName, int limit, int offset) {
        logger.debug("Finding SchedulerJobRecords by agentName: {} with limit={}, offset={}",
            agentName, limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateSchedulerJobRecord> countRoot = countQuery.from(HibernateSchedulerJobRecord.class);

        Predicate typePredicate = countRoot.get("type").in(ALL_JOB_TYPES);
        Predicate agentPredicate = cb.equal(countRoot.get("agentName"), agentName);

        countQuery.where(cb.and(typePredicate, agentPredicate));
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateSchedulerJobRecord> query = cb.createQuery(HibernateSchedulerJobRecord.class);
        Root<HibernateSchedulerJobRecord> root = query.from(HibernateSchedulerJobRecord.class);

        Predicate typePredicateData = root.get("type").in(ALL_JOB_TYPES);
        Predicate agentPredicateData = cb.equal(root.get("agentName"), agentName);

        query.where(cb.and(typePredicateData, agentPredicateData));
        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateSchedulerJobRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateSchedulerJobRecord> results = typedQuery.getResultList();

        logger.debug("Found {} SchedulerJobRecords out of {} total for agent: {}",
            results.size(), totalCount, agentName);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<HibernateSchedulerJobRecord> findByFilter(SchedulerJobSearchFilter filter, int limit, int offset,
                                                                    String sortColumn, String sortDirection) {
        logger.debug("Finding SchedulerJobRecords by filter with limit={}, offset={}", limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Build predicates
        List<Predicate> predicates = new ArrayList<>();

        // Type filter
        if (filter.getJobTypeFilter() != null && !filter.getJobTypeFilter().isEmpty()) {
            predicates.add(cb.equal(cb.literal(filter.getJobTypeFilter()), filter.getJobTypeFilter()));
        } else if (filter.getJobTypes() != null && !filter.getJobTypes().isEmpty()) {
            // Use provided job types
        } else {
            // Use all job types by default
        }

        // Build count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateSchedulerJobRecord> countRoot = countQuery.from(HibernateSchedulerJobRecord.class);

        List<Predicate> countPredicates = buildFilterPredicates(cb, countRoot, filter);
        countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Build data query
        CriteriaQuery<HibernateSchedulerJobRecord> query = cb.createQuery(HibernateSchedulerJobRecord.class);
        Root<HibernateSchedulerJobRecord> root = query.from(HibernateSchedulerJobRecord.class);

        List<Predicate> dataPredicates = buildFilterPredicates(cb, root, filter);
        query.where(cb.and(dataPredicates.toArray(new Predicate[0])));
        query.select(root);

        // Sorting
        if (sortColumn != null && !sortColumn.isEmpty() && sortDirection != null && !sortDirection.isEmpty()) {
            if ("ASCENDING".equals(sortDirection)) {
                query.orderBy(cb.asc(root.get(sortColumn)));
            } else {
                query.orderBy(cb.desc(root.get(sortColumn)));
            }
        } else {
            query.orderBy(cb.desc(root.get("jobName")));
        }

        TypedQuery<HibernateSchedulerJobRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateSchedulerJobRecord> results = typedQuery.getResultList();

        logger.debug("Found {} SchedulerJobRecords out of {} total by filter", results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    private List<Predicate> buildFilterPredicates(CriteriaBuilder cb, Root<HibernateSchedulerJobRecord> root,
                                                   SchedulerJobSearchFilter filter) {
        List<Predicate> predicates = new ArrayList<>();

        // Job type filter
        if (filter.getJobTypeFilter() != null && !filter.getJobTypeFilter().isEmpty()) {
            predicates.add(cb.equal(root.get("type"), filter.getJobTypeFilter()));
        } else if (filter.getJobTypes() != null && !filter.getJobTypes().isEmpty()) {
            predicates.add(root.get("type").in(filter.getJobTypes()));
        } else {
            predicates.add(root.get("type").in(ALL_JOB_TYPES));
        }

        // Job name filter
        if (filter.getJobNameFilter() != null && !filter.getJobNameFilter().isEmpty()) {
            predicates.add(cb.like(root.get("jobName"), "%" + filter.getJobNameFilter() + "%"));
        }

        // Display name filter
        if (filter.getDisplayNameFilter() != null && !filter.getDisplayNameFilter().isEmpty()) {
            predicates.add(cb.like(root.get("displayName"), "%" + filter.getDisplayNameFilter() + "%"));
        }

        // Not job name in filter
        if (filter.getNotJobNameInFilter() != null && !filter.getNotJobNameInFilter().isEmpty()) {
            predicates.add(cb.not(root.get("jobName").in(filter.getNotJobNameInFilter())));
        }

        // Context filter
        if (filter.getContextNames() != null && !filter.getContextNames().isEmpty()) {
            predicates.add(root.get("contextName").in(filter.getContextNames()));
        } else if (filter.getContextSearchFilter() != null && !filter.getContextSearchFilter().isEmpty()) {
            predicates.add(cb.or(
                cb.equal(root.get("contextName"), filter.getContextSearchFilter()),
                cb.equal(root.get("contextName"), JobConstants.GLOBAL_EVENT)
            ));
        }

        // Held filter
        if (filter.isHeld()) {
            predicates.add(cb.equal(root.get("held"), true));
        }

        // Skipped filter
        if (filter.isSkipped()) {
            predicates.add(cb.equal(root.get("skipped"), true));
        }

        // Target residing context only filter
        if (filter.isTargetResidingContextOnly() != null) {
            predicates.add(cb.equal(root.get("targetResidingContextOnly"), filter.isTargetResidingContextOnly()));
        }

        // Participates in lock filter
        if (filter.isParticipatesInLock() != null) {
            predicates.add(cb.equal(root.get("participatesInLock"), filter.isParticipatesInLock()));
        }

        return predicates;
    }

    @Override
    @Transactional(readOnly = true)
    public HibernateSchedulerJobRecord findById(String id) {
        logger.debug("Finding SchedulerJobRecord by id: {}", id);
        return entityManager.find(HibernateSchedulerJobRecord.class, id);
    }

    @Override
    @Transactional(readOnly = true)
    public HibernateSchedulerJobRecord findByContextIdAndJobName(String contextId, String jobName) {
        logger.debug("Finding SchedulerJobRecord by contextId: {} and jobName: {}", contextId, jobName);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<HibernateSchedulerJobRecord> query = cb.createQuery(HibernateSchedulerJobRecord.class);
        Root<HibernateSchedulerJobRecord> root = query.from(HibernateSchedulerJobRecord.class);

        Predicate typePredicate = root.get("type").in(ALL_JOB_TYPES);
        Predicate jobNamePredicate = cb.equal(root.get("jobName"), jobName);
        Predicate contextPredicate = cb.or(
            cb.equal(root.get("contextName"), contextId),
            cb.equal(root.get("contextName"), JobConstants.GLOBAL_EVENT)
        );

        query.where(cb.and(typePredicate, jobNamePredicate, contextPredicate));
        query.select(root);

        List<HibernateSchedulerJobRecord> results = entityManager.createQuery(query).getResultList();

        if (!results.isEmpty()) {
            return results.get(0);
        }

        return null;
    }

    @Override
    @Transactional
    public void delete(HibernateSchedulerJobRecord record) {
        if (record == null) {
            logger.warn("Attempted to delete null SchedulerJobRecord");
            return;
        }

        logger.debug("Deleting SchedulerJobRecord: {}", record.getId());

        switch (record.getType()) {
            case JobConstants.CONTEXT_START_JOB -> {
                HibernateContextStartJobRecord managedRecord = entityManager.find(HibernateContextStartJobRecord.class, record.getId());
                entityManager.remove(managedRecord);
            }
            case JobConstants.CONTEXT_TERMINAL_JOB -> {
                HibernateContextTerminalJobRecord managedRecord = entityManager.find(HibernateContextTerminalJobRecord.class, record.getId());
                entityManager.remove(managedRecord);
            }
            case JobConstants.FILE_EVENT_DRIVEN_JOB -> {
                HibernateFileEventDrivenJobRecord managedRecord = entityManager.find(HibernateFileEventDrivenJobRecord.class, record.getId());
                entityManager.remove(managedRecord);
            }
            case JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB -> {
                HibernateQuartzScheduleDrivenJobRecord managedRecord = entityManager.find(HibernateQuartzScheduleDrivenJobRecord.class, record.getId());
                entityManager.remove(managedRecord);
            }
            case JobConstants.INTERNAL_EVENT_DRIVEN_JOB -> {
                HibernateInternalEventDrivenJobRecord managedRecord = entityManager.find(HibernateInternalEventDrivenJobRecord.class, record.getId());
                entityManager.remove(managedRecord);
            }
            case JobConstants.GLOBAL_EVENT -> {
                HibernateGlobalEventJobRecord managedRecord = entityManager.find(HibernateGlobalEventJobRecord.class, record.getId());
                entityManager.remove(managedRecord);
            }
            default -> {
                HibernateSchedulerJobRecord managedRecord = entityManager.find(HibernateSchedulerJobRecord.class, record.getId());
                entityManager.remove(managedRecord);
            }
        }

        entityManager.flush();
    }

    @Override
    @Transactional
    public void deleteByAgentName(String agentName) {
        logger.debug("Deleting SchedulerJobRecords by agentName: {}", agentName);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaDelete<HibernateSchedulerJobRecord> delete = cb.createCriteriaDelete(HibernateSchedulerJobRecord.class);
        Root<HibernateSchedulerJobRecord> root = delete.from(HibernateSchedulerJobRecord.class);

        Predicate typePredicate = root.get("type").in(ALL_JOB_TYPES);
        Predicate agentPredicate = cb.equal(root.get("agentName"), agentName);

        delete.where(cb.and(typePredicate, agentPredicate));

        int deletedCount = entityManager.createQuery(delete).executeUpdate();

        logger.debug("Deleted {} SchedulerJobRecords for agent: {}", deletedCount, agentName);
    }

    @Override
    @Transactional
    public void deleteByContextName(String contextName) {
        logger.debug("Deleting SchedulerJobRecords by contextName: {}", contextName);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaDelete<HibernateSchedulerJobRecord> delete = cb.createCriteriaDelete(HibernateSchedulerJobRecord.class);
        Root<HibernateSchedulerJobRecord> root = delete.from(HibernateSchedulerJobRecord.class);

        Predicate typePredicate = root.get("type").in(ALL_JOB_TYPES);
        Predicate contextPredicate = cb.equal(root.get("contextName"), contextName);

        delete.where(cb.and(typePredicate, contextPredicate));

        int deletedCount = entityManager.createQuery(delete).executeUpdate();

        logger.debug("Deleted {} SchedulerJobRecords for context: {}", deletedCount, contextName);
    }

    @Override
    public void save(HibernateSchedulerJobRecord event) {
        throw new UnsupportedOperationException("It is not possible to save SchedulerJobRecord directly. " +
            "Please save child implementations of SchedulerJobRecord.");
    }

    /**
     * Find jobs by context and type
     *
     * @param contextName the context name
     * @param jobType     the job type
     * @param limit       result limit
     * @param offset      result offset
     * @return search results
     */
    @Transactional(readOnly = true)
    public SearchResults<HibernateSchedulerJobRecord> findByContextAndType(String contextName, String jobType, int limit, int offset) {
        logger.debug("Finding SchedulerJobRecords by contextName: {} and jobType: {} with limit={}, offset={}",
            contextName, jobType, limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateSchedulerJobRecord> countRoot = countQuery.from(HibernateSchedulerJobRecord.class);

        Predicate typePredicate = cb.equal(countRoot.get("type"), jobType);
        Predicate contextPredicate = cb.or(
            cb.equal(countRoot.get("contextName"), contextName),
            cb.equal(countRoot.get("contextName"), JobConstants.GLOBAL_EVENT)
        );

        countQuery.where(cb.and(typePredicate, contextPredicate));
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateSchedulerJobRecord> query = cb.createQuery(HibernateSchedulerJobRecord.class);
        Root<HibernateSchedulerJobRecord> root = query.from(HibernateSchedulerJobRecord.class);

        Predicate typePredicateData = cb.equal(root.get("type"), jobType);
        Predicate contextPredicateData = cb.or(
            cb.equal(root.get("contextName"), contextName),
            cb.equal(root.get("contextName"), JobConstants.GLOBAL_EVENT)
        );

        query.where(cb.and(typePredicateData, contextPredicateData));
        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateSchedulerJobRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateSchedulerJobRecord> results = typedQuery.getResultList();

        logger.debug("Found {} SchedulerJobRecords out of {} total for context: {} and type: {}",
            results.size(), totalCount, contextName, jobType);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    /**
     * Find jobs by type
     *
     * @param jobType the job type
     * @param limit   result limit
     * @param offset  result offset
     * @return search results
     */
    @Transactional(readOnly = true)
    public SearchResults<HibernateSchedulerJobRecord> findByType(String jobType, int limit, int offset) {
        logger.debug("Finding SchedulerJobRecords by jobType: {} with limit={}, offset={}",
            jobType, limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateSchedulerJobRecord> countRoot = countQuery.from(HibernateSchedulerJobRecord.class);

        Predicate typePredicate = cb.equal(countRoot.get("type"), jobType);
        countQuery.where(typePredicate);
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateSchedulerJobRecord> query = cb.createQuery(HibernateSchedulerJobRecord.class);
        Root<HibernateSchedulerJobRecord> root = query.from(HibernateSchedulerJobRecord.class);

        Predicate typePredicateData = cb.equal(root.get("type"), jobType);
        query.where(typePredicateData);
        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateSchedulerJobRecord> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateSchedulerJobRecord> results = typedQuery.getResultList();

        logger.debug("Found {} SchedulerJobRecords out of {} total for type: {}",
            results.size(), totalCount, jobType);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }
}
