package org.ikasan.scheduled.notification.service;

import org.ikasan.scheduled.notification.dao.SolrNotificationSendAuditDaoImpl;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAuditRecord;
import org.ikasan.spec.scheduled.notification.service.NotificationSendAuditService;
import org.ikasan.spec.solr.SolrServiceBase;

public class SolrNotificationSendAuditServiceImpl extends SolrServiceBase implements NotificationSendAuditService<NotificationSendAuditRecord>
{

    private SolrNotificationSendAuditDaoImpl dao;

    public SolrNotificationSendAuditServiceImpl(SolrNotificationSendAuditDaoImpl dao)
    {
        this.dao = dao;
        if(this.dao == null)
        {
            throw new IllegalArgumentException("SolrNotificationSendAuditDaoImpl cannot be null!");
        }
    }

    @Override
    public NotificationSendAuditRecord find(String contextInstanceId, String jobName, String monitorType, String notifierType) {
        return this.dao.find(contextInstanceId, jobName, monitorType, notifierType);
    }

    @Override
    public void save(NotificationSendAuditRecord notificationSendAuditRecord)
    {
        this.dao.setSolrUsername(this.solrUsername);
        this.dao.setSolrPassword(this.solrPassword);
        dao.save(notificationSendAuditRecord);
    }

}
