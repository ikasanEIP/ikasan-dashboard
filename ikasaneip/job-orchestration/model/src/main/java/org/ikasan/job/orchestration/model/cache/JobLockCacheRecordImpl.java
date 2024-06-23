package org.ikasan.job.orchestration.model.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.ikasan.job.orchestration.exception.EntityConversionException;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;

public class JobLockCacheRecordImpl implements JobLockCacheRecord {

    private static ObjectMapper objectMapper;

    static {
        objectMapper = ObjectMapperFactory.newInstance();
    }

    private String id;
    private String jobLockCache;
    private long timestamp;
    private long modifiedTimestamp;

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setJobLockCache(JobLockCacheData jobLockCache) {
        try {
            this.jobLockCache = objectMapper.writeValueAsString(jobLockCache);
        } catch (JsonProcessingException e) {
            throw new EntityConversionException("Could not convert entity to string: " + jobLockCache, e);
        }
    }

    @Override
    public JobLockCacheData getJobLockCache() {
        try {
            return objectMapper.readValue(jobLockCache, JobLockCacheDataImpl.class);
        } catch (JsonProcessingException e) {
            throw new EntityConversionException("Could not convert string to entity: " + jobLockCache, e);
        }

    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    @Override
    public boolean equals(Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
