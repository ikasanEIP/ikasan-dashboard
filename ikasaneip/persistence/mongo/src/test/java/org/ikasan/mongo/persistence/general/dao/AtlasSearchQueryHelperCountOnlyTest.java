package org.ikasan.mongo.persistence.general.dao;

import org.bson.Document;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * A {@code limit} of {@code 0} without a count asks for nothing, so {@link AtlasSearchQueryHelper} must not query the
 * database at all. (The count-only behaviour with a count is covered by {@link PagingConvention} in every DAO test.)
 */
public class AtlasSearchQueryHelperCountOnlyTest {

    @Test
    public void limit_0_without_count_runs_no_query() {
        MongoTemplate template = mock(MongoTemplate.class);
        AtlasSearchQueryHelper helper = new AtlasSearchQueryHelper(template, "default", true);
        AtlasSearchFilter filter = AtlasSearchFilter.builder().equals("type", "anything").build();

        AtlasSearchQueryHelper.SearchQueryResult<Document> result = helper.search(filter, null, 0, 0, false,
            Document.class);

        assertTrue(result.getResults().isEmpty());
        assertEquals(-1, result.getTotalCount());
        verifyNoInteractions(template);
    }
}
