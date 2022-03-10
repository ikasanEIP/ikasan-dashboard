package org.ikasan.scheduled.job.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.job.model.JobConstants;
import org.ikasan.scheduled.job.model.SolrInternalEventDrivenJobRecordImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.job.dao.InternalEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SolrInternalEventDrivenJobDaoImpl extends SolrDaoBase<InternalEventDrivenJobRecord>
    implements InternalEventDrivenJobDao<InternalEventDrivenJobRecord> {

    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(SolrInternalEventDrivenJobDaoImpl.class);


    private ObjectMapper objectMapper = ScheduledObjectMapperFactory.newInstance();

    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, InternalEventDrivenJobRecord event) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(TYPE, JobConstants.INTERNAL_EVENT_DRIVEN_JOB);
        try {
            document.addField(PAYLOAD_CONTENT, getInternalEventDrivenJob(event.getInternalEventDrivenJob()));
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Cannot convert QuartzScheduleDrivenJobRecord to string! [%s]", event), e);
        }

        document.addField(ID, JobConstants.INTERNAL_EVENT_DRIVEN_JOB + "_" + event.getAgentName() + "_"
            + event.getJobName() + "_" + event.getInternalEventDrivenJob().getContextId());
        document.addField(MODULE_NAME, event.getAgentName());
        document.addField(FLOW_NAME, event.getJobName());
        document.addField(COMPONENT_NAME, event.getInternalEventDrivenJob().getContextId());
        document.addField(CREATED_DATE_TIME, event.getTimestamp());
        document.setField(EXPIRY, expiry);

        logger.debug(String.format("Converted scheduled process event to SolrDocument[%s]", document));
        return document;
    }

    private String getInternalEventDrivenJob(InternalEventDrivenJob internalEventDrivenJob) throws JsonProcessingException {
        return this.objectMapper.writeValueAsString(internalEventDrivenJob);
    }

    @Override
    public SearchResults<? extends InternalEventDrivenJobRecord> findAll(int limit, int offset) {
        StringBuffer typeBuffer = new StringBuffer();
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(typeBuffer.toString());
        solrQuery.setRows(limit);
        solrQuery.setStart(offset);

        logger.debug("query: " + solrQuery);

        return super.findByQuery(solrQuery, SolrInternalEventDrivenJobRecordImpl.class);
    }

    @Override
    public SearchResults<? extends InternalEventDrivenJobRecord> findByContext(String contextId, int limit, int offset) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append(TYPE + COLON);
        queryBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB).append("\" ");
        queryBuffer.append(AND).append(" ").append(COMPONENT_NAME).append(COLON);
        queryBuffer.append("\"").append(contextId).append("\" ");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryBuffer.toString());
        logger.debug("query: " + solrQuery);

        return this.findByQuery(solrQuery, SolrInternalEventDrivenJobRecordImpl.class, offset, limit);
    }

    @Override
    public InternalEventDrivenJobRecord findById(String id) {
        SolrQuery query = super.buildIdQuery(id, JobConstants.INTERNAL_EVENT_DRIVEN_JOB);

        logger.debug("query: " + query);

        List<? extends InternalEventDrivenJobRecord> beans = this.findByQuery(query, SolrInternalEventDrivenJobRecordImpl.class).getResultList();

        if(beans.size() > 0)
        {
            return beans.get(0);
        }
        else
        {
            return null;
        }
    }
}
