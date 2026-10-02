package org.ikasan.rest.dashboard.model.systemevent;

import org.ikasan.spec.systemevent.SystemEventRecord;

import java.util.Date;
import java.util.StringJoiner;

/**
 * Created by Ikasan Development Team.
 */
public class SystemEventRecordImpl implements SystemEventRecord
{
    private String id;
    private String type;
    private String moduleName;
    private String actor;
    private String payload;
    private String action;
    private String subject;
    private long timestampLong;
    private long expiryLong;


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
        if(id.contains("-")) {
            return new Long(id.substring(id.lastIndexOf("-"+1)));
        }
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

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
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
        return new StringJoiner(", ", SystemEventRecordImpl.class.getSimpleName() + "[", "]").add("id='" + id + "'").add(
            "moduleName='" + moduleName + "'").add("actor='" + actor + "'").add("action='" + action + "'")
                                                                                       .add("subject='" + subject + "'")
                                                                                       .add("timestamp=" + timestampLong)
                                                                                       .add("expiry=" + expiryLong)
                                                                                       .toString();
    }
}
