package org.ikasan.orchestration.service.status;

import java.util.List;

import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditRecord;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.search.SearchResults;

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
}
