package org.ikasan.systemevent.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.solr.util.SolrSpecialCharacterEscapeUtil;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.spec.systemevent.SystemEventSearchDao;
import org.ikasan.spec.systemevent.SystemEventSearchFilter;
import org.ikasan.systemevent.model.SolrSystemEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

public class SolrSystemEventDaoImpl extends SolrDaoBase<SystemEvent> implements SystemEventSearchDao
{
    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(SolrSystemEventDaoImpl.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * We need to give this dao it's context.
     */
    public static final String SYSTEM_EVENT = "systemEvent";

    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, SystemEvent systemEvent)
    {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(TYPE, SYSTEM_EVENT);
        try {
            document.addField(PAYLOAD_CONTENT, getSystemEventContent(systemEvent));
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Cannot convert system event to string! [%s]", systemEvent));
        }

        if(systemEvent.getModuleName() != null){
            document.addField(ID, systemEvent.getModuleName()
                + "-" + SYSTEM_EVENT + "-" + systemEvent.getId());
            document.addField(MODULE_NAME, systemEvent.getModuleName());
        }
        else {
            document.addField(ID, SYSTEM_EVENT + "-" + systemEvent.getSubject() + "-" + systemEvent.getId());
        }

        document.addField(ACTOR, systemEvent.getActor());
        document.addField(SYSTEM_EVENT_SUBJECT, systemEvent.getSubject());
        document.addField(SYSTEM_EVENT_ACTION, systemEvent.getAction());
        document.addField(CREATED_DATE_TIME, systemEvent.getTimestamp().getTime());
        document.setField(EXPIRY, expiry);
        return document;
    }

    private String getSystemEventContent(SystemEvent systemEvent) throws JsonProcessingException {
        return this.objectMapper.writeValueAsString(systemEvent);
    }

    @Override
    public SystemEvent findById(String id) {
        SolrQuery query = super.buildIdQuery(id, SYSTEM_EVENT);

        logger.debug("query: " + query);

        SearchResults<? extends SystemEvent> searchResults = this
            .findByQuery(query, SolrSystemEvent.class, 0, 1);

        if(searchResults.getResultList().size() > 0) {
            return searchResults.getResultList().get(0);
        }
        else {
            return null;
        }
    }

    @Override
    public SearchResults<SystemEvent> findByFilter(SystemEventSearchFilter filter, int limit, int offset
        , String sortColumn, String sortOrder) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(SYSTEM_EVENT).append("\" ");

        if(filter.getActor() != null && !filter.getActor().isEmpty()) {
            queryBuffer.append(AND);
            queryBuffer.append(ACTOR).append(COLON)
                .append(WILDCARD)
                .append(SolrSpecialCharacterEscapeUtil.escape(filter.getActor()))
                .append(WILDCARD);
        }

        if(filter.getSubject() != null && !filter.getSubject().isEmpty()) {
            queryBuffer.append(AND);
            queryBuffer.append(SYSTEM_EVENT_SUBJECT).append(COLON);
            if(SolrSpecialCharacterEscapeUtil.containsSpecialChar(filter.getSubject()))queryBuffer.append("\"");
            queryBuffer.append(WILDCARD)
                .append(SolrSpecialCharacterEscapeUtil.escape(filter.getSubject()))
                .append(WILDCARD);
            if(SolrSpecialCharacterEscapeUtil.containsSpecialChar(filter.getSubject()))queryBuffer.append("\"");
        }

        if(filter.getAction() != null && !filter.getAction().isEmpty()) {
            queryBuffer.append(AND);
            queryBuffer.append(SYSTEM_EVENT_ACTION).append(COLON);
            if(SolrSpecialCharacterEscapeUtil.containsSpecialChar(filter.getAction()))queryBuffer.append("\"");
            queryBuffer.append(WILDCARD)
                .append(SolrSpecialCharacterEscapeUtil.escape(filter.getAction()))
                .append(WILDCARD);
            if(SolrSpecialCharacterEscapeUtil.containsSpecialChar(filter.getAction()))queryBuffer.append("\"");
        }

        if(filter.getSearchTerm() != null && !filter.getSearchTerm().isEmpty()) {
            queryBuffer.append(AND);
            queryBuffer.append(Arrays.stream(filter.getSearchTerm().split(" ")).map(term -> {
                StringBuffer termBuffer = new StringBuffer();
                termBuffer.append(PAYLOAD_CONTENT).append(COLON);
                if(SolrSpecialCharacterEscapeUtil.containsSpecialChar(term))termBuffer.append("\"");
                termBuffer.append(WILDCARD)
                    .append(SolrSpecialCharacterEscapeUtil.escape(term))
                    .append(WILDCARD);
                if(SolrSpecialCharacterEscapeUtil.containsSpecialChar(term))termBuffer.append("\"");
                return termBuffer.toString();
            }).collect(Collectors.joining(" AND ")));
        }

        if(filter.getEndTime() > 0 ) {
            StringBuffer dateBuffer = this.buildDatePredicate(CREATED_DATE_TIME, new Date(filter.getStartTime())
                , new Date(filter.getEndTime()));
            queryBuffer.append(AND).append(dateBuffer);
        }

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryBuffer.toString());

        logger.debug("query: " + solrQuery);

        if(sortColumn != null && !sortColumn.isEmpty()) {
            solrQuery.addSort(sortColumn, sortOrder != null && sortOrder.equals("ASCENDING")
                ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
        }
        else {
            // Default search to created date time descending
            solrQuery.addSort(CREATED_DATE_TIME, SolrQuery.ORDER.desc);
        }

        return this.findByQuery(solrQuery, SolrSystemEvent.class, offset, limit);
    }
}
