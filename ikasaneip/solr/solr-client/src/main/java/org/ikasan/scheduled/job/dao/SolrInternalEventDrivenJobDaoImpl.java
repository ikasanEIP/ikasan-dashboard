package org.ikasan.scheduled.job.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.job.model.SolrInternalEventDrivenJobRecordImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.job.dao.InternalEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJob;
import org.ikasan.spec.scheduled.job.model.InternalEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
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
            InternalEventDrivenJob job = event.getInternalEventDrivenJob();
            document.addField(ID, JobConstants.INTERNAL_EVENT_DRIVEN_JOB + "_" + event.getAgentName() + "_"
                + event.getJobName() + "_" + job.getContextName());
            document.addField(PAYLOAD_CONTENT, getInternalEventDrivenJob(job));
            document.setField(TARGET_RESIDING_CONTEXT_ONLY, job.isTargetResidingContextOnly());
            document.setField(PARTICIPATES_IN_LOCK, job.isParticipatesInLock());
            document.addField(COMPONENT_NAME, job.getContextName());
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException(String.format("Cannot convert InternalEventDrivenJob to string! [%s]", event), e);
        }

        document.addField(MODULE_NAME, event.getAgentName());
        document.addField(FLOW_NAME, event.getJobName());
        document.addField(CREATED_DATE_TIME, event.getTimestamp());
        document.addField(UPDATED_DATE_TIME, System.currentTimeMillis());
        document.addField(MODIFIED_BY, event.getModifiedBy());
        document.setField(EXPIRY, DO_NOT_EXPIRE);
        document.setField(HELD, event.isHeld());
        document.setField(SKIPPED, event.isSkipped());

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

    @Override
    public void skip(InternalEventDrivenJobRecord jobRecord, List<String> childContextNames, String actor) {
        InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();
        internalEventDrivenJob.setSkippedContexts(new HashMap<>());
        if(internalEventDrivenJob.isTargetResidingContextOnly()) {
            childContextNames.forEach(name ->
                internalEventDrivenJob.getSkippedContexts().put(name, true));
        }
        else {
            internalEventDrivenJob.getChildContextNames().forEach(name ->
                internalEventDrivenJob.getSkippedContexts().put(name, true));
        }

        jobRecord.setSkipped(true);
        jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
        jobRecord.setModifiedBy(actor);

        this.save(jobRecord);
    }

    @Override
    public void hold(InternalEventDrivenJobRecord jobRecord, List<String> childContextNames, String actor) {
        InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();
        internalEventDrivenJob.setHeldContexts(new HashMap<>());
        if(internalEventDrivenJob.isTargetResidingContextOnly()) {
            childContextNames.forEach(name ->
                internalEventDrivenJob.getHeldContexts().put(name, true));
        }
        else {
            internalEventDrivenJob.getChildContextNames().forEach(name ->
                internalEventDrivenJob.getHeldContexts().put(name, true));
        }

        jobRecord.setHeld(true);
        jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
        jobRecord.setModifiedBy(actor);

        this.save(jobRecord);
    }

    @Override
    public void enable(InternalEventDrivenJobRecord jobRecord, String actor) {
        InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();
        internalEventDrivenJob.setSkippedContexts(new HashMap<>());
        jobRecord.setSkipped(false);
        jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
        jobRecord.setModifiedBy(actor);

        this.save(jobRecord);
    }

    @Override
    public void release(InternalEventDrivenJobRecord jobRecord, String actor) {
        InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();
        internalEventDrivenJob.setHeldContexts(new HashMap<>());
        jobRecord.setHeld(false);
        jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
        jobRecord.setModifiedBy(actor);

        this.save(jobRecord);
    }

    @Override
    public void releaseAll(List<InternalEventDrivenJobRecord> jobRecords, String actor) {
        jobRecords.forEach(jobRecord -> {
            InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();
            internalEventDrivenJob.setHeldContexts(new HashMap<>());
            jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
            jobRecord.setHeld(false);
            jobRecord.setModifiedBy(actor);
        });

        save(jobRecords);
    }

    @Override
    public void holdAll(List<InternalEventDrivenJobRecord> jobRecords, String actor) {
        jobRecords.forEach(jobRecord -> {
            InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();
            if(internalEventDrivenJob.getChildContextNames() != null) {
                HashMap<String, Boolean> heldContexts = new HashMap<>();

                internalEventDrivenJob.getChildContextNames()
                    .forEach(name -> heldContexts.put(name, Boolean.TRUE));

                internalEventDrivenJob.setHeldContexts(heldContexts);
            }
            internalEventDrivenJob.setSkippedContexts(new HashMap<>());
            jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
            jobRecord.setSkipped(false);
            jobRecord.setHeld(true);
            jobRecord.setModifiedBy(actor);
        });

        save(jobRecords);
    }

    @Override
    public void enableAll(List<InternalEventDrivenJobRecord> jobRecords, String actor) {
        jobRecords.forEach(jobRecord -> {
            InternalEventDrivenJob internalEventDrivenJob = jobRecord.getInternalEventDrivenJob();
            internalEventDrivenJob.setSkippedContexts(new HashMap<>());
            jobRecord.setInternalEventDrivenJob(internalEventDrivenJob);
            jobRecord.setSkipped(false);
            jobRecord.setModifiedBy(actor);
        });

        save(jobRecords);
    }
}
