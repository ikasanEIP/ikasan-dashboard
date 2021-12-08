package org.ikasan.scheduled.event.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.scheduled.event.model.ScheduledProcessEventSearchResults;
import org.ikasan.scheduled.event.model.SolrScheduledProcessEvent;
import org.ikasan.scheduled.event.model.SolrScheduledProcessEventRecord;
import org.ikasan.spec.scheduled.event.model.ScheduledProcessEvent;
import org.ikasan.spec.solr.SolrConstants;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SolrScheduledProcessEventDao extends SolrDaoBase<ScheduledProcessEvent>
{
    /**
     * Logger for this class
     */
    private static Logger logger = LoggerFactory.getLogger(SolrScheduledProcessEventDao.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * We need to give this dao it's context.
     */
    public static final String SCHEDULED_PROCESS_EVENT = "scheduledProcessEvent";

    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, ScheduledProcessEvent scheduledProcessEvent)
    {
        SolrInputDocument document = new SolrInputDocument();
        document.addField(TYPE, SCHEDULED_PROCESS_EVENT);
        try {
            document.addField(PAYLOAD_CONTENT, getScheduledProcessEventContent(scheduledProcessEvent));
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Cannot convert scheduled process event to string! [%s]", scheduledProcessEvent));
        }

        document.addField(ID, scheduledProcessEvent.getAgentName()
            + "-" + SCHEDULED_PROCESS_EVENT + "-" + scheduledProcessEvent.hashCode());
        document.addField(MODULE_NAME, scheduledProcessEvent.getAgentName());

        document.addField(FLOW_NAME, scheduledProcessEvent.getJobGroup());
        document.addField(COMPONENT_NAME, scheduledProcessEvent.getJobName());

        if(scheduledProcessEvent.getFireTime() > 0) {
            document.addField(CREATED_DATE_TIME, scheduledProcessEvent.getFireTime());
        }
        document.setField(EXPIRY, expiry);

        logger.debug(String.format("Converted scheduled process event to SolrDocument[%s]", document));
        return document;
    }

    private String getScheduledProcessEventContent(ScheduledProcessEvent scheduledProcessEvent) throws JsonProcessingException {
        return this.objectMapper.writeValueAsString(scheduledProcessEvent);
    }

    public List<String> getAllAgentNames() {
        return super.fieldFacetQuery("type:\"moduleMetaData\" AND payload:\"*\\\"type\\\":\\\"SCHEDULER_AGENT\\\"*\"", "id");
    }

    public ScheduledProcessEventSearchResults<ScheduledProcessEvent> getScheduleProcessEvents(String agent, long startTime, long endTime) {
        StringBuffer agentQuery = super.buildFieldPredicate(addParenthesisToString(agent), SolrDaoBase.MODULE_NAME);
        StringBuffer typeQuery = super.buildFieldPredicate("scheduledProcessEvent", SolrDaoBase.TYPE);
        StringBuffer betweenDates = super.buildDatePredicate(SolrDaoBase.CREATED_DATE_TIME, new Date(startTime), new Date(endTime));

        SolrQuery query = new SolrQuery();
        query.setQuery(agentQuery.toString() + " AND " + typeQuery + " AND " + betweenDates);
        query.addSort(SolrDaoBase.CREATED_DATE_TIME, SolrQuery.ORDER.desc);

        query.setRows(0);
        try
        {
            QueryRequest req = new QueryRequest(query);
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);

            QueryResponse rsp = req.process(this.solrClient, SolrConstants.CORE);
            query.setRows((int)rsp.getResults().getNumFound());

            req = new QueryRequest(query);
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);
            rsp = req.process(this.solrClient, SolrConstants.CORE);

            return new ScheduledProcessEventSearchResults(this.convert(rsp.getBeans(SolrScheduledProcessEventRecord.class)),
                rsp.getResults().getNumFound(), rsp.getElapsedTime());
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error resolving solr solr scheduled process event by query [" + query
                + "] from the ikasan solr index!", e);
        }
    }

    public ScheduledProcessEventSearchResults<ScheduledProcessEvent> getScheduleProcessEvents(List<String> accessibleModules, long startTime, long endTime, String filter, boolean failuresOnly, int start, int limit,
                                                                                              String sortOrder) {
        StringBuffer typeQuery = super.buildFieldPredicate("scheduledProcessEvent", SolrDaoBase.TYPE);
        StringBuffer betweenDates = super.buildDatePredicate(SolrDaoBase.CREATED_DATE_TIME, new Date(startTime), new Date(endTime));

        SolrQuery query = new SolrQuery();

        StringBuffer queryBuffer = new StringBuffer(typeQuery + " AND " + betweenDates);

        if(accessibleModules != null && accessibleModules.size() > 0) {
            queryBuffer.append(" AND ").append(this.buildPredicate(MODULE_NAME, accessibleModules));
        }
        else if(accessibleModules != null && accessibleModules.size() == 0) {
            accessibleModules.add("NOTAVALIDMODULENAME");
            queryBuffer.append(" AND ").append(this.buildPredicate(MODULE_NAME, accessibleModules));
        }

        if(failuresOnly) {
            queryBuffer.append(" AND payload:\"*\\\"successful\\\":\\\"false\\\"*\"");
        }

        if(filter != null && !filter.isEmpty()) {
            queryBuffer.append(" AND (payload:*"+filter+"*").append(" OR payload:\""+filter).append("\")");
        }

        query.setQuery(queryBuffer.toString());

        if(sortOrder != null && !sortOrder.isEmpty())
        {
            if(sortOrder.equals("asc"))
            {
                query.addSort(SolrDaoBase.CREATED_DATE_TIME, SolrQuery.ORDER.asc);
            }
            else
            {
                query.addSort(SolrDaoBase.CREATED_DATE_TIME, SolrQuery.ORDER.desc);
            }
        }
        else
        {
            // Default
            query.addSort(SolrDaoBase.CREATED_DATE_TIME, SolrQuery.ORDER.desc);
        }

        query.setStart(start);
        query.setRows(limit);

        try
        {
            QueryRequest req = new QueryRequest(query);
            req.setBasicAuthCredentials(this.solrUsername, this.solrPassword);

            QueryResponse rsp = req.process(this.solrClient, SolrConstants.CORE);

            return new ScheduledProcessEventSearchResults(this.convert(rsp.getBeans(SolrScheduledProcessEventRecord.class)),
                rsp.getResults().getNumFound(), rsp.getElapsedTime());
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error resolving solr solr scheduled process event by query [" + query
                + "] from the ikasan solr index!", e);
        }
    }

    private List<ScheduledProcessEvent> convert(List<SolrScheduledProcessEventRecord> records) throws JsonProcessingException {
        List<ScheduledProcessEvent> converted = new ArrayList<>();

        for (SolrScheduledProcessEventRecord record : records) {
            converted.add(this.objectMapper.readValue(record.getScheduledProcessEvent(), SolrScheduledProcessEvent.class));
        }

        return converted;
    }

    private String addParenthesisToString(String value){
        return "\"" + value + "\"";
    }
}
