package org.ikasan.relational.persistence.scheduled.joblock.service;

import org.ikasan.relational.persistence.scheduled.joblock.model.HibernateJobLockCacheAuditRecord;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheAuditDao;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheDao;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheAuditRecord;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.search.SearchResults;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate implementation of JobLockCacheService for PostgreSQL persistence.
 *
 * This service provides operations for managing job lock cache records and their audit trail.
 * It delegates to DAOs for persistence operations and optionally creates audit snapshots.
 */
public class HibernateJobLockCacheServiceImpl implements JobLockCacheService {

    private final JobLockCacheDao jobLockCacheDao;
    private final JobLockCacheAuditDao jobLockCacheAuditDao;
    private final boolean saveJobLockCacheAudits;

    /**
     * Constructor with dependency injection and validation.
     *
     * @param jobLockCacheDao the DAO for job lock cache persistence (required)
     * @param jobLockCacheAuditDao the DAO for audit record persistence (required)
     * @param saveJobLockCacheAudits flag to enable/disable audit record creation
     * @throws IllegalArgumentException if any DAO is null
     */
    public HibernateJobLockCacheServiceImpl(JobLockCacheDao jobLockCacheDao,
                                           JobLockCacheAuditDao jobLockCacheAuditDao,
                                           boolean saveJobLockCacheAudits) {
        if (jobLockCacheDao == null) {
            throw new IllegalArgumentException("jobLockCacheDao cannot be null!");
        }
        if (jobLockCacheAuditDao == null) {
            throw new IllegalArgumentException("jobLockCacheAuditDao cannot be null!");
        }
        this.jobLockCacheDao = jobLockCacheDao;
        this.jobLockCacheAuditDao = jobLockCacheAuditDao;
        this.saveJobLockCacheAudits = saveJobLockCacheAudits;
    }

    /**
     * Saves a job lock cache record and optionally creates an audit snapshot.
     *
     * The cache record is always saved (upserted based on environment).
     * If audit saving is enabled, a separate audit record with unique ID is also created.
     *
     * @param record the job lock cache record to save
     */
    @Override
    @Transactional
    public void save(JobLockCacheRecord record) {
        this.jobLockCacheDao.save(record);

        if (this.saveJobLockCacheAudits) {
            JobLockCacheAuditRecord audit = new HibernateJobLockCacheAuditRecord();
            audit.setEnvironment(record.getEnvironment());
            audit.setJobLockCache(record.getJobLockCache());
            this.jobLockCacheAuditDao.save(audit);
        }
    }

    /**
     * Retrieves the job lock cache record for a specific environment.
     *
     * @param environment the environment identifier (null uses default environment)
     * @return the job lock cache record, or null if not found
     */
    @Override
    public JobLockCacheRecord get(String environment) {
        return this.jobLockCacheDao.get(environment);
    }

    /**
     * Retrieves audit records with pagination, ordered by timestamp descending (newest first).
     *
     * @param limit the maximum number of records to return
     * @param offset the number of records to skip
     * @return search results containing audit records and total count
     */
    @Override
    public SearchResults<JobLockCacheAuditRecord> findAll(int limit, int offset) {
        return this.jobLockCacheAuditDao.findAll(limit, offset);
    }
}
