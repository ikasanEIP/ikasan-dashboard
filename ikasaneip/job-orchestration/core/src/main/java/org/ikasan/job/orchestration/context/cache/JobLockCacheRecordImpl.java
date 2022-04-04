package org.ikasan.job.orchestration.context.cache;

import org.ikasan.job.orchestration.exception.EntityConversionException;
import org.ikasan.job.orchestration.model.context.JobLockHolderImpl;
import org.ikasan.job.orchestration.model.job.SchedulerJobImpl;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class JobLockCacheRecordImpl implements JobLockCacheRecord {

    private static ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        final var simpleModule = new SimpleModule()
            .addAbstractTypeMapping(SchedulerJob.class, SchedulerJobImpl.class)
            .addAbstractTypeMapping(JobLockHolder.class, JobLockHolderImpl.class);
        objectMapper.registerModule(simpleModule);
    }

    private String id;
    private String jobLockCache;
    private long timestamp;


    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setJobLockCache(JobLockCache jobLockCache) {
        try {
            this.jobLockCache = objectMapper.writeValueAsString(jobLockCache);
        } catch (JsonProcessingException e) {
            throw new EntityConversionException("Could not convert entity to string: " + jobLockCache, e);
        }
    }

    @Override
    public JobLockCache getJobLockCache() {
        try {
            return objectMapper.readValue(jobLockCache, JobLockCacheMachine.class);
        } catch (JsonProcessingException e) {
            throw new EntityConversionException("Could not convert string to entity: " + jobLockCache, e);
        }

    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
