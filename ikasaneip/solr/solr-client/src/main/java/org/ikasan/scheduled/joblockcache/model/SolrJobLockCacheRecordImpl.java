package org.ikasan.scheduled.joblockcache.model;

import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.scheduled.cache.SolrJobLockCacheMachine;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.job.model.SolrJobLockHolderImpl;
import org.ikasan.scheduled.job.model.SolrSchedulerJobImpl;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.solr.SolrDaoBase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class SolrJobLockCacheRecordImpl implements JobLockCacheRecord {

    private static ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        final var simpleModule = new SimpleModule()
            .addAbstractTypeMapping(SchedulerJob.class, SolrSchedulerJobImpl.class)
            .addAbstractTypeMapping(JobLockHolder.class, SolrJobLockHolderImpl.class);
        objectMapper.registerModule(simpleModule);
    }

    @Field(SolrDaoBase.ID)
    private String id;

    @Field(SolrDaoBase.PAYLOAD_CONTENT)
    private String jobLockCache;

    @Field(SolrDaoBase.CREATED_DATE_TIME)
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
            throw new SolrEntityConversionException("Could not convert entity to string: " + jobLockCache, e);
        }
    }

    @Override
    public JobLockCache getJobLockCache() {
        try {
            return objectMapper.readValue(jobLockCache, SolrJobLockCacheMachine.class);
        } catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert string to entity: " + jobLockCache, e);
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
