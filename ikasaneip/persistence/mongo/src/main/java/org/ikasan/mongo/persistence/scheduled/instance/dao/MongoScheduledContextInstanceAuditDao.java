package org.ikasan.mongo.persistence.scheduled.instance.dao;

import org.ikasan.mongo.persistence.scheduled.instance.model.MongoScheduledContextInstanceRecordImpl;
import org.ikasan.mongo.persistence.scheduled.instance.repository.MongoScheduledContextInstanceRepository;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditDao;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * Constructor
     *
     * @param repository the MongoDB repository
     */
    public MongoScheduledContextInstanceAuditDao(MongoScheduledContextInstanceRepository repository) {
        this.repository = repository;
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
        if (record instanceof MongoScheduledContextInstanceRecordImpl) {
            return (MongoScheduledContextInstanceRecordImpl) record;
        }

        MongoScheduledContextInstanceRecordImpl entity = new MongoScheduledContextInstanceRecordImpl();

        // Use the record's ID (audit ID) directly
        entity.setId(record.getId());
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

        return entity;
    }
}
