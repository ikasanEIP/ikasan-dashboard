package org.ikasan.orchestration.service.scheduled.context;

import org.ikasan.scheduled.context.model.ScheduledContextRecordLiteImpl;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.spec.scheduled.context.ScheduledContextRecordLite;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextViewDao;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.model.ScheduledContextSearchFilter;
import org.ikasan.spec.scheduled.context.model.ScheduledContextViewRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.search.SearchResults;

import java.util.List;
import java.util.stream.Collectors;

public class ScheduledContextServiceImpl implements ScheduledContextService {

    private ScheduledContextDao scheduledContextDao;
    private ScheduledContextViewDao scheduledContextViewDao;

    public ScheduledContextServiceImpl(ScheduledContextDao scheduledContextDao, ScheduledContextViewDao scheduledContextViewDao) {
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
    public SearchResults<ScheduledContextRecordLite> findByFilterLite(ScheduledContextSearchFilter filter, int limit
        , int offset, String sortColumn, String sortOrder) {
        SearchResults<ScheduledContextRecord> searchResults = this.findByFilter(filter, limit, offset, sortColumn, sortOrder);
        List<ScheduledContextRecordLite> records = searchResults.getResultList().stream()
            .map(scheduledContextRecord -> {
                ScheduledContextRecordLite scheduledContextRecordLite = new ScheduledContextRecordLiteImpl();
                scheduledContextRecordLite.setId(scheduledContextRecord.getId());
                scheduledContextRecordLite.setContextName(scheduledContextRecord.getContextName());
                scheduledContextRecordLite.setDescription(scheduledContextRecord.getContext().getDescription());
                scheduledContextRecordLite.setTimestamp(scheduledContextRecord.getModifiedTimestamp());
                scheduledContextRecordLite.setModifiedBy(scheduledContextRecord.getModifiedBy());
                scheduledContextRecordLite.setModifiedTimestamp(scheduledContextRecord.getModifiedTimestamp());
                scheduledContextRecordLite.setDisabled(scheduledContextRecord.isDisabled());
                scheduledContextRecordLite.setQuartzScheduleDrivenJobsDisabledForContext
                    (scheduledContextRecord.isQuartzScheduleDrivenJobsDisabledForContext());

                return scheduledContextRecordLite;
            })
            .collect(Collectors.toList());

        return new SearchResultsImpl<>(records, searchResults.getTotalNumberOfResults(), searchResults.getQueryResponseTime());
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

    @Override
    public void deleteContext(String contextName) {
        this.scheduledContextDao.deleteContext(contextName);
    }

    @Override
    public ScheduledContextRecord cloneContext(String contextName, String clonedContextName) {
        return null;
    }

    @Override
    public void enableScheduledJobs(ContextTemplate contextTemplate, String modifiedBy) {
        this.enableDisableScheduledJobs(contextTemplate, modifiedBy, false);
    }

    @Override
    public void disableScheduledJobs(ContextTemplate contextTemplate, String modifiedBy) {
        this.enableDisableScheduledJobs(contextTemplate, modifiedBy, true);
    }

    /**
     * Helper method to enable/disable scheduled jobs.
     *
     * @param disabled
     */
    private void enableDisableScheduledJobs(ContextTemplate contextTemplate, String modifiedBy, boolean disabled) {
        contextTemplate.setQuartzScheduleDrivenJobsDisabledForContext(disabled);
        ScheduledContextRecord scheduledContextRecord = this.findByName(contextTemplate.getName());
        scheduledContextRecord.setContext(contextTemplate);
        scheduledContextRecord.setModifiedBy(modifiedBy);
        this.save(scheduledContextRecord);
    }
}
