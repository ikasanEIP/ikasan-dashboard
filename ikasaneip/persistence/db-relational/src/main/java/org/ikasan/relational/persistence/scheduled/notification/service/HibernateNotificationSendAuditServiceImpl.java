package org.ikasan.relational.persistence.scheduled.notification.service;

import org.ikasan.relational.persistence.scheduled.notification.dao.HibernateNotificationSendAuditDaoImpl;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAuditRecord;
import org.ikasan.spec.scheduled.notification.service.NotificationSendAuditService;
import org.springframework.transaction.annotation.Transactional;

public class HibernateNotificationSendAuditServiceImpl implements NotificationSendAuditService<NotificationSendAuditRecord> {

    private HibernateNotificationSendAuditDaoImpl dao;

    public HibernateNotificationSendAuditServiceImpl(HibernateNotificationSendAuditDaoImpl dao) {
        this.dao = dao;
        if(this.dao == null) {
            throw new IllegalArgumentException("HibernateNotificationSendAuditDaoImpl cannot be null!");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationSendAuditRecord find(String contextInstanceId, String contextName, String jobName, String monitorType, String notifierType) {
        return this.dao.find(contextInstanceId, contextName, jobName, monitorType, notifierType);
    }

    @Override
    @Transactional
    public void save(NotificationSendAuditRecord notificationSendAuditRecord) {
        this.dao.save(notificationSendAuditRecord);
    }
}
