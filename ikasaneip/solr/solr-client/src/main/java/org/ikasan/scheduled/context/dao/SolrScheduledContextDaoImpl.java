package org.ikasan.scheduled.context.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.context.model.SolrScheduledContextRecordImpl;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.solr.SolrConstants;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SolrScheduledContextDaoImpl extends SolrDaoBase<ScheduledContextRecord> implements ScheduledContextDao
{
    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(SolrScheduledContextDaoImpl.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * We need to give this dao it's context.
     */
    public static final String SCHEDULED_CONTEXT = "scheduledContext";

    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, ScheduledContextRecord scheduledContextRecord) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(TYPE, SCHEDULED_CONTEXT);
        document.addField(PAYLOAD_CONTENT, scheduledContextRecord.getContext());
        document.addField(ID, scheduledContextRecord.getId() + "-" + SCHEDULED_CONTEXT);
        document.addField(MODULE_NAME, scheduledContextRecord.getContextName());
        document.addField(CREATED_DATE_TIME, scheduledContextRecord.getTimestamp());
        document.setField(EXPIRY, expiry);

        logger.debug(String.format("Converted scheduled context record to SolrDocument[%s]", document));
        return document;
    }

    @Override
    public List<? extends ScheduledContextRecord> findAll() {
        StringBuffer typeBuffer = new StringBuffer();
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(SCHEDULED_CONTEXT).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(typeBuffer.toString());

        logger.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery);
    }

    @Override
    public ScheduledContextRecord findById(String id) {
        SolrQuery query = super.buildIdQuery(id, SCHEDULED_CONTEXT);

        logger.debug("query: " + query);

        List<? extends ScheduledContextRecord> beans = this.findByQuery(query);

        if(beans.size() > 0)
        {
            return beans.get(0);
        }
        else
        {
            return null;
        }
    }

    /**
     * Helper method to find by query.
     *
     * @param query
     */
    private List<? extends ScheduledContextRecord> findByQuery(SolrQuery query) {
        logger.debug("queryString: " + query);

        try {
            QueryRequest req = new QueryRequest(query);
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);

            QueryResponse rsp = req.process(this.solrClient, SolrConstants.CORE);

            return rsp.getBeans(SolrScheduledContextRecordImpl.class);
        }
        catch (Exception e) {
            throw new RuntimeException("Error resolving scheduled context record meta data by query [" + query + "] from the ikasan solr index!", e);
        }
    }
}
