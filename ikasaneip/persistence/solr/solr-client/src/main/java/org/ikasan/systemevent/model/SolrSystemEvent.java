package org.ikasan.systemevent.model;

import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.systemevent.SystemEvent;

import java.util.Date;
import java.util.StringJoiner;

/**
 * Created by Ikasan Development Team.
 */
public class SolrSystemEvent implements SystemEvent
{

    @Field(EntityFields.ID)
    private String id;

    @Field(EntityFields.MODULE_NAME)
    private String moduleName;

    @Field(EntityFields.ACTOR)
    private String actor;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String payload;

    @Field(EntityFields.SYSTEM_EVENT_ACTION)
    private String action;

    @Field(EntityFields.SYSTEM_EVENT_SUBJECT)
    private String subject;

    @Field(EntityFields.CREATED_DATE_TIME)
    private long timestampLong;

    @Field(EntityFields.EXPIRY)
    private long expiryLong;



    /**
     * Used by solr to create results.
     */
    public SolrSystemEvent()
    {

    }

    @Override
    public String getModuleName()
    {
        return this.moduleName;
    }

    @Override
    public String getAction()
    {
        return action;
    }

    @Override
    public String getActor()
    {
        return actor;
    }

    @Override
    public Long getId()
    {
        return new Long(id);
    }

    @Override
    public String getSubject()
    {
        return subject;
    }

    public long getTimestampLong()
    {
        return this.timestampLong;
    }


    public long getExpiryLong()
    {
        return expiryLong;
    }

    @Override
    public Date getTimestamp(){
        return new Date(this.getTimestampLong());
    }

    @Override
    public Date getExpiry(){
        return new Date(this.getExpiryLong());
    }


    public void setId(String id)
    {
        this.id = id;
    }

    public void setModuleName(String moduleName)
    {
        this.moduleName = moduleName;
    }

    public void setActor(String actor)
    {
        this.actor = actor;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public void setTimestampLong(long timestamp)
    {
        this.timestampLong = timestamp;
    }

    public void setExpiryLong(long expiry)
    {
        this.expiryLong = expiry;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Override
    public String toString()
    {
        return new StringJoiner(", ", SolrSystemEvent.class.getSimpleName() + "[", "]").add("id='" + id + "'").add(
            "moduleName='" + moduleName + "'").add("actor='" + actor + "'").add("action='" + action + "'")
                                                                                       .add("subject='" + subject + "'")
                                                                                       .add("timestamp=" + timestampLong)
                                                                                       .add("expiry=" + expiryLong)
                                                                                       .toString();
    }
}
