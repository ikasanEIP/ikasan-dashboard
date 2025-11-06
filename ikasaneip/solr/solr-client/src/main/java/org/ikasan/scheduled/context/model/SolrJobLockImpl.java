package org.ikasan.scheduled.context.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.job.orchestration.util.serialise.SortedSchedulerJobLocalParticipantMapSerializer;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolrJobLockImpl implements JobLock {

    private String name;
    private long lockCount = 1;
    @JsonSerialize(using = SortedSchedulerJobLocalParticipantMapSerializer.class)
    private Map<String, List<SchedulerJobLockParticipant>>  jobs = new HashMap<>();
    private boolean exclusiveJobLock = false;

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setLockCount(long lockCount) {
        this.lockCount = lockCount;
    }

    @Override
    public long getLockCount() {
        return lockCount;
    }

    @Override
    public void setJobs(Map<String, List<SchedulerJobLockParticipant>>  jobs) {
        this.jobs = jobs;
    }

    @Override
    public Map<String, List<SchedulerJobLockParticipant>>  getJobs() {
        return jobs;
    }

    @Override
    public boolean isExclusiveJobLock() {
        return exclusiveJobLock;
    }

    @Override
    public void setExclusiveJobLock(boolean exclusiveJobLock) {
        this.exclusiveJobLock = exclusiveJobLock;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
