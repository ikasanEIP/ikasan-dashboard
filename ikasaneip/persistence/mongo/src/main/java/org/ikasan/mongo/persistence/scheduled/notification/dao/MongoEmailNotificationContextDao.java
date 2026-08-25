package org.ikasan.mongo.persistence.scheduled.notification.dao;

import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.notification.model.MongoEmailNotificationContextRecordImpl;
import org.ikasan.mongo.persistence.scheduled.notification.repository.MongoEmailNotificationContextRepository;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationContextDao;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContextRecord;
import org.ikasan.spec.search.SearchResults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.stream.Collectors;

import static org.ikasan.spec.entity.EntityFields.COMPONENT_NAME;
import static org.ikasan.spec.entity.EntityFields.TYPE;

public class MongoEmailNotificationContextDao implements EmailNotificationContextDao, BatchInsert<EmailNotificationContextRecord> {

    private final MongoEmailNotificationContextRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoEmailNotificationContextDao(MongoEmailNotificationContextRepository repository,
                                            MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public SearchResults<EmailNotificationContextRecord> findAll(int limit, int offset) {
        Query query = new Query();
        query.addCriteria(Criteria.where(TYPE).is(EMAIL_NOTIFICATION_CONTEXT));

        long totalCount = mongoTemplate.count(query, MongoEmailNotificationContextRecordImpl.class);

        query.with(PageRequest.of(offset / limit, limit));

        List<EmailNotificationContextRecord> results = mongoTemplate.find(query, MongoEmailNotificationContextRecordImpl.class)
            .stream()
            .map(record -> (EmailNotificationContextRecord) record)
            .collect(Collectors.toList());

        return new SearchResultsImpl<>(results, totalCount, 0);
    }

    @Override
    public SearchResults<EmailNotificationContextRecord> findByContextName(String contextName, int limit, int offset) {
        Criteria criteria = new Criteria().andOperator(
            Criteria.where(COMPONENT_NAME).is(contextName),
            Criteria.where(TYPE).is(EMAIL_NOTIFICATION_CONTEXT)
        );
        Query query = new Query(criteria);

        long totalCount = mongoTemplate.count(query, MongoEmailNotificationContextRecordImpl.class);

        query.with(PageRequest.of(offset / limit, limit));

        List<EmailNotificationContextRecord> results = mongoTemplate.find(query, MongoEmailNotificationContextRecordImpl.class)
            .stream()
            .map(record -> (EmailNotificationContextRecord) record)
            .collect(Collectors.toList());

        return new SearchResultsImpl<>(results, totalCount, 0);
    }

    @Override
    public void deleteByContextName(String contextName) {
        Criteria criteria = new Criteria().andOperator(
            Criteria.where(COMPONENT_NAME).is(contextName),
            Criteria.where(TYPE).is(EMAIL_NOTIFICATION_CONTEXT)
        );
        Query query = new Query(criteria);
        mongoTemplate.remove(query, MongoEmailNotificationContextRecordImpl.class);
    }

    @Override
    public void save(EmailNotificationContextRecord record) {
        MongoEmailNotificationContextRecordImpl mongoRecord = new MongoEmailNotificationContextRecordImpl();
        mongoRecord.setId(record.getEmailNotificationContext().getContextName());
        mongoRecord.setType(EMAIL_NOTIFICATION_CONTEXT);
        mongoRecord.setContextName(record.getContextName());
        mongoRecord.setEmailNotificationContext(record.getEmailNotificationContext());
        mongoRecord.setTimestamp(record.getTimestamp());
        mongoRecord.setModifiedTimestamp(record.getModifiedTimestamp());
        mongoRecord.setModifiedBy(record.getModifiedBy());

        // Set contextName from EmailNotificationContext if not already set
        if (mongoRecord.getContextName() == null || mongoRecord.getContextName().isEmpty()) {
            mongoRecord.setContextName(record.getEmailNotificationContext().getContextName());
        }

        // Set timestamp if not already set
        if (mongoRecord.getTimestamp() == 0) {
            mongoRecord.setTimestamp(System.currentTimeMillis());
        }

        // Always update modifiedTimestamp
        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());
        mongoRecord.setExpiry(-1);

        repository.save(mongoRecord);
    }

    @Override
    public void insert(List<EmailNotificationContextRecord> entities) {
        entities.forEach(this::save);
    }
}
