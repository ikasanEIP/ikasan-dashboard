package org.ikasan.scheduled.visualisation.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.context.model.SolrScheduledContextRecordImpl;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.scheduled.visualisation.model.SolrContextVisualisationLayoutRecordImpl;
import org.ikasan.spec.scheduled.visualisation.dao.ContextVisualisationLayoutDao;
import org.ikasan.spec.scheduled.visualisation.model.ContextVisualisationLayout;
import org.ikasan.spec.scheduled.visualisation.model.ContextVisualisationLayoutRecord;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SolrContextVisualisationLayoutDaoImpl extends SolrDaoBase<ContextVisualisationLayoutRecord>
    implements ContextVisualisationLayoutDao {
    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(SolrContextVisualisationLayoutDaoImpl.class);

    private static ObjectMapper objectMapper = ScheduledObjectMapperFactory.newInstance();

    public static final String CONTEXT_VISUALISATION_LAYOUT = "contextVisualisationLayout";

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, ContextVisualisationLayoutRecord event) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(TYPE, CONTEXT_VISUALISATION_LAYOUT);
        try {
            document.addField(PAYLOAD_CONTENT, this.getPayloadContents(event.getContextVisualisationLayout()));
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("An error has occurred converting a context visualisation layout to a String!", e);
        }
        document.addField(ID, CONTEXT_VISUALISATION_LAYOUT + "_" + event.getParentContext() + "_" + event.getContext());
        document.addField(MODULE_NAME, event.getParentContext());
        document.addField(COMPONENT_NAME, event.getContext());
        document.addField(CREATED_DATE_TIME, event.getTimestamp());
        document.addField(UPDATED_DATE_TIME, System.currentTimeMillis());
        document.addField(MODIFIED_BY, event.getModifiedBy());
        document.setField(EXPIRY, DO_NOT_EXPIRE);

        logger.debug(String.format("Converted context visualisation layout to SolrDocument[%s]", document));
        return document;
    }

    protected String getPayloadContents(ContextVisualisationLayout contextVisualisationLayout) throws JsonProcessingException {
        return objectMapper.writeValueAsString(contextVisualisationLayout);
    }

    @Override
    public ContextVisualisationLayoutRecord findById(String id) {
        SolrQuery query = super.buildIdQuery(id, CONTEXT_VISUALISATION_LAYOUT);

        logger.debug("query: " + query);

        SearchResults<? extends ContextVisualisationLayoutRecord> searchResults = this
            .findByQuery(query, SolrContextVisualisationLayoutRecordImpl.class, 0, 1);

        if(searchResults.getResultList().size() > 0) {
            return searchResults.getResultList().get(0);
        }
        else {
            return null;
        }
    }

    @Override
    public List<ContextVisualisationLayoutRecord> findByParentContext(String parentContext) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(CONTEXT_VISUALISATION_LAYOUT).append("\" ");
        queryBuffer.append(AND);
        queryBuffer.append(MODULE_NAME + COLON);
        queryBuffer.append("\"").append(parentContext).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryBuffer.toString());

        logger.debug("findByParentContext query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrScheduledContextRecordImpl.class).getResultList();
    }

    @Override
    public ContextVisualisationLayoutRecord findByParentContextAndContext(String parentContext, String context) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(CONTEXT_VISUALISATION_LAYOUT).append("\" ");
        queryBuffer.append(AND);
        queryBuffer.append(MODULE_NAME + COLON);
        queryBuffer.append("\"").append(parentContext).append("\" ");
        queryBuffer.append(AND);
        queryBuffer.append(COMPONENT_NAME + COLON);
        queryBuffer.append("\"").append(context).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryBuffer.toString());

        logger.debug("findByParentContextAndContext query: " + solrQuery);

        SearchResults<? extends ContextVisualisationLayoutRecord> searchResults = this
            .findByQuery(solrQuery, SolrContextVisualisationLayoutRecordImpl.class, 0, 1);

        if(searchResults.getResultList().size() > 0) {
            return searchResults.getResultList().get(0);
        }
        else {
            return null;
        }
    }
}
