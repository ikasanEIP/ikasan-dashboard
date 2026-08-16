package org.ikasan.mongo.persistence.scheduled.context.dao;

import org.ikasan.mongo.persistence.scheduled.context.model.MongoScheduledContextViewRecordImpl;
import org.ikasan.mongo.persistence.scheduled.context.repository.MongoScheduledContextViewRepository;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextViewDao;
import org.ikasan.spec.scheduled.context.model.ScheduledContextViewRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * MongoDB implementation of ScheduledContextViewDao.
 * This class provides access to scheduled context view records stored in MongoDB.
 */
public class MongoScheduledContextViewDao implements ScheduledContextViewDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoScheduledContextViewDao.class);
    private static final String SCHEDULED_CONTEXT_VIEW = "scheduledContextView";

    private final MongoScheduledContextViewRepository repository;
    private final MongoTemplate mongoTemplate;

    /**
     * Constructor
     *
     * @param repository the MongoDB repository
     * @param mongoTemplate the MongoDB template for queries
     */
    public MongoScheduledContextViewDao(MongoScheduledContextViewRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        if (this.repository == null) {
            throw new IllegalArgumentException("repository cannot be null!");
        }
        this.mongoTemplate = mongoTemplate;
        if (this.mongoTemplate == null) {
            throw new IllegalArgumentException("mongoTemplate cannot be null!");
        }
    }

    @Override
    public void save(ScheduledContextViewRecord record) {
        saveOrUpdate(record);
    }

    @Override
    public ScheduledContextViewRecord getContextView(String parentContextName, String contextName) {
        logger.debug("Getting context view for parentContextName={}, contextName={}", parentContextName, contextName);

        Criteria criteria = new Criteria().andOperator(
            Criteria.where("parentContextName").is(parentContextName),
            Criteria.where("contextName").is(contextName)
        );

        Query query = new Query(criteria);

        MongoScheduledContextViewRecordImpl result = mongoTemplate.findOne(query, MongoScheduledContextViewRecordImpl.class);

        if (result != null) {
            logger.debug("Found context view: {}", result.getId());
        } else {
            logger.debug("No context view found for parentContextName={}, contextName={}", parentContextName, contextName);
        }

        return result;
    }

    /**
     * Save or update a scheduled context view record.
     *
     * @param record the record to save
     */
    public void saveOrUpdate(ScheduledContextViewRecord record) {
        logger.debug("Saving context view for parentContextName={}, contextName={}",
            record.getParentContextName(), record.getContextName());

        MongoScheduledContextViewRecordImpl mongoRecord;
        if (record instanceof MongoScheduledContextViewRecordImpl) {
            mongoRecord = (MongoScheduledContextViewRecordImpl) record;
        } else {
            // Convert to MongoDB implementation
            mongoRecord = new MongoScheduledContextViewRecordImpl();
            mongoRecord.setId(record.getParentContextName() + "-" + record.getContextName() + "-" + SCHEDULED_CONTEXT_VIEW);
            mongoRecord.setParentContextName(record.getParentContextName());
            mongoRecord.setContextName(record.getContextName());
            mongoRecord.setContextView(record.getContextView());
            mongoRecord.setTimestamp(record.getTimestamp());
            mongoRecord.setModifiedTimestamp(System.currentTimeMillis());
            mongoRecord.setModifiedBy(record.getModifiedBy());
        }

        // Ensure ID is set according to pattern
        if (mongoRecord.getId() == null || mongoRecord.getId().isEmpty()) {
            mongoRecord.setId(record.getParentContextName() + "-" + record.getContextName() + "-" + SCHEDULED_CONTEXT_VIEW);
        }

        // Update modifiedTimestamp on save
        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());

        repository.save(mongoRecord);
        logger.debug("Saved context view with id={}", mongoRecord.getId());
    }

    /**
     * Delete a scheduled context view record.
     *
     * @param parentContextName the parent context name
     * @param contextName the context name
     */
    public void delete(String parentContextName, String contextName) {
        logger.debug("Deleting context view for parentContextName={}, contextName={}", parentContextName, contextName);

        ScheduledContextViewRecord record = getContextView(parentContextName, contextName);
        if (record != null) {
            repository.deleteById(record.getId());
            logger.debug("Deleted context view with id={}", record.getId());
        } else {
            logger.debug("No context view found to delete for parentContextName={}, contextName={}",
                parentContextName, contextName);
        }
    }
}
