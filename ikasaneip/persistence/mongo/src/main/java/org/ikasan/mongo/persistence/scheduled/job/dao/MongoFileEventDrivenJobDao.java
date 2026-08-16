package org.ikasan.mongo.persistence.scheduled.job.dao;

import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoFileEventDrivenJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoFileEventDrivenJobRepository;
import org.ikasan.spec.scheduled.job.dao.FileEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.model.FileEventDrivenJobRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.search.SearchResults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MongoDB implementation of FileEventDrivenJobDao.
 */
public class MongoFileEventDrivenJobDao implements FileEventDrivenJobDao<FileEventDrivenJobRecord> {

    private final MongoFileEventDrivenJobRepository repository;
    private final MongoTemplate mongoTemplate;

    /**
     * Constructor
     *
     * @param repository the MongoDB repository
     * @param mongoTemplate the MongoDB template for custom queries
     */
    public MongoFileEventDrivenJobDao(MongoFileEventDrivenJobRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(FileEventDrivenJobRecord event) {
        if (!(event instanceof MongoFileEventDrivenJobRecordImpl)) {
            throw new IllegalArgumentException("Event must be an instance of MongoFileEventDrivenJobRecordImpl");
        }

        MongoFileEventDrivenJobRecordImpl mongoRecord = (MongoFileEventDrivenJobRecordImpl) event;

        // Generate ID if not set
        if (mongoRecord.getId() == null || mongoRecord.getId().isEmpty()) {
            String id = JobConstants.FILE_EVENT_DRIVEN_JOB + "_"
                + mongoRecord.getAgentName() + "_"
                + mongoRecord.getJobName() + "_"
                + mongoRecord.getFileEventDrivenJob().getContextName();
            mongoRecord.setId(id);
        }

        // Set modified timestamp
        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());

        repository.save(mongoRecord);
    }

    @Override
    public void save(List<FileEventDrivenJobRecord> events) {
        for (FileEventDrivenJobRecord event : events) {
            save(event);
        }
    }

    @Override
    public SearchResults<FileEventDrivenJobRecord> findAll(int limit, int offset) {
        long startTime = System.currentTimeMillis();

        long totalCount = repository.count();

        Pageable pageable = PageRequest.of(offset / limit, limit);
        Query query = new Query();
        query.with(pageable);

        List<MongoFileEventDrivenJobRecordImpl> results = mongoTemplate.find(query, MongoFileEventDrivenJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SearchResults<FileEventDrivenJobRecord> findByContext(String contextId, int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = Criteria.where("contextName").is(contextId);
        Query query = new Query(criteria);

        long totalCount = mongoTemplate.count(query, MongoFileEventDrivenJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoFileEventDrivenJobRecordImpl> results = mongoTemplate.find(query, MongoFileEventDrivenJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public FileEventDrivenJobRecord findById(String id) {
        Optional<MongoFileEventDrivenJobRecordImpl> result = repository.findById(id);
        return result.orElse(null);
    }
}
