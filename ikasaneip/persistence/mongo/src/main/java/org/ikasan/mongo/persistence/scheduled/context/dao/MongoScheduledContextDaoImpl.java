package org.ikasan.mongo.persistence.scheduled.context.dao;

import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.context.model.MongoScheduledContextRecordImpl;
import org.ikasan.mongo.persistence.scheduled.context.repository.MongoScheduledContextRecordRepository;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.model.ScheduledContextSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

import static org.ikasan.spec.entity.EntityFields.ID;
import static org.ikasan.spec.entity.EntityFields.TYPE;

/**
 * MongoDB implementation of ScheduledContextDao.
 *
 * This DAO leverages:
 * - Spring Data MongoDB for repository operations
 * - MongoTemplate for complex queries
 * - MongoDB indexing for performance
 */
public class MongoScheduledContextDaoImpl implements ScheduledContextDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoScheduledContextDaoImpl.class);

    private static JsonMapper objectMapper = JsonMapper.builder().build();

    private final MongoScheduledContextRecordRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoScheduledContextDaoImpl(MongoScheduledContextRecordRepository repository,
                                        MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public SearchResults<ScheduledContextRecord> findAll() {
        return findAll(-1, -1);
    }

    @Override
    public ScheduledContextRecord findById(String id) {
        logger.debug("Finding ScheduledContextRecord by id: {}", id);

        Query query = new Query();
        query.addCriteria(Criteria.where(ID).is(id));
        query.addCriteria(Criteria.where(TYPE).is(SCHEDULED_CONTEXT_TYPE));

        ScheduledContextRecord result = mongoTemplate.findOne(query, MongoScheduledContextRecordImpl.class);

        logger.debug("Found ScheduledContextRecord: {}", result != null);
        return result;
    }

    @Override
    public SearchResults<ScheduledContextRecord> findAll(int limit, int offset) {
        logger.debug("Finding all ScheduledContextRecords with limit={}, offset={}", limit, offset);

        Query query = new Query();
        query.addCriteria(Criteria.where(TYPE).is(SCHEDULED_CONTEXT_TYPE));

        long totalCount = mongoTemplate.count(query, MongoScheduledContextRecordImpl.class);

        // Apply sorting
        query.with(Sort.by(Sort.Direction.DESC, EntityFields.CREATED_DATE_TIME));

        // Apply pagination
        if (limit > 0) {
            int page = offset > 0 ? offset / limit : 0;
            query.with(PageRequest.of(page, limit));
        }

        List<MongoScheduledContextRecordImpl> results = mongoTemplate.find(query, MongoScheduledContextRecordImpl.class);

        logger.debug("Found {} ScheduledContextRecords out of {} total", results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    public SearchResults<ScheduledContextRecord> findByFilter(ScheduledContextSearchFilter filter,
                                                               int limit,
                                                               int offset,
                                                               String sortColumn,
                                                               String sortOrder) {
        logger.debug("Finding ScheduledContextRecords by filter: {}", filter);

        Query query = new Query();
        query.addCriteria(Criteria.where(TYPE).is(SCHEDULED_CONTEXT_TYPE));

        // Build criteria from filter
        if (filter != null) {
            if (filter.getContextName() != null && !filter.getContextName().isEmpty()) {
                Criteria criteria = Criteria.where(EntityFields.MODULE_NAME)
                    .regex(filter.getContextName(), "i"); // Case-insensitive regex
                query.addCriteria(criteria);
            }
            if (filter.getContextNames() != null && !filter.getContextNames().isEmpty()) {
                Criteria criteria = Criteria.where(EntityFields.MODULE_NAME).in(filter.getContextNames());
                query.addCriteria(criteria);
            }
        }

        // Count total matching records
        long totalCount = mongoTemplate.count(query, MongoScheduledContextRecordImpl.class);

        // Apply sorting
        if (sortColumn != null && !sortColumn.isEmpty()) {
            Sort.Direction direction = "ASCENDING".equalsIgnoreCase(sortOrder)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
            query.with(Sort.by(direction, sortColumn));
        } else {
            // Default sort by modified timestamp descending
            query.with(Sort.by(Sort.Direction.DESC, EntityFields.CREATED_DATE_TIME));
        }

        // Apply pagination
        if (limit > 0) {
            int page = offset > 0 ? offset / limit : 0;
            query.with(PageRequest.of(page, limit));
        }

        List<MongoScheduledContextRecordImpl> results = mongoTemplate.find(query, MongoScheduledContextRecordImpl.class);

        logger.debug("Found {} ScheduledContextRecords out of {} total matching filter", results.size(), totalCount);

        return new SearchResultsImpl<>(new ArrayList<>(results), totalCount, 0L);
    }

    @Override
    public ScheduledContextRecord findByName(String name) {
        logger.debug("Finding ScheduledContextRecord by name: {}", name);

        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.MODULE_NAME).is(name));
        query.addCriteria(Criteria.where(TYPE).is(SCHEDULED_CONTEXT_TYPE));

        return mongoTemplate.findOne(query, MongoScheduledContextRecordImpl.class);
    }

    @Override
    public void save(ScheduledContextRecord scheduledContextRecord) {
        logger.debug("Saving ScheduledContextRecord: {}", scheduledContextRecord.getContextName());

        MongoScheduledContextRecordImpl mongoRecord = new MongoScheduledContextRecordImpl();
        mongoRecord.setId(scheduledContextRecord.getContextName() + "-" + SCHEDULED_CONTEXT_TYPE);
        mongoRecord.setType(SCHEDULED_CONTEXT_TYPE);

        try {
            ContextTemplate contextTemplate = scheduledContextRecord.getContext();
            mongoRecord.setContextTemplateJson(this.getPayloadContents(contextTemplate));
            mongoRecord.setDisabled(contextTemplate.isDisabled());
            mongoRecord.setQuartzScheduleDrivenJobsDisabledForContext
                (contextTemplate.isQuartzScheduleDrivenJobsDisabledForContext());
        }
        catch (JacksonException e) {
            throw new RuntimeException(String.format("Cannot convert FileEventDrivenJob to string! [%s]"
                , scheduledContextRecord.getContext()));
        }

        mongoRecord.setContextName(scheduledContextRecord.getContextName());
        mongoRecord.setTimestamp(scheduledContextRecord.getTimestamp());
        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());
        // only update modified field if populated.
        if(scheduledContextRecord.getModifiedBy() != null &&
            !scheduledContextRecord.getModifiedBy().isEmpty()) {
            mongoRecord.setModifiedBy(scheduledContextRecord.getModifiedBy());
        }
        mongoRecord.setExpiry(DO_NOT_EXPIRE);

        // Save to MongoDB
        repository.save(mongoRecord);

        logger.debug("Successfully saved ScheduledContextRecord: {}", mongoRecord.getContextName());
    }

    @Override
    public void deleteContext(String contextName) {
        logger.debug("Deleting ScheduledContextRecord with context name: {}", contextName);

        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.MODULE_NAME).is(contextName));
        query.addCriteria(Criteria.where(TYPE).is(SCHEDULED_CONTEXT_TYPE));

        mongoTemplate.remove(query, MongoScheduledContextRecordImpl.class);

        logger.debug("Successfully deleted ScheduledContextRecord: {}", contextName);
    }

    protected String getPayloadContents(ContextTemplate contextTemplate)  {
        return objectMapper.writeValueAsString(contextTemplate);
    }
}
