package org.ikasan.scheduled.notification.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationDetailsDao;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetails;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SolrEmailNotificationDetailsDaoImpl extends SolrDaoBase<EmailNotificationDetailsRecord>
                        implements EmailNotificationDetailsDao
{
    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(SolrEmailNotificationDetailsDaoImpl.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * We need to give this dao it's context.
     */
    public static final String EMAIL_NOTIFICATION_DETAILS = "emailNotificationDetails";

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, EmailNotificationDetailsRecord emailNotificationDetailsRecord)
    {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(TYPE, EMAIL_NOTIFICATION_DETAILS);
        try {
            document.addField(PAYLOAD_CONTENT, getEmailNotificationDetailsContent(emailNotificationDetailsRecord.getEmailNotificationDetails()));
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Cannot convert Email Notification Details to string! [%s]", emailNotificationDetailsRecord));
        }

        EmailNotificationDetails emailNotificationDetails = emailNotificationDetailsRecord.getEmailNotificationDetails();

        document.addField(ID, emailNotificationDetails.getJobName()+"_"+emailNotificationDetails.getChildContextName()+"_"+emailNotificationDetails.getMonitorType() );
        document.addField(MODULE_NAME, emailNotificationDetails.getJobName()); // JOB NAME
        document.addField(COMPONENT_NAME, emailNotificationDetails.getContextName()); // CONTEXT NAME
        document.addField(RELATED_EVENT, emailNotificationDetails.getMonitorType()); // MONITOR TYPE
        document.addField(CREATED_DATE_TIME, emailNotificationDetailsRecord.getTimestamp());
        document.addField(UPDATED_DATE_TIME, System.currentTimeMillis());
        document.addField(MODIFIED_BY, emailNotificationDetailsRecord.getModifiedBy());
        document.setField(EXPIRY, expiry);

        logger.debug(String.format("Converted scheduled context record to SolrDocument[%s]", document));
        return document;
    }

    private String getEmailNotificationDetailsContent(EmailNotificationDetails emailNotificationDetails) throws JsonProcessingException {
        return this.objectMapper.writeValueAsString(emailNotificationDetails);
    }

    @Override
    public SearchResults<EmailNotificationDetailsRecord> findAll(int limit, int offset) {
        StringBuffer typeBuffer = new StringBuffer();
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(EMAIL_NOTIFICATION_DETAILS).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(typeBuffer.toString());
        solrQuery.setRows(limit);
        solrQuery.setStart(offset);

        logger.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrEmailNotificationDetailsRecord.class);
    }

    @Override
    public SearchResults<EmailNotificationDetailsRecord> findByContextName(String contextName, int limit, int offset) {
        StringBuffer typeBuffer = new StringBuffer();
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(EMAIL_NOTIFICATION_DETAILS).append("\" ");
        typeBuffer.append(AND).append(" ").append(COMPONENT_NAME).append(COLON); // COMPONENT_NAME = contextName
        typeBuffer.append("\"").append(contextName).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(typeBuffer.toString());
        solrQuery.setRows(limit);
        solrQuery.setStart(offset);

        logger.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrEmailNotificationDetailsRecord.class);
    }

    @Override
    public EmailNotificationDetailsRecord findByJobNameAndMonitorType(String jobName, String contextName, String monitorType) {
        SolrQuery query = super.buildIdQuery(jobName+"_"+contextName+"_"+monitorType, EMAIL_NOTIFICATION_DETAILS);

        logger.debug("query: " + query);

        List<EmailNotificationDetailsRecord> beans = this.findByQuery(query, SolrEmailNotificationDetailsRecord.class).getResultList();

        if(beans.size() > 0)
        {
            return beans.get(0);
        }
        else
        {
            return null;
        }
    }

    @Override
    public void deleteByContextName(String contextName) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(EMAIL_NOTIFICATION_DETAILS).append("\" ");
        queryBuffer.append(AND).append(" ").append(COMPONENT_NAME).append(COLON); // COMPONENT_NAME = contextName
        queryBuffer.append("\"").append(contextName).append("\" ");
        logger.debug("deleteByContextName query: " + queryBuffer.toString());
        super.deleteByQuery(queryBuffer.toString());
    }
}
