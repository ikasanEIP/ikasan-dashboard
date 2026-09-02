package org.ikasan.mongo.persistence.scheduled.event.model;

import org.ikasan.spec.entity.EntityFields;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

public class MongoScheduledProcessEventRecord {
    @Id
    private String id;

    @Field(EntityFields.MODULE_NAME)
    private String agentName;

    @Field(EntityFields.FLOW_NAME)
    private String jobGroupName;

    @Field(EntityFields.COMPONENT_NAME)
    private String jobGroup;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String scheduledProcessEvent;

    @Field(EntityFields.CREATED_DATE_TIME)
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
        final StringBuffer sb = new StringBuffer("MongoScheduledProcessEventRecord{");
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
