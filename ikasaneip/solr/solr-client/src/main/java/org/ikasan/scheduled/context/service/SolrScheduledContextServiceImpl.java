package org.ikasan.scheduled.context.service;

import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.model.ScheduledContextSearchFilter;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.search.SearchResults;

public class SolrScheduledContextServiceImpl implements ScheduledContextService {

    private ScheduledContextDao dao;

    public SolrScheduledContextServiceImpl(ScheduledContextDao dao) {
        this.dao = dao;
        if(this.dao == null) {
            throw new IllegalArgumentException("dao cannot be null!");
        }
    }

    @Override
    public SearchResults<? extends ScheduledContextRecord> findAll() {
        return this.dao.findAll();
    }

    @Override
    public SearchResults<? extends ScheduledContextRecord> findAll(int limit, int offset) {
        return this.dao.findAll(limit, offset);
    }

    @Override
    public SearchResults<ScheduledContextRecord> findByFilter(ScheduledContextSearchFilter filter, int limit, int offset, String sortColumn, String sortOrder) {
        return this.dao.findByFilter(filter, limit, offset, sortColumn, sortOrder);
    }

    @Override
    public ScheduledContextRecord findById(String id) {
        return this.dao.findById(id);
    }

    @Override
    public ScheduledContextRecord findByName(String name) {
        return this.dao.findByName(name);
    }

    @Override
    public void save(ScheduledContextRecord scheduledContextRecord) {
        this.dao.save(scheduledContextRecord);
    }
}
