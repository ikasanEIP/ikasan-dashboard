package org.ikasan.scheduled.instance.service;


import java.util.List;

import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceDao;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditRecord;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.search.SearchResults;

public class SolrScheduledContextInstanceServiceImpl implements ScheduledContextInstanceService {
    private final ScheduledContextInstanceDao scheduledContextInstanceDao;
    private final ScheduledContextInstanceAuditDao scheduledContextInstanceAuditDao;
    private final boolean saveContextInstanceAuditRecords;

    public SolrScheduledContextInstanceServiceImpl(ScheduledContextInstanceDao scheduledContextInstanceDao,
                                                   ScheduledContextInstanceAuditDao scheduledContextInstanceAuditDao,
                                                   boolean saveContextInstanceAuditRecords) {
        if (scheduledContextInstanceDao == null) {
            throw new IllegalArgumentException("scheduledContextInstanceDao cannot be null!");
        }
        if (scheduledContextInstanceAuditDao == null) {
            throw new IllegalArgumentException("scheduledContextInstanceAuditDao cannot be null!");
        }

        this.scheduledContextInstanceDao = scheduledContextInstanceDao;
        this.scheduledContextInstanceAuditDao = scheduledContextInstanceAuditDao;
        this.saveContextInstanceAuditRecords = saveContextInstanceAuditRecords;
    }

    @Override
    public ScheduledContextInstanceRecord findById(String id) {
        return this.scheduledContextInstanceDao.findById(id);
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
    public void saveAudit(ScheduledContextInstanceAuditRecord scheduledContextInstanceAuditRecord) {
        if (this.saveContextInstanceAuditRecords) {
            this.scheduledContextInstanceAuditDao.save(scheduledContextInstanceAuditRecord);
        }
    }

    @Override
    public SearchResults<ScheduledContextInstanceAuditRecord> findAllAuditRecords(int limit, int offset) {
        return this.scheduledContextInstanceAuditDao.findAll(limit, offset);
    }

    @Override
    public SearchResults<ScheduledContextInstanceAuditRecord> findAllAuditRecordsByContextId(String contextId, int limit, int offset) {
        return this.scheduledContextInstanceAuditDao.findAllAuditRecordsByContextId(contextId, limit, offset);
    }

}
