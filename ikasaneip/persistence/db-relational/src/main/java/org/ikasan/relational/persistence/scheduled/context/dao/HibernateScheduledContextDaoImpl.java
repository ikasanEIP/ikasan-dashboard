package org.ikasan.relational.persistence.scheduled.context.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.context.model.HibernateScheduledContextRecordImpl;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.model.ScheduledContextSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Hibernate/PostgreSQL implementation of ScheduledContextDao.
 *
 * This DAO leverages:
 * - JPA Criteria API for type-safe queries
 * - PostgreSQL JSONB for efficient storage of complex ContextTemplate objects
 * - Spring transaction management
 * - Native PostgreSQL indexing for performance
 */
public class HibernateScheduledContextDaoImpl implements ScheduledContextDao {

    private static final Logger logger = LoggerFactory.getLogger(HibernateScheduledContextDaoImpl.class);

    @PersistenceContext(unitName = "hibernate-persistence")
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public SearchResults<ScheduledContextRecord> findAll() {
        return findAll(-1, -1);
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduledContextRecord findById(String id) {
        logger.debug("Finding ScheduledContextRecord by id: {}", id);

        HibernateScheduledContextRecordImpl result = entityManager.find(HibernateScheduledContextRecordImpl.class, id);

        logger.debug("Found ScheduledContextRecord: {}", result != null);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<ScheduledContextRecord> findAll(int limit, int offset) {
        logger.debug("Finding all ScheduledContextRecords with limit={}, offset={}", limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateScheduledContextRecordImpl> countRoot = countQuery.from(HibernateScheduledContextRecordImpl.class);
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateScheduledContextRecordImpl> query = cb.createQuery(HibernateScheduledContextRecordImpl.class);
        Root<HibernateScheduledContextRecordImpl> root = query.from(HibernateScheduledContextRecordImpl.class);
        query.select(root);
        query.orderBy(cb.desc(root.get("modifiedTimestamp")));

        TypedQuery<HibernateScheduledContextRecordImpl> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateScheduledContextRecordImpl> results = typedQuery.getResultList();

        logger.debug("Found {} ScheduledContextRecords out of {} total", results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<ScheduledContextRecord> findByFilter(ScheduledContextSearchFilter filter,
                                                               int limit,
                                                               int offset,
                                                               String sortColumn,
                                                               String sortOrder) {
        logger.debug("Finding ScheduledContextRecords by filter: {}", filter);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateScheduledContextRecordImpl> countRoot = countQuery.from(HibernateScheduledContextRecordImpl.class);
        List<Predicate> countPredicates = buildPredicates(cb, countRoot, filter);
        if (!countPredicates.isEmpty()) {
            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateScheduledContextRecordImpl> query = cb.createQuery(HibernateScheduledContextRecordImpl.class);
        Root<HibernateScheduledContextRecordImpl> root = query.from(HibernateScheduledContextRecordImpl.class);

        List<Predicate> dataPredicates = buildPredicates(cb, root, filter);
        if (!dataPredicates.isEmpty()) {
            query.where(cb.and(dataPredicates.toArray(new Predicate[0])));
        }

        // Apply sorting
        if (sortColumn != null && !sortColumn.isEmpty()) {
            Order order = "ASCENDING".equalsIgnoreCase(sortOrder)
                ? cb.asc(root.get(sortColumn))
                : cb.desc(root.get(sortColumn));
            query.orderBy(order);
        } else {
            // Default sort by modified timestamp descending
            query.orderBy(cb.desc(root.get("modifiedTimestamp")));
        }

        query.select(root);

        TypedQuery<HibernateScheduledContextRecordImpl> typedQuery = entityManager.createQuery(query);

        if (limit > 0) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > 0) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateScheduledContextRecordImpl> results = typedQuery.getResultList();

        logger.debug("Found {} ScheduledContextRecords out of {} total matching filter", results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    /**
     * Build predicates for filtering
     */
    private List<Predicate> buildPredicates(CriteriaBuilder cb,
                                           Root<HibernateScheduledContextRecordImpl> root,
                                           ScheduledContextSearchFilter filter) {
        List<Predicate> predicates = new ArrayList<>();

        if (filter != null) {
            // Filter by context name
            if (filter.getContextName() != null && !filter.getContextName().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("contextName")),
                    "%" + filter.getContextName().toLowerCase() + "%"));
            }
        }

        return predicates;
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduledContextRecord findByName(String name) {
        logger.debug("Finding ScheduledContextRecord by name: {}", name);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<HibernateScheduledContextRecordImpl> query = cb.createQuery(HibernateScheduledContextRecordImpl.class);
        Root<HibernateScheduledContextRecordImpl> root = query.from(HibernateScheduledContextRecordImpl.class);

        query.select(root);
        query.where(cb.equal(root.get("contextName"), name));

        List<HibernateScheduledContextRecordImpl> results = entityManager.createQuery(query)
            .setMaxResults(1)
            .getResultList();

        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    @Transactional
    public void save(ScheduledContextRecord scheduledContextRecord) {
        logger.debug("Saving ScheduledContextRecord: {}", scheduledContextRecord.getContextName());

        if (!(scheduledContextRecord instanceof HibernateScheduledContextRecordImpl)) {
            throw new IllegalArgumentException(
                "ScheduledContextRecord must be an instance of HibernateScheduledContextRecordImpl");
        }

        HibernateScheduledContextRecordImpl hibernateRecord = (HibernateScheduledContextRecordImpl) scheduledContextRecord;

        // Set timestamps if new
        if (hibernateRecord.getTimestamp() == 0) {
            hibernateRecord.setTimestamp(System.currentTimeMillis());
        }
        hibernateRecord.setModifiedTimestamp(System.currentTimeMillis());

        // Ensure ID is set
        if (hibernateRecord.getId() == null || hibernateRecord.getId().isEmpty()) {
            hibernateRecord.setId(hibernateRecord.getContextName());
        }

        // Merge (will insert or update)
        entityManager.merge(hibernateRecord);

        logger.debug("Successfully saved ScheduledContextRecord: {}", hibernateRecord.getContextName());
    }

    @Override
    @Transactional
    public void deleteContext(String contextName) {
        logger.debug("Deleting ScheduledContextRecord with context name: {}", contextName);

        ScheduledContextRecord record = findByName(contextName);
        if (record != null) {
            entityManager.remove(entityManager.contains(record) ? record : entityManager.merge(record));
            logger.debug("Successfully deleted ScheduledContextRecord: {}", contextName);
        } else {
            logger.warn("ScheduledContextRecord not found for deletion: {}", contextName);
        }
    }
}
