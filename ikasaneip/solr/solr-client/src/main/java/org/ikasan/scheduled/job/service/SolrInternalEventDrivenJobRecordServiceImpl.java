package org.ikasan.scheduled.job.service;

import org.ikasan.spec.scheduled.job.dao.InternalEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.search.SearchResults;

public class SolrInternalEventDrivenJobRecordServiceImpl implements InternalEventDrivenJobService {

    private InternalEventDrivenJobDao internalEventDrivenJobRecordDao;

    public SolrInternalEventDrivenJobRecordServiceImpl(InternalEventDrivenJobDao internalEventDrivenJobRecordDao) {
        this.internalEventDrivenJobRecordDao = internalEventDrivenJobRecordDao;
        if(this.internalEventDrivenJobRecordDao == null) {
            throw new IllegalArgumentException("internalEventDrivenJobRecordDao cannot be null!");
        }
    }

    @Override
    public SearchResults<InternalEventDrivenJobRecord> findAll(int limit, int offset) {
        return this.internalEventDrivenJobRecordDao.findAll(limit, offset);
    }

    @Override
    public SearchResults<InternalEventDrivenJobRecord> findByContext(String contextId, int limit, int offset) {
        return this.internalEventDrivenJobRecordDao.findByContext(contextId, limit, offset);
    }

    @Override
    public InternalEventDrivenJobRecord findById(String id) {
        return null;
    }

    @Override
    public void save(InternalEventDrivenJobRecord internalEventDrivenJobRecord) {
        this.internalEventDrivenJobRecordDao.save(internalEventDrivenJobRecord);
    }
}
