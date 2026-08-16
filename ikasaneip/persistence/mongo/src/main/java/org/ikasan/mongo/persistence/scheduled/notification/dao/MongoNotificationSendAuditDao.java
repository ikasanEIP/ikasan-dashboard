package org.ikasan.mongo.persistence.scheduled.notification.dao;

import org.ikasan.mongo.persistence.scheduled.notification.model.MongoNotificationSendAuditRecordImpl;
import org.ikasan.mongo.persistence.scheduled.notification.repository.MongoNotificationSendAuditRepository;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.scheduled.notification.dao.NotificationSendAuditDao;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAuditRecord;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.Optional;

public class MongoNotificationSendAuditDao implements NotificationSendAuditDao<NotificationSendAuditRecord>, BatchInsert<NotificationSendAuditRecord> {

    private final MongoNotificationSendAuditRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoNotificationSendAuditDao(MongoNotificationSendAuditRepository repository,
                                         MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public NotificationSendAuditRecord find(String contextInstanceId, String contextName, String jobName,
                                            String monitorType, String notifierType) {
        String id = generateId(contextInstanceId, contextName, jobName, monitorType, notifierType);
        Optional<MongoNotificationSendAuditRecordImpl> result = repository.findById(id);
        return result.orElse(null);
    }

    @Override
    public void save(NotificationSendAuditRecord record) {
        MongoNotificationSendAuditRecordImpl mongoRecord;

        if (record instanceof MongoNotificationSendAuditRecordImpl) {
            mongoRecord = (MongoNotificationSendAuditRecordImpl) record;
        } else {
            mongoRecord = new MongoNotificationSendAuditRecordImpl();
            mongoRecord.setId(record.getId());
            mongoRecord.setNotificationSendAudit(record.getNotificationSendAudit());
            mongoRecord.setTimestamp(record.getTimestamp());
            mongoRecord.setModifiedTimestamp(record.getModifiedTimestamp());
            mongoRecord.setModifiedBy(record.getModifiedBy());
        }

        // Generate ID if not already set
        if (mongoRecord.getId() == null || mongoRecord.getId().isEmpty()) {
            String id = generateId(mongoRecord);
            mongoRecord.setId(id);
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
    public void insert(List<NotificationSendAuditRecord> entities) {
        entities.forEach(this::save);
    }

    private String generateId(NotificationSendAuditRecord notificationSendAuditRecord) {
        return generateId(
            notificationSendAuditRecord.getNotificationSendAudit().getContextInstanceId(),
            notificationSendAuditRecord.getNotificationSendAudit().getContextName(),
            notificationSendAuditRecord.getNotificationSendAudit().getJobName(),
            notificationSendAuditRecord.getNotificationSendAudit().getMonitorType(),
            notificationSendAuditRecord.getNotificationSendAudit().getNotifierType()
        );
    }

    private String generateId(String contextInstanceId, String contextName, String jobName,
                             String monitorType, String notifierType) {
        StringBuilder sb = new StringBuilder(contextInstanceId);
        sb.append("_");
        sb.append(contextName);
        sb.append("_");
        sb.append(jobName);
        sb.append("_");
        sb.append(monitorType);
        sb.append("_");
        sb.append(notifierType);
        return sb.toString();
    }
}
