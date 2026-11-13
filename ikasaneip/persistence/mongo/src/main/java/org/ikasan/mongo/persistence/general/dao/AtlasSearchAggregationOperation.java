package org.ikasan.mongo.persistence.general.dao;

import org.bson.Document;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperationContext;

import java.util.List;

/**
 * Custom AggregationOperation for MongoDB Atlas Lucene $search.
 * This enables full-text search using Atlas Search indexes instead of the basic $text operator.
 *
 * <p>Atlas Search provides more powerful search capabilities including:
 * <ul>
 *   <li>Fuzzy matching</li>
 *   <li>Autocomplete</li>
 *   <li>Scoring and relevance ranking</li>
 *   <li>Compound queries</li>
 * </ul>
 *
 * @author Ikasan Development Team
 */
public class AtlasSearchAggregationOperation implements AggregationOperation {

    public static final String DEFAULT_INDEX_NAME = "default";

    private final Document searchDocument;

    /**
     * Creates a new Atlas Search aggregation operation.
     *
     * @param searchDocument the $search stage document
     */
    public AtlasSearchAggregationOperation(Document searchDocument) {
        this.searchDocument = searchDocument;
    }

    /**
     * Creates a {@code $search} stage with a {@code compound} operator and optional in-index sort and count.
     * This is the preferred builder: all filtering, sorting and counting happen inside the search index, so no
     * {@code $match}/{@code $sort}/{@code $count} stages are needed after {@code $search}.
     *
     * @param indexName the name of the Atlas Search index
     * @param compound the compound operator body ({@code must}/{@code mustNot}/{@code should}/{@code filter})
     * @param sort the search sort document, e.g. {@code {timestamp: -1}} (null for relevance order)
     * @param countType {@code "total"} or {@code "lowerBound"} to populate {@code $$SEARCH_META.count}, or null
     * @return the AtlasSearchAggregationOperation
     */
    public static AtlasSearchAggregationOperation compound(String indexName, Document compound, Document sort,
                                                           String countType) {
        Document searchStage = new Document()
            .append("index", indexName != null ? indexName : DEFAULT_INDEX_NAME)
            .append("compound", compound);
        if (sort != null && !sort.isEmpty()) {
            searchStage.append("sort", sort);
        }
        if (countType != null) {
            searchStage.append("count", new Document("type", countType));
        }
        return new AtlasSearchAggregationOperation(new Document("$search", searchStage));
    }

    /**
     * @return the {@code $search} stage document
     */
    public Document getSearchDocument() {
        return searchDocument;
    }

    /**
     * Creates a text search operation for the specified query and fields.
     *
     * @param indexName the name of the Atlas Search index
     * @param searchQuery the search query string
     * @param searchFields the fields to search in
     * @return the AtlasSearchAggregationOperation
     */
    public static AtlasSearchAggregationOperation textSearch(String indexName, String searchQuery, List<String> searchFields) {
        Document textSearch = new Document("text", new Document()
            .append("query", searchQuery)
            .append("path", searchFields));

        Document searchStage = new Document()
            .append("index", indexName != null ? indexName : DEFAULT_INDEX_NAME)
            .append("text", textSearch.get("text"));

        return new AtlasSearchAggregationOperation(new Document("$search", searchStage));
    }

    /**
     * Creates a text search operation with fuzzy matching.
     *
     * @param indexName the name of the Atlas Search index
     * @param searchQuery the search query string
     * @param searchFields the fields to search in
     * @param maxEdits the maximum number of single-character edits for fuzzy matching (1 or 2)
     * @return the AtlasSearchAggregationOperation
     */
    public static AtlasSearchAggregationOperation fuzzyTextSearch(String indexName, String searchQuery,
                                                                   List<String> searchFields, int maxEdits) {
        Document fuzzy = new Document("maxEdits", Math.min(Math.max(maxEdits, 1), 2));

        Document textSearch = new Document()
            .append("query", searchQuery)
            .append("path", searchFields)
            .append("fuzzy", fuzzy);

        Document searchStage = new Document()
            .append("index", indexName != null ? indexName : DEFAULT_INDEX_NAME)
            .append("text", textSearch);

        return new AtlasSearchAggregationOperation(new Document("$search", searchStage));
    }

    /**
     * Creates a wildcard search operation.
     *
     * @param indexName the name of the Atlas Search index
     * @param searchQuery the wildcard search query (supports * and ? wildcards)
     * @param searchFields the fields to search in
     * @return the AtlasSearchAggregationOperation
     */
    public static AtlasSearchAggregationOperation wildcardSearch(String indexName, String searchQuery,
                                                                  List<String> searchFields) {
        Document wildcardSearch = new Document()
            .append("query", searchQuery)
            .append("path", searchFields)
            .append("allowAnalyzedField", true);

        Document searchStage = new Document()
            .append("index", indexName != null ? indexName : DEFAULT_INDEX_NAME)
            .append("wildcard", wildcardSearch);

        return new AtlasSearchAggregationOperation(new Document("$search", searchStage));
    }

    /**
     * Creates a phrase search operation for exact phrase matching.
     *
     * @param indexName the name of the Atlas Search index
     * @param searchQuery the phrase to search for
     * @param searchFields the fields to search in
     * @return the AtlasSearchAggregationOperation
     */
    public static AtlasSearchAggregationOperation phraseSearch(String indexName, String searchQuery,
                                                                List<String> searchFields) {
        Document phraseSearch = new Document()
            .append("query", searchQuery)
            .append("path", searchFields);

        Document searchStage = new Document()
            .append("index", indexName != null ? indexName : DEFAULT_INDEX_NAME)
            .append("phrase", phraseSearch);

        return new AtlasSearchAggregationOperation(new Document("$search", searchStage));
    }

