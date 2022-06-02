package org.ikasan.scheduled.notification.service;

import org.ikasan.job.orchestration.model.notification.EmailNotificationDetails;
import org.ikasan.scheduled.notification.dao.SolrEmailNotificationDetailsDao;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrServiceBase;

import java.util.List;

public class SolrEmailNotificationDetailsServiceImpl extends SolrServiceBase implements EmailNotificationDetailsService<EmailNotificationDetails>
{

    private SolrEmailNotificationDetailsDao dao;

    public SolrEmailNotificationDetailsServiceImpl(SolrEmailNotificationDetailsDao dao)
    {
        this.dao = dao;
        if(this.dao == null)
        {
            throw new IllegalArgumentException("SolrEmailNotificationDetailsDao cannot be null!");
        }
    }

    @Override
    public SearchResults<? extends EmailNotificationDetails> findAll(int limit, int offset) {
        return this.dao.findAll(limit, offset);
    }

    @Override
    public EmailNotificationDetails findByJobNameAndMonitorType(String jobName, String monitorType) {
        return this.dao.findByJobNameAndMonitorType(jobName, monitorType);
    }

    @Override
    public void save(EmailNotificationDetails emailNotificationDetails)
    {
        this.dao.setSolrUsername(this.solrUsername);
        this.dao.setSolrPassword(this.solrPassword);
        dao.save(emailNotificationDetails);
    }

    @Override
    public void save(List<EmailNotificationDetails> emailNotificationDetails)
    {
        this.dao.setSolrUsername(this.solrUsername);
        this.dao.setSolrPassword(this.solrPassword);
        dao.save(emailNotificationDetails);

    }
}
