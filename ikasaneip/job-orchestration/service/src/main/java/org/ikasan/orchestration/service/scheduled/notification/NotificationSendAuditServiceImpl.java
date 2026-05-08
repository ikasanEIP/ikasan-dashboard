package org.ikasan.orchestration.service.scheduled.notification;

import org.ikasan.spec.scheduled.notification.dao.NotificationSendAuditDao;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAuditRecord;
import org.ikasan.spec.scheduled.notification.service.NotificationSendAuditService;

public class NotificationSendAuditServiceImpl implements NotificationSendAuditService<NotificationSendAuditRecord>
{

    private final NotificationSendAuditDao<NotificationSendAuditRecord> dao;

    /**
     * Constructs a new instance of NotificationSendAuditServiceImpl with the provided
     * NotificationSendAuditDao.
     *
     * @param dao The DAO responsible for interacting with the Solr-based data store to manage
     *            notification send audit records. Must not be null; passing a null value will result
     *            in an IllegalArgumentException.
     */
    public NotificationSendAuditServiceImpl(NotificationSendAuditDao<NotificationSendAuditRecord> dao)
    {
        this.dao = dao;
        if(this.dao == null)
        {
            throw new IllegalArgumentException("SolrNotificationSendAuditDaoImpl cannot be null!");
        }
    }

    @Override
    public NotificationSendAuditRecord find(String contextInstanceId, String contextName, String jobName, String monitorType, String notifierType) {
        return this.dao.find(contextInstanceId, contextName, jobName, monitorType, notifierType);
    }

    @Override
    public void save(NotificationSendAuditRecord notificationSendAuditRecord)
    {
        dao.save(notificationSendAuditRecord);
    }

}
