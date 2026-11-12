package org.ikasan.mongo.persistence.general.dao;

import com.mongodb.MongoCommandException;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Helper class for executing Atlas Search queries with filtering and pagination.
 * Provides methods to build and execute aggregation pipelines with Atlas $search.
 *
 * <p>This class automatically falls back to regex-based search when Atlas Search
 * is not available (e.g., when running against a non-Atlas MongoDB instance),
 * unless configured to use regex-only mode.
 *
 * <p>Configuration options:
 * <ul>
 *   <li>{@code useAtlasSearch=true} (default): Attempt Atlas Search first, fall back to regex if unavailable</li>
 *   <li>{@code useAtlasSearch=false}: Always use regex-based search (for non-Atlas deployments)</li>
 * </ul>
 *
 * @author Ikasan Development Team
 */
public class AtlasSearchQueryHelper {

    private static final Logger logger = LoggerFactory.getLogger(AtlasSearchQueryHelper.class);

    /**
     * MongoDB error code for Atlas Search not available.
     */
    private static final int ATLAS_SEARCH_NOT_AVAILABLE_ERROR_CODE = 6047401;

    private final MongoTemplate mongoTemplate;
    private String searchIndexName = AtlasSearchAggregationOperation.DEFAULT_INDEX_NAME;
    private boolean atlasSearchAvailable = true;
    private final boolean useAtlasSearch;

    /**
     * Creates an AtlasSearchQueryHelper with Atlas Search enabled by default.
     *
     * @param mongoTemplate the MongoTemplate to use for queries
     */
    public AtlasSearchQueryHelper(MongoTemplate mongoTemplate) {
        this(mongoTemplate, null, true);
    }

    /**
     * Creates an AtlasSearchQueryHelper with a custom search index name.
     *
     * @param mongoTemplate the MongoTemplate to use for queries
     * @param searchIndexName the Atlas Search index name to use
     */
    public AtlasSearchQueryHelper(MongoTemplate mongoTemplate, String searchIndexName) {
        this(mongoTemplate, searchIndexName, true);
    }

    /**
     * Creates an AtlasSearchQueryHelper with full configuration.
     *
     * @param mongoTemplate the MongoTemplate to use for queries
     * @param searchIndexName the Atlas Search index name to use (null for default)
     * @param useAtlasSearch whether to attempt Atlas Search (true) or always use regex (false)
     */
    public AtlasSearchQueryHelper(MongoTemplate mongoTemplate, String searchIndexName, boolean useAtlasSearch) {
        this.mongoTemplate = mongoTemplate;
        this.searchIndexName = searchIndexName != null ? searchIndexName : AtlasSearchAggregationOperation.DEFAULT_INDEX_NAME;
        this.useAtlasSearch = useAtlasSearch;
        if (!useAtlasSearch) {
            logger.info("Atlas Search is disabled by configuration, using regex-based search");
            this.atlasSearchAvailable = false;
        }
    }

    public void setSearchIndexName(String searchIndexName) {
        this.searchIndexName = searchIndexName != null ? searchIndexName : AtlasSearchAggregationOperation.DEFAULT_INDEX_NAME;
    }

    /**
     * Returns whether Atlas Search is configured to be used.
     *
     * @return true if Atlas Search is enabled by configuration
     */
    public boolean isUseAtlasSearch() {
        return useAtlasSearch;
    }

    /**
     * Returns whether Atlas Search is currently available.
     * This may be false even if useAtlasSearch is true, if Atlas returned an error.
     *
     * @return true if Atlas Search is available and can be used
     */
    public boolean isAtlasSearchAvailable() {
        return atlasSearchAvailable;
    }

