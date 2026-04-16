package org.ikasan.scheduled.job.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.job.model.SolrGlobalEventJobRecordImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.job.dao.GlobalEventJobDao;
import org.ikasan.spec.scheduled.job.model.GlobalEventJob;
import org.ikasan.spec.scheduled.job.model.GlobalEventJobRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

public class SolrGlobalEventJobDaoImpl extends SolrDaoBase<GlobalEventJobRecord>
    implements GlobalEventJobDao<GlobalEventJobRecord> {

    private static Logger logger = LoggerFactory.getLogger(SolrGlobalEventJobDaoImpl.class);
    private ObjectMapper objectMapper = ScheduledObjectMapperFactory.newInstance();

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, GlobalEventJobRecord event) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(TYPE, JobConstants.GLOBAL_EVENT_JOB);
        try {
            GlobalEventJob job = event.getGlobalEventJob();
            document.addField(ID, JobConstants.GLOBAL_EVENT_JOB + "_" + event.getAgentName() + "_"
                + event.getJobName() + "_" + job.getContextName());
            document.addField(PAYLOAD_CONTENT, getGlobalEventJob(job));
            document.addField(COMPONENT_NAME, job.getContextName());
        } catch (JsonProcessingException e) {
            throw new SolrEntityConversionException(String.format("Cannot convert GlobalEventJob to string! [%s]", event), e);
        }

        document.addField(MODULE_NAME, event.getAgentName());
        document.addField(FLOW_NAME, event.getJobName());
        document.addField(DISPLAY_NAME, event.getGlobalEventJob().getDisplayName());
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

    private String getGlobalEventJob(GlobalEventJob globalEventJob) throws JsonProcessingException {
        return this.objectMapper.writeValueAsString(globalEventJob);
    }

    @Override
    public SearchResults<GlobalEventJobRecord> findAll(int limit, int offset) {
        StringBuffer typeBuffer = new StringBuffer();
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.GLOBAL_EVENT_JOB).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(typeBuffer.toString());
        solrQuery.setRows(limit);
        solrQuery.setStart(offset);

        logger.debug("query: " + solrQuery);

        return super.findByQuery(solrQuery, SolrGlobalEventJobRecordImpl.class);
    }

    @Override
    public SearchResults<GlobalEventJobRecord> findByContext(String contextId, int limit, int offset) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.GLOBAL_EVENT_JOB).append("\" ");
        queryBuffer.append(AND).append(" ").append(COMPONENT_NAME).append(COLON);
        queryBuffer.append("\"").append(contextId).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryBuffer.toString());
        logger.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrGlobalEventJobRecordImpl.class, offset, limit);
    }

    @Override
    public GlobalEventJobRecord findById(String id) {
        SolrQuery query = super.buildIdQuery(id, JobConstants.GLOBAL_EVENT_JOB);

        logger.debug("query: " + query);

        List<? extends GlobalEventJobRecord> beans = this.findByQuery(query, SolrGlobalEventJobRecordImpl.class).getResultList();

        if (beans.size() > 0) {
            return beans.get(0);
        } else {
            return null;
        }
    }

    @Override
    public void skip(GlobalEventJobRecord jobRecord, List<String> childContextNames, String actor) {
        GlobalEventJob globalEventJob = jobRecord.getGlobalEventJob();
        globalEventJob.setSkippedContexts(new HashMap<>());
        childContextNames.forEach(name ->
            globalEventJob.getSkippedContexts().put(name, true));

        jobRecord.setGlobalEventJob(globalEventJob);
        jobRecord.setModifiedBy(actor);

        this.save(jobRecord);
    }
    @Override
    public void enable(GlobalEventJobRecord jobRecord, String actor) {
        GlobalEventJob globalEventJob = jobRecord.getGlobalEventJob();
        jobRecord.setGlobalEventJob(globalEventJob);
        jobRecord.setModifiedBy(actor);

        this.save(jobRecord);
    }
}
