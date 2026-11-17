package org.ikasan.mongo.persistence.scheduled.joblock.dao;

import org.ikasan.mongo.persistence.scheduled.joblock.model.MongoJobLockCacheRecordImpl;
import org.ikasan.mongo.persistence.scheduled.joblock.repository.MongoJobLockCacheRepository;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheDao;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import static org.ikasan.spec.entity.EntityFields.ID;
import static org.ikasan.spec.entity.EntityFields.TYPE;

public class MongoJobLockCacheDaoImpl implements JobLockCacheDao {

    public static final String JOB_LOCK_CACHE_ID = "jobLockCacheIdentifier";
    private static final Logger logger = LoggerFactory.getLogger(MongoJobLockCacheDaoImpl.class);

    private final MongoJobLockCacheRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoJobLockCacheDaoImpl(MongoJobLockCacheRepository repository,
                                    MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public JobLockCacheRecord get(String environment) {
        if(environment == null) {
            environment = JobLockCacheRecord.DEFAULT_ENVIRONMENT;
        }

        String id = JOB_LOCK_CACHE_ID + "__" + environment;
        logger.debug("Getting JobLockCacheRecord with id: {}", id);

        Query query = new Query();
        query.addCriteria(Criteria.where(ID).is(id));
        query.addCriteria(Criteria.where(TYPE).is(JOB_LOCK_CACHE_TYPE));

        return mongoTemplate.findOne(query, MongoJobLockCacheRecordImpl.class);
    }

    /**
     * Save the record. The record may be any {@link JobLockCacheRecord} implementation (for example the
     * {@code JobLockCacheRecordImpl} created by the job lock cache), so its fields are copied onto a new
     * {@link MongoJobLockCacheRecordImpl} rather than casting it. The record passed in is not modified.
     */
    @Override
    public void save(JobLockCacheRecord record) {
        if(record == null) {
            throw new IllegalArgumentException("JobLockCacheRecord can not be null!");
        }

        String environment = record.getEnvironment() == null
            ? JobLockCacheRecord.DEFAULT_ENVIRONMENT : record.getEnvironment();
        long now = System.currentTimeMillis();

        MongoJobLockCacheRecordImpl mongoRecord = new MongoJobLockCacheRecordImpl();
        mongoRecord.setId(JOB_LOCK_CACHE_ID + "__" + environment);
        mongoRecord.setEnvironment(environment);
        mongoRecord.setType(JOB_LOCK_CACHE_TYPE);
        mongoRecord.setJobLockCache(record.getJobLockCache());
        mongoRecord.setTimestamp(record.getTimestamp() == 0 ? now : record.getTimestamp());
        mongoRecord.setModifiedTimestamp(now);
        mongoRecord.setExpiry(-1);

        logger.debug("Saving JobLockCacheRecord with id: {}", mongoRecord.getId());
        repository.save(mongoRecord);
    }
}
