package org.ikasan.scheduled.notification.dao;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationContextRecordImpl;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationContextDao;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContext;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContextRecord;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import static org.ikasan.spec.entity.EntityFields.*;

public class SolrEmailNotificationContextDaoImpl extends SolrDaoBase<EmailNotificationContextRecord>
    implements EmailNotificationContextDao {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SolrEmailNotificationContextDaoImpl.class);

    private JsonMapper objectMapper = JsonMapper.builder().build();

    /**
     * We need to give this dao it's context.
     */
    public static final String EMAIL_NOTIFICATION_CONTEXT = "emailNotificationContext";

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, EmailNotificationContextRecord emailNotificationContextRecord)
    {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(TYPE, EMAIL_NOTIFICATION_CONTEXT);
        try {
            document.addField(PAYLOAD_CONTENT, getEmailNotificationContextContent(emailNotificationContextRecord.getEmailNotificationContext()));
        }
        catch (JacksonException e) {
            throw new RuntimeException(String.format("Cannot convert Email Notification Context to string! [%s]", emailNotificationContextRecord));
        }

        EmailNotificationContext emailNotificationContext = emailNotificationContextRecord.getEmailNotificationContext();

        document.addField(ID, emailNotificationContext.getContextName() );
        document.addField(COMPONENT_NAME, emailNotificationContext.getContextName()); // CONTEXT NAME
        document.addField(CREATED_DATE_TIME, emailNotificationContextRecord.getTimestamp());
        document.addField(UPDATED_DATE_TIME, System.currentTimeMillis());
        document.addField(MODIFIED_BY, emailNotificationContextRecord.getModifiedBy());
        document.setField(EXPIRY, DO_NOT_EXPIRE);

        LOGGER.debug(String.format("Converted scheduled context record to SolrDocument[%s]", document));
        return document;
    }

    private String getEmailNotificationContextContent(EmailNotificationContext emailNotificationContext)  {
        return this.objectMapper.writeValueAsString(emailNotificationContext);
    }

    @Override
    public SearchResults<EmailNotificationContextRecord> findAll(int limit, int offset) {
        StringBuffer typeBuffer = new StringBuffer();
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(EMAIL_NOTIFICATION_CONTEXT).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(typeBuffer.toString());
        solrQuery.setRows(limit);
        solrQuery.setStart(offset);

        LOGGER.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrEmailNotificationContextRecordImpl.class);
    }

    @Override
    public SearchResults<EmailNotificationContextRecord> findByContextName(String contextName, int limit, int offset) {
        StringBuffer typeBuffer = new StringBuffer();
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(EMAIL_NOTIFICATION_CONTEXT).append("\" ");
        typeBuffer.append(AND).append(" ").append(COMPONENT_NAME).append(COLON); // COMPONENT_NAME = contextName
        typeBuffer.append("\"").append(contextName).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(typeBuffer.toString());
        solrQuery.setRows(limit);
        solrQuery.setStart(offset);

        LOGGER.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrEmailNotificationContextRecordImpl.class);
    }

    @Override
    public void deleteByContextName(String contextName) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(EMAIL_NOTIFICATION_CONTEXT).append("\" ");
        queryBuffer.append(AND).append(" ").append(COMPONENT_NAME).append(COLON); // COMPONENT_NAME = contextName
        queryBuffer.append("\"").append(contextName).append("\" ");
        LOGGER.debug("deleteByContextName query: " + queryBuffer.toString());
        super.deleteByQuery(queryBuffer.toString());
    }
}
