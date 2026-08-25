package org.ikasan.mongo.persistence.scheduled.notification.dao;

import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.notification.model.MongoEmailNotificationDetailsRecordImpl;
import org.ikasan.mongo.persistence.scheduled.notification.repository.MongoEmailNotificationDetailsRepository;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationDetailsDao;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import org.ikasan.spec.search.SearchResults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.stream.Collectors;

import static org.ikasan.spec.entity.EntityFields.*;

public class MongoEmailNotificationDetailsDao implements EmailNotificationDetailsDao, BatchInsert<EmailNotificationDetailsRecord> {

    private final MongoEmailNotificationDetailsRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoEmailNotificationDetailsDao(MongoEmailNotificationDetailsRepository repository,
                                            MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public SearchResults<EmailNotificationDetailsRecord> findAll(int limit, int offset) {
        Query query = new Query();
        query.addCriteria(Criteria.where(TYPE).is(EMAIL_NOTIFICATION_DETAILS));

        long totalCount = mongoTemplate.count(query, MongoEmailNotificationDetailsRecordImpl.class);

        query.with(PageRequest.of(offset / limit, limit));

        List<EmailNotificationDetailsRecord> results = mongoTemplate.find(query, MongoEmailNotificationDetailsRecordImpl.class)
            .stream()
            .map(record -> (EmailNotificationDetailsRecord) record)
            .collect(Collectors.toList());

        return new SearchResultsImpl<>(results, totalCount, 0);
    }

    @Override
    public SearchResults<EmailNotificationDetailsRecord> findByContextName(String contextName, int limit, int offset) {
        Criteria criteria = new Criteria().andOperator(
            Criteria.where(COMPONENT_NAME).is(contextName),
            Criteria.where(TYPE).is(EMAIL_NOTIFICATION_DETAILS)
        );
        Query query = new Query(criteria);

        long totalCount = mongoTemplate.count(query, MongoEmailNotificationDetailsRecordImpl.class);

        query.with(PageRequest.of(offset / limit, limit));

        List<EmailNotificationDetailsRecord> results = mongoTemplate.find(query, MongoEmailNotificationDetailsRecordImpl.class)
            .stream()
            .map(record -> (EmailNotificationDetailsRecord) record)
            .collect(Collectors.toList());

        return new SearchResultsImpl<>(results, totalCount, 0);
    }

    @Override
    public EmailNotificationDetailsRecord findByJobNameAndMonitorType(String jobName, String childContextName, String monitorType) {
        String id = generateId(jobName, childContextName, monitorType);

        Query query = new Query();
        query.addCriteria(Criteria.where(ID).is(id));
        query.addCriteria(Criteria.where(TYPE).is(EMAIL_NOTIFICATION_DETAILS));

        return mongoTemplate.findOne(query, MongoEmailNotificationDetailsRecordImpl.class);
    }

    @Override
    public void deleteByContextName(String contextName) {
        Criteria criteria = new Criteria().andOperator(
            Criteria.where(COMPONENT_NAME).is(contextName),
            Criteria.where(TYPE).is(EMAIL_NOTIFICATION_DETAILS)
        );
        Query query = new Query(criteria);
        mongoTemplate.remove(query, MongoEmailNotificationDetailsRecordImpl.class);
    }

    @Override
    public void deleteByJobNameAndMonitorType(String jobName, String childContextName, String monitorType) {
        String id = generateId(jobName, childContextName, monitorType);

        Query query = new Query();
        query.addCriteria(Criteria.where(ID).is(id));
        query.addCriteria(Criteria.where(TYPE).is(EMAIL_NOTIFICATION_DETAILS));

        mongoTemplate.remove(query, MongoEmailNotificationDetailsRecordImpl.class);
    }

    @Override
    public void save(EmailNotificationDetailsRecord record) {
        MongoEmailNotificationDetailsRecordImpl mongoRecord = new MongoEmailNotificationDetailsRecordImpl();
        mongoRecord.setId(generateId(record.getEmailNotificationDetails().getJobName()
            , record.getEmailNotificationDetails().getChildContextName()
            , record.getEmailNotificationDetails().getMonitorType()));
        mongoRecord.setType(EMAIL_NOTIFICATION_DETAILS);
        mongoRecord.setJobName(record.getEmailNotificationDetails().getJobName());
        mongoRecord.setContextName(record.getEmailNotificationDetails().getContextName());
        mongoRecord.setMonitorType(record.getEmailNotificationDetails().getMonitorType());
        mongoRecord.setEmailNotificationDetails(record.getEmailNotificationDetails());
        mongoRecord.setTimestamp(record.getTimestamp());
        mongoRecord.setModifiedTimestamp(record.getModifiedTimestamp());
        mongoRecord.setModifiedBy(record.getModifiedBy());

        // Set fields from EmailNotificationDetails if not already set
        if (mongoRecord.getJobName() == null || mongoRecord.getJobName().isEmpty()) {
            mongoRecord.setJobName(record.getEmailNotificationDetails().getJobName());
        }
        if (mongoRecord.getContextName() == null || mongoRecord.getContextName().isEmpty()) {
            mongoRecord.setContextName(record.getEmailNotificationDetails().getContextName());
        }
        if (mongoRecord.getMonitorType() == null || mongoRecord.getMonitorType().isEmpty()) {
            mongoRecord.setMonitorType(record.getEmailNotificationDetails().getMonitorType());
        }

        // Set timestamp if not already set
        if (mongoRecord.getTimestamp() == 0) {
            mongoRecord.setTimestamp(System.currentTimeMillis());
        }

        // Always update modifiedTimestamp
        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());

        repository.save(mongoRecord);
    }

    @Override
    public void save(List<EmailNotificationDetailsRecord> records) {
        records.forEach(this::save);
    }

    @Override
    public void insert(List<EmailNotificationDetailsRecord> entities) {
        entities.forEach(this::save);
    }

    /**
     * Generates ID so that the format is correct
     * @param jobName jobName
     * @param childContextName childContextName
     * @param monitorType of Type org.ikasan.job.orchestration.model.notification.MonitorType
     * @return format = jobName_childContextName_monitorType
     */
    private String generateId(String jobName, String childContextName, String monitorType) {
        return jobName + "_" + childContextName + "_" + monitorType;
    }
}
