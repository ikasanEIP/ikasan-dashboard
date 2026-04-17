package org.ikasan.systemevent.service;

import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrServiceBase;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.spec.systemevent.SystemEventSearchFilter;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.ikasan.systemevent.dao.SolrSystemEventDaoImpl;

/**
 * Created by Ikasan Development Team on 23/09/2017.
 */
public class SolrSystemEventSearchServiceImpl extends SolrServiceBase implements SystemEventSearchService
{

    private SolrSystemEventDaoImpl systemEventDao;

    public SolrSystemEventSearchServiceImpl(SolrSystemEventDaoImpl systemEventDao)
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
