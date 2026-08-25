package org.ikasan.mongo.persistence.scheduled.job.dao;

import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.job.model.MongoBridgingJobRecordImpl;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoBridgingJobRepository;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.job.dao.BridgingJobDao;
import org.ikasan.spec.scheduled.job.model.BridgingJob;
import org.ikasan.spec.scheduled.job.model.BridgingJobRecord;
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
 * MongoDB implementation of BridgingJobDao.
 */
public class MongoBridgingJobDao implements BridgingJobDao<BridgingJobRecord> {

    private final MongoBridgingJobRepository repository;
    private final MongoTemplate mongoTemplate;

    /**
     * Constructor
     *
     * @param repository the MongoDB repository
     * @param mongoTemplate the MongoDB template for custom queries
     */
    public MongoBridgingJobDao(MongoBridgingJobRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(BridgingJobRecord event) {
        MongoBridgingJobRecordImpl mongoRecord = new MongoBridgingJobRecordImpl();
        mongoRecord.setType(JobConstants.BRIDGING_JOB);
        BridgingJob job = event.getBridgingJob();
        if(event.getId() != null) {
            mongoRecord.setId(event.getId());
        }
        else {
            mongoRecord.setId(job.getAgentName() + "_"
                + event.getJobName() + "_" + job.getContextName());
        }
        mongoRecord.setBridgingJob(event.getBridgingJob());
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

        // Set modified timestamp
        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());

        repository.save(mongoRecord);
    }

    @Override
    public void save(List<BridgingJobRecord> events) {
        for (BridgingJobRecord event : events) {
            save(event);
        }
    }

    @Override
    public SearchResults<BridgingJobRecord> findAll(int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.TYPE)
            .is(JobConstants.BRIDGING_JOB));

        long totalCount = mongoTemplate.count(query, MongoBridgingJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoBridgingJobRecordImpl> results = mongoTemplate.find(query, MongoBridgingJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public SearchResults<BridgingJobRecord> findByContext(String contextId, int limit, int offset) {
        long startTime = System.currentTimeMillis();

        Criteria criteria = new Criteria().andOperator(
            Criteria.where(EntityFields.COMPONENT_NAME).is(contextId),
            Criteria.where(EntityFields.TYPE)
                .is(JobConstants.BRIDGING_JOB)
        );
        Query query = new Query(criteria);

        long totalCount = mongoTemplate.count(query, MongoBridgingJobRecordImpl.class);

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        List<MongoBridgingJobRecordImpl> results = mongoTemplate.find(query, MongoBridgingJobRecordImpl.class);

        long queryTime = System.currentTimeMillis() - startTime;
        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, queryTime);
    }

    @Override
    public BridgingJobRecord findById(String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ID).is(id));
        query.addCriteria(Criteria.where(EntityFields.TYPE)
            .is(JobConstants.BRIDGING_JOB));

        return mongoTemplate.findOne(query, MongoBridgingJobRecordImpl.class);
    }
}
