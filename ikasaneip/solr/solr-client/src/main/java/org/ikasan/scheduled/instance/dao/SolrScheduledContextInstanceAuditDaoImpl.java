package org.ikasan.scheduled.instance.dao;

import java.util.UUID;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.instance.model.SolrScheduledContextInstanceAuditRecordImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditDao;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditRecord;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SolrScheduledContextInstanceAuditDaoImpl extends SolrDaoBase<ScheduledContextInstanceAuditRecord> implements ScheduledContextInstanceAuditDao {
    private static final ObjectMapper OBJECT_MAPPER = ScheduledObjectMapperFactory.newInstance();
    private static final Logger LOG = LoggerFactory.getLogger(SolrScheduledContextInstanceAuditDaoImpl.class);
    private static final String SCHEDULED_CONTEXT_AUDIT_INSTANCE_TYPE = "scheduledContextAuditInstance";
    private static final String SCHEDULED_CONTEXT_AUDIT_INSTANCE_ID = "scheduledContextAuditInstanceId";

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, ScheduledContextInstanceAuditRecord record) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(ID, SCHEDULED_CONTEXT_AUDIT_INSTANCE_ID + "_" + UUID.randomUUID());
        document.addField(TYPE, SCHEDULED_CONTEXT_AUDIT_INSTANCE_TYPE);
        try {
            document.addField(PAYLOAD_CONTENT, OBJECT_MAPPER.writeValueAsString(record.getScheduledContextInstanceAudit()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Cannot convert ScheduledContextInstanceAuditRecord to string! [%s]", record.getScheduledContextInstanceAudit()));
        }

        document.setField(FLOW_NAME, record.getScheduledContextInstanceAudit().getPreviousContextInstance().getId());
        document.addField(MODULE_NAME, record.getContextName());
        document.addField(CREATED_DATE_TIME, System.currentTimeMillis());
        document.setField(EXPIRY, expiry);

        LOG.debug(String.format("Converted ScheduledContextInstanceAuditRecord to SolrDocument[%s]", document));
        return document;
    }

    @Override
    public SearchResults<ScheduledContextInstanceAuditRecord> findAll(int limit, int offset) {
        String queryString = TYPE + COLON + SCHEDULED_CONTEXT_AUDIT_INSTANCE_TYPE;
        return getResults(queryString, limit, offset);
    }

    @Override
    public SearchResults<ScheduledContextInstanceAuditRecord> findAllAuditRecordsByContextId(String contextId, int limit, int offset) {
        String queryString = FLOW_NAME + COLON + contextId + AND + TYPE + COLON + SCHEDULED_CONTEXT_AUDIT_INSTANCE_TYPE;
        return getResults(queryString, limit, offset);
    }

    private SearchResults<ScheduledContextInstanceAuditRecord> getResults(String queryString, int limit, int offset) {
        SolrQuery query = new SolrQuery();
        query.setQuery(queryString);
        query.setRows(limit);
        query.setStart(offset);

        LOG.debug("query: " + query);
        return this.findByQuery(query, SolrScheduledContextInstanceAuditRecordImpl.class);
    }
}
