package org.ikasan.mongo.persistence.scheduled.notification.dao;

import org.ikasan.mongo.persistence.scheduled.notification.model.MongoNotificationSendAuditRecordImpl;
import org.ikasan.mongo.persistence.scheduled.notification.repository.MongoNotificationSendAuditRepository;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.scheduled.notification.dao.NotificationSendAuditDao;
import org.ikasan.spec.scheduled.notification.model.NotificationSendAuditRecord;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static org.ikasan.spec.entity.EntityFields.ID;
import static org.ikasan.spec.entity.EntityFields.TYPE;

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

        Query query = new Query();
        query.addCriteria(Criteria.where(ID).is(id));
        query.addCriteria(Criteria.where(TYPE).is(NOTIFICATION_SEND_AUDIT));

        return mongoTemplate.findOne(query, MongoNotificationSendAuditRecordImpl.class);
    }

    @Override
    public void save(NotificationSendAuditRecord record) {
        MongoNotificationSendAuditRecordImpl mongoRecord = new MongoNotificationSendAuditRecordImpl();
        mongoRecord.setId(generateId(record));
        mongoRecord.setType(NOTIFICATION_SEND_AUDIT);
        mongoRecord.setNotificationSendAudit(record.getNotificationSendAudit());
        if(record.getModifiedTimestamp() == 0) {
            mongoRecord.setTimestamp(System.currentTimeMillis());
        }
        else {
            mongoRecord.setTimestamp(record.getTimestamp());
        }
        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());
        mongoRecord.setModifiedBy(record.getModifiedBy());
        mongoRecord.setExpiry(-1);

        repository.save(mongoRecord);
    }

    @Override
    public void insert(List<NotificationSendAuditRecord> entities) {
        entities.forEach(this::save);
    }

    /**
     * Generates a unique identifier string for a given notification send audit record by extracting
     * and concatenating specific fields with underscores as separators.
     *
     * @param notificationSendAuditRecord the notification send audit record containing the details
     *                                     required to generate the identifier
     * @return a concatenated string representing the unique identifier based on the notification
     *         send audit record
     */
    private String generateId(NotificationSendAuditRecord notificationSendAuditRecord) {
        return generateId(
            notificationSendAuditRecord.getNotificationSendAudit().getContextInstanceId(),
            notificationSendAuditRecord.getNotificationSendAudit().getContextName(),
            notificationSendAuditRecord.getNotificationSendAudit().getJobName(),
            notificationSendAuditRecord.getNotificationSendAudit().getMonitorType(),
            notificationSendAuditRecord.getNotificationSendAudit().getNotifierType()
        );
    }

    /**
     * Generates a unique identifier string by concatenating the given parameters with underscores as separators.
     *
     * @param contextInstanceId the context instance ID to include in the generated ID
     * @param contextName the context name to include in the generated ID
     * @param jobName the job name to include in the generated ID
     * @param monitorType the monitor type to include in the generated ID
     * @param notifierType the notifier type to include in the generated ID
     * @return a concatenated string representing the unique identifier
     */
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
