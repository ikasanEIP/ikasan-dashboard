package org.ikasan.mongo.persistence.general.dao;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

/**
 * Applies the dashboard paging convention to a MongoDB {@link Query}.
 *
 * <p>Every DAO accepts an {@code offset} and a {@code limit}:</p>
 * <ul>
 *     <li>{@code limit = -1} (any negative value, or {@code null}) means "not paged": every matching document is
 *     returned.</li>
 *     <li>{@code limit = 0} means "count only": no documents are read, but a DAO that reports a total still reports
 *     the full number of matches. The offset is ignored.</li>
 *     <li>{@code offset <= 0} (or {@code null}) skips nothing.</li>
 * </ul>
 * <p>{@code offset = -1, limit = -1} therefore returns the full result set, and {@code offset = 0, limit = 0} returns
 * only the total.</p>
 *
 * <p>The offset is applied as an absolute number of documents to skip. It does not have to be a multiple of the
 * limit (unlike {@code PageRequest.of(offset / limit, limit)}, which rounds it down to a page boundary and fails for a
 * negative limit).</p>
 */
public final class MongoPaging {

    /** Value meaning "no offset" / "no limit". */
    public static final int UNPAGED = -1;

    /** Limit meaning "return no documents, only the total count". */
    public static final int COUNT_ONLY = 0;

    private MongoPaging() {
    }

    /**
     * @return {@code true} if {@code limit} restricts the number of results
     */
    public static boolean hasLimit(Integer limit) {
        return limit != null && limit > 0;
    }

    /**
     * @return {@code true} if {@code offset} skips results
     */
    public static boolean hasOffset(Integer offset) {
        return offset != null && offset > 0;
    }

    /**
     * @return {@code true} if {@code limit} asks for no documents, only the total count
     */
    public static boolean isCountOnly(Integer limit) {
        return limit != null && limit == COUNT_ONLY;
    }

    /**
     * Applies {@code offset} and {@code limit} to {@code query}; non-positive (or null) values are ignored.
     * Callers must handle {@link #isCountOnly(Integer) count only} themselves, or use
     * {@link #find(MongoTemplate, Query, Integer, Integer, Class)}.
     *
     * @return the same query, for chaining
     */
    public static Query apply(Query query, Integer offset, Integer limit) {
        if (hasOffset(offset)) {
            query.skip(offset);
        }
        if (hasLimit(limit)) {
            query.limit(limit);
        }
        return query;
    }

    /**
     * Runs {@code query} with the given paging. A {@link #isCountOnly(Integer) count only} limit returns an empty
     * list without querying the database.
     */
    public static <T> List<T> find(MongoTemplate mongoTemplate, Query query, Integer offset, Integer limit,
                                   Class<T> entityClass) {
        if (isCountOnly(limit)) {
            return List.of();
        }
        return mongoTemplate.find(apply(query, offset, limit), entityClass);
    }
}
