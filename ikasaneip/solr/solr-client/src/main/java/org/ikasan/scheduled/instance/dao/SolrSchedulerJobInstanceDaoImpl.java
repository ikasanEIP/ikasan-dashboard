package org.ikasan.scheduled.instance.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.instance.model.SolrContextInstanceAggregateJobStatusImpl;
import org.ikasan.scheduled.instance.model.SolrInternalEventDrivenJobInstanceImpl;
import org.ikasan.scheduled.instance.model.SolrSchedulerJobInstanceRecordImpl;
import org.ikasan.scheduled.instance.model.SolrSchedulerJobInstanceSearchFilterImpl;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.solr.util.SolrSpecialCharacterEscapeUtil;
import org.ikasan.spec.scheduled.instance.dao.SchedulerJobInstanceDao;
import org.ikasan.spec.scheduled.instance.model.*;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrConstants;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class SolrSchedulerJobInstanceDaoImpl extends SolrDaoBase<SchedulerJobInstanceRecord> implements SchedulerJobInstanceDao {

    private static ObjectMapper objectMapper = ScheduledObjectMapperFactory.newInstance();

    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(SolrSchedulerJobInstanceDaoImpl.class);

    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, SchedulerJobInstanceRecord schedulerJobInstanceRecord) {
        SolrInputDocument document = new SolrInputDocument();
        SchedulerJobInstance schedulerJobInstance = schedulerJobInstanceRecord.getSchedulerJobInstance();

        if(schedulerJobInstance instanceof FileEventDrivenJobInstance) {
            document.addField(ID, schedulerJobInstanceRecord.getJobName()
                + "_" + schedulerJobInstanceRecord.getContextInstanceId()
                + "_" + schedulerJobInstanceRecord.getChildContextName()
                + "_" + JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE);
            document.addField(TYPE, JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE);
        }
        else if(schedulerJobInstance instanceof InternalEventDrivenJobInstance) {
            document.addField(ID, schedulerJobInstanceRecord.getJobName()
                + "_" + schedulerJobInstanceRecord.getContextInstanceId()
                + "_" + schedulerJobInstanceRecord.getChildContextName()
                + "_" + JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
            document.addField(TYPE, JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
            document.addField(TARGET_RESIDING_CONTEXT_ONLY,
                ((InternalEventDrivenJobInstance)schedulerJobInstance).isTargetResidingContextOnly());
            document.addField(PARTICIPATES_IN_LOCK,
                ((InternalEventDrivenJobInstance)schedulerJobInstance).isParticipatesInLock());
        }
        else if(schedulerJobInstance instanceof QuartzScheduleDrivenJobInstance) {
            document.addField(ID, schedulerJobInstanceRecord.getJobName()
                + "_" + schedulerJobInstanceRecord.getContextInstanceId()
                + "_" + schedulerJobInstanceRecord.getChildContextName()
                + "_" + JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE);
            document.addField(TYPE, JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE);
        }
        else if(schedulerJobInstance instanceof GlobalEventJobInstance) {
            document.addField(ID, schedulerJobInstanceRecord.getJobName()
                + "_" + schedulerJobInstanceRecord.getContextInstanceId()
                + "_" + schedulerJobInstanceRecord.getChildContextName()
                + "_" + JobConstants.GLOBAL_EVENT_JOB_INSTANCE);
            document.addField(TYPE, JobConstants.GLOBAL_EVENT_JOB_INSTANCE);
        }
        else if(schedulerJobInstance instanceof ContextStartJobInstance) {
            document.addField(ID, schedulerJobInstanceRecord.getJobName()
                + "_" + schedulerJobInstanceRecord.getContextInstanceId()
                + "_" + schedulerJobInstanceRecord.getChildContextName()
                + "_" + JobConstants.CONTEXT_START_JOB_INSTANCE);
            document.addField(TYPE, JobConstants.CONTEXT_START_JOB_INSTANCE);
        }
        else if(schedulerJobInstance instanceof ContextTerminalJobInstance) {
            document.addField(ID, schedulerJobInstanceRecord.getJobName()
                + "_" + schedulerJobInstanceRecord.getContextInstanceId()
                + "_" + schedulerJobInstanceRecord.getChildContextName()
                + "_" + JobConstants.CONTEXT_TERMINAL_JOB_INSTANCE);
            document.addField(TYPE, JobConstants.CONTEXT_TERMINAL_JOB_INSTANCE);
        }
        else if(schedulerJobInstance instanceof LocalEventJobInstance) {
            document.addField(ID, schedulerJobInstanceRecord.getJobName()
                + "_" + schedulerJobInstanceRecord.getContextInstanceId()
                + "_" + schedulerJobInstanceRecord.getChildContextName()
                + "_" + JobConstants.LOCAL_EVENT_JOB_INSTANCE);
            document.addField(TYPE, JobConstants.LOCAL_EVENT_JOB_INSTANCE);
        }
        else {
            logger.info("here");
        }

        try {
            document.addField(PAYLOAD_CONTENT, this.getPayloadContents(schedulerJobInstance));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Cannot convert FileEventDrivenJob to string! [%s]"
                , schedulerJobInstanceRecord.getSchedulerJobInstance()));
        }

        if(schedulerJobInstance.getScheduledProcessEvent() != null) {
            document.addField(START_TIME, schedulerJobInstance.getScheduledProcessEvent().getFireTime());
            document.addField(END_TIME, schedulerJobInstance.getScheduledProcessEvent().getCompletionTime());
        }

        document.addField(DISPLAY_NAME, schedulerJobInstance.getDisplayName());
        document.addField(STATUS, schedulerJobInstanceRecord.getStatus());
        document.addField(MODULE_NAME, schedulerJobInstanceRecord.getJobName());
        document.addField(FLOW_NAME, schedulerJobInstanceRecord.getContextName());
        document.addField(CHILD_CONTEXT_NAME, schedulerJobInstanceRecord.getChildContextName());
        document.addField(COMPONENT_NAME, schedulerJobInstanceRecord.getContextInstanceId());
        document.addField(CREATED_DATE_TIME, schedulerJobInstanceRecord.getTimestamp());
        document.addField(UPDATED_DATE_TIME, System.currentTimeMillis());
        document.addField(MODIFIED_BY, schedulerJobInstanceRecord.getModifiedBy());
        document.addField(MANUALLY_SUBMITTED_BY, schedulerJobInstanceRecord.getManuallySubmittedBy());
        document.setField(EXPIRY, expiry);

        logger.debug(String.format("Converted scheduled context instance to SolrDocument[%s]", document));
        return document;
    }

    protected String getPayloadContents(SchedulerJobInstance contextInstance) throws JsonProcessingException {
        return objectMapper.writeValueAsString(contextInstance);
    }

    @Override
    public SchedulerJobInstanceRecord findById(String id) {
        StringBuffer queryString = new StringBuffer();
        queryString
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
    public boolean doesJobPlanInstanceContainRepeatingJobs(String contextInstanceId) {
        SolrSchedulerJobInstanceSearchFilterImpl filter = new SolrSchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId(contextInstanceId);
        SearchResults<SchedulerJobInstanceRecord> results =  this.getScheduledContextInstancesByFilter(filter
            , -1, -1, null, null);

        for(SchedulerJobInstanceRecord record: results.getResultList()) {
            SchedulerJobInstance instance = record.getSchedulerJobInstance();
            if(instance instanceof SolrInternalEventDrivenJobInstanceImpl) {
                if(((SolrInternalEventDrivenJobInstanceImpl)instance).isJobRepeatable()) {
                    return true;
                }
            }
        }

        return false;
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
            typeBuffer.append(OR).append(" ");
            typeBuffer.append(TYPE + COLON);
            typeBuffer.append("\"").append(JobConstants.GLOBAL_EVENT_JOB_INSTANCE).append("\" ");
            typeBuffer.append(OR).append(" ");
            typeBuffer.append(TYPE + COLON);
            typeBuffer.append("\"").append(JobConstants.LOCAL_EVENT_JOB_INSTANCE).append("\" ");

            if(filter.includeStartAndTerminalJobsInSearchResults()) {
                typeBuffer.append(OR).append(" ");
                typeBuffer.append(TYPE + COLON);
                typeBuffer.append("\"").append(JobConstants.CONTEXT_START_JOB_INSTANCE).append("\" ");
                typeBuffer.append(OR).append(" ");
                typeBuffer.append(TYPE + COLON);
                typeBuffer.append("\"").append(JobConstants.CONTEXT_TERMINAL_JOB_INSTANCE).append("\" ");
            }

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

        if(filter.getDisplayNameFilter() != null && !filter.getDisplayNameFilter().isEmpty()) {
            queryString.append(AND)
                .append(DISPLAY_NAME)
                .append(COLON)
                .append(WILDCARD)
                .append(SolrSpecialCharacterEscapeUtil.escape(filter.getDisplayNameFilter()))
                .append(WILDCARD);
        }

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

        if(filter.getStartTimeWindowStart() > 0 && filter.getStartTimeWindowEnd() > 0) {
            queryString.append(AND).append(START_TIME).append(COLON).append(" [")
                .append(filter.getStartTimeWindowStart()).append(TO)
                .append(filter.getStartTimeWindowEnd()).append("] ");
        }

        if(filter.getEndTimeWindowStart() > 0 && filter.getEndTimeWindowEnd() > 0) {
            queryString.append(AND).append(END_TIME).append(COLON).append(" [")
                .append(filter.getEndTimeWindowStart()).append(TO)
                .append(filter.getEndTimeWindowEnd()).append("] ");
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

    @Override
    public List<ContextInstanceAggregateJobStatus> getJobStatusCountForContextInstances(List<String> contextInstanceIds) {
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
        typeBuffer.append(OR).append(" ");
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.GLOBAL_EVENT_JOB_INSTANCE).append("\" ");
        typeBuffer.append(OR).append(" ");
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.LOCAL_EVENT_JOB_INSTANCE).append("\" ");
        typeBuffer.append(CLOSE_BRACKET);

        StringBuffer queryString = new StringBuffer();
        queryString.append(typeBuffer)
            .append(AND)
            .append(COMPONENT_NAME)
            .append(COLON)
            .append("%s");

        List<ContextInstanceAggregateJobStatus> results = new ArrayList<>();
        contextInstanceIds.forEach(id -> {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(String.format(queryString.toString(), id));
            solrQuery.setFacet(true);
            solrQuery.addFacetField(SolrDaoBase.STATUS, SolrDaoBase.FLOW_NAME);
            solrQuery.setRows(0);

            QueryRequest req = new QueryRequest(solrQuery, SolrRequest.METHOD.POST);
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);

            try {
                QueryResponse rsp = req.process(this.solrClient, SolrConstants.CORE);
                FacetField field = rsp.getFacetField(SolrDaoBase.STATUS);
                HashMap<String, Integer> jobStatusCount = new HashMap<>();

                field.getValues().forEach(count -> jobStatusCount.put(count.getName(), (int) count.getCount()));

                FacetField contextName = rsp.getFacetField(SolrDaoBase.FLOW_NAME);

                results.add(new SolrContextInstanceAggregateJobStatusImpl(id, contextName.getValues().get(0).getName(),
                    jobStatusCount, false));

            } catch (Exception e) {
                throw new RuntimeException("Error resolving aggregate jobs statuses record data by query [" + queryString
                    + "] from the Ikasan solr index!", e);
            }
        });
        return results;
    }

    @Override
    public List<ContextInstanceAggregateJobStatus> getJobStatusCountForContextInstancesConsiderNonTargetedDuplication(List<String> contextInstanceIds) {
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
        typeBuffer.append(OR).append(" ");
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.GLOBAL_EVENT_JOB_INSTANCE).append("\" ");
        typeBuffer.append(OR).append(" ");
        typeBuffer.append(TYPE + COLON);
        typeBuffer.append("\"").append(JobConstants.LOCAL_EVENT_JOB_INSTANCE).append("\" ");
        typeBuffer.append(CLOSE_BRACKET);

        StringBuffer queryString = new StringBuffer();
        queryString.append(typeBuffer)
            .append(AND)
            .append(COMPONENT_NAME)
            .append(COLON)
            .append("%s");

        List<ContextInstanceAggregateJobStatus> results = new ArrayList<>();
        contextInstanceIds.forEach(id -> {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(String.format(queryString.toString(), id));
            solrQuery.addField(SolrDaoBase.STATUS);
            solrQuery.addField(SolrDaoBase.FLOW_NAME);
            solrQuery.addField(SolrDaoBase.MODULE_NAME);
            solrQuery.addField(SolrDaoBase.TARGET_RESIDING_CONTEXT_ONLY);
            solrQuery.setRows(50);

            QueryRequest req = new QueryRequest(solrQuery, SolrRequest.METHOD.POST);
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);

            try {
                QueryResponse rsp = req.process(this.solrClient, SolrConstants.CORE);

                if (rsp.getResults().getNumFound() > 50) {
                    solrQuery.setRows((int) rsp.getResults().getNumFound());
                    rsp = req.process(this.solrClient, SolrConstants.CORE);
                }

                Map<String, Integer> statusCounts = new ConcurrentHashMap<>();
                List<String> countedJobs = new ArrayList<>();

                AtomicReference<String> contextName = new AtomicReference<>();

                rsp.getResults().forEach(doc -> {
                    String jobName = (String)doc.getFieldValue(SolrDaoBase.MODULE_NAME);
                    contextName.set((String) doc.getFieldValue(SolrDaoBase.FLOW_NAME));
                    String status = (String)doc.getFieldValue(SolrDaoBase.STATUS);
                    boolean targetResidingContextOnly
                        = doc.getFieldValue(SolrDaoBase.TARGET_RESIDING_CONTEXT_ONLY) != null
                            ? (boolean)doc.getFieldValue(SolrDaoBase.TARGET_RESIDING_CONTEXT_ONLY)
                            : false;

                    if(statusCounts.containsKey(status)) {
                        if(!countedJobs.contains(jobName) || (countedJobs.contains(jobName) && targetResidingContextOnly)) {
                            statusCounts.merge(status, 1, Integer::sum);
                        }
                        countedJobs.add(jobName);
                    }
                    else {
                        statusCounts.put(status, 1);
                        countedJobs.add(jobName);
                    }
                });

                results.add(new SolrContextInstanceAggregateJobStatusImpl(id, contextName.get(), statusCounts,
                    this.doesJobPlanInstanceContainRepeatingJobs(id)));

            } catch (Exception e) {
                throw new RuntimeException("Error resolving aggregate jobs statuses record data by query [" + queryString
                    + "] from the Ikasan solr index!", e);
            }
        });
        return results;
    }

    @Override
    public void deleteSchedulerJobInstances(String contextInstanceId) {
        StringBuffer queryBuffer = new StringBuffer(COMPONENT_NAME).append(COLON).append("\"").append(contextInstanceId).append("\"")
            .append(AND)
            .append(OPEN_BRACKET)
            .append(TYPE).append(COLON).append(JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE)
            .append(OR)
            .append(TYPE).append(COLON).append(JobConstants.GLOBAL_EVENT_JOB_INSTANCE)
            .append(OR)
            .append(TYPE).append(COLON).append(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE)
            .append(OR)
            .append(TYPE).append(COLON).append(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE)
            .append(OR)
            .append(TYPE + COLON).append(JobConstants.CONTEXT_START_JOB_INSTANCE)
            .append(OR)
            .append(TYPE + COLON).append(JobConstants.CONTEXT_TERMINAL_JOB_INSTANCE)
            .append(OR)
            .append(TYPE + COLON).append(JobConstants.LOCAL_EVENT_JOB_INSTANCE)
            .append(CLOSE_BRACKET);

        super.deleteByQuery(queryBuffer.toString());
    }
}
