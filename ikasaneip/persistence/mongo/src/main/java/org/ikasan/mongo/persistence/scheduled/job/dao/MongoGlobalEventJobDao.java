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
import java.util.Optional;

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
        if (!(event instanceof MongoGlobalEventJobRecordImpl)) {
            throw new IllegalArgumentException("Event must be an instance of MongoGlobalEventJobRecordImpl");
        }

        MongoGlobalEventJobRecordImpl mongoRecord = (MongoGlobalEventJobRecordImpl) event;

        // Generate ID if not set
        if (mongoRecord.getId() == null || mongoRecord.getId().isEmpty()) {
            String id = JobConstants.GLOBAL_EVENT_JOB + "_"
                + mongoRecord.getAgentName() + "_"
                + mongoRecord.getJobName() + "_"
                + mongoRecord.getGlobalEventJob().getContextName();
            mongoRecord.setId(id);
        }

        // Set modified timestamp
        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());

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

        long totalCount = repository.count();

        Pageable pageable = PageRequest.of(offset / limit, limit);
        Query query = new Query();
        query.with(pageable);

        List<MongoGlobalEventJobRecordImpl> results = mongoTemplate.find(query, MongoGlobalEventJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SearchResults<GlobalEventJobRecord> findByContext(String contextId, int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = Criteria.where("contextName").is(contextId);
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
        Optional<MongoGlobalEventJobRecordImpl> result = repository.findById(id);
        return result.orElse(null);
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
