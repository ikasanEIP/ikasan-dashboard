package org.ikasan.relational.persistence.scheduled.profile.service;

import org.ikasan.spec.scheduled.profile.dao.ContextProfileDao;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Hibernate/PostgreSQL implementation of ContextProfileService.
 *
 * This service provides business logic for managing context profiles using
 * Hibernate persistence. It delegates to DAO layer for data access.
 */
public class HibernateContextProfileServiceImpl implements ContextProfileService {

    private static final Logger logger = LoggerFactory.getLogger(HibernateContextProfileServiceImpl.class);

    private final ContextProfileDao contextProfileDao;

    /**
     * Constructor with required dependencies
     *
     * @param contextProfileDao DAO for context profile operations
     */
    public HibernateContextProfileServiceImpl(ContextProfileDao contextProfileDao) {
        this.contextProfileDao = contextProfileDao;
        if (this.contextProfileDao == null) {
            throw new IllegalArgumentException("contextProfileDao cannot be null!");
        }
    }

    @Override
    @Transactional
    public void save(ContextProfileRecord contextProfileRecord) {
        logger.debug("Saving context profile: {}-{}",
            contextProfileRecord.getProfileName(), contextProfileRecord.getContextName());
        this.contextProfileDao.save(contextProfileRecord);
    }

    @Override
    @Transactional
    public void save(List<ContextProfileRecord> records) {
        logger.debug("Saving {} context profile records", records.size());
        for (ContextProfileRecord record : records) {
            this.contextProfileDao.save(record);
        }
    }

    @Override
    @Transactional
    public void deleteByContextName(String contextName) {
        logger.debug("Deleting context profiles by context name: {}", contextName);
        this.contextProfileDao.deleteByContextName(contextName);
    }

    @Override
    @Transactional(readOnly = true)
    public ContextProfileRecord findById(String id) {
        logger.debug("Finding context profile by id: {}", id);
        return this.contextProfileDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<ContextProfileRecord> findByFilter(ContextProfileSearchFilter contextProfileSearchFilter,
                                                             int limit, int offset,
                                                             String sortColumn, String sortOrder) {
        logger.debug("Finding context profiles by filter with limit={}, offset={}", limit, offset);
        return this.contextProfileDao.findByFilter(contextProfileSearchFilter, limit, offset, sortColumn, sortOrder);
    }
}
