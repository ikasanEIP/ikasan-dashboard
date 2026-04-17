package org.ikasan.relational.persistence.scheduled.job.service;

import org.ikasan.relational.persistence.scheduled.job.dao.HibernateInternalEventDrivenJobDaoImpl;
import org.ikasan.relational.persistence.scheduled.job.model.HibernateInternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate/PostgreSQL implementation of InternalEventDrivenJobService.
 *
 * This service provides business logic for managing internal event driven job records
 * using Hibernate persistence. It delegates to the DAO layer for data access operations.
 */
public class HibernateInternalEventDrivenJobServiceImpl implements InternalEventDrivenJobService {

    private static final Logger logger = LoggerFactory.getLogger(HibernateInternalEventDrivenJobServiceImpl.class);

    private final HibernateInternalEventDrivenJobDaoImpl internalEventDrivenJobDao;

    /**
     * Constructor with required dependency.
     *
     * @param internalEventDrivenJobDao DAO for internal event driven job operations
     * @throws IllegalArgumentException if internalEventDrivenJobDao is null
     */
    public HibernateInternalEventDrivenJobServiceImpl(HibernateInternalEventDrivenJobDaoImpl internalEventDrivenJobDao) {
        this.internalEventDrivenJobDao = internalEventDrivenJobDao;
        if (this.internalEventDrivenJobDao == null) {
            throw new IllegalArgumentException("internalEventDrivenJobDao cannot be null!");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<InternalEventDrivenJobRecord> findAll(int limit, int offset) {
        logger.debug("Finding all InternalEventDrivenJobRecords with limit={}, offset={}", limit, offset);
        SearchResults<HibernateInternalEventDrivenJobRecord> results =
            this.internalEventDrivenJobDao.findAll(limit, offset);
        return castSearchResults(results);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<InternalEventDrivenJobRecord> findByContext(String contextId, int limit, int offset) {
        logger.debug("Finding InternalEventDrivenJobRecords by context: {} with limit={}, offset={}",
            contextId, limit, offset);
        SearchResults<HibernateInternalEventDrivenJobRecord> results =
            this.internalEventDrivenJobDao.findByContext(contextId, limit, offset);
        return castSearchResults(results);
    }

    @Override
    @Transactional(readOnly = true)
    public InternalEventDrivenJobRecord findById(String id) {
        logger.debug("Finding InternalEventDrivenJobRecord by id: {}", id);
        return this.internalEventDrivenJobDao.findById(id);
    }

    @Override
    @Transactional
    public void save(InternalEventDrivenJobRecord internalEventDrivenJobRecord) {
        if (internalEventDrivenJobRecord == null) {
            throw new IllegalArgumentException("internalEventDrivenJobRecord cannot be null!");
        }

        logger.debug("Saving InternalEventDrivenJobRecord: {}", internalEventDrivenJobRecord.getId());

        if (!(internalEventDrivenJobRecord instanceof HibernateInternalEventDrivenJobRecord)) {
            throw new IllegalArgumentException(
                "internalEventDrivenJobRecord must be an instance of HibernateInternalEventDrivenJobRecord!");
        }

        this.internalEventDrivenJobDao.save((HibernateInternalEventDrivenJobRecord) internalEventDrivenJobRecord);
    }

    /**
     * Helper method to cast SearchResults from Hibernate-specific type to interface type.
     *
     * @param hibernateResults the Hibernate-specific search results
     * @return SearchResults with interface type
     */
    @SuppressWarnings("unchecked")
    private SearchResults<InternalEventDrivenJobRecord> castSearchResults(
            SearchResults<HibernateInternalEventDrivenJobRecord> hibernateResults) {
        return (SearchResults<InternalEventDrivenJobRecord>) (SearchResults<?>) hibernateResults;
    }
}
