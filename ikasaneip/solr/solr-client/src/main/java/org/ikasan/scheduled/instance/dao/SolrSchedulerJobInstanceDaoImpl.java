package org.ikasan.scheduled.instance.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.instance.model.SolrSchedulerJobInstanceSearchFilterImpl;
import org.ikasan.scheduled.instance.model.SolrSchedulerJobInstanceRecordImpl;
import org.ikasan.spec.scheduled.instance.dao.SchedulerJobInstanceDao;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrSchedulerJobInstanceDaoImpl extends SolrDaoBase<SchedulerJobInstanceRecord> implements SchedulerJobInstanceDao {

    private static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(SolrSchedulerJobInstanceDaoImpl.class);

    /**
     * We need to give this dao it's context.
     */
    public static final String SCHEDULED_JOB_INSTANCE = "schedulerJobInstance";

    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, SchedulerJobInstanceRecord schedulerJobInstanceRecord) {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(ID, schedulerJobInstanceRecord.getJobName() + "_"
            + schedulerJobInstanceRecord.getContextInstanceId() + "_" + SCHEDULED_JOB_INSTANCE);
        document.addField(TYPE, SCHEDULED_JOB_INSTANCE);
        try {
            document.addField(PAYLOAD_CONTENT, this.getPayloadContents(schedulerJobInstanceRecord.getSchedulerJobInstance()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Cannot convert FileEventDrivenJob to string! [%s]"
                , schedulerJobInstanceRecord.getSchedulerJobInstance()));
        }
        document.addField(STATUS, schedulerJobInstanceRecord.getStatus());
        document.addField(MODULE_NAME, schedulerJobInstanceRecord.getJobName());
        document.addField(FLOW_NAME, schedulerJobInstanceRecord.getContextName());
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
        SolrQuery query = super.buildIdQuery(id, SCHEDULED_JOB_INSTANCE);

        logger.debug("query: " + query);

        SearchResults<SchedulerJobInstanceRecord> searchResults = this.findByQuery(query, SolrSchedulerJobInstanceRecordImpl.class, 0, 1);
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
        queryString.append(TYPE).append(COLON).append(SCHEDULED_JOB_INSTANCE)
            .append(AND)
            .append(MODULE_NAME).append(COLON)
            .append(filter.getJobName() != null && !filter.getJobName().isEmpty() ? filter.getJobName() : "*");

        queryString.append(AND)
            .append(FLOW_NAME)
            .append(COLON)
            .append(filter.getContextName() != null && !filter.getContextName().isEmpty() ? filter.getContextName() : "*");

        queryString.append(AND)
            .append(COMPONENT_NAME)
            .append(COLON)
            .append(filter.getContextInstanceId() != null && !filter.getContextInstanceId().isEmpty() ? filter.getContextInstanceId() : "*");

        if(filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            queryString.append(AND).append(STATUS).append(COLON).append(filter.getStatus());
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
