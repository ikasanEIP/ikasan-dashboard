package org.ikasan.scheduled.context.service;

import org.ikasan.spec.scheduled.context.dao.ScheduledContextInstanceDao;
import org.ikasan.spec.scheduled.context.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.scheduled.context.service.ScheduledContextInstanceService;

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
}
