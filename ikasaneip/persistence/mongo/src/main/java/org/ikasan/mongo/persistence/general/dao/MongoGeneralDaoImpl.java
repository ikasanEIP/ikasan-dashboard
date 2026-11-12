package org.ikasan.mongo.persistence.general.dao;

import org.bson.Document;
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
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * MongoDB implementation of general DAO for Ikasan documents.
 * Implements ESBSearchDao and EntityDeleteDao interfaces.
 *
 * <p>This implementation uses MongoDB Atlas Lucene $search for full-text search
 * capabilities instead of the basic $text operator. Atlas Search provides
 * more powerful search features including fuzzy matching, autocomplete,
 * and better relevance scoring.
 */
public class MongoGeneralDaoImpl implements
    ESBSearchDao<IkasanDocumentSearchResults, MongoIkasanDocument>,
    EntityDeleteDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoGeneralDaoImpl.class);

    public static final String ASCENDING = "ASCENDING";
    public static final String DESCENDING = "DESCENDING";

    /**
     * The default Atlas Search index name.
     */
    public static final String DEFAULT_SEARCH_INDEX = "default";

    /**
     * Fields to search in for full-text search.
     */
    private static final List<String> SEARCH_FIELDS = List.of(
        EntityFields.PAYLOAD_CONTENT,
        EntityFields.ERROR_DETAIL,
        EntityFields.ERROR_MESSAGE,
        EntityFields.EVENT
    );

    private final MongoIkasanDocumentRepository repository;
    private final MongoTemplate mongoTemplate;
    private String searchIndexName = DEFAULT_SEARCH_INDEX;

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

    /**
     * Constructor with custom search index name.
     *
     * @param repository the MongoDB repository
     * @param mongoTemplate the MongoTemplate for complex queries
     * @param searchIndexName the name of the Atlas Search index
     */
    public MongoGeneralDaoImpl(MongoIkasanDocumentRepository repository, MongoTemplate mongoTemplate, String searchIndexName) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.searchIndexName = searchIndexName != null ? searchIndexName : DEFAULT_SEARCH_INDEX;
    }

    /**
     * Sets the Atlas Search index name.
     *
     * @param searchIndexName the name of the Atlas Search index
     */
    public void setSearchIndexName(String searchIndexName) {
        this.searchIndexName = searchIndexName != null ? searchIndexName : DEFAULT_SEARCH_INDEX;
    }

    /**
     * Gets the Atlas Search index name.
     *
     * @return the name of the Atlas Search index
     */
    public String getSearchIndexName() {
        return this.searchIndexName;
    }

    @Override
    public IkasanDocumentSearchResults search(Set<String> identifiers, int offset, int resultSize, String sortField, String sortOrder) {
        if (identifiers == null || identifiers.isEmpty()) {
            return new MongoIkasanDocumentSearchResults(List.of(), 0L, 0L);
        }

        long queryStartTime = System.currentTimeMillis();

        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ID).in(identifiers));

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
            logger.debug("Executing MongoDB query by identifiers: {}", query);

            // Get total count for pagination
            long totalCount = mongoTemplate.count(query, MongoIkasanDocument.class);

            // Add pagination
            query.skip(offset);
            query.limit(resultSize);

            // Execute query
            List<IkasanESBDocument> results = mongoTemplate
                .find(query, MongoIkasanDocument.class).stream()
                .map(doc -> (IkasanESBDocument)doc)
                .toList();

            long queryTime = System.currentTimeMillis() - queryStartTime;

            return new MongoIkasanDocumentSearchResults(results, totalCount, queryTime);
        } catch (Exception e) {
            throw new RuntimeException("Caught exception performing MongoDB search by identifiers!", e);
        }
    }

    @Override
    public IkasanDocumentSearchResults search(String searchString, long startTime, long endTime, int resultSize,
                                                     List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        return this.searchBase(null, null, null, null, searchString,
            startTime, endTime, 0, resultSize, entityTypes, negateQuery, sortField, sortOrder, false);
    }

    @Override
    public IkasanDocumentSearchResults search(String searchString, long startTime, long endTime, int offset, int resultSize,
                                                     List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        return this.searchBase(null, null, null, null, searchString, startTime,
            endTime, offset, resultSize, entityTypes, negateQuery, sortField, sortOrder, false);
    }

    @Override
    public IkasanDocumentSearchResults search(Set<String> moduleNames, Set<String> flowNames, String searchString, long startTime,
                                                     long endTime, int resultSize, boolean negateQuery, String sortField, String sortOrder) {
        return this.searchBase(moduleNames, flowNames, null, null, searchString, startTime, endTime,
            0, resultSize, null, negateQuery, sortField, sortOrder,  false);
    }

    @Override
    public IkasanDocumentSearchResults search(Set<String> moduleNames, Set<String> flowNames, String searchString,
                                                     long startTime, long endTime, int resultSize, List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        return this.searchBase(moduleNames, flowNames, null, null, searchString, startTime, endTime,
            0, resultSize, entityTypes, negateQuery, sortField, sortOrder, false);
    }

    @Override
    public IkasanDocumentSearchResults search(Set<String> moduleNames, Set<String> flowNames, Set<String> componentNames,
                                                     String eventId, String searchString, long startTime,
                                                     long endTime, int offset, int resultSize, List<String> entityTypes, boolean negateQuery, String sortField, String sortOrder) {
        return this.searchBase(moduleNames, flowNames, componentNames, eventId, searchString, startTime, endTime, offset,
            resultSize, entityTypes, negateQuery, sortField, sortOrder, false);
    }

    @Override
    public IkasanDocumentSearchResults searchByHarvestReceivedTime(Set<String> moduleNames, Set<String> flowNames
        , Set<String> componentNames, String eventId, String searchString, long harvestReceivedStartTime
        , long harvestReceivedEndTime, int offset, int resultSize, List<String> entityTypes, boolean negateQuery
        , String sortField, String sortOrder) {
        return this.searchBase(moduleNames, flowNames, componentNames, eventId, searchString, harvestReceivedStartTime, harvestReceivedEndTime, offset,
            resultSize, entityTypes, negateQuery, sortField, sortOrder, true);
    }

    /**
     * Base search method that builds and executes MongoDB queries.
     * Uses Atlas Lucene $search when searchString is provided for full-text search.
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
     * @param harvestReceivedTimestamp whether to use harvest received timestamp for time filtering
     * @return the search results
     */
    protected IkasanDocumentSearchResults searchBase(Set<String> moduleNames, Set<String> flowNames, Set<String> componentNames,
                                                          String eventId, String searchString, long startTime, long endTime, int offset, int resultSize, List<String> entityTypes, boolean negateQuery,
                                                          String sortField, String sortOrder, boolean harvestReceivedTimestamp) {
        long queryStartTime = System.currentTimeMillis();

        // If searchString is provided, use Atlas Search aggregation
        if (searchString != null && !searchString.isEmpty()) {
            return searchWithAtlasSearch(moduleNames, flowNames, componentNames, eventId, searchString,
                startTime, endTime, offset, resultSize, entityTypes, negateQuery, sortField, sortOrder,
                harvestReceivedTimestamp, queryStartTime);
        }

        // Otherwise use standard query
        return searchWithStandardQuery(moduleNames, flowNames, componentNames, eventId,
            startTime, endTime, offset, resultSize, entityTypes, negateQuery, sortField, sortOrder,
            harvestReceivedTimestamp, queryStartTime);
    }

    /**
     * Execute search using Atlas Lucene $search aggregation pipeline.
     */
    private IkasanDocumentSearchResults searchWithAtlasSearch(Set<String> moduleNames, Set<String> flowNames,
                                                                     Set<String> componentNames, String eventId, String searchString,
                                                                     long startTime, long endTime, int offset, int resultSize,
                                                                     List<String> entityTypes, boolean negateQuery, String sortField,
                                                                     String sortOrder, boolean harvestReceivedTimestamp, long queryStartTime) {
        try {
            List<AggregationOperation> operations = new ArrayList<>();

            // Add Atlas $search stage as the first stage
            AggregationOperation searchStage = AtlasSearchAggregationOperation.textSearch(
                searchIndexName, searchString, SEARCH_FIELDS);
            operations.add(searchStage);

            // Build match criteria for filtering
            Criteria matchCriteria = buildMatchCriteria(moduleNames, flowNames, componentNames, eventId,
                startTime, endTime, entityTypes, harvestReceivedTimestamp);

            if (matchCriteria != null) {
                operations.add(Aggregation.match(matchCriteria));
            }

            // Add sorting
            Sort sort = buildSort(sortField, sortOrder);
            operations.add(Aggregation.sort(sort));

            // First, get the total count (without skip/limit)
            List<AggregationOperation> countOperations = new ArrayList<>(operations);
            countOperations.add(Aggregation.count().as("total"));

            Aggregation countAggregation = Aggregation.newAggregation(countOperations);
            AggregationResults<Document> countResults = mongoTemplate.aggregate(
                countAggregation, MongoIkasanDocument.class, Document.class);

            long totalCount = 0;
            if (countResults.getMappedResults() != null && !countResults.getMappedResults().isEmpty()) {
                Document countDoc = countResults.getMappedResults().get(0);
                totalCount = countDoc.getInteger("total", 0);
            }

            // Add pagination
            operations.add(Aggregation.skip((long) offset));
            operations.add(Aggregation.limit(resultSize));

            // Execute aggregation
            Aggregation aggregation = Aggregation.newAggregation(operations);
            logger.debug("Executing Atlas Search aggregation: {}", aggregation);

            AggregationResults<MongoIkasanDocument> results = mongoTemplate.aggregate(
                aggregation, MongoIkasanDocument.class, MongoIkasanDocument.class);

            List<IkasanESBDocument> resultList = results.getMappedResults().stream()
                .map(doc -> (IkasanESBDocument) doc)
                .toList();

            long queryTime = System.currentTimeMillis() - queryStartTime;
            return new MongoIkasanDocumentSearchResults(resultList, totalCount, queryTime);

        } catch (Exception e) {
            logger.error("Error executing Atlas Search query", e);
            throw new RuntimeException("Caught exception performing Atlas Search!", e);
        }
    }

    /**
     * Execute search using standard MongoDB query (no full-text search).
     */
    private IkasanDocumentSearchResults searchWithStandardQuery(Set<String> moduleNames, Set<String> flowNames,
                                                                       Set<String> componentNames, String eventId,
                                                                       long startTime, long endTime, int offset, int resultSize,
                                                                       List<String> entityTypes, boolean negateQuery, String sortField,
                                                                       String sortOrder, boolean harvestReceivedTimestamp, long queryStartTime) {
        Query query = buildQuery(moduleNames, flowNames, componentNames, eventId,
            startTime, endTime, entityTypes, harvestReceivedTimestamp);

        // Add sorting
        query.with(buildSort(sortField, sortOrder));

        try {
            logger.debug("Executing MongoDB query: {}", query);

            // Get total count for pagination
            long totalCount = mongoTemplate.count(query, MongoIkasanDocument.class);

            // Add pagination
            query.skip(offset);
            query.limit(resultSize);

            // Execute query
            List<IkasanESBDocument> results = mongoTemplate
                .find(query, MongoIkasanDocument.class).stream()
                .map(doc -> (IkasanESBDocument) doc)
                .toList();

            long queryTime = System.currentTimeMillis() - queryStartTime;

            return new MongoIkasanDocumentSearchResults(results, totalCount, queryTime);
        } catch (Exception e) {
            throw new RuntimeException("Caught exception performing general ikasan search!", e);
        }
    }

    /**
     * Build Sort object from sort field and order.
     */
    private Sort buildSort(String sortField, String sortOrder) {
        if (sortField != null && !sortField.isEmpty() && sortOrder != null && !sortOrder.isEmpty()) {
            if (sortOrder.equals(DESCENDING)) {
                return Sort.by(Sort.Direction.DESC, sortField);
            } else {
                return Sort.by(Sort.Direction.ASC, sortField);
            }
        }
        // Default sort by timestamp descending
        return Sort.by(Sort.Direction.DESC, EntityFields.CREATED_DATE_TIME);
    }

    /**
     * Build match criteria for aggregation pipeline filtering.
     * Uses exact match or $in for multiple values instead of regex for better performance.
     */
    private Criteria buildMatchCriteria(Set<String> moduleNames, Set<String> flowNames, Set<String> componentNames,
                                        String eventId, long startTime, long endTime, List<String> entityTypes,
                                        boolean harvestReceivedTimestamp) {
        List<Criteria> criteriaList = new ArrayList<>();

        // Module names filter - use exact match or $in
        if (moduleNames != null && !moduleNames.isEmpty()) {
            if (moduleNames.size() == 1) {
                criteriaList.add(Criteria.where(EntityFields.MODULE_NAME).is(moduleNames.stream().findFirst().get()));
            } else {
                criteriaList.add(Criteria.where(EntityFields.MODULE_NAME).in(moduleNames));
            }
        }

        // Flow names filter - use exact match or $in
        if (flowNames != null && !flowNames.isEmpty()) {
            if (flowNames.size() == 1) {
                criteriaList.add(Criteria.where(EntityFields.FLOW_NAME).is(flowNames.stream().findFirst().get()));
            } else {
                criteriaList.add(Criteria.where(EntityFields.FLOW_NAME).in(flowNames));
            }
        }

        // Component names filter - use exact match or $in
        if (componentNames != null && !componentNames.isEmpty()) {
            if (componentNames.size() == 1) {
                criteriaList.add(Criteria.where(EntityFields.COMPONENT_NAME).is(componentNames.stream().findFirst().get()));
            } else {
                criteriaList.add(Criteria.where(EntityFields.COMPONENT_NAME).in(componentNames));
            }
        }

        // Event ID filter - use exact match on either field
        if (eventId != null && !eventId.isEmpty()) {
            criteriaList.add(new Criteria().orOperator(
                Criteria.where(EntityFields.EVENT).is(eventId),
                Criteria.where(EntityFields.ERROR_URI).is(eventId)));
        }

        // Timestamp range filter
        String timeField = harvestReceivedTimestamp ? EntityFields.HARVEST_RECEIVED_TIMESTAMP : EntityFields.CREATED_DATE_TIME;
        if (startTime > 0 && endTime > 0) {
            criteriaList.add(Criteria.where(timeField).gte(startTime).lte(endTime));
        } else if (startTime > 0) {
            criteriaList.add(Criteria.where(timeField).gte(startTime));
        } else if (endTime > 0) {
            criteriaList.add(Criteria.where(timeField).lte(endTime));
        }

        // Entity types filter
        if (entityTypes != null && !entityTypes.isEmpty()) {
            criteriaList.add(Criteria.where(EntityFields.TYPE).in(entityTypes));
        }

        if (criteriaList.isEmpty()) {
            return null;
        } else if (criteriaList.size() == 1) {
            return criteriaList.get(0);
        } else {
            return new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        }
    }

    /**
     * Build MongoDB query from search parameters (without full-text search).
     *
     * @param moduleNames the module names to filter by
     * @param flowNames the flow names to filter by
     * @param componentNames the component names to filter by
     * @param eventId the event ID to filter by
     * @param startTime the start timestamp
     * @param endTime the end timestamp
     * @param entityTypes the entity types to filter by
     * @param harvestReceivedTimestamp whether to use harvest received timestamp for time filtering
     * @return the constructed query
     */
    protected Query buildQuery(Set<String> moduleNames, Set<String> flowNames, Set<String> componentNames,
                               String eventId, long startTime, long endTime, List<String> entityTypes,
                               boolean harvestReceivedTimestamp) {
        Query query = new Query();

        Criteria matchCriteria = buildMatchCriteria(moduleNames, flowNames, componentNames, eventId,
            startTime, endTime, entityTypes, harvestReceivedTimestamp);

        if (matchCriteria != null) {
            query.addCriteria(matchCriteria);
        }

        return query;
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
        query.addCriteria(Criteria.where(EntityFields.EXPIRY).gt(0).lt(currentTime));

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
