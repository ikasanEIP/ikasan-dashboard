package org.ikasan.mongo.persistence.scheduled.job.dao;

import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoContextStartJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoContextStartJobRepository;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.job.dao.ContextStartJobDao;
import org.ikasan.spec.scheduled.job.model.ContextStartJob;
import org.ikasan.spec.scheduled.job.model.ContextStartJobRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.search.SearchResults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB implementation of ContextStartJobDao.
 */
public class MongoContextStartJobDao implements ContextStartJobDao<ContextStartJobRecord> {

    private final MongoContextStartJobRepository repository;
    private final MongoTemplate mongoTemplate;

    /**
     * Constructor
     *
     * @param repository the MongoDB repository
     * @param mongoTemplate the MongoDB template for custom queries
     */
    public MongoContextStartJobDao(MongoContextStartJobRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(ContextStartJobRecord event) {
        MongoContextStartJobRecordImpl mongoRecord = new MongoContextStartJobRecordImpl();
        mongoRecord.setType(JobConstants.CONTEXT_START_JOB);

        ContextStartJob job = event.getContextStartJob();
        if(event.getId() != null) {
            mongoRecord.setId(event.getId());
        }
        else {
            mongoRecord.setId(job.getAgentName() + "_"
                + event.getJobName() + "_" + job.getContextName());
        }
        mongoRecord.setContextStartJob(job);
        mongoRecord.setContextName(job.getContextName());
        mongoRecord.setAgentName(job.getAgentName());
        mongoRecord.setDisplayName(job.getDisplayName());
        mongoRecord.setJobName(event.getJobName());
        mongoRecord.setTimestamp(event.getTimestamp());
        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());
        // only update modified by field if populated.
        if(event.getModifiedBy() != null &&
            !event.getModifiedBy().isEmpty()) {
            mongoRecord.setModifiedBy(event.getModifiedBy());
        }
        mongoRecord.setExpiry(-1);

        repository.save(mongoRecord);
    }

    @Override
    public void save(List<ContextStartJobRecord> events) {
        for (ContextStartJobRecord event : events) {
            save(event);
        }
    }

    @Override
    public SearchResults<ContextStartJobRecord> findAll(int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(JobConstants.CONTEXT_START_JOB));

        long totalCount = mongoTemplate.count(query, MongoContextStartJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoContextStartJobRecordImpl> results = mongoTemplate.find(query, MongoContextStartJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SearchResults<ContextStartJobRecord> findByContext(String contextId, int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = new Criteria().andOperator(
            Criteria.where(EntityFields.COMPONENT_NAME).is(contextId),
            Criteria.where(EntityFields.TYPE).is(JobConstants.CONTEXT_START_JOB)
        );
        Query query = new Query(criteria);

        long totalCount = mongoTemplate.count(query, MongoContextStartJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoContextStartJobRecordImpl> results = mongoTemplate.find(query, MongoContextStartJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public ContextStartJobRecord findById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ID).is(id));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(JobConstants.CONTEXT_START_JOB));

        return mongoTemplate.findOne(query, MongoContextStartJobRecordImpl.class);
    }
}
