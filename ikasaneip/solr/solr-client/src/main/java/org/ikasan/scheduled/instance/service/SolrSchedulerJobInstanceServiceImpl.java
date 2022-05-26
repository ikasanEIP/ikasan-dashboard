package org.ikasan.scheduled.instance.service;

import org.ikasan.scheduled.instance.dao.SolrSchedulerJobInstanceDaoImpl;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceSearchFilter;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.search.SearchResults;

public class SolrSchedulerJobInstanceServiceImpl implements SchedulerJobInstanceService {

    private SolrSchedulerJobInstanceDaoImpl solrSchedulerJobInstanceDao;


    public SolrSchedulerJobInstanceServiceImpl(SolrSchedulerJobInstanceDaoImpl solrSchedulerJobInstanceDao) {
        this.solrSchedulerJobInstanceDao = solrSchedulerJobInstanceDao;
        if (solrSchedulerJobInstanceDao == null) {
            throw new IllegalArgumentException("solrSchedulerJobInstanceDao cannot be null!");
        }
    }

    @Override
    public SchedulerJobInstanceRecord findById(String id) {
        return this.solrSchedulerJobInstanceDao.findById(id);
    }

    @Override
    public void save(SchedulerJobInstanceRecord scheduledContextInstanceRecord) {
        this.solrSchedulerJobInstanceDao.save(scheduledContextInstanceRecord);
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextInstanceId(String contextInstanceId, int limit, int offset, String sortField, String sortDirection) {
        return this.solrSchedulerJobInstanceDao.getSchedulerJobInstancesByContextInstanceId(contextInstanceId, limit, offset, sortField, sortDirection);
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextName(String contextName, int limit, int offset, String sortField, String sortDirection) {
        return this.getSchedulerJobInstancesByContextName(contextName, limit, offset, sortField, sortDirection);
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getScheduledContextInstancesByFilter(SchedulerJobInstanceSearchFilter filter, int limit, int offset, String sortField, String sortDirection) {
        return this.getScheduledContextInstancesByFilter(filter, limit, offset, sortField, sortDirection);
    }
}
