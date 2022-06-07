package org.ikasan.scheduled.notification.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.notification.model.SolrNotificationSendAuditRecord;
import org.ikasan.spec.scheduled.notification.dao.NotificationSendAuditDao;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAudit;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAuditRecord;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SolrNotificationSendAuditDaoImpl extends SolrDaoBase<NotificationSendAuditRecord>
                        implements NotificationSendAuditDao<NotificationSendAuditRecord>
{
    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(SolrNotificationSendAuditDaoImpl.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * We need to give this dao it's context.
     */
    public static final String NOTIFICATION_SEND_AUDIT = "notificationSendAudit";

    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, NotificationSendAuditRecord notificationSendAuditRecord)
    {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(TYPE, NOTIFICATION_SEND_AUDIT);
        try {
            document.addField(PAYLOAD_CONTENT, getNotificationSendAuditContent(notificationSendAuditRecord.getNotificationSendAudit()));
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Cannot convert Email Notification Details to string! [%s]", notificationSendAuditRecord));
        }

        document.addField(ID, generateId(notificationSendAuditRecord));
        document.addField(CREATED_DATE_TIME, notificationSendAuditRecord.getTimestamp());
        document.addField(UPDATED_DATE_TIME, System.currentTimeMillis());
        document.addField(MODIFIED_BY, notificationSendAuditRecord.getModifiedBy());
        document.setField(EXPIRY, expiry);

        logger.debug(String.format("Converted scheduled context record to SolrDocument[%s]", document));
        return document;
    }

    private String generateId(NotificationSendAuditRecord notificationSendAuditRecord) {
        StringBuffer sb = new StringBuffer(notificationSendAuditRecord.getNotificationSendAudit().getContextInstanceId());
        sb.append("_");
        sb.append(notificationSendAuditRecord.getNotificationSendAudit().getJobName());
        sb.append("_");
        sb.append(notificationSendAuditRecord.getNotificationSendAudit().getMonitorType());
        sb.append("_");
        sb.append(notificationSendAuditRecord.getNotificationSendAudit().getNotifierType());
        return sb.toString();
    }

    private String getNotificationSendAuditContent(NotificationSendAudit notificationSendAudit) throws JsonProcessingException {
        return this.objectMapper.writeValueAsString(notificationSendAudit);
    }

    @Override
    public NotificationSendAuditRecord find(String contextInstanceId, String jobName, String monitorType, String notifierType) {
        SolrQuery query = super.buildIdQuery(contextInstanceId+"_"+jobName+"_"+monitorType+"_"+notifierType, NOTIFICATION_SEND_AUDIT);

        logger.debug("query: " + query);

        List<NotificationSendAuditRecord> beans = this.findByQuery(query, SolrNotificationSendAuditRecord.class).getResultList();

        if(beans.size() > 0)
        {
            return beans.get(0);
        }
        else
        {
            return null;
        }
    }
}
