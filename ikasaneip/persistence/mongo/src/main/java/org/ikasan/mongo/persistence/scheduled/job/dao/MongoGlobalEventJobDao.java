package org.ikasan.mongo.persistence.scheduled.job.dao;

import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoGlobalEventJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoGlobalEventJobRepository;
import org.ikasan.spec.scheduled.job.dao.GlobalEventJobDao;
import org.ikasan.spec.scheduled.job.model.GlobalEventJob;
import org.ikasan.spec.scheduled.job.model.GlobalEventJobRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.search.SearchResults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.ikasan.spec.entity.EntityFields.ID;
import static org.ikasan.spec.entity.EntityFields.TYPE;
import static org.ikasan.spec.scheduled.job.model.JobConstants.GLOBAL_EVENT_JOB;

/**
 * MongoDB implementation of GlobalEventJobDao.
 */
public class MongoGlobalEventJobDao implements GlobalEventJobDao<GlobalEventJobRecord> {

    private final MongoGlobalEventJobRepository repository;
    private final MongoTemplate mongoTemplate;

    /**
     * Constructor
     *
     * @param repository the MongoDB repository
     * @param mongoTemplate the MongoDB template for custom queries
     */
    public MongoGlobalEventJobDao(MongoGlobalEventJobRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(GlobalEventJobRecord event) {
        MongoGlobalEventJobRecordImpl mongoRecord = new MongoGlobalEventJobRecordImpl();

        mongoRecord.setType(JobConstants.GLOBAL_EVENT_JOB);

        GlobalEventJob job = event.getGlobalEventJob();

        if(event.getId() != null && !event.getId().isEmpty()) {
            mongoRecord.setId(event.getId());
        }
        else {
            mongoRecord.setId(JobConstants.GLOBAL_EVENT_JOB + "_" + event.getAgentName() + "_"
                + event.getJobName() + "_" + job.getContextName());
        }
        mongoRecord.setGlobalEventJob(job);
        mongoRecord.setContextName(job.getContextName());
        mongoRecord.setAgentName(event.getAgentName());
        mongoRecord.setJobName(event.getJobName());
        mongoRecord.setDisplayName(event.getGlobalEventJob().getDisplayName());
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
    public void save(List<GlobalEventJobRecord> events) {
        for (GlobalEventJobRecord event : events) {
            save(event);
        }
    }

    @Override
    public SearchResults<GlobalEventJobRecord> findAll(int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Query query = new Query();
        query.addCriteria(Criteria.where(TYPE).is(GLOBAL_EVENT_JOB));

        long totalCount = mongoTemplate.count(query, MongoGlobalEventJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoGlobalEventJobRecordImpl> results = mongoTemplate.find(query, MongoGlobalEventJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SearchResults<GlobalEventJobRecord> findByContext(String contextId, int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = new Criteria().andOperator(
            Criteria.where("contextName").is(contextId),
            Criteria.where(TYPE).is(GLOBAL_EVENT_JOB)
        );
        Query query = new Query(criteria);

        long totalCount = mongoTemplate.count(query, MongoGlobalEventJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoGlobalEventJobRecordImpl> results = mongoTemplate.find(query, MongoGlobalEventJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public GlobalEventJobRecord findById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where(ID).is(id));
        query.addCriteria(Criteria.where(TYPE).is(GLOBAL_EVENT_JOB));

        return mongoTemplate.findOne(query, MongoGlobalEventJobRecordImpl.class);
    }

    @Override
    public void skip(GlobalEventJobRecord jobRecord, List<String> childContextNames, String actor) {
        GlobalEventJob globalEventJob = jobRecord.getGlobalEventJob();
        globalEventJob.setSkippedContexts(new HashMap<>());
        childContextNames.forEach(name ->
            globalEventJob.getSkippedContexts().put(name, true));

        jobRecord.setGlobalEventJob(globalEventJob);
        jobRecord.setModifiedBy(actor);

        this.save(jobRecord);
    }

    @Override
    public void enable(GlobalEventJobRecord jobRecord, String actor) {
        GlobalEventJob globalEventJob = jobRecord.getGlobalEventJob();
        jobRecord.setGlobalEventJob(globalEventJob);
        jobRecord.setModifiedBy(actor);

        this.save(jobRecord);
    }
}
