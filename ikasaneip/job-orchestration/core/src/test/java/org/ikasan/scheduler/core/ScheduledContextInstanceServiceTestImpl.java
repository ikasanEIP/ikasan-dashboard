package org.ikasan.scheduler.core;


import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
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
}
