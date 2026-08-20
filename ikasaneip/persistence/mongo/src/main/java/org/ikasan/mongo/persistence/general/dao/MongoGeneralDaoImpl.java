package org.ikasan.mongo.persistence.general.dao;

import org.ikasan.mongo.persistence.general.model.MongoIkasanDocument;
import org.ikasan.mongo.persistence.general.model.MongoIkasanDocumentSearchResults;
import org.ikasan.mongo.persistence.general.repository.MongoIkasanDocumentRepository;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.persistence.dao.EntityDeleteDao;
import org.ikasan.spec.search.dao.ESBSearchDao;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Set;

/**
 * MongoDB implementation of general DAO for Ikasan documents.
 * Implements ESBSearchDao and EntityDeleteDao interfaces.
 */
public class MongoGeneralDaoImpl implements
    ESBSearchDao<IkasanDocumentSearchResults, MongoIkasanDocument>,
    EntityDeleteDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoGeneralDaoImpl.class);

    public static final String ASCENDING = "ASCENDING";
    public static final String DESCENDING = "DESCENDING";

    private final MongoIkasanDocumentRepository repository;
    private final MongoTemplate mongoTemplate;

    /**
     * Constructor
     *
     * @param repository the MongoDB repository
     * @param mongoTemplate the MongoTemplate for complex queries
     */
    public MongoGeneralDaoImpl(MongoIkasanDocumentRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public IkasanDocumentSearchResults search(String searchString, long startTime, long endTime, int resultSize,
                                                     List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        return this.searchBase(null, null, null, null, searchString,
            startTime, endTime, 0, resultSize, entityTypes, negateQuery, sortField, sortOrder);
    }

    @Override
    public IkasanDocumentSearchResults search(String searchString, long startTime, long endTime, int offset, int resultSize,
                                                     List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        return this.searchBase(null, null, null, null, searchString, startTime,
            endTime, offset, resultSize, entityTypes, negateQuery, sortField, sortOrder);
    }

    @Override
    public IkasanDocumentSearchResults search(Set<String> moduleNames, Set<String> flowNames, String searchString, long startTime,
                                                     long endTime, int resultSize, boolean negateQuery, String sortField, String sortOrder) {
        return this.searchBase(moduleNames, flowNames, null, null, searchString, startTime, endTime,
            0, resultSize, null, negateQuery, sortField, sortOrder);
    }

    @Override
    public IkasanDocumentSearchResults search(Set<String> moduleNames, Set<String> flowNames, String searchString,
                                                     long startTime, long endTime, int resultSize, List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        return this.searchBase(moduleNames, flowNames, null, null, searchString, startTime, endTime,
            0, resultSize, entityTypes, negateQuery, sortField, sortOrder);
    }

    @Override
    public IkasanDocumentSearchResults search(Set<String> moduleNames, Set<String> flowNames, Set<String> componentNames,
                                                     String eventId, String searchString, long startTime,
                                                     long endTime, int offset, int resultSize, List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        return this.searchBase(moduleNames, flowNames, componentNames, eventId, searchString, startTime, endTime, offset,
            resultSize, entityTypes, negateQuery, sortField, sortOrder);
    }

    /**
     * Base search method that builds and executes MongoDB queries.
     *
     * @param moduleNames the module names to filter by
     * @param flowNames the flow names to filter by
     * @param componentNames the component names to filter by
     * @param eventId the event ID to filter by
     * @param searchString the search string for text search
     * @param startTime the start timestamp
     * @param endTime the end timestamp
     * @param offset the offset for pagination
     * @param resultSize the maximum number of results
     * @param entityTypes the entity types to filter by
     * @param negateQuery whether to negate the query
     * @param sortField the field to sort by
     * @param sortOrder the sort order (ASCENDING or DESCENDING)
     * @return the search results
     */
    protected IkasanDocumentSearchResults searchBase(Set<String> moduleNames, Set<String> flowNames, Set<String> componentNames,
                                                          String eventId, String searchString, long startTime, long endTime, int offset, int resultSize, List<String> entityTypes, boolean negateQuery,
                                                          String sortField, String sortOrder) {
        long queryStartTime = System.currentTimeMillis();

        Query query = new Query();

        // Build criteria
        Criteria criteria = buildCriteria(moduleNames, flowNames, componentNames, eventId, searchString, startTime, endTime, entityTypes, negateQuery);
        query.addCriteria(criteria);

        // Add pagination
        query.skip(offset);
        query.limit(resultSize);

        // Add sorting
        if (sortField != null && !sortField.isEmpty() && sortOrder != null && !sortOrder.isEmpty()) {
            if (sortOrder.equals(DESCENDING)) {
                query.with(Sort.by(Sort.Direction.DESC, sortField));
            } else {
                query.with(Sort.by(Sort.Direction.ASC, sortField));
            }
        } else {
            // Default sort by timestamp descending
            query.with(Sort.by(Sort.Direction.DESC, EntityFields.CREATED_DATE_TIME));
        }

        try {
            logger.debug("Executing MongoDB query: {}", query);

            // Execute query
            List<IkasanESBDocument> results = mongoTemplate
                .find(query, MongoIkasanDocument.class).stream()
                .map(doc -> (IkasanESBDocument)doc)
                .toList();

            // Get total count for pagination
            Query countQuery = new Query();
            countQuery.addCriteria(criteria);
            long totalCount = mongoTemplate.count(countQuery, MongoIkasanDocument.class);

            long queryTime = System.currentTimeMillis() - queryStartTime;

            return new MongoIkasanDocumentSearchResults(results, totalCount, queryTime);
        } catch (Exception e) {
            throw new RuntimeException("Caught exception performing general ikasan search!", e);
        }
    }

    /**
     * Build MongoDB criteria from search parameters.
     *
     * @param moduleNames the module names to filter by
     * @param flowNames the flow names to filter by
     * @param componentNames the component names to filter by
     * @param eventId the event ID to filter by
     * @param searchString the search string for text search
     * @param startTime the start timestamp
     * @param endTime the end timestamp
     * @param entityTypes the entity types to filter by
     * @param negateQuery whether to negate the query
     * @return the constructed criteria
     */
    protected Criteria buildCriteria(Set<String> moduleNames, Set<String> flowNames, Set<String> componentNames,
                                      String eventId, String searchString, long startTime, long endTime, List<String> entityTypes, boolean negateQuery) {
        java.util.List<Criteria> criteriaList = new java.util.ArrayList<>();

        // Module names filter
        if (moduleNames != null && !moduleNames.isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.MODULE_NAME).in(moduleNames));
        }

        // Flow names filter
        if (flowNames != null && !flowNames.isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.FLOW_NAME).in(flowNames));
        }

        // Component names filter
        if (componentNames != null && !componentNames.isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.COMPONENT_NAME).in(componentNames));
        }

        // Event ID filter
        if (eventId != null && !eventId.isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.EVENT).is(eventId));
        }

        // Timestamp range filter
        if (startTime > 0 && endTime > 0) {
            criteriaList.add(Criteria.where(EntityFields.CREATED_DATE_TIME).gte(startTime).lte(endTime));
        } else if (startTime > 0) {
            criteriaList.add(Criteria.where(EntityFields.CREATED_DATE_TIME).gte(startTime));
        } else if (endTime > 0) {
            criteriaList.add(Criteria.where(EntityFields.CREATED_DATE_TIME).lte(endTime));
        }

        // Entity types filter
        if (entityTypes != null && !entityTypes.isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.TYPE).in(entityTypes));
        }

        // Search string filter - search in payload and error details
        if (searchString != null && !searchString.isEmpty()) {
            Criteria searchCriteria = new Criteria().orOperator(
                Criteria.where(EntityFields.PAYLOAD_CONTENT).regex(searchString, "i"),
                Criteria.where(EntityFields.ERROR_DETAIL).regex(searchString, "i"),
                Criteria.where(EntityFields.ERROR_MESSAGE).regex(searchString, "i")
            );

            if (negateQuery) {
                criteriaList.add(new Criteria().norOperator(searchCriteria));
            } else {
                criteriaList.add(searchCriteria);
            }
        }

        // Combine all criteria with AND
        if (criteriaList.isEmpty()) {
            return new Criteria();
        } else if (criteriaList.size() == 1) {
            return criteriaList.get(0);
        } else {
            return new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        }
    }

    @Override
    public MongoIkasanDocument findById(String type, String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ID).is(id).and(EntityFields.TYPE).is(type));

        List<MongoIkasanDocument> results = mongoTemplate.find(query, MongoIkasanDocument.class);
        return results.stream().findFirst().orElse(null);
    }

    @Override
    public MongoIkasanDocument findByErrorUri(String type, String uri) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ERROR_URI).is(uri).and(EntityFields.TYPE).is(type));

        List<MongoIkasanDocument> results = mongoTemplate.find(query, MongoIkasanDocument.class);
        return results.stream().findFirst().orElse(null);
    }

    /**
     * Save or update a document.
     *
     * @param document the document to save
     */
    public void saveOrUpdate(MongoIkasanDocument document) {
        repository.save(document);
    }

    /**
     * Save or update multiple documents.
     *
     * @param documents the documents to save
     */
    public void saveOrUpdate(List<MongoIkasanDocument> documents) {
        repository.saveAll(documents);
    }

    @Override
    public void removeExpired() {
        long currentTime = System.currentTimeMillis();
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.EXPIRY).lt(currentTime));

        long deletedCount = mongoTemplate.remove(query, MongoIkasanDocument.class).getDeletedCount();
        logger.info("Deleted {} expired documents", deletedCount);
    }

    @Override
    public void removeById(String type, String id) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ID).is(id).and(EntityFields.TYPE).is(type));

        mongoTemplate.remove(query, MongoIkasanDocument.class);
    }
}
