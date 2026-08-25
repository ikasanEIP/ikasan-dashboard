package org.ikasan.mongo.persistence.scheduled.job.dao;

import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoFileEventDrivenJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoFileEventDrivenJobRepository;
import org.ikasan.spec.entity.EntityFields;
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

import static org.ikasan.spec.entity.EntityFields.ID;
import static org.ikasan.spec.entity.EntityFields.TYPE;
import static org.ikasan.spec.scheduled.job.model.JobConstants.FILE_EVENT_DRIVEN_JOB;

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
        MongoFileEventDrivenJobRecordImpl mongoRecord = new MongoFileEventDrivenJobRecordImpl();
        if(event.getId() != null && !event.getId().isEmpty()) {
            mongoRecord.setId(event.getId());
        }
        else {
            mongoRecord.setId(JobConstants.FILE_EVENT_DRIVEN_JOB + "_" + event.getAgentName() + "_" + event.getJobName()
                + "_" + event.getFileEventDrivenJob().getContextName());
        }
        mongoRecord.setType(JobConstants.FILE_EVENT_DRIVEN_JOB);
        mongoRecord.setFileEventDrivenJob(event.getFileEventDrivenJob());
        mongoRecord.setAgentName(event.getAgentName());
        mongoRecord.setJobName(event.getJobName());
        mongoRecord.setDisplayName(event.getDisplayName());
        mongoRecord.setContextName(event.getContextName());
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
    public void save(List<FileEventDrivenJobRecord> events) {
        for (FileEventDrivenJobRecord event : events) {
            save(event);
        }
    }

    @Override
    public SearchResults<FileEventDrivenJobRecord> findAll(int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Query query = new Query();
        query.addCriteria(Criteria.where(TYPE).is(FILE_EVENT_DRIVEN_JOB));

        long totalCount = mongoTemplate.count(query, MongoFileEventDrivenJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoFileEventDrivenJobRecordImpl> results = mongoTemplate.find(query, MongoFileEventDrivenJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SearchResults<FileEventDrivenJobRecord> findByContext(String contextId, int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = new Criteria().andOperator(
            Criteria.where(EntityFields.COMPONENT_NAME).is(contextId),
            Criteria.where(TYPE).is(FILE_EVENT_DRIVEN_JOB)
        );
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
        Query query = new Query();
        query.addCriteria(Criteria.where(ID).is(id));
        query.addCriteria(Criteria.where(TYPE).is(FILE_EVENT_DRIVEN_JOB));

        return mongoTemplate.findOne(query, MongoFileEventDrivenJobRecordImpl.class);
    }
}
