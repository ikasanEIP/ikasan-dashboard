package org.ikasan.scheduled.instance.dao;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.instance.model.SolrScheduledContextInstanceAuditAggregateRecordImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.solr.util.SolrSpecialCharacterEscapeUtil;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditAggregateDao;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateRecord;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrConstants;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.ikasan.spec.entity.EntityFields.*;

public class SolrScheduledContextInstanceAuditAggregateDaoImpl extends SolrDaoBase<ScheduledContextInstanceAuditAggregateRecord> implements ScheduledContextInstanceAuditAggregateDao {
    private static final JsonMapper OBJECT_MAPPER = ScheduledObjectMapperFactory.newInstance();

    private static final Logger LOG = LoggerFactory.getLogger(SolrScheduledContextInstanceAuditAggregateDaoImpl.class);

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, ScheduledContextInstanceAuditAggregateRecord record) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(ID, SCHEDULED_CONTEXT_INSTANCE_AUDIT_AGGREGATE_ID + "_" + UUID.randomUUID());
        document.addField(TYPE, SCHEDULED_CONTEXT_INSTANCE_AUDIT_AGGREGATE_TYPE);
        try {
            document.addField(PAYLOAD_CONTENT, OBJECT_MAPPER.writeValueAsString(record.getScheduledContextInstanceAuditAggregate()));
        } catch (JacksonException e) {
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

        document.setField(IS_REPEATING_JOB, record.isRepeatingJob());
        document.setField(STATUS, record.getStatus());
        document.setField(JOB_TYPE, record.getJobType());
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

        queryString.append(AND)
            .append(STATUS)
            .append(COLON)
            .append(filter.getStatus() != null && !filter.getStatus().isEmpty()
                ? "*"+SolrSpecialCharacterEscapeUtil.escape(filter.getStatus())+"*" : "*");

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

    @Override
    public Map<String, Map<String, Integer>> getRepeatingJobStatusCounts(List<String> contextInstanceIds) {
        Map<String, Map<String, Integer>> results = new HashMap<>();

        StringBuffer queryString = new StringBuffer();

        queryString.append(TYPE + COLON).append(SCHEDULED_CONTEXT_INSTANCE_AUDIT_AGGREGATE_TYPE);

        queryString.append(AND)
            .append(FLOW_NAME)
            .append(COLON)
            .append("%s");

        queryString.append(AND)
            .append(IS_REPEATING_JOB)
            .append(COLON)
            .append(true);

        contextInstanceIds.forEach(id -> {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(String.format(queryString.toString(), id));
            solrQuery.setFacet(true);
            solrQuery.addFacetField(STATUS);
            solrQuery.setRows(0);

            QueryRequest req = new QueryRequest(solrQuery, SolrRequest.METHOD.POST);
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);

            try {
                QueryResponse rsp = req.process(this.solrClient, SolrConstants.CORE);
                FacetField field = rsp.getFacetField(STATUS);
                HashMap<String, Integer> jobStatusCount = new HashMap<>();

                field.getValues().forEach(count -> jobStatusCount.put(count.getName(), (int) count.getCount()));

                results.put(id, jobStatusCount);

            } catch (Exception e) {
                throw new RuntimeException("Error resolving repeating job status count by query [" + queryString
                    + "] from the Ikasan solr index!", e);
            }
        });

        return results;
    }
}
