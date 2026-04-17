package org.ikasan.scheduled.notification.service;

import org.ikasan.scheduled.notification.dao.SolrEmailNotificationDetailsDaoImpl;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrServiceBase;

import java.util.ArrayList;
import java.util.List;

public class SolrEmailNotificationDetailsServiceImpl extends SolrServiceBase implements EmailNotificationDetailsService
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
    public SearchResults<EmailNotificationDetailsRecord> findByContextName(String contextName, int limit, int offset) {
        return this.dao.findByContextName(contextName, limit, offset);
    }

    @Override
    public EmailNotificationDetailsRecord findByJobNameAndMonitorType(String jobName, String childContextName, String monitorType) {
        return this.dao.findByJobNameAndMonitorType(jobName,childContextName, monitorType);
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

    @Override
    public void saveEmailNotificationDetails(List<EmailNotificationDetails> emailNotificationDetails) {
        List<EmailNotificationDetailsRecord> records = new ArrayList<>();
        emailNotificationDetails.forEach(notification -> records.add(createEmailNotificationDetailsRecord(notification)));
        this.save(records);
    }

    private EmailNotificationDetailsRecord createEmailNotificationDetailsRecord(EmailNotificationDetails details) {
        EmailNotificationDetailsRecord record = new SolrEmailNotificationDetailsRecord();
        record.setEmailNotificationDetails(details);
        record.setTimestamp(System.currentTimeMillis());
        // Rest of the details for the record will be set by SolrEmailNotificationDetailsDaoImpl
        return record;
    }

    @Override
    public void deleteByContextName(String contextName) { this.dao.deleteByContextName(contextName); }

    @Override
    public void deleteByJobNameAndMonitorType(String jobName, String childContextName, String monitorType) {
        this.dao.deleteByJobNameAndMonitorType(jobName, childContextName, monitorType);
    }
}
