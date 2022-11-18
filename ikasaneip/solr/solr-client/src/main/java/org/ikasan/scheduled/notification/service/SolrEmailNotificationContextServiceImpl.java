package org.ikasan.scheduled.notification.service;

import org.ikasan.scheduled.notification.dao.SolrEmailNotificationContextDaoImpl;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationContextRecordImpl;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContextRecord;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrServiceBase;

public class SolrEmailNotificationContextServiceImpl extends SolrServiceBase implements EmailNotificationContextService {

    private SolrEmailNotificationContextDaoImpl dao;

    public SolrEmailNotificationContextServiceImpl(SolrEmailNotificationContextDaoImpl dao) {
        this.dao = dao;
        if(this.dao == null) {
            throw new IllegalArgumentException("SolrEmailNotificationContextDaoImpl cannot be null!");
        }
    }

    @Override
    public SearchResults<EmailNotificationContextRecord> findAll(int limit, int offset) {
        return this.dao.findAll(limit, offset);
    }

    @Override
    public SearchResults<EmailNotificationContextRecord> findByContextName(String contextName, int limit, int offset) {
        return this.dao.findByContextName(contextName, limit, offset);
    }

    @Override
    public void save(EmailNotificationContextRecord emailNotificationContextRecord) {
        this.dao.setSolrUsername(this.solrUsername);
        this.dao.setSolrPassword(this.solrPassword);
        this.dao.save(emailNotificationContextRecord);
    }

    @Override
    public void saveEmailNotificationContext(EmailNotificationContext emailNotificationContext) {
        EmailNotificationContextRecord record = new SolrEmailNotificationContextRecordImpl();
        record.setEmailNotificationContext(emailNotificationContext);
        record.setTimestamp(System.currentTimeMillis());
        // Rest of the details for the record will be set by SolrEmailNotificationContextDaoImpl
        this.save(record);
    }

    @Override
    public void deleteByContextName(String contextName) {
        this.dao.deleteByContextName(contextName);
    }
}
