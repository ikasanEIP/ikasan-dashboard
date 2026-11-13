package org.ikasan.mongo.persistence.general.dao;

import org.bson.Document;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * A single, store-agnostic description of a search filter that can be rendered either as the clauses of an
 * Atlas Search {@code compound} operator (so that <em>all</em> filtering happens inside the {@code $search}
 * stage, in the Lucene index) or as a Spring Data {@link Criteria} (for the regex fallback used when Atlas
 * Search is not available, and for plain queries without a search term).
 *
 * <p>Clauses are split into two groups:
 * <ul>
 *   <li><b>filter</b> clauses (equals, in, range, exists, anyOf) are exact, non-scoring restrictions and are
 *   rendered into {@code compound.filter};</li>
 *   <li><b>text</b> clauses (contains, containsAny, text) are user-entered search terms and are rendered into
 *   {@code compound.must} (or {@code compound.mustNot} when the filter is negated).</li>
 * </ul>
 *
 * <p>Builder methods silently ignore {@code null}/empty values so DAOs do not need to guard every
 * optional filter field.
 *
 * <p>Index requirements: string fields used with {@code equals}/{@code in}/{@code anyOf} must be indexed with the
 * Atlas {@code token} type, numeric/date fields used with {@code range} with the {@code number}/{@code date}
 * types, and fields used with {@code contains} with a lower-casing analyzer. See
 * {@code src/main/resources/mongo/indexes/ikasan.search.json}.
 */
public final class AtlasSearchFilter {

    private final List<Clause> filterClauses;
    private final List<Clause> textClauses;
    private final boolean negateText;

