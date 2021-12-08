package org.ikasan.scheduled.event.model;

import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.spec.solr.SolrDaoBase;

public class SolrScheduledProcessEventRecord {
    @Field(SolrDaoBase.ID)
    private String id;

    @Field(SolrDaoBase.MODULE_NAME)
    private String agentName;

    @Field(SolrDaoBase.FLOW_NAME)
    private String jobGroupName;

    @Field(SolrDaoBase.COMPONENT_NAME)
    private String jobGroup;

    @Field(SolrDaoBase.PAYLOAD_CONTENT)
    private String scheduledProcessEvent;

    @Field(SolrDaoBase.CREATED_DATE_TIME)
    private long timestamp;

    public String getId()
    {
        return this.id;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getJobGroupName() {
        return jobGroupName;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public String getScheduledProcessEvent()
    {
        return this.scheduledProcessEvent;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("SolrScheduledProcessEventRecord{");
        sb.append("id='").append(id).append('\'');
        sb.append(", agentName='").append(agentName).append('\'');
        sb.append(", jobGroupName='").append(jobGroupName).append('\'');
        sb.append(", jobGroup='").append(jobGroup).append('\'');
        sb.append(", scheduledProcessEvent='").append(scheduledProcessEvent).append('\'');
        sb.append(", timestamp='").append(timestamp).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
