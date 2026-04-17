package org.ikasan.relational.persistence.scheduled.notification.service;

import org.ikasan.relational.persistence.scheduled.notification.dao.HibernateEmailNotificationDetailsDaoImpl;
import org.ikasan.relational.persistence.scheduled.notification.model.HibernateEmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.search.SearchResults;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

public class HibernateEmailNotificationDetailsServiceImpl implements EmailNotificationDetailsService {

    private HibernateEmailNotificationDetailsDaoImpl dao;

    public HibernateEmailNotificationDetailsServiceImpl(HibernateEmailNotificationDetailsDaoImpl dao) {
        this.dao = dao;
        if(this.dao == null) {
            throw new IllegalArgumentException("HibernateEmailNotificationDetailsDaoImpl cannot be null!");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<EmailNotificationDetailsRecord> findAll(int limit, int offset) {
        return this.dao.findAll(limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResults<EmailNotificationDetailsRecord> findByContextName(String contextName, int limit, int offset) {
        return this.dao.findByContextName(contextName, limit, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public EmailNotificationDetailsRecord findByJobNameAndMonitorType(String jobName, String childContextName, String monitorType) {
        return this.dao.findByJobNameAndMonitorType(jobName, childContextName, monitorType);
    }

    @Override
    @Transactional
    public void save(EmailNotificationDetailsRecord emailNotificationDetailsRecord) {
        this.dao.save(emailNotificationDetailsRecord);
    }

    @Override
    @Transactional
    public void save(List<EmailNotificationDetailsRecord> emailNotificationDetailsRecords) {
        this.dao.save(emailNotificationDetailsRecords);
    }

    @Override
    @Transactional
    public void saveEmailNotificationDetails(List<EmailNotificationDetails> emailNotificationDetails) {
        List<EmailNotificationDetailsRecord> records = new ArrayList<>();
        emailNotificationDetails.forEach(notification -> records.add(createEmailNotificationDetailsRecord(notification)));
        this.save(records);
    }

    private EmailNotificationDetailsRecord createEmailNotificationDetailsRecord(EmailNotificationDetails details) {
        EmailNotificationDetailsRecord record = new HibernateEmailNotificationDetailsRecord();
        record.setEmailNotificationDetails(details);
        record.setTimestamp(System.currentTimeMillis());
        return record;
    }

    @Override
    @Transactional
    public void deleteByContextName(String contextName) {
        this.dao.deleteByContextName(contextName);
    }

    @Override
    @Transactional
    public void deleteByJobNameAndMonitorType(String jobName, String childContextName, String monitorType) {
        this.dao.deleteByJobNameAndMonitorType(jobName, childContextName, monitorType);
    }
}