    /**
     * Execute an Atlas Search query with compound OR wildcard across multiple fields.
     * Falls back to regex-based search if Atlas Search is not available.
     *
     * @param searchString the search string
     * @param searchFields the fields to search in
     * @param matchCriteria additional match criteria (can be null)
     * @param sort the sort specification
     * @param offset the offset for pagination
     * @param limit the maximum number of results
     * @param resultClass the class of results to return
     * @param <T> the result type
     * @return the search results
     */
    public <T> SearchQueryResult<T> searchWithWildcard(String searchString, List<String> searchFields,
                                                        Criteria matchCriteria, Sort sort, int offset, int limit,
                                                        Class<T> resultClass) {
        // If we know Atlas Search is not available, use regex fallback directly
        if (!atlasSearchAvailable) {
            return searchWithRegexFallback(searchString, searchFields, matchCriteria, sort, offset, limit, resultClass);
        }

        try {
            return executeAtlasSearch(searchString, searchFields, matchCriteria, sort, offset, limit, resultClass);
        } catch (UncategorizedMongoDbException e) {
            if (isAtlasSearchNotAvailableError(e)) {
                logger.info("Atlas Search not available, falling back to regex-based search");
                atlasSearchAvailable = false;
                return searchWithRegexFallback(searchString, searchFields, matchCriteria, sort, offset, limit, resultClass);
            }
            throw e;
        }
    }

    /**
     * Execute Atlas Search aggregation pipeline.
     */
    private <T> SearchQueryResult<T> executeAtlasSearch(String searchString, List<String> searchFields,
                                                         Criteria matchCriteria, Sort sort, int offset, int limit,
                                                         Class<T> resultClass) {
        List<AggregationOperation> operations = new ArrayList<>();

        // Add Atlas $search stage as the first stage
        AggregationOperation searchStage = AtlasSearchAggregationOperation.compoundOrWildcardSearch(
            searchIndexName, searchString, searchFields);
        operations.add(searchStage);

        // Add match criteria if provided
        if (matchCriteria != null) {
            operations.add(Aggregation.match(matchCriteria));
        }

        // Add sorting
        if (sort != null) {
            operations.add(Aggregation.sort(sort));
        }

        // Get total count first
        List<AggregationOperation> countOperations = new ArrayList<>(operations);
        countOperations.add(Aggregation.count().as("total"));

        Aggregation countAggregation = Aggregation.newAggregation(countOperations);
        AggregationResults<Document> countResults = mongoTemplate.aggregate(
            countAggregation, resultClass, Document.class);

        long totalCount = 0;
        if (countResults.getMappedResults() != null && !countResults.getMappedResults().isEmpty()) {
            Document countDoc = countResults.getMappedResults().get(0);
            totalCount = countDoc.getInteger("total", 0);
        }

        // Add pagination
        if (offset > 0) {
            operations.add(Aggregation.skip((long) offset));
        }
        if (limit > 0) {
            operations.add(Aggregation.limit(limit));
        }

        // Execute aggregation
        Aggregation aggregation = Aggregation.newAggregation(operations);
        AggregationResults<T> results = mongoTemplate.aggregate(aggregation, resultClass, resultClass);

        return new SearchQueryResult<>(results.getMappedResults(), totalCount);
    }

    /**
     * Fallback search using regex when Atlas Search is not available.
     */
    private <T> SearchQueryResult<T> searchWithRegexFallback(String searchString, List<String> searchFields,
                                                              Criteria matchCriteria, Sort sort, int offset, int limit,
                                                              Class<T> resultClass) {
        Query query = new Query();

        // Build regex search criteria (OR across all search fields)
        String searchPattern = ".*" + Pattern.quote(searchString) + ".*";
        List<Criteria> searchCriteriaList = searchFields.stream()
            .map(field -> Criteria.where(field).regex(searchPattern, "i"))
            .toList();

        Criteria searchCriteria = new Criteria().orOperator(searchCriteriaList.toArray(new Criteria[0]));

        // Combine with match criteria if provided
        if (matchCriteria != null) {
            query.addCriteria(new Criteria().andOperator(searchCriteria, matchCriteria));
        } else {
            query.addCriteria(searchCriteria);
        }

        // Add sorting
        if (sort != null) {
            query.with(sort);
        }

        // Get total count
        long totalCount = mongoTemplate.count(query, resultClass);

        // Apply pagination
        if (offset > 0) {
            query.skip(offset);
        }
        if (limit > 0) {
            query.limit(limit);
        }

        // Execute query
        List<T> results = mongoTemplate.find(query, resultClass);

        return new SearchQueryResult<>(results, totalCount);
    }

