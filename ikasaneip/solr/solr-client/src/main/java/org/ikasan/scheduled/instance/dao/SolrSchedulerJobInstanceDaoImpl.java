package org.ikasan.scheduled.instance.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.instance.model.SolrSchedulerJobInstanceRecordImpl;
import org.ikasan.scheduled.instance.model.SolrSchedulerJobInstanceSearchFilterImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.solr.util.SolrSpecialCharacterEscapeUtil;
import org.ikasan.spec.scheduled.instance.dao.SchedulerJobInstanceDao;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrSchedulerJobInstanceDaoImpl extends SolrDaoBase<SchedulerJobInstanceRecord> implements SchedulerJobInstanceDao {

    private static ObjectMapper objectMapper = ScheduledObjectMapperFactory.newInstance();

    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(SolrSchedulerJobInstanceDaoImpl.class);



    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, SchedulerJobInstanceRecord schedulerJobInstanceRecord) {
        SolrInputDocument document = new SolrInputDocument();
        if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof FileEventDrivenJobInstance) {
            document.addField(ID, schedulerJobInstanceRecord.getJobName()
                + "_" + schedulerJobInstanceRecord.getContextInstanceId()
                + "_" + schedulerJobInstanceRecord.getChildContextName()
                + "_" + JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE);
            document.addField(TYPE, JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE);
        }
        else if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof InternalEventDrivenJobInstance) {
            document.addField(ID, schedulerJobInstanceRecord.getJobName()
                + "_" + schedulerJobInstanceRecord.getContextInstanceId()
                + "_" + schedulerJobInstanceRecord.getChildContextName()
                + "_" + JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
            document.addField(TYPE, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
            document.addField(TARGET_RESIDING_CONTEXT_ONLY,
                ((InternalEventDrivenJobInstance)schedulerJobInstanceRecord.getSchedulerJobInstance()).isTargetResidingContextOnly());
            document.addField(PARTICIPATES_IN_LOCK,
                ((InternalEventDrivenJobInstance)schedulerJobInstanceRecord.getSchedulerJobInstance()).isParticipatesInLock());
        }
        else if(schedulerJobInstanceRecord.getSchedulerJobInstance() instanceof QuartzScheduleDrivenJobInstance) {
            document.addField(ID, schedulerJobInstanceRecord.getJobName()
                + "_" + schedulerJobInstanceRecord.getContextInstanceId()
                + "_" + schedulerJobInstanceRecord.getChildContextName()
                + "_" + JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE);
            document.addField(TYPE, JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE);
        }

        try {
            document.addField(PAYLOAD_CONTENT, this.getPayloadContents(schedulerJobInstanceRecord.getSchedulerJobInstance()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Cannot convert FileEventDrivenJob to string! [%s]"
                , schedulerJobInstanceRecord.getSchedulerJobInstance()));
        }
        document.addField(STATUS, schedulerJobInstanceRecord.getStatus());
        document.addField(MODULE_NAME, schedulerJobInstanceRecord.getJobName());
        document.addField(FLOW_NAME, schedulerJobInstanceRecord.getContextName());
        document.addField(CHILD_CONTEXT_NAME, schedulerJobInstanceRecord.getChildContextName());
        document.addField(COMPONENT_NAME, schedulerJobInstanceRecord.getContextInstanceId());
        document.addField(CREATED_DATE_TIME, schedulerJobInstanceRecord.getTimestamp());
        document.addField(UPDATED_DATE_TIME, System.currentTimeMillis());
        document.addField(MODIFIED_BY, schedulerJobInstanceRecord.getModifiedBy());
        document.setField(EXPIRY, expiry);

        logger.debug(String.format("Converted scheduled context instance to SolrDocument[%s]", document));
        return document;
    }

    protected String getPayloadContents(SchedulerJobInstance contextInstance) throws JsonProcessingException {
        return objectMapper.writeValueAsString(contextInstance);
    }

    @Override
    public SchedulerJobInstanceRecord findById(String id) {
        StringBuffer typeBuffer = new StringBuffer();
        typeBuffer.append(OPEN_BRACKET);
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE).append("\" ");
        typeBuffer.append(OR).append(" ");
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE).append("\" ");
        typeBuffer.append(OR).append(" ");
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE).append("\" ");
        typeBuffer.append(CLOSE_BRACKET);

        StringBuffer queryString = new StringBuffer();
        queryString.append(typeBuffer)
            .append(AND)
            .append(ID).append(COLON)
            .append("\"").append(SolrSpecialCharacterEscapeUtil.escape(id)).append("\"");

        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(queryString.toString());

        logger.debug("query: " + solrQuery);

        SearchResults<SchedulerJobInstanceRecord> searchResults = this.findByQuery(solrQuery, SolrSchedulerJobInstanceRecordImpl.class, 0, 1);
        return searchResults.getResultList().size() > 0 ? searchResults.getResultList().get(0) : null;
    }


    @Override
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextInstanceId(String contextInstanceId, int limit, int offset, String sortField, String sortDirection) {
        SolrSchedulerJobInstanceSearchFilterImpl filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId(contextInstanceId);
        return this.getScheduledContextInstancesByFilter(filter, limit, offset, sortField, sortDirection);
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getSchedulerJobInstancesByContextName(String contextName, int limit, int offset, String sortField, String sortDirection) {
        SolrSchedulerJobInstanceSearchFilterImpl filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setContextName(contextName);
        return this.getScheduledContextInstancesByFilter(filter, limit, offset, sortField, sortDirection);
    }

    @Override
    public SearchResults<SchedulerJobInstanceRecord> getScheduledContextInstancesByFilter(SchedulerJobInstanceSearchFilter filter, int limit, int offset, String sortField, String sortDirection) {

        StringBuffer queryString = new StringBuffer();

        if(filter.getJobType() != null && !filter.getJobType().isEmpty()) {
            queryString.append(TYPE + COLON).append(filter.getJobType());
        }
        else {
            StringBuffer typeBuffer = new StringBuffer();
            typeBuffer.append(OPEN_BRACKET);
            typeBuffer.append(TYPE + COLON);
            typeBuffer.append("\"").append(JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE).append("\" ");
            typeBuffer.append(OR).append(" ");
            typeBuffer.append(TYPE + COLON);
            typeBuffer.append("\"").append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE).append("\" ");
            typeBuffer.append(OR).append(" ");
            typeBuffer.append(TYPE + COLON);
            typeBuffer.append("\"").append(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE).append("\" ");
            typeBuffer.append(CLOSE_BRACKET);

            queryString.append(typeBuffer);
        }


        queryString.append(AND)
            .append(MODULE_NAME).append(COLON)
            .append(filter.getJobName() != null && !filter.getJobName().isEmpty() ? SolrSpecialCharacterEscapeUtil.escape(filter.getJobName()) : "*");

        queryString.append(AND)
            .append(FLOW_NAME)
                .append(COLON)
            .append(filter.getContextName() != null && !filter.getContextName().isEmpty() ? SolrSpecialCharacterEscapeUtil.escape(filter.getContextName()) : "*");

        queryString.append(AND)
            .append(COMPONENT_NAME)
            .append(COLON)
            .append(filter.getContextInstanceId() != null && !filter.getContextInstanceId().isEmpty() ? SolrSpecialCharacterEscapeUtil.escape(filter.getContextInstanceId()) : "*");

        queryString.append(AND)
            .append(CHILD_CONTEXT_NAME)
            .append(COLON)
            .append(filter.getChildContextName() != null && !filter.getChildContextName().isEmpty() ? SolrSpecialCharacterEscapeUtil.escape(filter.getChildContextName()) : "*");

        if(filter.isTargetResidingContextOnly() != null && filter.isTargetResidingContextOnly().booleanValue()) {
            queryString.append(AND)
                .append(TARGET_RESIDING_CONTEXT_ONLY)
                .append(COLON)
                .append(true);
        }

        if(filter.isParticipatesInLock() != null) {
            queryString.append(AND)
                .append(PARTICIPATES_IN_LOCK)
                .append(COLON)
                .append(filter.isParticipatesInLock());
        }

        if(filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            if(filter.getStatus().equals(InstanceStatus.SKIPPED.name())) {
                filter.setStatus(InstanceStatus.SKIPPED.toString());
                queryString.append(AND).append(OPEN_BRACKET)
                    .append(STATUS).append(COLON).append(filter.getStatus())
                    .append(OR)
                    .append(STATUS).append(COLON).append(InstanceStatus.SKIPPED_COMPLETE.name())
                    .append(OR)
                    .append(STATUS).append(COLON).append(InstanceStatus.SKIPPED_RUNNING.name())
                    .append(CLOSE_BRACKET);
            }
            else {
                queryString.append(AND).append(STATUS).append(COLON).append(filter.getStatus());
            }
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

        return this.findByQuery(solrQuery, SolrSchedulerJobInstanceRecordImpl.class, offset, limit);
    }
}
