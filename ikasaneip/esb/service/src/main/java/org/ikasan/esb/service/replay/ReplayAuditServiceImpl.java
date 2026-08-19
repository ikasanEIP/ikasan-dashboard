package org.ikasan.esb.service.replay;

import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.replay.ReplayAuditEvent;

import java.util.List;

public class ReplayAuditServiceImpl implements BatchInsert<ReplayAuditEvent>
{
    private EntityDao<ReplayAuditEvent> solrReplayAuditDao;

    /**
     * Constructor for ReplayAuditServiceImpl.
     * This initializes the service with a provided data access object for managing ReplayAuditEvent entities.
     * Throws an IllegalArgumentException if the provided DAO is null.
     *
     * @param solrReplayAuditDao the data access object responsible for handling ReplayAuditEvent entities.
     *                           Must not be null.
     */
    public ReplayAuditServiceImpl(EntityDao<ReplayAuditEvent> solrReplayAuditDao)
    {
        this.solrReplayAuditDao = solrReplayAuditDao;
        if(this.solrReplayAuditDao == null)
        {
            throw new IllegalArgumentException("solrReplayAuditDao cannot be null!");
        }
    }

    @Override
    public void insert(List<ReplayAuditEvent> entities) {
        this.solrReplayAuditDao.save(entities);
    }
}
