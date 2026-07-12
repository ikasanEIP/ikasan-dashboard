package org.ikasan.scheduled.instance.dao;

import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.instance.model.SolrScheduledContextInstanceRecordImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditDao;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

public class SolrScheduledContextInstanceAuditDaoImpl extends SolrDaoBase<ScheduledContextInstanceRecord> implements ScheduledContextInstanceAuditDao {
    private static final JsonMapper OBJECT_MAPPER = ScheduledObjectMapperFactory.newInstance();

    private static final Logger LOG = LoggerFactory.getLogger(SolrScheduledContextInstanceAuditDaoImpl.class);
    private static final String SCHEDULED_CONTEXT_INSTANCE_AUDIT_TYPE = "scheduledContextInstanceAudit";
    public static final String SCHEDULED_CONTEXT_INSTANCE_AUDIT_ID = "scheduledContextInstanceAuditId";

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, ScheduledContextInstanceRecord record) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(ID, record.getId());
        document.addField(TYPE, SCHEDULED_CONTEXT_INSTANCE_AUDIT_TYPE);
        try {
            document.addField(PAYLOAD_CONTENT, OBJECT_MAPPER.writeValueAsString(record.getContextInstance()));
        } catch (JacksonException e) {
            throw new SolrEntityConversionException(String.format("Cannot convert ScheduledContextInstanceRecord to string! [%s]"
                , record.getContextInstance()));
        }

        document.setField(COMPONENT_NAME, record.getContextInstanceId());
        document.addField(MODULE_NAME, record.getContextName());
        document.addField(CREATED_DATE_TIME, System.currentTimeMillis());
        document.setField(EXPIRY, expiry);

        LOG.debug(String.format("Converted ScheduledContextInstanceRecord to SolrDocument[%s]", document));
        return document;
    }

    @Override
    public ScheduledContextInstanceRecord findById(String id) {
        SearchResults<ScheduledContextInstanceRecord> searchResults = this.findByQuery(buildIdQuery(id, SCHEDULED_CONTEXT_INSTANCE_AUDIT_TYPE)
            , SolrScheduledContextInstanceRecordImpl.class, 0, 1);
        return searchResults.getResultList().size() > 0 ? searchResults.getResultList().get(0) : null;
    }
}
