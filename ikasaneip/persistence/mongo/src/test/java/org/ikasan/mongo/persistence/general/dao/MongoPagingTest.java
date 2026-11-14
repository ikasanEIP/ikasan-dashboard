package org.ikasan.mongo.persistence.general.dao;

import org.junit.Test;
import org.springframework.data.mongodb.core.query.Query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class MongoPagingTest {

    @Test
    public void unpaged_query_has_no_skip_or_limit() {
        Query query = MongoPaging.apply(new Query(), MongoPaging.UNPAGED, MongoPaging.UNPAGED);

        assertEquals(0, query.getSkip());
        assertEquals(0, query.getLimit());
        assertFalse(query.isLimited());
    }

    @Test
    public void null_and_zero_values_are_unpaged() {
        Query query = MongoPaging.apply(new Query(), null, null);
        assertEquals(0, query.getSkip());
        assertFalse(query.isLimited());

        query = MongoPaging.apply(new Query(), 0, 0);
        assertEquals(0, query.getSkip());
        assertFalse(query.isLimited());
    }

    @Test
    public void offset_and_limit_are_applied_as_given() {
        Query query = new Query();
        assertSame(query, MongoPaging.apply(query, 25, 10));

        // absolute offset, not rounded down to a page boundary
        assertEquals(25, query.getSkip());
        assertEquals(10, query.getLimit());
    }

    @Test
    public void limit_without_offset_and_offset_without_limit() {
        Query query = MongoPaging.apply(new Query(), -1, 10);
        assertEquals(0, query.getSkip());
        assertEquals(10, query.getLimit());

        query = MongoPaging.apply(new Query(), 10, -1);
        assertEquals(10, query.getSkip());
        assertFalse(query.isLimited());
    }

    @Test
    public void has_limit_and_has_offset() {
        assertTrue(MongoPaging.hasLimit(1));
        assertFalse(MongoPaging.hasLimit(-1));
        assertFalse(MongoPaging.hasLimit(0));
        assertFalse(MongoPaging.hasLimit(null));

        assertTrue(MongoPaging.hasOffset(1));
        assertFalse(MongoPaging.hasOffset(-1));
        assertFalse(MongoPaging.hasOffset(0));
        assertFalse(MongoPaging.hasOffset(null));
    }
}
