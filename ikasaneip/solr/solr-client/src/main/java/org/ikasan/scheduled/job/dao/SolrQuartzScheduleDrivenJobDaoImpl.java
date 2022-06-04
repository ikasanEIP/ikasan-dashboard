package org.ikasan.scheduled.job.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.job.model.JobConstants;
import org.ikasan.scheduled.job.model.SolrQuartzScheduleDrivenJobRecordImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.job.dao.QuartzScheduleDrivenJobDao;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJob;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJobRecord;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrConstants;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SolrQuartzScheduleDrivenJobDaoImpl extends SolrDaoBase<QuartzScheduleDrivenJobRecord>
    implements QuartzScheduleDrivenJobDao<QuartzScheduleDrivenJobRecord> {

    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(SolrQuartzScheduleDrivenJobDaoImpl.class);


    private ObjectMapper objectMapper = ScheduledObjectMapperFactory.newInstance();
    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, QuartzScheduleDrivenJobRecord event) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(TYPE, JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB);
        try {
            document.addField(PAYLOAD_CONTENT, getQuartzScheduleDrivenJob(event.getQuartzScheduleDrivenJob()));
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException(String.format("Cannot convert QuartzScheduleDrivenJobRecord to string! [%s]", event), e);
        }

        document.addField(ID, JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB + "_" + event.getAgentName() + "_" + event.getJobName()
            + "_" + event.getQuartzScheduleDrivenJob().getContextId());
        document.addField(MODULE_NAME, event.getAgentName());
        document.addField(FLOW_NAME, event.getJobName());
        document.addField(COMPONENT_NAME, event.getQuartzScheduleDrivenJob().getContextId());
        document.addField(CREATED_DATE_TIME, event.getTimestamp());
        document.addField(UPDATED_DATE_TIME, System.currentTimeMillis());
        document.addField(MODIFIED_BY, event.getModifiedBy());
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

        SearchResults<? extends QuartzScheduleDrivenJobRecord> beans = this.findByQuery(query, SolrQuartzScheduleDrivenJobRecordImpl.class);

        if(beans.getResultList().size() > 0)
        {
            return beans.getResultList().get(0);
        }
        else
        {
            return null;
        }
    }
}
