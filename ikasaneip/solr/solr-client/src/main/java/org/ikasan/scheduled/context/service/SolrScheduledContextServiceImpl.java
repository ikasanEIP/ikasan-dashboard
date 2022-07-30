package org.ikasan.scheduled.context.service;

import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextViewDao;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.model.ScheduledContextSearchFilter;
import org.ikasan.spec.scheduled.context.model.ScheduledContextViewRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.search.SearchResults;

public class SolrScheduledContextServiceImpl implements ScheduledContextService {

    private ScheduledContextDao scheduledContextDao;
    private ScheduledContextViewDao scheduledContextViewDao;

    public SolrScheduledContextServiceImpl(ScheduledContextDao scheduledContextDao, ScheduledContextViewDao scheduledContextViewDao) {
        this.scheduledContextDao = scheduledContextDao;
        if(this.scheduledContextDao == null) {
            throw new IllegalArgumentException("dao cannot be null!");
        }
        this.scheduledContextViewDao = scheduledContextViewDao;
        if(this.scheduledContextViewDao == null) {
            throw new IllegalArgumentException("scheduledContextViewDao cannot be null!");
        }
    }

    @Override
    public SearchResults<? extends ScheduledContextRecord> findAll() {
        return this.scheduledContextDao.findAll();
    }

    @Override
    public SearchResults<? extends ScheduledContextRecord> findAll(int limit, int offset) {
        return this.scheduledContextDao.findAll(limit, offset);
    }

    @Override
    public SearchResults<ScheduledContextRecord> findByFilter(ScheduledContextSearchFilter filter, int limit, int offset, String sortColumn, String sortOrder) {
        return this.scheduledContextDao.findByFilter(filter, limit, offset, sortColumn, sortOrder);
    }

    @Override
    public ScheduledContextRecord findById(String id) {
        return this.scheduledContextDao.findById(id);
    }

    @Override
    public ScheduledContextRecord findByName(String name) {
        return this.scheduledContextDao.findByName(name);
    }

    @Override
    public void save(ScheduledContextRecord scheduledContextRecord) {
        this.scheduledContextDao.save(scheduledContextRecord);
    }

    @Override
    public ScheduledContextViewRecord getContextView(String parentContextName, String contextName) {
        return scheduledContextViewDao.getContextView(parentContextName, contextName);
    }

    @Override
    public void saveContextView(ScheduledContextViewRecord contextView) {
        this.scheduledContextViewDao.save(contextView);
    }
}
