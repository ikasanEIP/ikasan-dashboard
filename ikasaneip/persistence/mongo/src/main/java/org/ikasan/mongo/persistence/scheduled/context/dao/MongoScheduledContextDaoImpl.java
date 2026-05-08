package org.ikasan.mongo.persistence.scheduled.context.dao;

import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.context.model.MongoScheduledContextRecordImpl;
import org.ikasan.mongo.persistence.scheduled.context.repository.MongoScheduledContextRecordRepository;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.scheduled.context.model.ScheduledContextSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;

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

        ScheduledContextRecord result = repository.findById(id).orElse(null);

        logger.debug("Found ScheduledContextRecord: {}", result != null);
        return result;
    }

    @Override
    public SearchResults<ScheduledContextRecord> findAll(int limit, int offset) {
        logger.debug("Finding all ScheduledContextRecords with limit={}, offset={}", limit, offset);

        long totalCount = repository.count();

        List<MongoScheduledContextRecordImpl> results;

        if (limit > 0) {
            int page = offset > 0 ? offset / limit : 0;
            Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "modifiedTimestamp"));
            Page<MongoScheduledContextRecordImpl> resultPage = repository.findAll(pageable);
            results = resultPage.getContent();
        } else {
            results = repository.findAll(Sort.by(Sort.Direction.DESC, "modifiedTimestamp"));
        }

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

        // Build criteria from filter
        if (filter != null) {
            if (filter.getContextName() != null && !filter.getContextName().isEmpty()) {
                Criteria criteria = Criteria.where("contextName")
                    .regex(filter.getContextName(), "i"); // Case-insensitive regex
                query.addCriteria(criteria);
            }
            if (filter.getContextNames() != null && !filter.getContextNames().isEmpty()) {
                Criteria criteria = Criteria.where("contextName").in(filter.getContextNames());
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
            query.with(Sort.by(Sort.Direction.DESC, "modifiedTimestamp"));
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

        return repository.findByContextName(name).orElse(null);
    }

    @Override
    public void save(ScheduledContextRecord scheduledContextRecord) {
        logger.debug("Saving ScheduledContextRecord: {}", scheduledContextRecord.getContextName());

        MongoScheduledContextRecordImpl mongoRecord;

        if (scheduledContextRecord instanceof MongoScheduledContextRecordImpl) {
            mongoRecord = (MongoScheduledContextRecordImpl) scheduledContextRecord;
        } else {
            // Convert to MongoDB implementation
            mongoRecord = new MongoScheduledContextRecordImpl(scheduledContextRecord.getContextName());
            mongoRecord.setContext(scheduledContextRecord.getContext());
            mongoRecord.setTimestamp(scheduledContextRecord.getTimestamp());
            mongoRecord.setModifiedTimestamp(scheduledContextRecord.getModifiedTimestamp());
            mongoRecord.setModifiedBy(scheduledContextRecord.getModifiedBy());
        }

        // Set timestamps if new
        if (mongoRecord.getTimestamp() == 0) {
            mongoRecord.setTimestamp(System.currentTimeMillis());
        }
        mongoRecord.setModifiedTimestamp(System.currentTimeMillis());

        // Ensure ID is set
        if (mongoRecord.getId() == null || mongoRecord.getId().isEmpty()) {
            mongoRecord.setId(mongoRecord.getContextName());
        }

        // Save to MongoDB
        repository.save(mongoRecord);

        logger.debug("Successfully saved ScheduledContextRecord: {}", mongoRecord.getContextName());
    }

    @Override
    public void deleteContext(String contextName) {
        logger.debug("Deleting ScheduledContextRecord with context name: {}", contextName);

        repository.deleteByContextName(contextName);

        logger.debug("Successfully deleted ScheduledContextRecord: {}", contextName);
    }
}
