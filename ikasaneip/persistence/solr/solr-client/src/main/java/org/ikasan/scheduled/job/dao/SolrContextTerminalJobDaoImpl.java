package org.ikasan.scheduled.job.dao;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.job.model.SolrContextTerminalJobRecordImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.job.dao.ContextTerminalJobDao;
import org.ikasan.spec.scheduled.job.model.ContextTerminalJob;
import org.ikasan.spec.scheduled.job.model.ContextTerminalJobRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

public class SolrContextTerminalJobDaoImpl extends SolrDaoBase<ContextTerminalJobRecord>
    implements ContextTerminalJobDao<ContextTerminalJobRecord> {

    private static Logger logger = LoggerFactory.getLogger(SolrContextTerminalJobDaoImpl.class);
    private JsonMapper objectMapper = ScheduledObjectMapperFactory.newInstance();

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, ContextTerminalJobRecord event) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(TYPE, JobConstants.CONTEXT_TERMINAL_JOB);
        try {
            ContextTerminalJob job = event.getContextTerminalJob();
            document.addField(ID, job.getAgentName() + "_"
                + event.getJobName() + "_" + job.getContextName());
            document.addField(PAYLOAD_CONTENT, getContextTerminalJob(job));
            document.addField(COMPONENT_NAME, job.getContextName());
            document.addField(DISPLAY_NAME, job.getDisplayName());
            document.addField(MODULE_NAME, job.getAgentName());
        } catch (JacksonException e) {
            throw new SolrEntityConversionException(String.format("Cannot convert GlobalEventJob to string! [%s]", event), e);
        }

        document.addField(FLOW_NAME, event.getJobName());
        document.addField(CREATED_DATE_TIME, event.getTimestamp());
        document.addField(UPDATED_DATE_TIME, System.currentTimeMillis());
        // only update modified by field if populated.
        if(event.getModifiedBy() != null &&
            !event.getModifiedBy().isEmpty()) {
            document.addField(MODIFIED_BY, event.getModifiedBy());
        }
        document.setField(EXPIRY, DO_NOT_EXPIRE);

        logger.debug(String.format("Converted scheduled process event to SolrDocument[%s]", document));
        return document;
    }

    private String getContextTerminalJob(ContextTerminalJob contextTerminalJob) {
        return this.objectMapper.writeValueAsString(contextTerminalJob);
    }

    @Override
    public SearchResults<ContextTerminalJobRecord> findAll(int limit, int offset) {
        StringBuffer typeBuffer = new StringBuffer();
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.CONTEXT_TERMINAL_JOB).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(typeBuffer.toString());
        solrQuery.setRows(limit);
        solrQuery.setStart(offset);

        logger.debug("query: " + solrQuery);

        return super.findByQuery(solrQuery, SolrContextTerminalJobRecordImpl.class);
    }

    @Override
    public SearchResults<ContextTerminalJobRecord> findByContext(String contextId, int limit, int offset) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.CONTEXT_TERMINAL_JOB).append("\" ");
        queryBuffer.append(AND).append(" ").append(COMPONENT_NAME).append(COLON);
        queryBuffer.append("\"").append(contextId).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryBuffer.toString());
        logger.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrContextTerminalJobRecordImpl.class, offset, limit);
    }

    @Override
    public ContextTerminalJobRecord findById(String id) {
        SolrQuery query = super.buildIdQuery(id, JobConstants.CONTEXT_TERMINAL_JOB);

        logger.debug("query: " + query);

        List<? extends ContextTerminalJobRecord> beans = this.findByQuery(query, SolrContextTerminalJobRecordImpl.class).getResultList();

        if (beans.size() > 0) {
            return beans.get(0);
        } else {
            return null;
        }
    }
}
