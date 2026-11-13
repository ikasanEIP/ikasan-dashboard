package org.ikasan.mongo.persistence.general.dao;

import com.mongodb.MongoCommandException;
import org.bson.Document;
import org.ikasan.spec.entity.EntityFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * Executes searches described by an {@link AtlasSearchFilter}.
 *
 * <p>When Atlas Search is available the whole request &ndash; search terms, filters, sort and total count &ndash; is
 * pushed into a single {@code $search} stage so that it is resolved inside the Lucene index (mongot):
 * <pre>
 * { $search: { index, compound: { must: [terms], filter: [type/module/flow/time...] },
 *              sort: { timestamp: -1 }, count: { type: "total" } } }
 * { $skip: offset } { $limit: limit }
 * { $set: { _searchMeta: "$$SEARCH_META" } }      // only when a total count is requested
 * </pre>
 * No {@code $match} or {@code $sort} stage is ever placed after {@code $search}: mongod only fetches the documents of
 * the requested page.
 *
 * <p>If Atlas Search is disabled by configuration or the server rejects {@code $search} (non-Atlas deployment), the
 * same filter is executed as a standard query with case-insensitive regexes via {@link AtlasSearchFilter#toCriteria()}.
 * The standard path is also used when the requested sort field is not sortable in the search index.
 */
public class AtlasSearchQueryHelper {

    private static final Logger logger = LoggerFactory.getLogger(AtlasSearchQueryHelper.class);

    /**
     * MongoDB error code returned when {@code $search} is used against a non-Atlas deployment.
     */
    private static final int ATLAS_SEARCH_NOT_AVAILABLE_ERROR_CODE = 6047401;

    private static final String SEARCH_META_FIELD = "_searchMeta";

    /**
     * Fields indexed as sortable types (token / number / date / boolean) in
     * {@code src/main/resources/mongo/indexes/ikasan.search.json}. Sorting on any other field falls back to the
     * standard query path, because {@code $search} silently mis-orders documents on unindexed sort fields.
     */
    public static final Set<String> DEFAULT_SORTABLE_FIELDS = Set.of(
        EntityFields.TYPE,
        EntityFields.MODULE_NAME,
        EntityFields.FLOW_NAME,
        EntityFields.COMPONENT_NAME,
        EntityFields.CREATED_DATE_TIME,
        EntityFields.UPDATED_DATE_TIME,
        EntityFields.EXPIRY,
        EntityFields.ERROR_URI,
        EntityFields.STATUS,
        EntityFields.START_TIME,
        EntityFields.END_TIME,
        EntityFields.DISPLAY_NAME,
        EntityFields.CHILD_CONTEXT_NAME,
        EntityFields.ACTOR,
        EntityFields.SYSTEM_EVENT_SUBJECT,
        EntityFields.SYSTEM_EVENT_ACTION,
        "harvestReceivedTimestamp",
        "fireTime",
        "agentName",
        "jobName",
        "jobGroup"
    );

    private final MongoTemplate mongoTemplate;
    private final boolean useAtlasSearch;
    private String searchIndexName;
    private Set<String> sortableFields = DEFAULT_SORTABLE_FIELDS;
    private volatile boolean atlasSearchAvailable;
    private final Map<Class<?>, UnaryOperator<String>> fieldPropertyNames = new ConcurrentHashMap<>();

    /**
     * Creates a helper with Atlas Search enabled and the default index name.
     */
    public AtlasSearchQueryHelper(MongoTemplate mongoTemplate) {
        this(mongoTemplate, null, true);
    }

    /**
     * Creates a helper with Atlas Search enabled and a custom index name.
     */
    public AtlasSearchQueryHelper(MongoTemplate mongoTemplate, String searchIndexName) {
        this(mongoTemplate, searchIndexName, true);
    }

    /**
     * @param mongoTemplate the template to execute queries with
     * @param searchIndexName the Atlas Search index name ({@code null} for {@code default})
     * @param useAtlasSearch whether to attempt Atlas Search (true) or always use the regex path (false)
     */
    public AtlasSearchQueryHelper(MongoTemplate mongoTemplate, String searchIndexName, boolean useAtlasSearch) {
        this.mongoTemplate = mongoTemplate;
        this.searchIndexName = searchIndexName != null ? searchIndexName : AtlasSearchAggregationOperation.DEFAULT_INDEX_NAME;
        this.useAtlasSearch = useAtlasSearch;
        this.atlasSearchAvailable = useAtlasSearch;
        if (!useAtlasSearch) {
            logger.info("Atlas Search is disabled by configuration, using regex-based search");
        }
    }

    public void setSearchIndexName(String searchIndexName) {
        this.searchIndexName = searchIndexName != null ? searchIndexName : AtlasSearchAggregationOperation.DEFAULT_INDEX_NAME;
    }

    public String getSearchIndexName() {
        return searchIndexName;
    }

    /**
     * Override the set of fields that are sortable inside {@code $search} (must match the search index definition).
     */
    public void setSortableFields(Set<String> sortableFields) {
        this.sortableFields = sortableFields != null ? Set.copyOf(sortableFields) : DEFAULT_SORTABLE_FIELDS;
    }

    public boolean isUseAtlasSearch() {
        return useAtlasSearch;
    }

    /**
     * @return false if Atlas Search is disabled by configuration or the server has rejected {@code $search}
     */
    public boolean isAtlasSearchAvailable() {
        return atlasSearchAvailable;
    }

    /**
     * Execute a search, returning the requested page and the total number of matches.
     *
     * @see #search(AtlasSearchFilter, Sort, int, int, boolean, Class)
     */
    public <T> SearchQueryResult<T> search(AtlasSearchFilter filter, Sort sort, int offset, int limit,
                                           Class<T> resultClass) {
        return search(filter, sort, offset, limit, true, resultClass);
    }

    /**
     * Execute a search.
     *
     * <p>Atlas Search is used when the filter carries search terms; filters without terms are cheaper as a standard
     * B-tree indexed query and are executed that way.
     *
     * @param filter the filter (terms + restrictions)
     * @param sort the sort, or {@code null}/unsorted for relevance order (Atlas) / natural order (standard)
     * @param offset number of matches to skip ({@code <= 0} for none)
     * @param limit maximum number of matches to return ({@code <= 0} for no limit)
     * @param includeCount whether to compute the total number of matches
     * @param resultClass the entity class to map results to
     * @return the page and, if requested, the total count (otherwise {@code -1})
     */
    public <T> SearchQueryResult<T> search(AtlasSearchFilter filter, Sort sort, int offset, int limit,
                                           boolean includeCount, Class<T> resultClass) {
        if (filter.hasTextClauses() && atlasSearchAvailable) {
            if (!isSortableInSearch(sort)) {
                logger.debug("Sort [{}] is not sortable in the Atlas Search index, using standard query", sort);
            } else {
                try {
                    return executeAtlasSearch(filter, sort, offset, limit, includeCount, resultClass);
                } catch (UncategorizedMongoDbException e) {
                    if (!isAtlasSearchNotAvailableError(e)) {
                        throw e;
                    }
                    logger.info("Atlas Search not available, falling back to regex-based search");
                    atlasSearchAvailable = false;
                }
            }
        }
        return executeStandardQuery(filter, sort, offset, limit, includeCount, resultClass);
    }

    /**
     * Build the {@code $search} stage for the given filter. Exposed for tests and for callers that need to compose
     * further pipeline stages (which must not filter or sort).
     */
    public AtlasSearchAggregationOperation buildSearchStage(AtlasSearchFilter filter, Sort sort, boolean includeCount) {
        return AtlasSearchAggregationOperation.compound(searchIndexName, filter.toAtlasCompound(),
            toSearchSort(sort), includeCount ? "total" : null);
    }

    private <T> SearchQueryResult<T> executeAtlasSearch(AtlasSearchFilter filter, Sort sort, int offset, int limit,
                                                        boolean includeCount, Class<T> resultClass) {
        List<AggregationOperation> operations = new ArrayList<>();
        operations.add(buildSearchStage(filter, sort, includeCount));
        if (offset > 0) {
            operations.add(Aggregation.skip((long) offset));
        }
        if (limit > 0) {
            operations.add(Aggregation.limit(limit));
        }
        if (includeCount) {
            operations.add(context -> new Document("$set", new Document(SEARCH_META_FIELD, "$$SEARCH_META")));
        }

        Aggregation aggregation = Aggregation.newAggregation(operations);
        logger.debug("Executing Atlas Search: {}", aggregation);

        List<Document> rawResults = mongoTemplate.aggregate(aggregation, resultClass, Document.class).getMappedResults();

        long totalCount = -1;
        List<T> results = new ArrayList<>(rawResults.size());
        for (Document raw : rawResults) {
            Object meta = raw.remove(SEARCH_META_FIELD);
            if (totalCount < 0 && meta instanceof Document metaDoc) {
                totalCount = readTotal(metaDoc);
            }
            results.add(mongoTemplate.getConverter().read(resultClass, raw));
        }

        if (includeCount && totalCount < 0) {
            // empty page: either nothing matched, or the offset is past the end -> ask the index only
            totalCount = offset > 0 ? executeSearchMetaCount(filter, resultClass) : 0;
        }
        return new SearchQueryResult<>(results, totalCount);
    }

    private long executeSearchMetaCount(AtlasSearchFilter filter, Class<?> resultClass) {
        Document searchMeta = new Document("index", searchIndexName)
            .append("compound", filter.toAtlasCompound())
            .append("count", new Document("type", "total"));
        Aggregation aggregation = Aggregation.newAggregation(
            (AggregationOperation) context -> new Document("$searchMeta", searchMeta));

        List<Document> meta = mongoTemplate.aggregate(aggregation, resultClass, Document.class).getMappedResults();
        return meta.isEmpty() ? 0 : readTotal(meta.get(0));
    }

    private <T> SearchQueryResult<T> executeStandardQuery(AtlasSearchFilter filter, Sort sort, int offset, int limit,
                                                          boolean includeCount, Class<T> resultClass) {
        UnaryOperator<String> toProperty = storedNameToPropertyName(resultClass);
        Criteria criteria = filter.toCriteria(toProperty);
        Query query = criteria != null ? new Query(criteria) : new Query();
        if (sort != null && sort.isSorted()) {
            query.with(Sort.by(sort.stream()
                .map(order -> order.withProperty(toProperty.apply(order.getProperty())))
                .toList()));
        }

        long totalCount = includeCount ? mongoTemplate.count(query, resultClass) : -1;

        if (offset > 0) {
            query.skip(offset);
        }
        if (limit > 0) {
            query.limit(limit);
        }
        return new SearchQueryResult<>(mongoTemplate.find(query, resultClass), totalCount);
    }

    /**
     * Filters and sorts are expressed with stored field names (the same names Atlas Search uses), while Spring's
     * query mapper resolves keys as entity property names. Where an entity stores a property under a different name
     * (e.g. {@code eventId} stored as {@code event}), translate the stored name to the property name so the mapper
     * resolves it directly instead of guessing; this matters when the stored name collides with an unrelated bean
     * property such as an interface {@code getEvent()} accessor.
     */
    private UnaryOperator<String> storedNameToPropertyName(Class<?> entityClass) {
        return fieldPropertyNames.computeIfAbsent(entityClass, type -> {
            MongoPersistentEntity<?> entity = mongoTemplate.getConverter().getMappingContext().getPersistentEntity(type);
            if (entity == null) {
                return UnaryOperator.identity();
            }
            Map<String, String> renamed = new HashMap<>();
            entity.doWithProperties((PropertyHandler<MongoPersistentProperty>) property -> {
                if (!property.getFieldName().equals(property.getName())) {
                    renamed.put(property.getFieldName(), property.getName());
                }
            });
            if (renamed.isEmpty()) {
                return UnaryOperator.identity();
            }
            return name -> renamed.getOrDefault(name, name);
        });
    }

    private boolean isSortableInSearch(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return true;
        }
        return sort.stream().allMatch(order -> sortableFields.contains(order.getProperty()));
    }

    private static Document toSearchSort(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return null;
        }
        Document searchSort = new Document();
        sort.forEach(order -> searchSort.append(order.getProperty(), order.isAscending() ? 1 : -1));
        return searchSort;
    }

    private static long readTotal(Document searchMeta) {
        Object count = searchMeta.get("count");
        if (count instanceof Document countDoc) {
            Object total = countDoc.containsKey("total") ? countDoc.get("total") : countDoc.get("lowerBound");
            if (total instanceof Number number) {
                return number.longValue();
            }
        }
        return 0;
    }

    private static boolean isAtlasSearchNotAvailableError(UncategorizedMongoDbException e) {
        return e.getCause() instanceof MongoCommandException mce
            && mce.getErrorCode() == ATLAS_SEARCH_NOT_AVAILABLE_ERROR_CODE;
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

        /**
         * @return the total number of matches, or {@code -1} if the count was not requested
         */
        public long getTotalCount() {
            return totalCount;
        }
    }
}
