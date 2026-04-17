package org.ikasan.relational.persistence.scheduled.notification.service;

import org.ikasan.relational.persistence.scheduled.notification.dao.HibernateEmailNotificationContextDaoImpl;
import org.ikasan.relational.persistence.scheduled.notification.model.HibernateEmailNotificationContextRecord;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContextRecord;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.search.SearchResults;
import org.springframework.transaction.annotation.Transactional;

public class HibernateEmailNotificationContextServiceImpl implements EmailNotificationContextService {

    private HibernateEmailNotificationContextDaoImpl dao;

    public HibernateEmailNotificationContextServiceImpl(HibernateEmailNotificationContextDaoImpl dao) {
        this.dao = dao;
        if(this.dao == null) {
            throw new IllegalArgumentException("HibernateEmailNotificationContextDaoImpl cannot be null!");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<EmailNotificationContextRecord> findAll(int limit, int offset) {
        return this.dao.findAll(limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<EmailNotificationContextRecord> findByContextName(String contextName, int limit, int offset) {
        return this.dao.findByContextName(contextName, limit, offset);
    }

    @Override
    @Transactional
    public void save(EmailNotificationContextRecord emailNotificationContextRecord) {
        this.dao.save(emailNotificationContextRecord);
    }

    @Override
    @Transactional
    public void saveEmailNotificationContext(EmailNotificationContext emailNotificationContext) {
        EmailNotificationContextRecord record = new HibernateEmailNotificationContextRecord();
        record.setEmailNotificationContext(emailNotificationContext);
        record.setTimestamp(System.currentTimeMillis());
        this.save(record);
    }

    @Override
    @Transactional
    public void deleteByContextName(String contextName) {
        this.dao.deleteByContextName(contextName);
    }
}
