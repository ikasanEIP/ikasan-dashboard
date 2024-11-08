package org.ikasan.scheduled.job.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.job.model.SolrContextStartJobRecordImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.job.dao.ContextStartJobDao;
import org.ikasan.spec.scheduled.job.model.ContextStartJob;
import org.ikasan.spec.scheduled.job.model.ContextStartJobRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SolrContextStartJobDaoImpl extends SolrDaoBase<ContextStartJobRecord>
    implements ContextStartJobDao<ContextStartJobRecord> {

    private static Logger logger = LoggerFactory.getLogger(SolrContextStartJobDaoImpl.class);
    private ObjectMapper objectMapper = ScheduledObjectMapperFactory.newInstance();

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, ContextStartJobRecord event) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(TYPE, JobConstants.CONTEXT_START_JOB);
        try {
            ContextStartJob job = event.getContextStartJob();
            document.addField(ID, job.getAgentName() + "_"
                + event.getJobName() + "_" + job.getContextName());
            document.addField(PAYLOAD_CONTENT, getContextStartJob(job));
            document.addField(COMPONENT_NAME, job.getContextName());
            document.addField(MODULE_NAME, job.getAgentName());
            document.addField(DISPLAY_NAME,job.getDisplayName());
        } catch (JsonProcessingException e) {
            throw new SolrEntityConversionException(String.format("Cannot convert GlobalEventJob to string! [%s]", event), e);
        }

        document.addField(FLOW_NAME, event.getJobName());
        document.addField(CREATED_DATE_TIME, event.getTimestamp());
        document.addField(UPDATED_DATE_TIME, System.currentTimeMillis());
        document.addField(MODIFIED_BY, event.getModifiedBy());
        document.setField(EXPIRY, DO_NOT_EXPIRE);

        logger.debug(String.format("Converted scheduled process event to SolrDocument[%s]", document));
        return document;
    }

    private String getContextStartJob(ContextStartJob contextStartJob) throws JsonProcessingException {
        return this.objectMapper.writeValueAsString(contextStartJob);
    }

    @Override
    public SearchResults<? extends ContextStartJobRecord> findAll(int limit, int offset) {
        StringBuffer typeBuffer = new StringBuffer();
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.CONTEXT_START_JOB).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(typeBuffer.toString());
        solrQuery.setRows(limit);
        solrQuery.setStart(offset);

        logger.debug("query: " + solrQuery);

        return super.findByQuery(solrQuery, SolrContextStartJobRecordImpl.class);
    }

    @Override
    public SearchResults<? extends ContextStartJobRecord> findByContext(String contextId, int limit, int offset) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.CONTEXT_START_JOB).append("\" ");
        queryBuffer.append(AND).append(" ").append(COMPONENT_NAME).append(COLON);
        queryBuffer.append("\"").append(contextId).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryBuffer.toString());
        logger.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrContextStartJobRecordImpl.class, offset, limit);
    }

    @Override
    public ContextStartJobRecord findById(String id) {
        SolrQuery query = super.buildIdQuery(id, JobConstants.CONTEXT_START_JOB);

        logger.debug("query: " + query);

        List<? extends ContextStartJobRecord> beans = this.findByQuery(query, SolrContextStartJobRecordImpl.class).getResultList();

        if (beans.size() > 0) {
            return beans.get(0);
        } else {
            return null;
        }
    }
}
