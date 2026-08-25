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

public class MongoJobLockCacheDao implements JobLockCacheDao {

    public static final String JOB_LOCK_CACHE_ID = "jobLockCacheIdentifier";
    private static final Logger logger = LoggerFactory.getLogger(MongoJobLockCacheDao.class);

    private final MongoJobLockCacheRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoJobLockCacheDao(MongoJobLockCacheRepository repository,
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

    @Override
    public void save(JobLockCacheRecord record) {
        MongoJobLockCacheRecordImpl mongoRecord = (MongoJobLockCacheRecordImpl) record;

        mongoRecord.setId(JOB_LOCK_CACHE_ID + "__" + record.getEnvironment());
        mongoRecord.setType(JOB_LOCK_CACHE_TYPE);
        mongoRecord.setJobLockCache(record.getJobLockCache());
        if(record.getTimestamp() == 0) {
            mongoRecord.setTimestamp(System.currentTimeMillis());
        }
        else {
            mongoRecord.setTimestamp(record.getTimestamp());
        }

        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());
        mongoRecord.setExpiry(-1);

        logger.debug("Saving JobLockCacheRecord with id: {}", mongoRecord.getId());
        repository.save(mongoRecord);
    }
}
