package org.ikasan.scheduled.notification.service;

import org.ikasan.scheduled.notification.dao.SolrEmailNotificationDetailsDaoImpl;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrServiceBase;

import java.util.List;

public class SolrEmailNotificationDetailsServiceImpl extends SolrServiceBase implements EmailNotificationDetailsService<EmailNotificationDetailsRecord>
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
    public SearchResults<EmailNotificationDetailsRecord> findAll(int limit, int offset) {
        return this.dao.findAll(limit, offset);
    }

    @Override
    public EmailNotificationDetailsRecord findByJobNameAndMonitorType(String jobName, String monitorType) {
        return this.dao.findByJobNameAndMonitorType(jobName, monitorType);
    }

    @Override
    public void save(EmailNotificationDetailsRecord emailNotificationDetailsRecord)
    {
        this.dao.setSolrUsername(this.solrUsername);
        this.dao.setSolrPassword(this.solrPassword);
        dao.save(emailNotificationDetailsRecord);
    }

    @Override
    public void save(List<EmailNotificationDetailsRecord> emailNotificationDetailsRecords)
    {
        this.dao.setSolrUsername(this.solrUsername);
        this.dao.setSolrPassword(this.solrPassword);
        dao.save(emailNotificationDetailsRecords);
    }
}
