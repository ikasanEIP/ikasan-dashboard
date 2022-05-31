package org.ikasan.notification.monitor.mock;

import org.ikasan.spec.scheduled.instance.model.ContextInstanceSearchFilter;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditRecord;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.search.SearchResults;

import java.util.List;

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
    public void saveAudit(ScheduledContextInstanceAuditRecord scheduledContextInstanceAuditRecord) {
    }

    @Override
    public SearchResults<ScheduledContextInstanceAuditRecord> findAllAuditRecords(int limit, int offset) {
        return null;
    }

    @Override
    public SearchResults<ScheduledContextInstanceAuditRecord> findAllAuditRecordsByContextId(String contextId, int limit, int offset) {
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
