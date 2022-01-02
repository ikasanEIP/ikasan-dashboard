package org.ikasan.scheduled.instance.service;


import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceDao;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;

import java.util.List;

public class SolrScheduledContextInstanceServiceImpl implements ScheduledContextInstanceService {
    private ScheduledContextInstanceDao scheduledContextInstanceDao;

    public SolrScheduledContextInstanceServiceImpl(ScheduledContextInstanceDao scheduledContextInstanceDao) {
        this.scheduledContextInstanceDao = scheduledContextInstanceDao;
        if(this.scheduledContextInstanceDao == null) {
            throw new IllegalArgumentException("scheduledContextInstanceDao cannot be null!");
        }
    }

    @Override
    public ScheduledContextInstanceRecord findById(String id) {
        return null;
    }

    @Override
    public void save(ScheduledContextInstanceRecord scheduledContextInstanceRecord) {
        this.scheduledContextInstanceDao.save(scheduledContextInstanceRecord);
    }

    @Override
    public List<? extends ScheduledContextInstanceRecord> getScheduledContextInstancesByStatus(List<InstanceStatus> instanceStatuses) {
        return this.scheduledContextInstanceDao.getScheduledContextInstancesByStatus(instanceStatuses);
    }
}
