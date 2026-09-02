package org.ikasan.mongo.persistence.scheduled.instance.model;

import org.ikasan.spec.scheduled.instance.model.ContextInstanceAggregateJobStatus;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

import java.util.HashMap;
import java.util.Map;

public class MongoContextInstanceAggregateJobStatusImpl implements ContextInstanceAggregateJobStatus {
    private final String contextInstanceId;
    private final String contextInstanceName;
    private final Map<InstanceStatus, Integer> statusCounts = new HashMap<>();
    private final Map<String, Integer> repeatingJobsStatusCounts = new HashMap<>();
    private boolean containsRepeatableJobs;

    public MongoContextInstanceAggregateJobStatusImpl(String contextInstanceId, String contextInstanceName) {
        this.contextInstanceId = contextInstanceId;
        this.contextInstanceName = contextInstanceName;
    }

    public void addStatusCount(InstanceStatus status, int count) {
        statusCounts.put(status, statusCounts.getOrDefault(status, 0) + count);
    }

    @Override
    public String getContextInstanceId() {
        return contextInstanceId;
    }

    @Override
    public String getContextInstanceName() {
        return contextInstanceName;
    }

    @Override
    public int getStatusCount(InstanceStatus instanceStatus) {
        return statusCounts.getOrDefault(instanceStatus, 0);
    }

    @Override
    public boolean containsRepeatableJobs() {
        return containsRepeatableJobs;
    }

    @Override
    public void setContainsRepeatableJobs(boolean containsRepeatableJobs) {
        this.containsRepeatableJobs = containsRepeatableJobs;
    }

    @Override
    public int repeatingJobInstanceStatusCount(InstanceStatus instanceStatus) {
        String statusKey = instanceStatus.name();
        return repeatingJobsStatusCounts.getOrDefault(statusKey, 0);
    }

    @Override
    public void setRepeatingJobsStatusCounts(Map<String, Integer> repeatingJobsStatusCounts) {
        this.repeatingJobsStatusCounts.clear();
        if (repeatingJobsStatusCounts != null) {
            this.repeatingJobsStatusCounts.putAll(repeatingJobsStatusCounts);
        }
    }
}
