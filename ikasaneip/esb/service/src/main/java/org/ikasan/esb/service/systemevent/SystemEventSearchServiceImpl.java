package org.ikasan.esb.service.systemevent;

import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.spec.systemevent.SystemEventSearchDao;
import org.ikasan.spec.systemevent.SystemEventSearchFilter;
import org.ikasan.spec.systemevent.SystemEventSearchService;

/**
 * Created by Ikasan Development Team on 23/09/2017.
 */
public class SystemEventSearchServiceImpl implements SystemEventSearchService
{

    private SystemEventSearchDao systemEventDao;

    /**
     * Constructor for SystemEventSearchServiceImpl, which initializes the service
     * with a specified data access object for system events.
     *
     * @param systemEventDao the data access object responsible for interacting
     *                       with the persistence layer for system events.
     *                       Must not be null, otherwise an IllegalArgumentException
     *                       will be thrown.
     */
    public SystemEventSearchServiceImpl(SystemEventSearchDao systemEventDao)
    {
        this.systemEventDao = systemEventDao;
        if(this.systemEventDao == null)
        {
            throw new IllegalArgumentException("systemEventDao cannot be null!");
        }
    }

    @Override
    public SystemEvent findById(String id) {
        return this.systemEventDao.findById(id);
    }

    @Override
    public SearchResults<SystemEvent> findByFilter(SystemEventSearchFilter searchFilter, int limit, int offset, String sortColumn, String sortOrder) {
        return this.systemEventDao.findByFilter(searchFilter, limit, offset, sortColumn, sortOrder);
    }
}
