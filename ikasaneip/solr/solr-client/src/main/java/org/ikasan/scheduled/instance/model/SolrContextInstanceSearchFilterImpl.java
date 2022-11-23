package org.ikasan.scheduled.instance.model;

import org.ikasan.spec.scheduled.instance.model.ContextInstanceSearchFilter;

public class SolrContextInstanceSearchFilterImpl implements ContextInstanceSearchFilter {
    private String contextSearchFilter = null;
    private String contextInstanceId = null;
    private long createdTimestamp;
    private long modifiedTimestamp;
    private long startTime;
    private long endTime;
    private String status;

    public String getContextSearchFilter()
    {
        return contextSearchFilter;
    }

    public void setContextSearchFilter(String contextSearchFilter)
    {
        this.contextSearchFilter = contextSearchFilter;
    }

    public String getContextInstanceId() {
        return contextInstanceId;
    }

    public void setContextInstanceId(String contextInstanceId) {
        this.contextInstanceId = contextInstanceId;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @Override
    public long getEndTime() {
        return endTime;
    }

    @Override
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
