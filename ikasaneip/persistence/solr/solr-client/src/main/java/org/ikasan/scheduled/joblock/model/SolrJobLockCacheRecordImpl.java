package org.ikasan.scheduled.joblock.model;

import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.util.ScheduledConcurrentObjectMapperFactory;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

public class SolrJobLockCacheRecordImpl implements JobLockCacheRecord {

    private static JsonMapper objectMapper = ScheduledConcurrentObjectMapperFactory.newInstance();

    @Field(EntityFields.ID)
    private String id;

    private String environment;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String jobLockCache;

    @Field(EntityFields.CREATED_DATE_TIME)
    private long timestamp;

    @Field(EntityFields.UPDATED_DATE_TIME)
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
        } catch (JacksonException e) {
            throw new SolrEntityConversionException("Could not convert entity to string: " + jobLockCache, e);
        }
    }

    @Override
    public JobLockCacheData getJobLockCache() {
        try {
            return objectMapper.readValue(jobLockCache, SolrJobLockCacheDataImpl.class);
        } catch (JacksonException e) {
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
