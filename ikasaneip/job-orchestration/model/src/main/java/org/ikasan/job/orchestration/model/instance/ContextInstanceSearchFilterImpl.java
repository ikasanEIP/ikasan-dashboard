package org.ikasan.job.orchestration.model.instance;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceSearchFilter;

import java.util.List;

public class ContextInstanceSearchFilterImpl implements ContextInstanceSearchFilter {
    private String contextSearchFilter = null;
    private List<String> contextInstanceNames;
    private String contextInstanceId = null;
    private long createdTimestamp;
    private long modifiedTimestamp;
    private long startTime;
    private long endTime;
    private long startTimeStart;
    private long endTimeStart;
    private long startTimeEnd;
    private long endTimeEnd;
    private String status;

    public String getContextSearchFilter()
    {
        return contextSearchFilter;
    }

    public void setContextSearchFilter(String contextSearchFilter)
    {
        this.contextSearchFilter = contextSearchFilter;
    }

    public List<String> getContextInstanceNames() {
        return contextInstanceNames;
    }

    public void setContextInstanceNames(List<String> contextInstanceNames) {
        this.contextInstanceNames = contextInstanceNames;
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

    @Override
    public long getStartTimeStart() {
        return startTimeStart;
    }

    @Override
    public void setStartTimeStart(long startTimeStart) {
        this.startTimeStart = startTimeStart;
    }

    @Override
    public long getEndTimeStart() {
        return endTimeStart;
    }

    @Override
    public void setEndTimeStart(long endTimeStart) {
        this.endTimeStart = endTimeStart;
    }

    @Override
    public long getStartTimeEnd() {
        return startTimeEnd;
    }

    @Override
    public void setStartTimeEnd(long startTimeEnd) {
        this.startTimeEnd = startTimeEnd;
    }

    @Override
    public long getEndTimeEnd() {
        return endTimeEnd;
    }

    @Override
    public void setEndTimeEnd(long endTimeEnd) {
        this.endTimeEnd = endTimeEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
