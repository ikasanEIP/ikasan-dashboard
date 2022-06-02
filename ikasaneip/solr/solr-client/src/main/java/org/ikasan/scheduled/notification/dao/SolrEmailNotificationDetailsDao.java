package org.ikasan.scheduled.notification.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.job.orchestration.model.notification.EmailNotificationDetails;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationDetails;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SolrEmailNotificationDetailsDao extends SolrDaoBase<EmailNotificationDetails>
                        implements EmailNotificationDetailsDao<EmailNotificationDetails>
{
    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(SolrEmailNotificationDetailsDao.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * We need to give this dao it's context.
     */
    public static final String EMAIL_NOTIFICATION_DETAILS = "emailNotificationDetails";

    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, EmailNotificationDetails emailNotificationDetails)
    {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(TYPE, EMAIL_NOTIFICATION_DETAILS);
        try {
            document.addField(PAYLOAD_CONTENT, getEmailNotificationDetailsContent(emailNotificationDetails));
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Cannot convert Email Notification Details to string! [%s]", emailNotificationDetails));
        }

        document.addField(ID, emailNotificationDetails.getJobName()+"_"+emailNotificationDetails.getMonitorType());

        document.addField(CREATED_DATE_TIME, emailNotificationDetails.getTimestampLong());
        document.setField(EXPIRY, expiry);
        return document;
    }

    private String getEmailNotificationDetailsContent(EmailNotificationDetails emailNotificationDetails) throws JsonProcessingException {
        return this.objectMapper.writeValueAsString(emailNotificationDetails);
    }

    @Override
    public SearchResults<? extends EmailNotificationDetails> findAll(int limit, int offset) {
        StringBuffer typeBuffer = new StringBuffer();
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(EMAIL_NOTIFICATION_DETAILS).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(typeBuffer.toString());
        solrQuery.setRows(limit);
        solrQuery.setStart(offset);

        logger.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrEmailNotificationDetails.class);
    }

    @Override
    public EmailNotificationDetails findByJobNameAndMonitorType(String jobName, String monitorType) {
        SolrQuery query = super.buildIdQuery(jobName+"_"+monitorType, EMAIL_NOTIFICATION_DETAILS);

        logger.debug("query: " + query);

        List<? extends EmailNotificationDetails> beans = this.findByQuery(query, SolrEmailNotificationDetails.class).getResultList();

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
