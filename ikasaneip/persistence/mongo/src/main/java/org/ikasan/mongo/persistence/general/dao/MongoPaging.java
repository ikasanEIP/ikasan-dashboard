package org.ikasan.mongo.persistence.general.dao;

import org.springframework.data.mongodb.core.query.Query;

/**
 * Applies the dashboard paging convention to a MongoDB {@link Query}.
 *
 * <p>Every DAO accepts an {@code offset} and a {@code limit}. A value of {@code -1} (or any value {@code <= 0} for the
 * limit, {@code < 0} for the offset) means "not paged": an offset of {@code -1} skips nothing and a limit of {@code -1}
 * returns every matching document. {@code offset = -1, limit = -1} therefore returns the full result set.</p>
 *
 * <p>The offset is applied as an absolute number of documents to skip. It does not have to be a multiple of the
 * limit (unlike {@code PageRequest.of(offset / limit, limit)}, which rounds it down to a page boundary and fails for a
 * negative limit).</p>
 */
public final class MongoPaging {

    /** Value meaning "no offset" / "no limit". */
    public static final int UNPAGED = -1;

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
     * Applies {@code offset} and {@code limit} to {@code query}; non-positive (or null) values are ignored.
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
}
