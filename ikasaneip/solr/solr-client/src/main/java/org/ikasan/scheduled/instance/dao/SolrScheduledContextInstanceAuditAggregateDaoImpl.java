package org.ikasan.scheduled.instance.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.instance.model.SolrScheduledContextInstanceAuditAggregateRecordImpl;
import org.ikasan.scheduled.instance.model.SolrSchedulerJobInstanceRecordImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.solr.util.SolrSpecialCharacterEscapeUtil;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditAggregateDao;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateRecord;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class SolrScheduledContextInstanceAuditAggregateDaoImpl extends SolrDaoBase<ScheduledContextInstanceAuditAggregateRecord> implements ScheduledContextInstanceAuditAggregateDao {
    private static final ObjectMapper OBJECT_MAPPER = ScheduledObjectMapperFactory.newInstance();

    private static final Logger LOG = LoggerFactory.getLogger(SolrScheduledContextInstanceAuditAggregateDaoImpl.class);
    private static final String SCHEDULED_CONTEXT_INSTANCE_AUDIT_AGGREGATE_TYPE = "scheduledContextInstanceAuditAggregate";
    private static final String SCHEDULED_CONTEXT_INSTANCE_AUDIT_AGGREGATE_ID = "scheduledContextInstanceAuditAggregateId";

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, ScheduledContextInstanceAuditAggregateRecord record) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(ID, SCHEDULED_CONTEXT_INSTANCE_AUDIT_AGGREGATE_ID + "_" + UUID.randomUUID());
        document.addField(TYPE, SCHEDULED_CONTEXT_INSTANCE_AUDIT_AGGREGATE_TYPE);
        try {
            document.addField(PAYLOAD_CONTENT, OBJECT_MAPPER.writeValueAsString(record.getScheduledContextInstanceAuditAggregate()));
        } catch (JsonProcessingException e) {
            throw new SolrEntityConversionException(String.format("Cannot convert ScheduledContextInstanceAuditAggregateRecord to string! [%s]"
                , record.getScheduledContextInstanceAuditAggregate()));
        }

        document.setField(FLOW_NAME, record.getContextInstanceId());
        document.addField(MODULE_NAME, record.getContextName());
        if(record.getScheduledProcessEventName() != null) {
            document.addField(COMPONENT_NAME, record.getScheduledProcessEventName().toLowerCase());
        }
        document.addField(CREATED_DATE_TIME, System.currentTimeMillis());

        if(record.getScheduledContextInstanceAuditAggregate().getSchedulerJobInitiationEvents() != null &&
            !record.getScheduledContextInstanceAuditAggregate().getSchedulerJobInitiationEvents().isEmpty()) {
            StringBuffer eventsBuffer = new StringBuffer();
            record.getScheduledContextInstanceAuditAggregate()
                .getSchedulerJobInitiationEvents().forEach(event -> eventsBuffer.append(event.getJobName()).append(" "));

            document.addField(EVENT, eventsBuffer.toString().toLowerCase());
        }

        document.setField(EXPIRY, expiry);

        LOG.debug(String.format("Converted ScheduledContextInstanceAuditRecord to SolrDocument[%s]", document));
        return document;
    }

    @Override
    public SearchResults<ScheduledContextInstanceAuditAggregateRecord> findAll(int limit, int offset, String sortField, String sortDirection) {
        return this.findScheduledContextInstanceAuditAggregateRecordsByFilter(new ScheduledContextInstanceAuditAggregateSearchFilter(),
            limit, offset, sortField, sortDirection);
    }

    @Override
    public SearchResults<ScheduledContextInstanceAuditAggregateRecord> findScheduledContextInstanceAuditAggregateRecordsByFilter
        (ScheduledContextInstanceAuditAggregateSearchFilter filter, int limit, int offset, String sortField, String sortDirection) {
        StringBuffer queryString = new StringBuffer();

        queryString.append(TYPE + COLON).append(SCHEDULED_CONTEXT_INSTANCE_AUDIT_AGGREGATE_TYPE);

        queryString.append(AND)
            .append(MODULE_NAME).append(COLON)
            .append(filter.getContextName() != null && !filter.getContextName().isEmpty() ? "*"+SolrSpecialCharacterEscapeUtil.escape(filter.getContextName())+"*" : "*");

        queryString.append(AND)
            .append(FLOW_NAME)
            .append(COLON)
            .append(filter.getContextInstanceId() != null && !filter.getContextInstanceId().isEmpty() ? "*"+SolrSpecialCharacterEscapeUtil.escape(filter.getContextInstanceId()+"*") : "*");

        queryString.append(AND)
            .append(COMPONENT_NAME)
            .append(COLON)
            .append(filter.getScheduledProcessEventName() != null && !filter.getScheduledProcessEventName().isEmpty()
                ? "*"+SolrSpecialCharacterEscapeUtil.escape(filter.getScheduledProcessEventName().toLowerCase())+"*" : "*");

        if(filter.getRaisedInitiationEventName() != null && !filter.getRaisedInitiationEventName().isEmpty()) {
            queryString.append(AND)
                .append(EVENT)
                .append(COLON)
                .append("*"+SolrSpecialCharacterEscapeUtil.escape(filter.getRaisedInitiationEventName().toLowerCase())+"*");
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

        return this.findByQuery(solrQuery, SolrScheduledContextInstanceAuditAggregateRecordImpl.class, offset, limit);
    }
}
