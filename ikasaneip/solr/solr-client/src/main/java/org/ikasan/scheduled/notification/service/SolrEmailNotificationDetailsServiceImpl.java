package org.ikasan.scheduled.notification.service;

import org.ikasan.scheduled.notification.dao.SolrEmailNotificationDetailsDaoImpl;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationDetailsRecord;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrServiceBase;

import java.util.List;

public class SolrEmailNotificationDetailsServiceImpl extends SolrServiceBase implements EmailNotificationDetailsService<SolrEmailNotificationDetailsRecord>
{

    private SolrEmailNotificationDetailsDaoImpl dao;

    public SolrEmailNotificationDetailsServiceImpl(SolrEmailNotificationDetailsDaoImpl dao)
    {
        this.dao = dao;
        if(this.dao == null)
        {
            throw new IllegalArgumentException("SolrEmailNotificationDetailsDao cannot be null!");
        }
    }

    @Override
    public SearchResults<SolrEmailNotificationDetailsRecord> findAll(int limit, int offset) {
        return this.dao.findAll(limit, offset);
    }

    @Override
    public SolrEmailNotificationDetailsRecord findByJobNameAndMonitorType(String jobName, String monitorType) {
        return this.dao.findByJobNameAndMonitorType(jobName, monitorType);
    }

    @Override
    public void save(SolrEmailNotificationDetailsRecord solrEmailNotificationDetailsRecord)
    {
        this.dao.setSolrUsername(this.solrUsername);
        this.dao.setSolrPassword(this.solrPassword);
        dao.save(solrEmailNotificationDetailsRecord);
    }

    @Override
    public void save(List<SolrEmailNotificationDetailsRecord> solrEmailNotificationDetailsRecords)
    {
        this.dao.setSolrUsername(this.solrUsername);
        this.dao.setSolrPassword(this.solrPassword);
        dao.save(solrEmailNotificationDetailsRecords);
    }
}
