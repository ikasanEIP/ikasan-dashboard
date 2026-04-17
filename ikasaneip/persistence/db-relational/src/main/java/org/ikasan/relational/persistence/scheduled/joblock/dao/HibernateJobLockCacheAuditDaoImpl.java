package org.ikasan.relational.persistence.scheduled.joblock.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.ikasan.relational.persistence.scheduled.SearchResultsImpl;
import org.ikasan.relational.persistence.scheduled.joblock.model.HibernateJobLockCacheAuditRecord;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheAuditDao;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheAuditRecord;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Hibernate/PostgreSQL implementation of JobLockCacheAuditDao.
 *
 * This DAO manages persistence operations for job lock cache audit records using JPA Criteria API.
 * Audit records use auto-generated UUID primary keys to allow multiple historical snapshots.
 */
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class HibernateJobLockCacheAuditDaoImpl implements JobLockCacheAuditDao {

    private static final Logger logger = LoggerFactory.getLogger(HibernateJobLockCacheAuditDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Default constructor for dependency injection
     */
    public HibernateJobLockCacheAuditDaoImpl() {
    }

    @Override
    @Transactional
    public void save(JobLockCacheAuditRecord jobLockCacheAuditRecord) {
        if (jobLockCacheAuditRecord == null) {
            throw new IllegalArgumentException("jobLockCacheAuditRecord cannot be null!");
        }

        logger.debug("Saving JobLockCacheAuditRecord with ID: {}", jobLockCacheAuditRecord.getId());

        if (!(jobLockCacheAuditRecord instanceof HibernateJobLockCacheAuditRecord)) {
            throw new IllegalArgumentException(
                "jobLockCacheAuditRecord must be an instance of HibernateJobLockCacheAuditRecord!");
        }

        HibernateJobLockCacheAuditRecord hibernateRecord = (HibernateJobLockCacheAuditRecord) jobLockCacheAuditRecord;

        // Ensure environment is set
        if (hibernateRecord.getEnvironment() == null) {
            hibernateRecord.setEnvironment(JobLockCacheAuditRecord.DEFAULT_ENVIRONMENT);
        }

        // Set timestamps
        if (hibernateRecord.getTimestamp() == 0) {
            hibernateRecord.setTimestamp(System.currentTimeMillis());
        }
        hibernateRecord.setModifiedTimestamp(System.currentTimeMillis());

        // Persist (insert) - audit records are always new
        entityManager.persist(hibernateRecord);
        entityManager.flush();

        logger.debug("Successfully saved JobLockCacheAuditRecord with ID: {}", hibernateRecord.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<JobLockCacheAuditRecord> findAll(int limit, int offset) {
        logger.debug("Finding all JobLockCacheAuditRecords with limit={}, offset={}", limit, offset);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<HibernateJobLockCacheAuditRecord> countRoot = countQuery.from(HibernateJobLockCacheAuditRecord.class);
        countQuery.select(cb.count(countRoot));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<HibernateJobLockCacheAuditRecord> query = cb.createQuery(HibernateJobLockCacheAuditRecord.class);
        Root<HibernateJobLockCacheAuditRecord> root = query.from(HibernateJobLockCacheAuditRecord.class);
        query.select(root);
        query.orderBy(cb.desc(root.get("timestamp"))); // Order by timestamp descending (newest first)

        TypedQuery<HibernateJobLockCacheAuditRecord> typedQuery = entityManager.createQuery(query);

        if (limit > -1) {
            typedQuery.setMaxResults(limit);
        }
        if (offset > -1) {
            typedQuery.setFirstResult(offset);
        }

        List<HibernateJobLockCacheAuditRecord> results = typedQuery.getResultList();

        logger.debug("Found {} JobLockCacheAuditRecords out of {} total", results.size(), totalCount);

        return new SearchResultsImpl<>(castToInterfaceList(results), totalCount, 0L);
    }

    /**
     * Helper method to cast list of Hibernate records to interface list
     *
     * @param hibernateRecords the Hibernate-specific records
     * @return list with interface type
     */
    private List<JobLockCacheAuditRecord> castToInterfaceList(List<HibernateJobLockCacheAuditRecord> hibernateRecords) {
        return new ArrayList<>(hibernateRecords);
    }
}
