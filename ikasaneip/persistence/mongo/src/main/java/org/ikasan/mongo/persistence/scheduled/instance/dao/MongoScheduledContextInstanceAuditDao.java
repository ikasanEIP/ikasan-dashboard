package org.ikasan.mongo.persistence.scheduled.instance.dao;

import org.ikasan.mongo.persistence.scheduled.instance.model.MongoScheduledContextInstanceRecordImpl;
import org.ikasan.mongo.persistence.scheduled.instance.repository.MongoScheduledContextInstanceRepository;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditDao;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MongoDB implementation of ScheduledContextInstanceAuditDao.
 *
 * This DAO provides audit trail functionality for scheduled context instances,
 * storing immutable historical records of context instance states.
 *
 * @author Ikasan Development Team
 */
public class MongoScheduledContextInstanceAuditDao implements ScheduledContextInstanceAuditDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoScheduledContextInstanceAuditDao.class);

    private final MongoScheduledContextInstanceRepository repository;
    private final long daysToKeep;

    /**
     * Constructor
     *
     * @param repository the MongoDB repository
     */
    public MongoScheduledContextInstanceAuditDao(MongoScheduledContextInstanceRepository repository, long daysToKeep) {
        this.repository = repository;
        this.daysToKeep = daysToKeep;
    }

    @Override
    public void save(ScheduledContextInstanceRecord scheduledContextInstanceRecord) {
        MongoScheduledContextInstanceRecordImpl entity = convertToEntity(scheduledContextInstanceRecord);
        repository.save(entity);
        logger.debug("Saved scheduled context instance audit record with id: {}", entity.getId());
    }

    @Override
    public ScheduledContextInstanceRecord findById(String id) {
        logger.debug("Finding scheduled context instance audit record by id: {}", id);
        return repository.findById(id).orElse(null);
    }

    /**
     * Convert ScheduledContextInstanceRecord to MongoDB entity
     *
     * @param record the record to convert
     * @return the entity ready for persistence
     */
    private MongoScheduledContextInstanceRecordImpl convertToEntity(ScheduledContextInstanceRecord record) {
        MongoScheduledContextInstanceRecordImpl entity = new MongoScheduledContextInstanceRecordImpl();

        // Use the record's ID (audit ID) directly
        if(record.getId() != null) {
            entity.setId(record.getId());
        }
        else {
            entity.setId(SCHEDULED_CONTEXT_INSTANCE_AUDIT_ID + "_" + UUID.randomUUID());
        }
        entity.setType(SCHEDULED_CONTEXT_INSTANCE_AUDIT_TYPE);
        entity.setContextName(record.getContextName());
        entity.setContextInstanceId(record.getContextInstanceId());
        entity.setContextInstance(record.getContextInstance());
        entity.setStatus(record.getStatus());
        entity.setTimestamp(record.getTimestamp());
        entity.setModifiedTimestamp(record.getModifiedTimestamp());
        entity.setModifiedBy(record.getModifiedBy());
        entity.setStartTime(record.getStartTime());
        entity.setEndTime(record.getEndTime());
        entity.setContainsRepeatingJobs(record.isContainsRepeatingJobs());
        entity.setExpiry(this.daysToKeep * TimeUnit.DAYS.toMillis(1) + System.currentTimeMillis());

        return entity;
    }
}
