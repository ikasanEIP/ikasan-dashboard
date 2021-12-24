package org.ikasan.scheduled.instance.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.instance.model.SolrScheduledContextInstanceRecordImpl;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceDao;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.solr.SolrConstants;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SolrScheduledContextInstanceDaoImpl extends SolrDaoBase<ScheduledContextInstanceRecord> implements ScheduledContextInstanceDao {

    private static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(SolrScheduledContextInstanceDaoImpl.class);

    /**
     * We need to give this dao it's context.
     */
    public static final String SCHEDULED_CONTEXT_INSTANCE = "scheduledContextInstance";

    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, ScheduledContextInstanceRecord scheduledContextInstanceRecord) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(ID, scheduledContextInstanceRecord.getContextName() + "_" + SCHEDULED_CONTEXT_INSTANCE);
        document.addField(TYPE, SCHEDULED_CONTEXT_INSTANCE);
        try {
            document.addField(PAYLOAD_CONTENT, this.getPayloadContents(scheduledContextInstanceRecord.getContextInstance()));
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Cannot convert FileEventDrivenJob to string! [%s]"
                , scheduledContextInstanceRecord.getContextInstance()));
        }
        document.addField(STATUS, scheduledContextInstanceRecord.getStatus());
        document.addField(MODULE_NAME, scheduledContextInstanceRecord.getContextName());
        document.addField(CREATED_DATE_TIME, scheduledContextInstanceRecord.getTimestamp());

        document.setField(EXPIRY, expiry);

        logger.debug(String.format("Converted scheduled context instance to SolrDocument[%s]", document));
        return document;
    }

    protected String getPayloadContents(ContextInstance contextInstance) throws JsonProcessingException {
        return objectMapper.writeValueAsString(contextInstance);
    }

    @Override
    public ScheduledContextInstanceRecord findById(String id) {
        SolrQuery query = super.buildIdQuery(id, SCHEDULED_CONTEXT_INSTANCE);

        logger.debug("query: " + query);

        List<SolrScheduledContextInstanceRecordImpl> beans = this.findByQuery(query);

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
    private List<SolrScheduledContextInstanceRecordImpl> findByQuery(SolrQuery query) {
        logger.debug("queryString: " + query);

        try {
            QueryRequest req = new QueryRequest(query);
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);

            QueryResponse rsp = req.process(this.solrClient, SolrConstants.CORE);

            return rsp.getBeans(SolrScheduledContextInstanceRecordImpl.class);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error resolving scheduled context instance record data by query [" + query + "] from the ikasan solr index!", e);
        }
    }
}
