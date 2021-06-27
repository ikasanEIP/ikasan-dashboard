package org.ikasan.scheduled.model;

import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.spec.solr.SolrDaoBase;

public class SolrScheduledProcessEvent {
    @Field(SolrDaoBase.ID)
    private String id;

    @Field(SolrDaoBase.MODULE_NAME)
    private String agentName;

    @Field(SolrDaoBase.PAYLOAD_CONTENT)
    private String scheduledProcessEvent;


    public String getId()
    {
        return this.id;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getScheduledProcessEvent()
    {
        return this.scheduledProcessEvent;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("SolrScheduledProcessEvent{");
        sb.append("id='").append(id).append('\'');
        sb.append(", agentName='").append(agentName).append('\'');
        sb.append(", scheduledProcessEvent='").append(scheduledProcessEvent).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
