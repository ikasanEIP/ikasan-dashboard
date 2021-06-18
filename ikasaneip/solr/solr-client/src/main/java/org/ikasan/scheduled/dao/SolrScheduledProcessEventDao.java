package org.ikasan.scheduled.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.spec.scheduled.ScheduledProcessEvent;
import org.ikasan.spec.solr.SolrDaoBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

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

        if(scheduledProcessEvent.getAgentName() != null){
            document.addField(ID, scheduledProcessEvent.getAgentName()
                + "-" + SCHEDULED_PROCESS_EVENT + "-" + UUID.randomUUID());
            document.addField(MODULE_NAME, scheduledProcessEvent.getAgentName());
        }
        else {
            document.addField(ID, SCHEDULED_PROCESS_EVENT + "-" + UUID.randomUUID());
        }

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

    public List<String> getAllAgents() {
        return super.fieldFacetQuery("type:wiretap AND moduleName:murex*", "moduleName");
    }

    public List<String> getJobGroupsForAgent(String agent) {
        return super.fieldFacetQuery("type:wiretap AND moduleName:" + addParenthesisToString(agent), "flowName");
    }

    public List<String> getJobsForAgentAndJobGroup(String agent, String jobGroup) {
        return super.fieldFacetQuery("type:wiretap AND moduleName:"
            + addParenthesisToString(agent) + " AND flowName:" + addParenthesisToString(jobGroup), "componentName");
    }

    public List<String> getConfigurationsForAgent(String agent) {
        return null;
    }

    private String addParenthesisToString(String value){
        return "\"" + value + "\"";
    }
}
