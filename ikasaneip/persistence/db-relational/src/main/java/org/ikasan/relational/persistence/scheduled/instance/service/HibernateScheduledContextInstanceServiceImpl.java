package org.ikasan.relational.persistence.scheduled.instance.service;

import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditAggregateDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceDao;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Hibernate/PostgreSQL implementation of ScheduledContextInstanceService.
 *
 * This service provides business logic for managing scheduled context instances using
 * Hibernate persistence. It delegates to DAO layer for data access.
 */
public class HibernateScheduledContextInstanceServiceImpl implements ScheduledContextInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(HibernateScheduledContextInstanceServiceImpl.class);

    private final ScheduledContextInstanceDao scheduledContextInstanceDao;
    private final ScheduledContextInstanceAuditDao scheduledContextInstanceAuditDao;
    private final ScheduledContextInstanceAuditAggregateDao scheduledContextInstanceAuditAggregateDao;

    /**
     * Constructor with required dependencies
     *
     * @param scheduledContextInstanceDao              DAO for context instance operations
     * @param scheduledContextInstanceAuditDao         DAO for context instance audit operations
     * @param scheduledContextInstanceAuditAggregateDao DAO for aggregate audit operations
     */
    public HibernateScheduledContextInstanceServiceImpl(ScheduledContextInstanceDao scheduledContextInstanceDao,
                                                         ScheduledContextInstanceAuditDao scheduledContextInstanceAuditDao,
                                                         ScheduledContextInstanceAuditAggregateDao scheduledContextInstanceAuditAggregateDao) {
        this.scheduledContextInstanceDao = scheduledContextInstanceDao;
        if (this.scheduledContextInstanceDao == null) {
            throw new IllegalArgumentException("scheduledContextInstanceDao cannot be null!");
        }
        this.scheduledContextInstanceAuditDao = scheduledContextInstanceAuditDao;
        if (this.scheduledContextInstanceAuditDao == null) {
            throw new IllegalArgumentException("scheduledContextInstanceAuditDao cannot be null!");
        }
        this.scheduledContextInstanceAuditAggregateDao = scheduledContextInstanceAuditAggregateDao;
        // Aggregate DAO is optional for now (not yet fully implemented)
        // if (this.scheduledContextInstanceAuditAggregateDao == null) {
        //     throw new IllegalArgumentException("scheduledContextInstanceAuditAggregateDao cannot be null!");
        // }
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduledContextInstanceRecord findById(String id) {
        logger.debug("Finding context instance by id: {}", id);
        return this.scheduledContextInstanceDao.findById(id);
    }

    @Override
    @Transactional
    public void deleteById(String id) {
        logger.debug("Deleting context instance by id: {}", id);
        this.scheduledContextInstanceDao.deleteById(id);
    }

    @Override
    @Transactional
    public void save(ScheduledContextInstanceRecord scheduledContextInstanceRecord) {
        logger.debug("Saving context instance: {}", scheduledContextInstanceRecord.getId());
        this.scheduledContextInstanceDao.save(scheduledContextInstanceRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByStatus(
            List<InstanceStatus> instanceStatuses) {
        logger.debug("Getting context instances by statuses: {}", instanceStatuses);
        return this.scheduledContextInstanceDao.getScheduledContextInstancesByStatus(instanceStatuses);
    }

    @Override
    @Transactional
    public void saveAudit(ScheduledContextInstanceAuditAggregateRecord scheduledContextInstanceAuditAggregateRecord,
                          ContextInstance previousContextInstance,
                          ContextInstance updatedContextInstance) {
        logger.debug("Saving audit record for context instance: {}",
            scheduledContextInstanceAuditAggregateRecord.getContextInstanceId());
        if (this.scheduledContextInstanceAuditAggregateDao != null) {
            this.scheduledContextInstanceAuditAggregateDao.save(scheduledContextInstanceAuditAggregateRecord);
        } else {
            logger.warn("Aggregate audit DAO not available - audit not saved");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduledContextInstanceRecord findAuditRecordById(String id) {
        logger.debug("Finding audit record by id: {}", id);
        return this.scheduledContextInstanceAuditDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<ScheduledContextInstanceAuditAggregateRecord> findAllAuditRecords(
            int limit, int offset, String sortField, String sortDirection) {
        logger.debug("Finding all audit records with limit={}, offset={}", limit, offset);
        if (this.scheduledContextInstanceAuditAggregateDao != null) {
            return this.scheduledContextInstanceAuditAggregateDao.findAll(limit, offset, sortField, sortDirection);
        }
        throw new UnsupportedOperationException("Aggregate audit DAO not available");
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<ScheduledContextInstanceAuditAggregateRecord> findAllAuditRecordsByFilter(
            ScheduledContextInstanceAuditAggregateSearchFilter filter,
            int limit, int offset, String sortField, String sortDirection) {
        logger.debug("Finding audit records by filter");
        if (this.scheduledContextInstanceAuditAggregateDao != null) {
            return this.scheduledContextInstanceAuditAggregateDao.findScheduledContextInstanceAuditAggregateRecordsByFilter(
                filter, limit, offset, sortField, sortDirection);
        }
        throw new UnsupportedOperationException("Aggregate audit DAO not available");
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByStatus(
            List<InstanceStatus> instanceStatuses, int limit, int offset) {
        logger.debug("Getting context instances by statuses with limit={}, offset={}", limit, offset);
        return this.scheduledContextInstanceDao.getScheduledContextInstancesByStatus(instanceStatuses, limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByContextName(
            String contextName, int limit, int offset, String sortField, String sortDirection) {
        logger.debug("Getting context instances by context name: {}", contextName);
        return this.scheduledContextInstanceDao.getScheduledContextInstancesByContextName(
            contextName, limit, offset, sortField, sortDirection);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByContextName(
            String contextName, long startTimestamp, long endTimestamp,
            int limit, int offset, String sortField, String sortDirection) {
        logger.debug("Getting context instances by context name: {} with time window [{}, {}]",
            contextName, startTimestamp, endTimestamp);
        return this.scheduledContextInstanceDao.getScheduledContextInstancesByContextName(
            contextName, startTimestamp, endTimestamp, limit, offset, sortField, sortDirection);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByFilter(
            ContextInstanceSearchFilter filter, int limit, int offset, String sortField, String sortDirection) {
        logger.debug("Getting context instances by filter");
        return this.scheduledContextInstanceDao.getScheduledContextInstancesByFilter(
            filter, limit, offset, sortField, sortDirection);
    }
}
