package org.ikasan.relational.persistence.scheduled.joblock.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.ikasan.relational.persistence.scheduled.joblock.model.HibernateJobLockCacheRecord;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheDao;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate/PostgreSQL implementation of JobLockCacheDao.
 *
 * This DAO manages persistence operations for job lock cache records using JPA.
 * The environment is used as the primary key, ensuring one lock cache per environment.
 */
public class HibernateJobLockCacheDaoImpl implements JobLockCacheDao {

    private static final Logger logger = LoggerFactory.getLogger(HibernateJobLockCacheDaoImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Default constructor for dependency injection
     */
    public HibernateJobLockCacheDaoImpl() {
    }

    @Override
    @Transactional
    public void save(JobLockCacheRecord jobLockCacheRecord) {
        if (jobLockCacheRecord == null) {
            throw new IllegalArgumentException("jobLockCacheRecord cannot be null!");
        }

        String environment = jobLockCacheRecord.getEnvironment();
        if (environment == null) {
            environment = JobLockCacheRecord.DEFAULT_ENVIRONMENT;
        }

        logger.debug("Saving JobLockCacheRecord for environment: {}", environment);

        if (!(jobLockCacheRecord instanceof HibernateJobLockCacheRecord)) {
            throw new IllegalArgumentException(
                "jobLockCacheRecord must be an instance of HibernateJobLockCacheRecord!");
        }

        HibernateJobLockCacheRecord hibernateRecord = (HibernateJobLockCacheRecord) jobLockCacheRecord;

        // Ensure environment is set
        if (hibernateRecord.getEnvironment() == null) {
            hibernateRecord.setEnvironment(JobLockCacheRecord.DEFAULT_ENVIRONMENT);
        }

        // Set timestamps
        if (hibernateRecord.getTimestamp() == 0) {
            hibernateRecord.setTimestamp(System.currentTimeMillis());
        }
        hibernateRecord.setModifiedTimestamp(System.currentTimeMillis());

        // Merge will insert if new or update if exists (based on environment primary key)
        entityManager.merge(hibernateRecord);
        entityManager.flush();

        logger.debug("Successfully saved JobLockCacheRecord for environment: {}", environment);
    }

    @Override
    @Transactional(readOnly = true)
    public JobLockCacheRecord get(String environment) {
        if (environment == null) {
            environment = JobLockCacheRecord.DEFAULT_ENVIRONMENT;
        }

        logger.debug("Getting JobLockCacheRecord for environment: {}", environment);

        HibernateJobLockCacheRecord record = entityManager.find(HibernateJobLockCacheRecord.class, environment);

        if (record != null) {
            logger.debug("Found JobLockCacheRecord for environment: {}", environment);
        } else {
            logger.debug("No JobLockCacheRecord found for environment: {}", environment);
        }

        return record;
    }

    /**
     * Delete a JobLockCacheRecord by environment (useful for testing)
     *
     * @param environment the environment identifier
     */
    @Transactional
    public void delete(String environment) {
        if (environment == null) {
            environment = JobLockCacheRecord.DEFAULT_ENVIRONMENT;
        }

        logger.debug("Deleting JobLockCacheRecord for environment: {}", environment);

        HibernateJobLockCacheRecord record = entityManager.find(HibernateJobLockCacheRecord.class, environment);
        if (record != null) {
            entityManager.remove(record);
            entityManager.flush();
            logger.debug("Successfully deleted JobLockCacheRecord for environment: {}", environment);
        } else {
            logger.debug("No JobLockCacheRecord found to delete for environment: {}", environment);
        }
    }

    /**
     * Delete all JobLockCacheRecords (useful for testing)
     */
    @Transactional
    public void deleteAll() {
        logger.debug("Deleting all JobLockCacheRecords");

        int deletedCount = entityManager.createQuery("DELETE FROM HibernateJobLockCacheRecord")
            .executeUpdate();

        logger.debug("Deleted {} JobLockCacheRecords", deletedCount);
    }
}
