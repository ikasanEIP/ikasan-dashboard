package org.ikasan.scheduled.context.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.context.model.SolrScheduledContextRecordImpl;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrScheduledContextDaoImpl extends SolrDaoBase<ScheduledContextRecord> implements ScheduledContextDao
{
    private static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(SolrScheduledContextDaoImpl.class);

    /**
     * We need to give this dao it's context.
     */
    public static final String SCHEDULED_CONTEXT = "scheduledContext";

    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, ScheduledContextRecord scheduledContextRecord) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(TYPE, SCHEDULED_CONTEXT);
        try {
            document.addField(PAYLOAD_CONTENT, this.getPayloadContents(scheduledContextRecord.getContext()));
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Cannot convert FileEventDrivenJob to string! [%s]"
                , scheduledContextRecord.getContext()));
        }
        document.addField(ID, scheduledContextRecord.getContextName() + "-" + SCHEDULED_CONTEXT);
        document.addField(MODULE_NAME, scheduledContextRecord.getContextName());
        document.addField(CREATED_DATE_TIME, scheduledContextRecord.getTimestamp());
        document.setField(EXPIRY, expiry);

        logger.debug(String.format("Converted scheduled context record to SolrDocument[%s]", document));
        return document;
    }

    protected String getPayloadContents(ContextTemplate contextTemplate) throws JsonProcessingException {
        return objectMapper.writeValueAsString(contextTemplate);
    }

    @Override
    public SearchResults<ScheduledContextRecord> findAll() {
        StringBuffer typeBuffer = new StringBuffer();
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(SCHEDULED_CONTEXT).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(typeBuffer.toString());

        logger.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrScheduledContextRecordImpl.class,-1, -1);
    }

    @Override
    public ScheduledContextRecord findById(String id) {
        SolrQuery query = super.buildIdQuery(id + "-" + SCHEDULED_CONTEXT, SCHEDULED_CONTEXT);

        logger.debug("query: " + query);

        SearchResults<? extends ScheduledContextRecord> searchResults = this
            .findByQuery(query, SolrScheduledContextRecordImpl.class, 0, 1);

        if(searchResults.getResultList().size() > 0)
        {
            return searchResults.getResultList().get(0);
        }
        else
        {
            return null;
        }
    }

    @Override
    public ScheduledContextRecord findByName(String name) {
        SolrQuery query = new SolrQuery(super.buildFieldPredicate(name, MODULE_NAME)
            .append(" AND ").append(super.buildFieldPredicate(SCHEDULED_CONTEXT, TYPE)).toString());

        logger.debug("query: " + query);

        SearchResults<? extends ScheduledContextRecord> searchResults = this
            .findByQuery(query, SolrScheduledContextRecordImpl.class, 0, 1);

        if(searchResults.getResultList().size() > 0)
        {
            return searchResults.getResultList().get(0);
        }
        else
        {
            return null;
        }
    }
}
