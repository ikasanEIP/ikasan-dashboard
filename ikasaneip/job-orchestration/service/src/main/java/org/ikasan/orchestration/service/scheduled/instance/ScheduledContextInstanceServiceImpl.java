package org.ikasan.orchestration.service.scheduled.instance;

import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceAuditRecordImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceRecordImpl;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditAggregateDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceDao;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.search.SearchResults;

import java.util.List;

public class ScheduledContextInstanceServiceImpl implements ScheduledContextInstanceService {
    private final ScheduledContextInstanceDao scheduledContextInstanceDao;
    private final ScheduledContextInstanceAuditDao scheduledContextInstanceAuditDao;
    private final ScheduledContextInstanceAuditAggregateDao scheduledContextInstanceAuditAggregateDao;
    private final boolean saveContextInstanceAuditRecords;
    private final boolean saveContextInstanceAuditDeltaRecords;

    /**
     * Constructs a new instance of ScheduledContextInstanceServiceImpl.
     *
     * @param scheduledContextInstanceDao the DAO for managing scheduled context instances; must not be null
     * @param scheduledContextInstanceAuditDao the DAO for managing audit records of scheduled context instances; must not be null
     * @param scheduledContextInstanceAuditAggregateDao the DAO for managing aggregate audit records of scheduled context instances; must not be null
     * @param saveContextInstanceAuditRecords flag indicating whether to save regular audit records for scheduled context instances
     * @param saveContextInstanceAuditDeltaRecords flag indicating whether to save delta audit records for scheduled context instances
     * @throws IllegalArgumentException if any of the DAO parameters are null
     */
    public ScheduledContextInstanceServiceImpl(ScheduledContextInstanceDao scheduledContextInstanceDao,
                                               ScheduledContextInstanceAuditDao scheduledContextInstanceAuditDao,
                                               ScheduledContextInstanceAuditAggregateDao scheduledContextInstanceAuditAggregateDao,
                                               boolean saveContextInstanceAuditRecords, boolean saveContextInstanceAuditDeltaRecords) {
        this.scheduledContextInstanceDao = scheduledContextInstanceDao;
        if (this.scheduledContextInstanceDao == null) {
            throw new IllegalArgumentException("scheduledContextInstanceDao cannot be null!");
        }
        this.scheduledContextInstanceAuditDao = scheduledContextInstanceAuditDao;
        if (this.scheduledContextInstanceAuditDao == null) {
            throw new IllegalArgumentException("scheduledContextInstanceAuditDao cannot be null!");
        }
        this.scheduledContextInstanceAuditAggregateDao = scheduledContextInstanceAuditAggregateDao;
        if (this.scheduledContextInstanceAuditAggregateDao == null) {
            throw new IllegalArgumentException("scheduledContextInstanceAuditAggregateDao cannot be null!");
        }

        this.saveContextInstanceAuditRecords = saveContextInstanceAuditRecords;
        this.saveContextInstanceAuditDeltaRecords = saveContextInstanceAuditDeltaRecords;
    }

    @Override
    public ScheduledContextInstanceRecord findById(String id) {
        return this.scheduledContextInstanceDao.findById(id);
    }

    @Override
    public void deleteById(String id) {
        this.scheduledContextInstanceDao.deleteById(id);
    }

    @Override
    public void save(ScheduledContextInstanceRecord scheduledContextInstanceRecord) {
        this.scheduledContextInstanceDao.save(scheduledContextInstanceRecord);
    }

    @Override
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByStatus(List<InstanceStatus> instanceStatuses) {
        return this.scheduledContextInstanceDao.getScheduledContextInstancesByStatus(instanceStatuses);
    }

    @Override
    public ScheduledContextInstanceRecord findAuditRecordById(String id) {
        return this.scheduledContextInstanceAuditDao.findById(id);
    }

    @Override
    public void saveAudit(ScheduledContextInstanceAuditAggregateRecord scheduledContextInstanceAuditAggregateRecord, ContextInstance previousContextInstance,
                          ContextInstance updatedContextInstance) {
        if (this.saveContextInstanceAuditRecords) {
            ScheduledContextInstanceAuditAggregate auditAggregate = scheduledContextInstanceAuditAggregateRecord
                .getScheduledContextInstanceAuditAggregate();

            if(this.saveContextInstanceAuditDeltaRecords) {
                ScheduledContextInstanceAuditRecordImpl previousContextInstanceRecord = new ScheduledContextInstanceAuditRecordImpl();
                previousContextInstanceRecord.setContextName(previousContextInstance.getName());
                previousContextInstanceRecord.setContextInstanceId(previousContextInstance.getId());
                previousContextInstanceRecord.setContextInstance(previousContextInstance);
                this.scheduledContextInstanceAuditDao.save(previousContextInstanceRecord);

                ScheduledContextInstanceAuditRecordImpl updatedContextInstanceRecord = new ScheduledContextInstanceAuditRecordImpl();
                updatedContextInstanceRecord.setContextName(updatedContextInstance.getName());
                updatedContextInstanceRecord.setContextInstanceId(updatedContextInstance.getId());
                updatedContextInstanceRecord.setContextInstance(updatedContextInstance);
                this.scheduledContextInstanceAuditDao.save(updatedContextInstanceRecord);


                auditAggregate.setPreviousContextInstanceAuditId(previousContextInstanceRecord.getId());
                auditAggregate.setUpdatedContextInstanceAuditId(updatedContextInstanceRecord.getId());
            }

            scheduledContextInstanceAuditAggregateRecord.setScheduledContextInstanceAuditAggregate(auditAggregate);

            this.scheduledContextInstanceAuditAggregateDao.save(scheduledContextInstanceAuditAggregateRecord);
        }
    }

    @Override
    public SearchResults<ScheduledContextInstanceAuditAggregateRecord> findAllAuditRecords(int limit, int offset, String sortField, String sortDirection) {
        return this.scheduledContextInstanceAuditAggregateDao.findAll(limit, offset, sortField, sortDirection);
    }

    @Override
    public SearchResults<ScheduledContextInstanceAuditAggregateRecord> findAllAuditRecordsByFilter(ScheduledContextInstanceAuditAggregateSearchFilter filter, int limit, int offset, String sortField, String sortDirection) {
        return this.scheduledContextInstanceAuditAggregateDao.findScheduledContextInstanceAuditAggregateRecordsByFilter(filter, limit, offset, sortField, sortDirection);
    }

    @Override
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByStatus(List<InstanceStatus> instanceStatuses, int limit, int offset) {
        return this.scheduledContextInstanceDao.getScheduledContextInstancesByStatus(instanceStatuses, limit, offset);
    }

    @Override
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByContextName(String contextName, int limit, int offset
        , String sortField, String sortDirection) {
        return this.scheduledContextInstanceDao.getScheduledContextInstancesByContextName(contextName, limit, offset, sortField, sortDirection);
    }

    @Override
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByContextName(String contextName, long startTimestamp, long endTimestamp
        , int limit, int offset, String sortField, String sortDirection) {
        return this.scheduledContextInstanceDao.getScheduledContextInstancesByContextName(contextName, startTimestamp
            , endTimestamp, limit, offset, sortField, sortDirection);
    }

    @Override
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByFilter(ContextInstanceSearchFilter filter, int limit, int offset, String sortField, String sortDirection) {
        return this.scheduledContextInstanceDao.getScheduledContextInstancesByFilter(filter, limit, offset, sortField, sortDirection);
    }
}
