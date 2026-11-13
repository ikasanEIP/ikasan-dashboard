package org.ikasan.mongo.persistence.general.dao;

import org.bson.Document;
import org.junit.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link AtlasSearchFilter} and the {@code $search} stage built by {@link AtlasSearchQueryHelper}.
 */
public class AtlasSearchFilterTest {

    @Test
    public void builder_ignores_null_and_empty_values() {
        AtlasSearchFilter filter = AtlasSearchFilter.builder()
            .equals("type", null)
            .equals("status", "")
            .in("moduleName", null)
            .in("flowName", List.of())
            .rangeMillis("timestamp", 0, -1)
            .range("startTime", null, null)
            .contains("moduleName", "  ")
            .containsAny(List.of("payload"), null)
            .text(List.of("payload"), "")
            .build();

        assertFalse(filter.hasFilterClauses());
        assertFalse(filter.hasTextClauses());
        assertNull(filter.toCriteria());
    }

    @Test
    public void filters_are_rendered_into_compound_filter_and_terms_into_must() {
        AtlasSearchFilter filter = AtlasSearchFilter.builder()
            .equals("type", "ERROR")
            .in("moduleName", List.of("m1", "m2"))
            .in("flowName", List.of("f1"))
            .rangeMillis("timestamp", 100L, 200L)
            .containsAny(List.of("payload", "errorUri"), "Order-1")
            .build();

        Document compound = filter.toAtlasCompound();

        assertEquals(List.of(
            new Document("equals", new Document("path", "type").append("value", "ERROR")),
            new Document("in", new Document("path", "moduleName").append("value", List.of("m1", "m2"))),
            // a single-valued "in" collapses to equals
            new Document("equals", new Document("path", "flowName").append("value", "f1")),
            new Document("range", new Document("path", "timestamp").append("gte", 100L).append("lte", 200L))
        ), compound.get("filter"));

        assertEquals(List.of(
            new Document("wildcard", new Document("path", List.of("payload", "errorUri"))
                .append("query", "*order-1*")
                .append("allowAnalyzedField", true))
        ), compound.get("must"));

        assertFalse(compound.containsKey("mustNot"));
    }

    @Test
    public void open_range_bounds_are_omitted() {
        Document compound = AtlasSearchFilter.builder()
            .rangeMillis("timestamp", 100L, 0L)
            .range("startTime", null, new Date(5L))
            .build()
            .toAtlasCompound();

        assertEquals(List.of(
            new Document("range", new Document("path", "timestamp").append("gte", 100L)),
            new Document("range", new Document("path", "startTime").append("lte", new Date(5L)))
        ), compound.get("filter"));
    }

    @Test
    public void anyOf_is_rendered_as_nested_should() {
        Document compound = AtlasSearchFilter.builder()
            .anyOf(AtlasSearchFilter.eq("event", "e1"), AtlasSearchFilter.eq("errorUri", "e1"))
            .build()
            .toAtlasCompound();

        Document anyOf = ((List<?>) compound.get("filter")).stream().map(Document.class::cast).findFirst().orElseThrow();
        Document nested = (Document) anyOf.get("compound");
        assertEquals(1, nested.get("minimumShouldMatch"));
        assertEquals(2, ((List<?>) nested.get("should")).size());
    }

    @Test
    public void wildcard_metacharacters_in_user_input_are_escaped() {
        Document compound = AtlasSearchFilter.builder()
            .contains("moduleName", "a*b?c\\d")
            .build()
            .toAtlasCompound();

        Document wildcard = (Document) ((Document) ((List<?>) compound.get("must")).get(0)).get("wildcard");
        assertEquals("*a\\*b\\?c\\\\d*", wildcard.get("query"));
        assertEquals("moduleName", wildcard.get("path"));
    }

    @Test
    public void negated_terms_are_rendered_into_mustNot_with_a_match_all_filter() {
        Document compound = AtlasSearchFilter.builder()
            .text(List.of("payload"), "order")
            .negateText(true)
            .build()
            .toAtlasCompound();

        assertFalse(compound.containsKey("must"));
        assertEquals(List.of(new Document("text", new Document("path", "payload").append("query", "order"))),
            compound.get("mustNot"));
        assertEquals(List.of(new Document("exists", new Document("path", "type"))), compound.get("filter"));
    }

    @Test
    public void criteria_fallback_matches_atlas_semantics() {
        AtlasSearchFilter filter = AtlasSearchFilter.builder()
            .equals("type", "ERROR")
            .contains("moduleName", "a.b")
            .build();

        Document criteria = filter.toCriteria().getCriteriaObject();
        List<?> and = (List<?>) criteria.get("$and");
        assertEquals(2, and.size());
        assertEquals(new Document("type", "ERROR"), and.get(0));

        // user input is quoted, so regex metacharacters are matched literally (case-insensitively)
        String regex = ((Document) and.get(1)).get("moduleName").toString();
        assertTrue(regex, regex.contains("\\Qa.b\\E"));
    }

    @Test
    public void negated_criteria_use_nor() {
        Criteria criteria = AtlasSearchFilter.builder()
            .equals("type", "ERROR")
            .text(List.of("payload", "event"), "order")
            .negateText(true)
            .build()
            .toCriteria();

        List<?> and = (List<?>) criteria.getCriteriaObject().get("$and");
        assertTrue(((Document) and.get(1)).containsKey("$nor"));
    }

    @Test
    public void search_stage_carries_filter_sort_and_count_in_a_single_stage() {
        AtlasSearchQueryHelper helper = new AtlasSearchQueryHelper(null, "myIndex");
        AtlasSearchFilter filter = AtlasSearchFilter.builder()
            .equals("type", "ERROR")
            .contains("moduleName", "abc")
            .build();

        Document stage = helper.buildSearchStage(filter,
            Sort.by(Sort.Order.desc("timestamp"), Sort.Order.asc("moduleName")), true).getSearchDocument();

        assertEquals(Set.of("$search"), stage.keySet());
        Document search = (Document) stage.get("$search");
        assertEquals("myIndex", search.get("index"));
        assertEquals(filter.toAtlasCompound(), search.get("compound"));
        assertEquals(new Document("timestamp", -1).append("moduleName", 1), search.get("sort"));
        assertEquals(new Document("type", "total"), search.get("count"));
    }

    @Test
    public void search_stage_omits_sort_and_count_when_not_requested() {
        AtlasSearchQueryHelper helper = new AtlasSearchQueryHelper(null);
        Document search = (Document) helper.buildSearchStage(
            AtlasSearchFilter.builder().contains("moduleName", "abc").build(), Sort.unsorted(), false)
            .getSearchDocument().get("$search");

        assertFalse(search.containsKey("sort"));
        assertFalse(search.containsKey("count"));
        assertEquals("default", search.get("index"));
    }
}
