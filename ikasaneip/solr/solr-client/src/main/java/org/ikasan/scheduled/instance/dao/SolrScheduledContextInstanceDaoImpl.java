package org.ikasan.scheduled.instance.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.instance.model.SolrScheduledContextInstanceRecordImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.solr.util.SolrSpecialCharacterEscapeUtil;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceDao;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceSearchFilter;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class SolrScheduledContextInstanceDaoImpl extends SolrDaoBase<ScheduledContextInstanceRecord> implements ScheduledContextInstanceDao {

    private static ObjectMapper objectMapper = ScheduledObjectMapperFactory.newInstance();

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
        document.addField(ID, scheduledContextInstanceRecord.getContextInstance().getId() + "_" + SCHEDULED_CONTEXT_INSTANCE);
        document.addField(TYPE, SCHEDULED_CONTEXT_INSTANCE);
        try {
            document.addField(PAYLOAD_CONTENT, this.getPayloadContents(scheduledContextInstanceRecord.getContextInstance()));
        } catch (JsonProcessingException e) {
            throw new SolrEntityConversionException(String.format("Cannot convert FileEventDrivenJob to string! [%s]", scheduledContextInstanceRecord.getContextInstance()));
        }
        document.addField(STATUS, scheduledContextInstanceRecord.getStatus());
        document.addField(MODULE_NAME, scheduledContextInstanceRecord.getContextName());
        document.addField(COMPONENT_NAME, scheduledContextInstanceRecord.getContextInstance().getId());
        document.addField(CREATED_DATE_TIME, scheduledContextInstanceRecord.getTimestamp());
        document.addField(UPDATED_DATE_TIME, System.currentTimeMillis());
        document.addField(MODIFIED_BY, scheduledContextInstanceRecord.getModifiedBy());
        document.setField(EXPIRY, expiry);
        document.setField(START_TIME, scheduledContextInstanceRecord.getContextInstance().getStartTime());
        document.setField(END_TIME, scheduledContextInstanceRecord.getContextInstance().getEndTime());

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

        SearchResults<ScheduledContextInstanceRecord> searchResults = this.findByQuery(query, SolrScheduledContextInstanceRecordImpl.class, 0, 1);
        return searchResults.getResultList().size() > 0 ? searchResults.getResultList().get(0) : null;
    }

    @Override
    public void deleteById(String id) {
        super.removeById(SCHEDULED_CONTEXT_INSTANCE, id + "_" + SCHEDULED_CONTEXT_INSTANCE);
    }

    @Override
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByStatus(List<InstanceStatus> instanceStatuses) {
        return this.getScheduledContextInstancesByStatus(instanceStatuses, -1, -1);
    }

    @Override
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByStatus(List<InstanceStatus> instanceStatuses, int limit, int offset) {
        String listOfStatus = super.buildStringListQueryPart(instanceStatuses
            .stream()
            .map(Enum::toString)
            .collect(Collectors.toList()), STATUS).toString();

        String queryString = TYPE + COLON + SCHEDULED_CONTEXT_INSTANCE + AND + listOfStatus;

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryString);

        return this.findByQuery(solrQuery, SolrScheduledContextInstanceRecordImpl.class, offset, limit);
    }

    @Override
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByContextName(String contextName, int limit, int offset
        , String sortField, String sortDirection) {
        StringBuffer queryString = new StringBuffer();
        queryString.append(TYPE).append(COLON).append(SCHEDULED_CONTEXT_INSTANCE)
            .append(AND)
            .append(MODULE_NAME).append(COLON).append(SolrSpecialCharacterEscapeUtil.escape(contextName));

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryString.toString());

        if(sortField != null && !sortField.isEmpty()) {
            solrQuery.addSort(sortField, sortDirection != null && sortDirection.toLowerCase().equals("asc") ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
        }

        return this.findByQuery(solrQuery, SolrScheduledContextInstanceRecordImpl.class, offset, limit);
    }

    @Override
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByContextName(String contextName, long startTimestamp, long endTimestamp
        , int limit, int offset, String sortField, String sortDirection) {
        StringBuffer queryString = new StringBuffer();
        queryString.append(TYPE).append(COLON).append(SCHEDULED_CONTEXT_INSTANCE)
            .append(AND)
            .append(MODULE_NAME).append(COLON).append(SolrSpecialCharacterEscapeUtil.escape(contextName));

        if(startTimestamp > 0 || endTimestamp > 0) {
            queryString.append(AND).append(CREATED_DATE_TIME).append(COLON).append("[").append(startTimestamp)
                .append(TO).append(endTimestamp).append("]");
        }

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryString.toString());

        if(sortField != null && !sortField.isEmpty()) {
            solrQuery.addSort(sortField, sortDirection != null && sortDirection.equals("ASCENDING") ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
        }

        return this.findByQuery(solrQuery, SolrScheduledContextInstanceRecordImpl.class, offset, limit);
    }

    @Override
    public SearchResults<ScheduledContextInstanceRecord> getScheduledContextInstancesByFilter(ContextInstanceSearchFilter filter, int limit, int offset, String sortField, String sortDirection) {
        StringBuffer queryString = new StringBuffer();
        queryString.append(TYPE).append(COLON).append(SCHEDULED_CONTEXT_INSTANCE);

        if(filter.getContextInstanceNames() != null && !filter.getContextInstanceNames().isEmpty()) {
            queryString.append(AND).append(OPEN_BRACKET);
            List<String> predicates = new ArrayList<>();
            filter.getContextInstanceNames().forEach(name -> predicates.add(new StringBuffer().append(MODULE_NAME)
                .append(COLON).append("\"").append(name).append("\"").toString()));
            queryString.append(predicates.stream().collect(Collectors.joining(OR)));
            queryString.append(CLOSE_BRACKET);
        }

        if(filter.getContextSearchFilter() != null && !filter.getContextSearchFilter().isEmpty()) {
            queryString.append(AND)
                .append(MODULE_NAME).append(COLON)
                .append(filter.getContextSearchFilter() != null && !filter.getContextSearchFilter().isEmpty() ? WILDCARD+SolrSpecialCharacterEscapeUtil.escape(filter.getContextSearchFilter())+WILDCARD : "*");
        }

        if(filter.getContextInstanceId() != null && !filter.getContextInstanceId().isEmpty()) {
            queryString.append(AND)
                .append(COMPONENT_NAME)
                .append(COLON)
                .append(WILDCARD)
                .append(SolrSpecialCharacterEscapeUtil.escape(filter.getContextInstanceId()))
                .append(WILDCARD);
        }

        if(filter.getCreatedTimestamp() > 0) {
            queryString.append(AND).append(CREATED_DATE_TIME).append(COLON).append("[").append(this.atStartOfDay(new Date(filter.getCreatedTimestamp())))
                .append(TO).append(this.atEndOfDay(new Date(filter.getCreatedTimestamp()))).append("]");
        }

        if(filter.getModifiedTimestamp() > 0) {
            queryString.append(AND).append(UPDATED_DATE_TIME).append(COLON).append("[").append(this.atStartOfDay(new Date(filter.getModifiedTimestamp())))
                .append(TO).append(this.atEndOfDay(new Date(filter.getModifiedTimestamp()))).append("]");
        }

//        if(filter.getStartTime() > 0) {
//            queryString.append(AND).append(START_TIME).append(COLON).append("[").append(this.atStartOfDay(new Date(filter.getStartTime())))
//                .append(TO).append(this.atEndOfDay(new Date(filter.getStartTime()))).append("]");
//        }
//
//        if(filter.getEndTime() > 0) {
//            queryString.append(AND).append(END_TIME).append(COLON).append("[").append(this.atStartOfDay(new Date(filter.getEndTime())))
//                .append(TO).append(this.atEndOfDay(new Date(filter.getEndTime()))).append("]");
//        }

        if(filter.getStartTimeStart() > 0 && filter.getStartTimeEnd() > 0) {
            queryString.append(AND).append(START_TIME).append(COLON).append("[").append(filter.getStartTimeStart())
                .append(TO).append(filter.getStartTimeEnd()).append("]");
        }

        if(filter.getEndTimeStart() > 0 && filter.getEndTimeEnd() > 0) {
            queryString.append(AND).append(END_TIME).append(COLON).append("[").append(filter.getEndTimeStart())
                .append(TO).append(filter.getEndTimeEnd()).append("]");
        }

        if(filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            queryString.append(AND).append(STATUS).append(COLON).append(filter.getStatus());
        }

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryString.toString());

        if(sortField != null && !sortField.isEmpty()) {
            solrQuery.addSort(sortField, sortDirection != null && sortDirection.equals("ASCENDING") ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
        }
        else {
            // Default search to created date time descending
            solrQuery.addSort(CREATED_DATE_TIME, SolrQuery.ORDER.desc);
        }

        return this.findByQuery(solrQuery, SolrScheduledContextInstanceRecordImpl.class, offset, limit);
    }

    public long atEndOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime().getTime();
    }

    public long atStartOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime().getTime();
    }
}
