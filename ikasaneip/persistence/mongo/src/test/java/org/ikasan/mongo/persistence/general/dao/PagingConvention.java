package org.ikasan.mongo.persistence.general.dao;

import org.ikasan.spec.metadata.BusinessStreamMetadataSearchResults;
import org.ikasan.spec.metadata.ModuleMetadataSearchResults;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test assertion for the DAO paging convention: an offset or limit of {@code -1} means "not paged", so
 * {@code offset = -1, limit = -1} returns every match, and the offset is an absolute number of documents to skip
 * (it does not have to be a multiple of the limit).
 *
 * <p>Usage, for a DAO method taking {@code (limit, offset)}:</p>
 * <pre>
 * PagingConvention.assertFollowed(25, (offset, limit) -&gt; PagingConvention.page(dao.findAll(limit, offset)));
 * </pre>
 */
public final class PagingConvention {

    /** One executed query: the number of results returned, and the reported total ({@code -1} if not reported). */
    public record Page(int size, long total) {
    }

    /** Runs the query under test with the given paging. */
    @FunctionalInterface
    public interface PagedQuery {
        Page run(int offset, int limit);
    }

    private PagingConvention() {
    }

    public static Page page(SearchResults<?> results) {
        return new Page(results.getResultList().size(), results.getTotalNumberOfResults());
    }

    public static Page page(IkasanDocumentSearchResults results) {
        return new Page(results.getResultList().size(), results.getTotalNumberOfResults());
    }

    public static Page page(ModuleMetadataSearchResults results) {
        return new Page(results.getResultList().size(), results.getTotalNumberOfResults());
    }

    public static Page page(BusinessStreamMetadataSearchResults results) {
        return new Page(results.getResultList().size(), results.getTotalNumberOfResults());
    }

    public static Page page(AtlasSearchQueryHelper.SearchQueryResult<?> results) {
        return new Page(results.getResults().size(), results.getTotalCount());
    }

    public static Page page(List<?> results) {
        return new Page(results.size(), -1);
    }

    /**
     * Asserts that {@code query} follows the paging convention when exactly {@code total} documents match.
     *
     * @param total the number of matching documents; at least 7, so the paged checks are meaningful
     */
    public static void assertFollowed(int total, PagedQuery query) {
        assertTrue("need at least 7 matching documents to check paging", total >= 7);

        check(query, -1, -1, total, total, "offset -1 / limit -1 must return every match");
        check(query, 0, -1, total, total, "limit -1 must return every match");
        check(query, -1, 3, 3, total, "offset -1 must skip nothing");
        check(query, 0, 3, 3, total, "first page");
        check(query, 2, -1, total - 2, total, "limit -1 with an offset must return the rest");

        // an offset that is not a multiple of the limit is still an absolute offset
        int offset = total - 2;
        if (offset % 3 == 0) {
            offset--;
        }
        check(query, offset, 3, Math.min(3, total - offset), total, "offset that is not a multiple of the limit");
        check(query, total, 3, 0, total, "offset past the end");
    }

    private static void check(PagedQuery query, int offset, int limit, int expectedSize, int expectedTotal,
                              String message) {
        Page page = query.run(offset, limit);
        String where = message + " [offset=" + offset + ", limit=" + limit + "]";
        assertEquals(where, expectedSize, page.size());
        if (page.total() >= 0) {
            assertEquals(where + ": total count", expectedTotal, page.total());
        }
    }
}