    /**
     * Creates a regex search operation.
     *
     * @param indexName the name of the Atlas Search index
     * @param pattern the regex pattern to search for
     * @param searchFields the fields to search in
     * @return the AtlasSearchAggregationOperation
     */
    public static AtlasSearchAggregationOperation regexSearch(String indexName, String pattern,
                                                               List<String> searchFields) {
        Document regexSearch = new Document()
            .append("query", pattern)
            .append("path", searchFields)
            .append("allowAnalyzedField", true);

        Document searchStage = new Document()
            .append("index", indexName != null ? indexName : DEFAULT_INDEX_NAME)
            .append("regex", regexSearch);

        return new AtlasSearchAggregationOperation(new Document("$search", searchStage));
    }

    /**
     * Creates a compound OR search across multiple fields with wildcard matching.
     * Useful for case-insensitive substring matching like SQL LIKE '%value%'.
     *
     * @param indexName the name of the Atlas Search index
     * @param searchQuery the search query (will be wrapped with wildcards)
     * @param searchFields the fields to search in (OR across all)
     * @return the AtlasSearchAggregationOperation
     */
    public static AtlasSearchAggregationOperation compoundOrWildcardSearch(String indexName, String searchQuery,
                                                                            List<String> searchFields) {
        // Wrap query with wildcards if not already wrapped
        final String wildcardQuery = wrapWithWildcards(searchQuery);

        // Build should clauses for each field (OR logic)
        List<Document> shouldClauses = searchFields.stream()
            .map(field -> new Document("wildcard", new Document()
                .append("query", wildcardQuery)
                .append("path", field)
                .append("allowAnalyzedField", true)))
            .toList();

        Document compound = new Document()
            .append("should", shouldClauses)
            .append("minimumShouldMatch", 1);

        Document searchStage = new Document()
            .append("index", indexName != null ? indexName : DEFAULT_INDEX_NAME)
            .append("compound", compound);

        return new AtlasSearchAggregationOperation(new Document("$search", searchStage));
    }

    /**
     * Creates a compound OR text search across multiple fields.
     * Uses text search which is more flexible than wildcard for full-text.
     *
     * @param indexName the name of the Atlas Search index
     * @param searchQuery the search query
     * @param searchFields the fields to search in (OR across all)
     * @return the AtlasSearchAggregationOperation
     */
    public static AtlasSearchAggregationOperation compoundOrTextSearch(String indexName, String searchQuery,
                                                                        List<String> searchFields) {
        // Build should clauses for each field (OR logic)
        List<Document> shouldClauses = searchFields.stream()
            .map(field -> new Document("text", new Document()
                .append("query", searchQuery)
                .append("path", field)))
            .toList();

        Document compound = new Document()
            .append("should", shouldClauses)
            .append("minimumShouldMatch", 1);

        Document searchStage = new Document()
            .append("index", indexName != null ? indexName : DEFAULT_INDEX_NAME)
            .append("compound", compound);

        return new AtlasSearchAggregationOperation(new Document("$search", searchStage));
    }

    /**
     * Creates a single-field wildcard search for case-insensitive substring matching.
     *
     * @param indexName the name of the Atlas Search index
     * @param searchQuery the search query (will be wrapped with wildcards)
     * @param field the field to search in
     * @return the AtlasSearchAggregationOperation
     */
    public static AtlasSearchAggregationOperation singleFieldWildcardSearch(String indexName, String searchQuery,
                                                                             String field) {
        // Wrap query with wildcards if not already wrapped
        final String wildcardQuery = wrapWithWildcards(searchQuery);

        Document wildcardSearch = new Document()
            .append("query", wildcardQuery)
            .append("path", field)
            .append("allowAnalyzedField", true);

        Document searchStage = new Document()
            .append("index", indexName != null ? indexName : DEFAULT_INDEX_NAME)
            .append("wildcard", wildcardSearch);

        return new AtlasSearchAggregationOperation(new Document("$search", searchStage));
    }

    /**
     * Creates a compound AND (must) search with field-specific wildcard clauses.
     * Each entry in fieldValuePairs maps a field name to the value to search for in that field.
     * All conditions must match (AND logic).
     *
     * @param indexName the name of the Atlas Search index
     * @param fieldValuePairs list of field-value pairs to search
     * @return the AtlasSearchAggregationOperation
     */
    public static AtlasSearchAggregationOperation compoundMustWildcardSearch(String indexName,
                                                                               List<FieldValuePair> fieldValuePairs) {
        // Build must clauses for each field-value pair (AND logic)
        List<Document> mustClauses = fieldValuePairs.stream()
            .map(pair -> new Document("wildcard", new Document()
                .append("query", wrapWithWildcards(pair.value()))
                .append("path", pair.field())
                .append("allowAnalyzedField", true)))
            .toList();

        Document compound = new Document()
            .append("must", mustClauses);

        Document searchStage = new Document()
            .append("index", indexName != null ? indexName : DEFAULT_INDEX_NAME)
            .append("compound", compound);

        return new AtlasSearchAggregationOperation(new Document("$search", searchStage));
    }

    /**
     * Helper method to wrap a search query with wildcards for substring matching.
     *
     * @param searchQuery the search query
     * @return the query wrapped with wildcards
     */
    private static String wrapWithWildcards(String searchQuery) {
        String result = searchQuery;
        if (!searchQuery.startsWith("*")) {
            result = "*" + result;
        }
        if (!searchQuery.endsWith("*")) {
            result = result + "*";
        }
        return result;
    }

    /**
     * Record for field-value pairs used in compound searches.
     */
    public record FieldValuePair(String field, String value) {}

    @Override
    public Document toDocument(AggregationOperationContext context) {
        return searchDocument;
    }
}
