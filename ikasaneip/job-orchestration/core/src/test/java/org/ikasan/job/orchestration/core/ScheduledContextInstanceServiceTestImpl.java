package org.ikasan.job.orchestration.core;


import java.util.List;

import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.search.SearchResults;

// TODO bad practice to be mocked!
public class ScheduledContextInstanceServiceTestImpl implements ScheduledContextInstanceService {

    @Override
    public ScheduledContextInstanceRecord findById(String id) {
        return null;
    }

    @Override
    public void save(ScheduledContextInstanceRecord scheduledContextInstanceRecord) {
    }

    @Override
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByStatus(List<InstanceStatus> instanceStatuses) {
        return null;
    }

    @Override
    public void saveAudit(ScheduledContextInstanceAuditAggregateRecord scheduledContextInstanceAuditAggregateRecord, ContextInstance previousContextInstance, ContextInstance updatedContextInstance) {

    }

    @Override
    public ScheduledContextInstanceRecord findAuditRecordById(String id) {
        return null;
    }

    @Override
    public SearchResults<ScheduledContextInstanceAuditAggregateRecord> findAllAuditRecords(int limit, int offset, String sortField, String sortDirection) {
        return null;
    }

    @Override
    public SearchResults<ScheduledContextInstanceAuditAggregateRecord> findAllAuditRecordsByFilter(ScheduledContextInstanceAuditAggregateSearchFilter filter, int limit, int offset, String sortField, String sortDirection) {
        return null;
    }


    @Override
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByStatus(List<InstanceStatus> instanceStatuses, int limit, int offset) {
        return null;
    }

    @Override
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByContextName(String contextName, int limit, int offset, String sortField, String sortDirection) {
        return null;
    }

    @Override
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByContextName(String contextName, long startTimestamp, long endTimestamp, int limit, int offset, String sortField, String sortDirection) {
        return null;
    }

    @Override
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByFilter(ContextInstanceSearchFilter filter, int limit, int offset, String sortField, String sortDirection) {
        return null;
    }
}