    /**
     * Execute an Atlas Search query with text search across multiple fields.
     * Falls back to regex-based search if Atlas Search is not available.
     *
     * @param searchString the search string
     * @param searchFields the fields to search in
     * @param matchCriteria additional match criteria (can be null)
     * @param sort the sort specification
     * @param offset the offset for pagination
     * @param limit the maximum number of results
     * @param resultClass the class of results to return
     * @param <T> the result type
     * @return the search results
     */
    public <T> SearchQueryResult<T> searchWithText(String searchString, List<String> searchFields,
                                                    Criteria matchCriteria, Sort sort, int offset, int limit,
                                                    Class<T> resultClass) {
        // If we know Atlas Search is not available, use regex fallback directly
        if (!atlasSearchAvailable) {
            return searchWithRegexFallback(searchString, searchFields, matchCriteria, sort, offset, limit, resultClass);
        }

        try {
            return executeAtlasTextSearch(searchString, searchFields, matchCriteria, sort, offset, limit, resultClass);
        } catch (UncategorizedMongoDbException e) {
            if (isAtlasSearchNotAvailableError(e)) {
                logger.info("Atlas Search not available, falling back to regex-based search");
                atlasSearchAvailable = false;
                return searchWithRegexFallback(searchString, searchFields, matchCriteria, sort, offset, limit, resultClass);
            }
            throw e;
        }
    }

    /**
     * Execute Atlas text search aggregation pipeline.
     */
    private <T> SearchQueryResult<T> executeAtlasTextSearch(String searchString, List<String> searchFields,
                                                             Criteria matchCriteria, Sort sort, int offset, int limit,
                                                             Class<T> resultClass) {
        List<AggregationOperation> operations = new ArrayList<>();

        // Add Atlas $search stage as the first stage
        AggregationOperation searchStage = AtlasSearchAggregationOperation.textSearch(
            searchIndexName, searchString, searchFields);
        operations.add(searchStage);

        // Add match criteria if provided
        if (matchCriteria != null) {
            operations.add(Aggregation.match(matchCriteria));
        }

        // Add sorting
        if (sort != null) {
            operations.add(Aggregation.sort(sort));
        }

        // Get total count first
        List<AggregationOperation> countOperations = new ArrayList<>(operations);
        countOperations.add(Aggregation.count().as("total"));

        Aggregation countAggregation = Aggregation.newAggregation(countOperations);
        AggregationResults<Document> countResults = mongoTemplate.aggregate(
            countAggregation, resultClass, Document.class);

        long totalCount = 0;
        if (countResults.getMappedResults() != null && !countResults.getMappedResults().isEmpty()) {
            Document countDoc = countResults.getMappedResults().get(0);
            totalCount = countDoc.getInteger("total", 0);
        }

        // Add pagination
        if (offset > 0) {
            operations.add(Aggregation.skip((long) offset));
        }
        if (limit > 0) {
            operations.add(Aggregation.limit(limit));
        }

        // Execute aggregation
        Aggregation aggregation = Aggregation.newAggregation(operations);
        AggregationResults<T> results = mongoTemplate.aggregate(aggregation, resultClass, resultClass);

        return new SearchQueryResult<>(results.getMappedResults(), totalCount);
    }

    /**
     * Check if the exception indicates Atlas Search is not available.
     */
    private boolean isAtlasSearchNotAvailableError(UncategorizedMongoDbException e) {
        Throwable cause = e.getCause();
        if (cause instanceof MongoCommandException) {
            MongoCommandException mce = (MongoCommandException) cause;
            return mce.getErrorCode() == ATLAS_SEARCH_NOT_AVAILABLE_ERROR_CODE;
        }
        return false;
    }

    /**
     * Result container for search queries.
     */
    public static class SearchQueryResult<T> {
        private final List<T> results;
        private final long totalCount;

        public SearchQueryResult(List<T> results, long totalCount) {
            this.results = results;
            this.totalCount = totalCount;
        }

        public List<T> getResults() {
            return results;
        }

        public long getTotalCount() {
            return totalCount;
        }
    }
}
