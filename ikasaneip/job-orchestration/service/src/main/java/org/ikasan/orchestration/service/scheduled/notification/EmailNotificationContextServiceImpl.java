package org.ikasan.orchestration.service.scheduled.notification;

import org.ikasan.job.orchestration.model.notification.EmailNotificationContextRecordImpl;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationContextDao;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContextRecord;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.search.SearchResults;

public class EmailNotificationContextServiceImpl implements EmailNotificationContextService {

    private final EmailNotificationContextDao dao;

    /**
     * Constructs a new instance of EmailNotificationContextServiceImpl with the provided
     * EmailNotificationContextDao.
     *
     * @param dao The DAO responsible for interacting with the Solr-based data store to manage
     *            email notification contexts. Must not be null; passing a null value will result
     *            in an IllegalArgumentException.
     */
    public EmailNotificationContextServiceImpl(EmailNotificationContextDao dao) {
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
        this.dao.save(emailNotificationContextRecord);
    }

    @Override
    public void saveEmailNotificationContext(EmailNotificationContext emailNotificationContext) {
        EmailNotificationContextRecord record = new EmailNotificationContextRecordImpl();
        record.setEmailNotificationContext(emailNotificationContext);
        record.setTimestamp(System.currentTimeMillis());

        this.save(record);
    }

    @Override
    public void deleteByContextName(String contextName) {
        this.dao.deleteByContextName(contextName);
    }
}
