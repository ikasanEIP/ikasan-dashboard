package org.ikasan.mongo.persistence.scheduled.profile.dao;

import org.ikasan.mongo.persistence.scheduled.SearchResultsImpl;
import org.ikasan.mongo.persistence.scheduled.profile.model.MongoContextProfileImpl;
import org.ikasan.mongo.persistence.scheduled.profile.model.MongoContextProfileRecordImpl;
import org.ikasan.mongo.persistence.scheduled.profile.repository.MongoContextProfileRepository;
import org.ikasan.spec.scheduled.profile.dao.ContextProfileDao;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.ikasan.spec.entity.EntityFields.ID;
import static org.ikasan.spec.entity.EntityFields.TYPE;

/**
 * MongoDB implementation of ContextProfileDao.
 * This class provides access to context profile records stored in MongoDB.
 */
public class MongoContextProfileDao implements ContextProfileDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoContextProfileDao.class);

    private final MongoContextProfileRepository repository;
    private final MongoTemplate mongoTemplate;

    /**
     * Constructor
     *
     * @param repository the MongoDB repository
     * @param mongoTemplate the MongoDB template for queries
     */
    public MongoContextProfileDao(MongoContextProfileRepository repository, MongoTemplate mongoTemplate) {
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
    public void save(ContextProfileRecord record) {
        saveInternal(record);
    }

    @Override
    public void save(List<ContextProfileRecord> records) {
        if (records != null) {
            for (ContextProfileRecord record : records) {
                saveInternal(record);
            }
        }
    }

    private void saveInternal(ContextProfileRecord record) {
        logger.debug("Saving context profile for profileName={}, contextName={}",
            record.getProfileName(), record.getContextName());

        MongoContextProfileRecordImpl mongoRecord = new MongoContextProfileRecordImpl();
        mongoRecord.setId(record.getProfileName() + "-"
            + record.getContextName() + "-" + CONTEXT_PROFILE_TYPE);
        mongoRecord.setType(CONTEXT_PROFILE_TYPE);
        mongoRecord.setProfileName(record.getProfileName());
        mongoRecord.setContextName(record.getContextName());
        mongoRecord.setOwner(record.getOwner());
        mongoRecord.setContextProfile(record.getContextProfile());
        mongoRecord.setAccessGroups(record.getAccessGroups());
        mongoRecord.setAccessUsers(record.getAccessUsers());
        mongoRecord.setCreatedDateTime(record.getCreatedDateTime());
        mongoRecord.setModifiedDateTime(record.getModifiedDateTime());
        mongoRecord.setModifiedBy(record.getModifiedBy());

        // Initialize context profile if null
        if (mongoRecord.getContextProfile() == null) {
            mongoRecord.setContextProfile(new MongoContextProfileImpl());
        }

        // Initialize access lists if null
        if (mongoRecord.getAccessGroups() == null) {
            mongoRecord.setAccessGroups(new ArrayList<>());
        }
        if (mongoRecord.getAccessUsers() == null) {
            mongoRecord.setAccessUsers(new ArrayList<>());
        }

        // Set timestamps
        if (mongoRecord.getCreatedDateTime() == 0) {
            mongoRecord.setCreatedDateTime(System.currentTimeMillis());
        }

        mongoRecord.setModifiedDateTime(System.currentTimeMillis());
        mongoRecord.setExpiry(-1);

        repository.save(mongoRecord);
        logger.debug("Saved context profile with id={}", mongoRecord.getId());
    }

    @Override
    public void deleteByContextName(String contextName) {
        logger.debug("Deleting context profiles by contextName={}", contextName);

        Criteria criteria = new Criteria().andOperator(
            Criteria.where("contextName").is(contextName),
            Criteria.where(TYPE).is(CONTEXT_PROFILE_TYPE)
        );
        Query query = new Query(criteria);

        long deletedCount = mongoTemplate.remove(query, MongoContextProfileRecordImpl.class).getDeletedCount();
        logger.debug("Deleted {} context profile(s) with contextName={}", deletedCount, contextName);
    }

    @Override
    public ContextProfileRecord findById(String id) {
        logger.debug("Finding context profile by id={}", id);

        Query query = new Query();
        query.addCriteria(Criteria.where(ID).is(id));
        query.addCriteria(Criteria.where(TYPE).is(CONTEXT_PROFILE_TYPE));

        MongoContextProfileRecordImpl result = mongoTemplate.findOne(query, MongoContextProfileRecordImpl.class);

        if (result != null) {
            logger.debug("Found context profile with id={}", id);
            return result;
        } else {
            logger.debug("No context profile found with id={}", id);
            return null;
        }
    }

    @Override
    public SearchResults<ContextProfileRecord> findByFilter(ContextProfileSearchFilter filter, int limit, int offset, String sortColumn, String sortOrder) {
        logger.debug("Finding context profiles by filter: profileName={}, contextName={}, owner={}, user={}, accessRoles={}",
            filter.getProfileName(), filter.getContextName(), filter.getOwner(), filter.getUser(), filter.getAccessRoles());

        long startTime = System.currentTimeMillis();

        List<Criteria> criteriaList = new ArrayList<>();

        // Always add type filter
        criteriaList.add(Criteria.where(TYPE).is(CONTEXT_PROFILE_TYPE));

        // Profile name filter
        if (filter.getProfileName() != null && !filter.getProfileName().isEmpty()) {
            criteriaList.add(Criteria.where("profileName").is(filter.getProfileName()));
        }

        // Context name filter
        if (filter.getContextName() != null && !filter.getContextName().isEmpty()) {
            criteriaList.add(Criteria.where("contextName").is(filter.getContextName()));
        }

        // Owner filter
        if (filter.getOwner() != null && !filter.getOwner().isEmpty()) {
            criteriaList.add(Criteria.where("owner").is(filter.getOwner()));
        }

        // User filter - check if user matches any string in accessUsers list (partial match)
        if (filter.getUser() != null && !filter.getUser().isEmpty()) {
            criteriaList.add(Criteria.where("accessUsers").regex(".*" + Pattern.quote(filter.getUser()) + ".*", "i"));
        }

        // Access roles filter - check if any of the roles match strings in accessGroups list (partial match)
        if (filter.getAccessRoles() != null && !filter.getAccessRoles().isEmpty()) {
            List<Criteria> roleCriteria = new ArrayList<>();
            for (String role : filter.getAccessRoles()) {
                roleCriteria.add(Criteria.where("accessGroups").regex(".*" + Pattern.quote(role) + ".*", "i"));
            }
            criteriaList.add(new Criteria().orOperator(roleCriteria.toArray(new Criteria[0])));
        }

        // Build query
        Query query;
        if (criteriaList.size() == 1) {
            query = new Query(criteriaList.get(0));
        } else {
            query = new Query(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        // Get total count
        long totalCount = mongoTemplate.count(query, MongoContextProfileRecordImpl.class);

        // Apply sorting
        if (sortColumn != null && !sortColumn.isEmpty()) {
            Sort.Direction direction = (sortOrder != null && sortOrder.equals("ASCENDING"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
            query.with(Sort.by(direction, sortColumn));
        } else {
            // Default sort by created date time descending
            query.with(Sort.by(Sort.Direction.DESC, "createdDateTime"));
        }

        // Apply pagination
        if (limit > 0 && offset >= 0) {
            Pageable pageable = PageRequest.of(offset / limit, limit);
            query.with(pageable);
        } else if (limit > 0) {
            query.limit(limit);
        }

        List<MongoContextProfileRecordImpl> results = mongoTemplate.find(query, MongoContextProfileRecordImpl.class);

        long queryResponseTime = System.currentTimeMillis() - startTime;

        logger.debug("Found {} context profile(s) out of {} total in {}ms",
            results.size(), totalCount, queryResponseTime);

        return new SearchResultsImpl<>(
            new ArrayList<>(results),
            totalCount,
            queryResponseTime
        );
    }
}
