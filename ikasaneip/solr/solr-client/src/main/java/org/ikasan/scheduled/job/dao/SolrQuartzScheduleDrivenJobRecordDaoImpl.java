package org.ikasan.scheduled.job.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.job.model.JobConstants;
import org.ikasan.scheduled.job.model.SolrFileEventDrivenJobRecordImpl;
import org.ikasan.scheduled.job.model.SolrQuartzScheduleDrivenJobRecordImpl;
import org.ikasan.spec.scheduled.job.dao.QuartzScheduleDrivenJobRecordDao;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJobRecord;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrConstants;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SolrQuartzScheduleDrivenJobRecordDaoImpl extends SolrDaoBase<QuartzScheduleDrivenJobRecord>
    implements QuartzScheduleDrivenJobRecordDao<QuartzScheduleDrivenJobRecord> {

    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(SolrQuartzScheduleDrivenJobRecordDaoImpl.class);


    private ObjectMapper objectMapper = new ObjectMapper();
    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, QuartzScheduleDrivenJobRecord event) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(TYPE, JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB);
        try {
            document.addField(PAYLOAD_CONTENT, getQuartzScheduleDrivenJob(event.getQuartzScheduleDrivenJob()));
            document.addField(COMPONENT_NAME, event.getQuartzScheduleDrivenJob().getContextId());
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Cannot convert QuartzScheduleDrivenJobRecord to string! [%s]", event), e);
        }

        document.addField(ID, event.getId());
        document.addField(MODULE_NAME, event.getAgentName());
        document.addField(FLOW_NAME, event.getJobName());
        document.addField(CREATED_DATE_TIME, event.getTimestamp());
        document.setField(EXPIRY, expiry);

        logger.debug(String.format("Converted scheduled process event to SolrDocument[%s]", document));
        return document;
    }

    private String getQuartzScheduleDrivenJob(QuartzScheduleDrivenJob quartzScheduleDrivenJob) throws JsonProcessingException {
        return this.objectMapper.writeValueAsString(quartzScheduleDrivenJob);
    }

    @Override
    public SearchResults<? extends QuartzScheduleDrivenJobRecord> findAll(int limit, int offset) {
        return null;
    }

    @Override
    public SearchResults<? extends QuartzScheduleDrivenJobRecord> findByContext(String contextId, int limit, int offset) {
        return null;
    }

    @Override
    public QuartzScheduleDrivenJobRecord findById(String id) {
        SolrQuery query = super.buildIdQuery(id, JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB);

        logger.debug("query: " + query);

        List<? extends QuartzScheduleDrivenJobRecord> beans = this.findByQuery(query);

        if(beans.size() > 0)
        {
            return beans.get(0);
        }
        else
        {
            return null;
        }
    }

    /**
     * Helper method to find by query.
     *
     * @param query
     */
    private List<? extends QuartzScheduleDrivenJobRecord> findByQuery(SolrQuery query) {
        logger.debug("queryString: " + query);

        try {
            QueryRequest req = new QueryRequest(query);
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);

            QueryResponse rsp = req.process(this.solrClient, SolrConstants.CORE);

            return rsp.getBeans(SolrQuartzScheduleDrivenJobRecordImpl.class);
        }
        catch (Exception e) {
            throw new RuntimeException("Error resolving SolrQuartzScheduleDrivenJobRecordImpl by query [" + query + "] from the ikasan solr index!", e);
        }
    }
}