    private AtlasSearchFilter(List<Clause> filterClauses, List<Clause> textClauses, boolean negateText) {
        this.filterClauses = Collections.unmodifiableList(filterClauses);
        this.textClauses = Collections.unmodifiableList(textClauses);
        this.negateText = negateText;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return true if this filter carries at least one user search term (and therefore benefits from Atlas Search)
     */
    public boolean hasTextClauses() {
        return !textClauses.isEmpty();
    }

    public boolean hasFilterClauses() {
        return !filterClauses.isEmpty();
    }

    public boolean isNegateText() {
        return negateText;
    }

    /**
     * @return the Atlas Search operators for {@code compound.filter}
     */
    public List<Document> toAtlasFilterClauses() {
        return filterClauses.stream().map(Clause::toAtlas).toList();
    }

    /**
     * @return the Atlas Search operators for {@code compound.must} (or {@code mustNot} when negated)
     */
    public List<Document> toAtlasTextClauses() {
        return textClauses.stream().map(Clause::toAtlas).toList();
    }

    /**
     * Render the {@code compound} operator body for this filter.
     *
     * @return the compound document, e.g. {@code {must:[...], filter:[...]}}
     */
    public Document toAtlasCompound() {
        Document compound = new Document();
        List<Document> text = toAtlasTextClauses();
        List<Document> filter = new ArrayList<>(toAtlasFilterClauses());

        if (!text.isEmpty()) {
            compound.append(negateText ? "mustNot" : "must", text);
        }
        // compound needs at least one positive clause; with only mustNot (or nothing) match every indexed document
        if (filter.isEmpty() && (text.isEmpty() || negateText)) {
            filter.add(ExistsClause.MATCH_ALL.toAtlas());
        }
        if (!filter.isEmpty()) {
            compound.append("filter", filter);
        }
        return compound;
    }

    /**
     * Render this filter as Spring Data criteria for the non-Atlas code paths.
     *
     * @return the criteria, or {@code null} if the filter is empty
     */
    public Criteria toCriteria() {
        return toCriteria(UnaryOperator.identity());
    }

    /**
     * Render this filter as Spring Data criteria, translating each stored field name with {@code paths} (e.g. to the
     * entity property name, so Spring's query mapper can resolve it without falling back).
     *
     * @param paths maps a stored field name to the key used in the criteria
     * @return the criteria, or {@code null} if the filter is empty
     */
    public Criteria toCriteria(UnaryOperator<String> paths) {
        List<Criteria> criteria = new ArrayList<>();
        filterClauses.forEach(clause -> criteria.add(clause.toCriteria(paths)));

        if (!textClauses.isEmpty()) {
            Criteria[] text = textClauses.stream().map(clause -> clause.toCriteria(paths)).toArray(Criteria[]::new);
            if (negateText) {
                criteria.add(new Criteria().norOperator(text));
            } else {
                criteria.addAll(List.of(text));
            }
        }

        if (criteria.isEmpty()) {
            return null;
        }
        if (criteria.size() == 1) {
            return criteria.get(0);
        }
        return new Criteria().andOperator(criteria.toArray(new Criteria[0]));
    }

    @Override
    public String toString() {
        return toAtlasCompound().toJson();
    }

    // ------------------------------------------------------------------------------------------------------------
    // Clause factories (usable with Builder.anyOf)
    // ------------------------------------------------------------------------------------------------------------

    public static Clause eq(String path, Object value) {
        return new EqualsClause(path, value);
    }

    public static Clause in(String path, Collection<?> values) {
        return new InClause(path, List.copyOf(values));
    }

    // ------------------------------------------------------------------------------------------------------------
    // Builder
    // ------------------------------------------------------------------------------------------------------------

    public static final class Builder {
        private final List<Clause> filterClauses = new ArrayList<>();
        private final List<Clause> textClauses = new ArrayList<>();
        private boolean negateText;

        private Builder() {
        }

        /**
         * Exact match. Ignored if the value is {@code null} or an empty string.
         */
        public Builder equals(String path, Object value) {
            if (value != null && !(value instanceof String s && s.isEmpty())) {
                filterClauses.add(new EqualsClause(path, value));
            }
            return this;
        }

        /**
         * Match any of the given values. Ignored if the collection is {@code null} or empty.
         */
        public Builder in(String path, Collection<?> values) {
            if (values != null && !values.isEmpty()) {
                filterClauses.add(values.size() == 1
                    ? new EqualsClause(path, values.iterator().next())
                    : new InClause(path, List.copyOf(values)));
            }
            return this;
        }

        /**
         * Inclusive range on an epoch-millisecond field; a bound {@code <= 0} is treated as open.
         * Ignored if both bounds are open.
         */
        public Builder rangeMillis(String path, long from, long to) {
            return range(path, from > 0 ? from : null, to > 0 ? to : null);
        }

        /**
         * Inclusive range; a {@code null} bound is treated as open. Ignored if both bounds are {@code null}.
         * Bounds may be {@link Number} or {@link java.util.Date} and must match the stored/indexed type.
         */
        public Builder range(String path, Object gte, Object lte) {
            if (gte != null || lte != null) {
                filterClauses.add(new RangeClause(path, gte, lte));
            }
            return this;
        }

        /**
         * Match documents where at least one of the given clauses matches.
         */
        public Builder anyOf(Clause... clauses) {
            if (clauses != null && clauses.length > 0) {
                filterClauses.add(clauses.length == 1 ? clauses[0] : new AnyOfClause(List.of(clauses)));
            }
            return this;
        }

        public Builder anyOf(List<Clause> clauses) {
            return anyOf(clauses.toArray(new Clause[0]));
        }

        /**
         * Case-insensitive substring match on a single field (SQL {@code LIKE '%term%'}).
         * Multiple contains clauses are AND'ed. Ignored if the term is blank.
         */
        public Builder contains(String path, String term) {
            if (term != null && !term.isBlank()) {
                textClauses.add(new WildcardClause(List.of(path), term.trim()));
            }
            return this;
        }

        /**
         * Case-insensitive substring match against any of the given fields. Ignored if the term is blank.
         */
        public Builder containsAny(List<String> paths, String term) {
            if (term != null && !term.isBlank() && paths != null && !paths.isEmpty()) {
                textClauses.add(new WildcardClause(List.copyOf(paths), term.trim()));
            }
            return this;
        }

        /**
         * Full-text (analysed token) match against any of the given fields. Ignored if the term is blank.
         */
        public Builder text(List<String> paths, String term) {
            if (term != null && !term.isBlank() && paths != null && !paths.isEmpty()) {
                textClauses.add(new TextClause(List.copyOf(paths), term.trim()));
            }
            return this;
        }

        /**
         * When true, documents matching the text clauses are excluded rather than included.
         */
        public Builder negateText(boolean negate) {
            this.negateText = negate;
            return this;
        }

        public AtlasSearchFilter build() {
            return new AtlasSearchFilter(new ArrayList<>(filterClauses), new ArrayList<>(textClauses), negateText);
        }
    }

    // ------------------------------------------------------------------------------------------------------------
    // Clauses
    // ------------------------------------------------------------------------------------------------------------

    /**
     * A single search restriction renderable for both Atlas Search and standard MongoDB queries.
     */
    public sealed interface Clause permits EqualsClause, InClause, RangeClause, AnyOfClause, ExistsClause,
        WildcardClause, TextClause {

        Document toAtlas();

        Criteria toCriteria(UnaryOperator<String> paths);
    }

    record EqualsClause(String path, Object value) implements Clause {
        @Override
        public Document toAtlas() {
            return new Document("equals", new Document("path", path).append("value", value));
        }

        @Override
        public Criteria toCriteria(UnaryOperator<String> paths) {
            return Criteria.where(paths.apply(path)).is(value);
        }
    }

    record InClause(String path, List<?> values) implements Clause {
        @Override
        public Document toAtlas() {
            return new Document("in", new Document("path", path).append("value", values));
        }

        @Override
        public Criteria toCriteria(UnaryOperator<String> paths) {
            return Criteria.where(paths.apply(path)).in(values);
        }
    }

    record RangeClause(String path, Object gte, Object lte) implements Clause {
        @Override
        public Document toAtlas() {
            Document range = new Document("path", path);
            if (gte != null) {
                range.append("gte", gte);
            }
            if (lte != null) {
                range.append("lte", lte);
            }
            return new Document("range", range);
        }

        @Override
        public Criteria toCriteria(UnaryOperator<String> paths) {
            Criteria criteria = Criteria.where(paths.apply(path));
            if (gte != null) {
                criteria = criteria.gte(gte);
            }
            if (lte != null) {
                criteria = criteria.lte(lte);
            }
            return criteria;
        }
    }

    record AnyOfClause(List<Clause> clauses) implements Clause {
        @Override
        public Document toAtlas() {
            return new Document("compound", new Document()
                .append("should", clauses.stream().map(Clause::toAtlas).toList())
                .append("minimumShouldMatch", 1));
        }

        @Override
        public Criteria toCriteria(UnaryOperator<String> paths) {
            return new Criteria().orOperator(clauses.stream()
                .map(clause -> clause.toCriteria(paths)).toArray(Criteria[]::new));
        }
    }

    record ExistsClause(String path) implements Clause {
        /**
         * Every Ikasan document carries a discriminating {@code type}, which is always indexed.
         */
        static final ExistsClause MATCH_ALL = new ExistsClause("type");

        @Override
        public Document toAtlas() {
            return new Document("exists", new Document("path", path));
        }

        @Override
        public Criteria toCriteria(UnaryOperator<String> paths) {
            return Criteria.where(paths.apply(path)).exists(true);
        }
    }

    /**
     * Case-insensitive substring match. Rendered as an Atlas {@code wildcard} query {@code *term*} with the term
     * lower-cased (fields are indexed with lower-casing analyzers) and wildcard metacharacters escaped, so the
     * semantics match the regex fallback ({@link RegexCriteria#containsIgnoreCase}).
     */
    record WildcardClause(List<String> paths, String term) implements Clause {
        @Override
        public Document toAtlas() {
            return new Document("wildcard", new Document()
                .append("path", paths.size() == 1 ? paths.get(0) : paths)
                .append("query", "*" + escapeWildcard(term.toLowerCase(Locale.ROOT)) + "*")
                .append("allowAnalyzedField", true));
        }

        @Override
        public Criteria toCriteria(UnaryOperator<String> pathMapper) {
            if (paths.size() == 1) {
                return RegexCriteria.containsIgnoreCase(pathMapper.apply(paths.get(0)), term);
            }
            return new Criteria().orOperator(paths.stream()
                .map(path -> RegexCriteria.containsIgnoreCase(pathMapper.apply(path), term))
                .toArray(Criteria[]::new));
        }
    }

    /**
     * Analysed full-text match. The regex fallback approximates this with a case-insensitive substring match.
     */
    record TextClause(List<String> paths, String term) implements Clause {
        @Override
        public Document toAtlas() {
            return new Document("text", new Document()
                .append("path", paths.size() == 1 ? paths.get(0) : paths)
                .append("query", term));
        }

        @Override
        public Criteria toCriteria(UnaryOperator<String> pathMapper) {
            return new WildcardClause(paths, term).toCriteria(pathMapper);
        }
    }

    /**
     * Escape the Atlas wildcard metacharacters ({@code \ * ?}) so user input is matched literally.
     */
    static String escapeWildcard(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (char c : value.toCharArray()) {
            if (c == '\\' || c == '*' || c == '?') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
