package org.ikasan.mongo.persistence.scheduled.job.dao;

import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoQuartzScheduleDrivenJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoQuartzScheduleDrivenJobRepository;
import org.ikasan.spec.scheduled.job.dao.QuartzScheduleDrivenJobDao;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.QuartzScheduleDrivenJobRecord;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;

public class MongoQuartzScheduleDrivenJobDao implements QuartzScheduleDrivenJobDao<QuartzScheduleDrivenJobRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(MongoQuartzScheduleDrivenJobDao.class);

    private final MongoQuartzScheduleDrivenJobRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoQuartzScheduleDrivenJobDao(MongoQuartzScheduleDrivenJobRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(QuartzScheduleDrivenJobRecord event) {
        if (!(event instanceof MongoQuartzScheduleDrivenJobRecordImpl)) {
            throw new IllegalArgumentException("Event must be an instance of MongoQuartzScheduleDrivenJobRecordImpl");
        }

        MongoQuartzScheduleDrivenJobRecordImpl mongoRecord = (MongoQuartzScheduleDrivenJobRecordImpl) event;

        // Generate ID if not set
        if (mongoRecord.getId() == null || mongoRecord.getId().isEmpty()) {
            String id = JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB + "_"
                + mongoRecord.getAgentName() + "_"
                + mongoRecord.getJobName() + "_"
                + mongoRecord.getContextName();
            mongoRecord.setId(id);
        }

        // Set modified timestamp
        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());

        repository.save(mongoRecord);
    }

    @Override
    public void save(List<QuartzScheduleDrivenJobRecord> events) {
        events.forEach(this::save);
    }

    @Override
    public SearchResults<QuartzScheduleDrivenJobRecord> findAll(int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Query query = new Query();
        long totalCount = mongoTemplate.count(query, MongoQuartzScheduleDrivenJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoQuartzScheduleDrivenJobRecordImpl> results = mongoTemplate.find(query, MongoQuartzScheduleDrivenJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SearchResults<QuartzScheduleDrivenJobRecord> findByContext(String contextId, int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = Criteria.where("contextName").is(contextId);
        Query query = new Query(criteria);

        long totalCount = mongoTemplate.count(query, MongoQuartzScheduleDrivenJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoQuartzScheduleDrivenJobRecordImpl> results = mongoTemplate.find(query, MongoQuartzScheduleDrivenJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public QuartzScheduleDrivenJobRecord findById(String id) {
        return repository.findById(id).orElse(null);
    }
}
