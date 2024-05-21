package org.ikasan.scheduled.instance.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.spec.scheduled.instance.model.ContextInstanceAggregateJobStatus;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

import java.util.Map;
import java.util.Objects;

public class SolrContextInstanceAggregateJobStatusImpl implements ContextInstanceAggregateJobStatus {
    private String contextInstanceId;
    private String contextInstanceName;
    private Map<String, Integer> statusCounts;
    private Map<String, Integer> repeatingJobsStatusCounts;
    private boolean containsRepeatableJobs;

    /**
     * Constructor
     *
     * @param contextInstanceId
     * @param contextInstanceName
     * @param statusCounts
     */
    public SolrContextInstanceAggregateJobStatusImpl(String contextInstanceId, String contextInstanceName
        , Map<String, Integer> statusCounts, boolean containsRepeatableJobs) {
        this.contextInstanceId = contextInstanceId;
        this.contextInstanceName = contextInstanceName;
        this.statusCounts = statusCounts;
        this.containsRepeatableJobs = containsRepeatableJobs;
    }

    @Override
    public String getContextInstanceId() {
        return this.contextInstanceId;
    }

    @Override
    public String getContextInstanceName() {
        return this.contextInstanceName;
    }

    @Override
    public int getStatusCount(InstanceStatus instanceStatus) {
        if(!statusCounts.containsKey(instanceStatus.name())) {
            return 0;
        }

        return statusCounts.get(instanceStatus.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(contextInstanceId);
    }

    @Override
    public boolean containsRepeatableJobs() {
        return this.containsRepeatableJobs;
    }

    @Override
    public void setContainsRepeatableJobs(boolean containsRepeatableJobs) {
        this.containsRepeatableJobs = containsRepeatableJobs;
    }

    @Override
    public int repeatingJobInstanceStatusCount(InstanceStatus instanceStatus) {
        if(!this.containsRepeatableJobs) return 0;
        else if(!repeatingJobsStatusCounts.containsKey(instanceStatus.name())) {
            return 0;
        }

        return repeatingJobsStatusCounts.get(instanceStatus.name());
    }

    @Override
    public void setRepeatingJobsStatusCounts(Map<String, Integer> repeatingJobsStatusCounts) {
        this.repeatingJobsStatusCounts = repeatingJobsStatusCounts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SolrContextInstanceAggregateJobStatusImpl that = (SolrContextInstanceAggregateJobStatusImpl) o;
        return Objects.equals(contextInstanceId, that.contextInstanceId);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
