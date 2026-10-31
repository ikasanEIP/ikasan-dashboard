package org.ikasan.rest.dashboard.util;

import org.ikasan.spec.scheduled.context.ScheduledContextRecordLite;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.model.ScheduledContextSearchFilter;
import org.ikasan.spec.scheduled.context.model.ScheduledContextViewRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.search.SearchResults;

import java.util.HashMap;
import java.util.Map;

public class TestScheduledContextService implements ScheduledContextService {

    private Map<String, ScheduledContextRecord> contextMap = new HashMap<>();

    public void save(String key, ScheduledContextRecord scheduledContextRecord) {
        contextMap.put(key, scheduledContextRecord);
    }

    @Override
    public SearchResults<? extends ScheduledContextRecord> findAll() {
        return null;
    }

    @Override
    public SearchResults<? extends ScheduledContextRecord> findAll(int limit, int offset) {
        return null;
    }

    @Override
    public SearchResults<ScheduledContextRecord> findByFilter(ScheduledContextSearchFilter filter, int limit, int offset, String sortColumn, String sortOrder) {
        return null;
    }

    @Override
    public SearchResults<ScheduledContextRecordLite> findByFilterLite(ScheduledContextSearchFilter filter, int limit, int offset, String sortColumn, String sortOrder) {
        return null;
    }

    @Override
    public ScheduledContextRecord findById(String id) {
        return contextMap.get(id);
    }

    @Override
    public ScheduledContextRecord findByName(String name) {
        return contextMap.get(name);
    }

    @Override
    public void save(ScheduledContextRecord scheduledContextRecord) {
        if (scheduledContextRecord != null && scheduledContextRecord.getContextName() != null) {
            contextMap.put(scheduledContextRecord.getContextName(), scheduledContextRecord);
        }
    }

    @Override
    public ScheduledContextViewRecord getContextView(String parentContextName, String contextName) {
        return null;
    }

    @Override
    public void saveContextView(ScheduledContextViewRecord contextView) {
    }

    @Override
    public void deleteContext(String contextName) {
        contextMap.remove(contextName);
    }

    @Override
    public ScheduledContextRecord cloneContext(String contextName, String clonedContextName) {
        return null;
    }

    @Override
    public void enableScheduledJobs(ContextTemplate contextTemplate, String modifiedBy) {
    }

    @Override
    public void disableScheduledJobs(ContextTemplate contextTemplate, String modifiedBy) {
    }

    public void clear() {
        contextMap.clear();
    }
}
