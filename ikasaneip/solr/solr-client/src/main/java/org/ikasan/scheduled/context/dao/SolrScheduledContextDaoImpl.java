package org.ikasan.scheduled.context.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.context.model.SolrScheduledContextRecordImpl;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.model.ScheduledContextSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
            ContextTemplate contextTemplate = scheduledContextRecord.getContext();
            document.addField(PAYLOAD_CONTENT, this.getPayloadContents(contextTemplate));
            document.addField(DISABLED, contextTemplate.isDisabled());
            document.addField(QUARTZ_SCHEDULED_JOBS_DISABLED, contextTemplate.isQuartzScheduleDrivenJobsDisabledForContext());
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException(String.format("Cannot convert FileEventDrivenJob to string! [%s]"
                , scheduledContextRecord.getContext()));
        }
        document.addField(ID, scheduledContextRecord.getContextName() + "-" + SCHEDULED_CONTEXT);
        document.addField(MODULE_NAME, scheduledContextRecord.getContextName());
        document.addField(CREATED_DATE_TIME, scheduledContextRecord.getTimestamp());
        document.addField(UPDATED_DATE_TIME, System.currentTimeMillis());
        // only update modified field if populated.
        if(scheduledContextRecord.getModifiedBy() != null &&
            !scheduledContextRecord.getModifiedBy().isEmpty()) {
            document.addField(MODIFIED_BY, scheduledContextRecord.getModifiedBy());
        }
        document.setField(EXPIRY, DO_NOT_EXPIRE);

        logger.debug(String.format("Converted scheduled context record to SolrDocument[%s]", document));
        return document;
    }

    protected String getPayloadContents(ContextTemplate contextTemplate) throws JsonProcessingException {
        return objectMapper.writeValueAsString(contextTemplate);
    }

    @Override
    public SearchResults<ScheduledContextRecord> findAll() {
        return this.findAll(-1, -1);
    }

    @Override
    public ScheduledContextRecord findById(String id) {
        SolrQuery query = super.buildIdQuery(id + "-" + SCHEDULED_CONTEXT, SCHEDULED_CONTEXT);

        logger.debug("query: " + query);

        SearchResults<? extends ScheduledContextRecord> searchResults = this
            .findByQuery(query, SolrScheduledContextRecordImpl.class, 0, 1);

        if(searchResults.getResultList().size() > 0) {
            return searchResults.getResultList().get(0);
        }
        else {
            return null;
        }
    }

    @Override
    public SearchResults<ScheduledContextRecord> findAll(int limit, int offset) {
        StringBuffer typeBuffer = new StringBuffer();
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(SCHEDULED_CONTEXT).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(typeBuffer.toString());

        logger.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrScheduledContextRecordImpl.class, offset, limit);
    }

    @Override
    public SearchResults<ScheduledContextRecord> findByFilter(ScheduledContextSearchFilter filter, int limit, int offset, String sortColumn, String sortOrder) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(SCHEDULED_CONTEXT).append("\" ");

        if(filter.getContextName() != null && !filter.getContextName().isEmpty()) {
            queryBuffer.append(AND);
            queryBuffer.append(MODULE_NAME).append(COLON).append(WILDCARD).append(filter.getContextName().replaceAll("\\ ", "\\\\ ")).append(WILDCARD);
        }

        if(filter.getContextNames() != null && !filter.getContextNames().isEmpty()) {
            queryBuffer.append(AND).append(OPEN_BRACKET);
            List<String> predicates = new ArrayList<>();
            filter.getContextNames().forEach(name -> predicates.add(new StringBuffer().append(MODULE_NAME)
                .append(COLON).append("\"").append(name).append("\"").toString()));
            queryBuffer.append(predicates.stream().collect(Collectors.joining(OR)));
            queryBuffer.append(CLOSE_BRACKET);
        }

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryBuffer.toString());

        logger.debug("query: " + solrQuery);

        if(sortColumn != null && !sortColumn.isEmpty()) {
            solrQuery.addSort(sortColumn, sortOrder != null && sortOrder.equals("ASCENDING") ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
        }
        else {
            // Default search to created date time descending
            solrQuery.addSort(CREATED_DATE_TIME, SolrQuery.ORDER.desc);
        }

        return this.findByQuery(solrQuery, SolrScheduledContextRecordImpl.class, offset, limit);
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

    @Override
    public void deleteContext(String contextName) {
        super.removeById(SCHEDULED_CONTEXT, contextName + "-" + SCHEDULED_CONTEXT);
    }
}
