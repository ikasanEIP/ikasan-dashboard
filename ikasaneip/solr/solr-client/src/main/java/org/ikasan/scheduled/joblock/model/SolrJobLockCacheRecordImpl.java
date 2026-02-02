package org.ikasan.scheduled.joblock.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.solr.SolrDaoBase;

public class SolrJobLockCacheRecordImpl implements JobLockCacheRecord {

    private static ObjectMapper objectMapper = ScheduledObjectMapperFactory.newInstance();

    @Field(SolrDaoBase.ID)
    private String id;

    private String environment;

    @Field(SolrDaoBase.PAYLOAD_CONTENT)
    private String jobLockCache;

    @Field(SolrDaoBase.CREATED_DATE_TIME)
    private long timestamp;

    @Field(SolrDaoBase.UPDATED_DATE_TIME)
    private long modifiedTimestamp;

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getEnvironment() {
        if(this.environment != null) {
            return environment;
        }
        else if(this.id != null && this.id.contains("__")) {
            return id.substring(id.indexOf("__")+2, id.length());
        }
        else {
            return JobLockCacheRecord.DEFAULT_ENVIRONMENT;
        }
    }

    @Override
    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    @Override
    public void setJobLockCache(JobLockCacheData jobLockCache) {
        try {
            this.jobLockCache = objectMapper.writeValueAsString(jobLockCache);
        } catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert entity to string: " + jobLockCache, e);
        }
    }

    @Override
    public JobLockCacheData getJobLockCache() {
        try {
            return objectMapper.readValue(jobLockCache, SolrJobLockCacheDataImpl.class);
        } catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert string to entity: " + jobLockCache, e);
        }
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public long getModifiedTimestamp() {
        return 0;
    }
}
