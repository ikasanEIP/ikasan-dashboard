package org.ikasan.esb.service.systemevent;

import org.ikasan.spec.entity.EsbEntityDao;
import org.ikasan.spec.entity.EsbEntityService;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.systemevent.SystemEvent;

import java.util.List;

/**
 * Created by Ikasan Development Team on 23/09/2017.
 */
public class SystemEventServiceImpl
    implements BatchInsert<SystemEvent>, EsbEntityService<SystemEvent>
{
    private EsbEntityDao<SystemEvent> systemEventDao;

    /**
     * Constructor for SystemEventServiceImpl, which initializes the service
     * with a specified data access object for system events.
     *
     * @param systemEventDao the data access object responsible for interacting
     *                       with the persistence layer for system events.
     *                       Must not be null, otherwise an IllegalArgumentException
     *                       will be thrown.
     */
    public SystemEventServiceImpl(EsbEntityDao<SystemEvent> systemEventDao) {
        this.systemEventDao = systemEventDao;
        if(this.systemEventDao == null) {
            throw new IllegalArgumentException("systemEventDao cannot be null!");
        }
    }

    @Override
    public void insert(List<SystemEvent> systemEvents)
    {
        this.save(systemEvents);
    }

    @Override
    public void save(SystemEvent systemEvent) {
        systemEventDao.save(systemEvent);
    }

    @Override
    public void save(List<SystemEvent> systemEvents) {
        systemEventDao.save(systemEvents);
    }
}
