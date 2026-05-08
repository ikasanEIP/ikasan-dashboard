package org.ikasan.orchestration.service.scheduled.notification;

import org.ikasan.job.orchestration.model.notification.EmailNotificationDetailsRecordImpl;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationDetailsDao;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.search.SearchResults;

import java.util.ArrayList;
import java.util.List;

/**
 * A service implementation that interacts with a Solr-based data store to manage email notification details.
 * This class provides methods for CRUD operations, as well as functionality to query email notification details
 * based on context or job-specific criteria. It leverages an underlying DAO for Solr operations.
 */
public class EmailNotificationDetailsServiceImpl implements EmailNotificationDetailsService
{

    private final EmailNotificationDetailsDao dao;


    /**
     * Constructs a new instance of EmailNotificationDetailsServiceImpl with the provided
     * EmailNotificationDetailsDao.
     *
     * @param dao The DAO responsible for interacting with the Solr-based data store to manage
     *            email notification details. Must not be null; passing a null value will
     *            result in an IllegalArgumentException.
     */
    public EmailNotificationDetailsServiceImpl(EmailNotificationDetailsDao dao)
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
        dao.save(emailNotificationDetailsRecord);
    }

    @Override
    public void save(List<EmailNotificationDetailsRecord> emailNotificationDetailsRecords)
    {
        dao.save(emailNotificationDetailsRecords);
    }

    @Override
    public void saveEmailNotificationDetails(List<EmailNotificationDetails> emailNotificationDetails) {
        List<EmailNotificationDetailsRecord> records = new ArrayList<>();
        emailNotificationDetails.forEach(notification -> records.add(createEmailNotificationDetailsRecord(notification)));
        this.save(records);
    }

    private EmailNotificationDetailsRecord createEmailNotificationDetailsRecord(EmailNotificationDetails details) {
        EmailNotificationDetailsRecord record = new EmailNotificationDetailsRecordImpl();
        record.setEmailNotificationDetails(details);
        record.setTimestamp(System.currentTimeMillis());

        return record;
    }

    @Override
    public void deleteByContextName(String contextName) { this.dao.deleteByContextName(contextName); }

    @Override
    public void deleteByJobNameAndMonitorType(String jobName, String childContextName, String monitorType) {
        this.dao.deleteByJobNameAndMonitorType(jobName, childContextName, monitorType);
    }
}
