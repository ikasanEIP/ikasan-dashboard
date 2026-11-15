package org.ikasan.mongo.persistence.scheduled.context.dao;

import org.bson.Document;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.SolrScheduledContextSearchFilterImpl;
import org.ikasan.mongo.persistence.general.dao.AtlasLocalSupport;
import org.ikasan.mongo.persistence.general.dao.AtlasSearchQueryHelper;
import org.ikasan.mongo.persistence.general.dao.PagingConvention;
import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.mongo.persistence.scheduled.context.model.MongoScheduledContextRecordImpl;
import org.ikasan.mongo.persistence.scheduled.context.repository.MongoScheduledContextRecordRepository;
import org.ikasan.spec.scheduled.context.model.ScheduledContextRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * {@link MongoScheduledContextDaoImpl#findByFilter} against Atlas Local with the shipped search index: the context
 * name search, name list filter, sort and count must all be answered by {@code $search}.
 */
public class MongoScheduledContextDaoImplAtlasTest {

    private static final String DB = "atlas_scheduled_context";
    private static final long NOW = System.currentTimeMillis();
    private static final long HOUR = 3_600_000L;

    private static AtlasSearchQueryHelper helper;
    private static MongoScheduledContextDaoImpl dao;

    @BeforeClass
    public static void seed() {
        MongoTemplate template = AtlasLocalSupport.freshDatabase(DB);
        helper = AtlasLocalSupport.searchHelper(template);
        dao = new MongoScheduledContextDaoImpl(new MongoRepositoryFactory(template)
            .getRepository(MongoScheduledContextRecordRepository.class), template, helper);

        dao.save(context("production-context", NOW - 3 * HOUR));
        dao.save(context("staging-context", NOW - 2 * HOUR));
        dao.save(context("development-context", NOW - HOUR));
        dao.save(context("Prod-Archive", NOW));
        for (int i = 0; i < 8; i++) {
            dao.save(context("paging-" + i, NOW - 10 * HOUR - i));
        }
        // another document type in the shared collection with a matching name: must never be returned
        template.getCollection(MongoConstants.IKASAN_COLLECTION_NAME).insertOne(new Document("type", "contextInstance")
            .append("moduleName", "production-context").append("timestamp", NOW));

        AtlasLocalSupport.applyShippedIndexes(DB);
        AtlasLocalSupport.awaitSearchable(DB, MongoConstants.IKASAN_COLLECTION_NAME, "type", 13);
    }

    @Before
    public void atlasSearchStillInUse() {
        assertTrue("Atlas Search must not have fallen back to regex", helper.isAtlasSearchAvailable());
    }

    @Test
    public void context_name_search_is_case_insensitive_and_sorted_newest_first_by_default() {
        SearchResults<ScheduledContextRecord> results = find(() -> dao.findByFilter(filter("PROD", null), -1, -1,
            null, null));

        assertEquals(List.of("Prod-Archive", "production-context"), names(results));
        assertEquals(2, results.getTotalNumberOfResults());
    }

    @Test
    public void context_name_search_restricted_to_a_list_of_contexts() {
        SearchResults<ScheduledContextRecord> results = find(() -> dao.findByFilter(
            filter("context", List.of("production-context", "staging-context", "Prod-Archive")), -1, -1, null, null));

        assertEquals(List.of("staging-context", "production-context"), names(results));
        assertEquals(2, results.getTotalNumberOfResults());
    }

    @Test
    public void sort_by_context_name() {
        SearchResults<ScheduledContextRecord> ascending = find(() -> dao.findByFilter(filter("-context", null), -1, -1,
            "moduleName", "ASCENDING"));
        assertEquals(List.of("development-context", "production-context", "staging-context"), names(ascending));

        SearchResults<ScheduledContextRecord> descending = find(() -> dao.findByFilter(filter("-context", null), -1,
            -1, "moduleName", "DESCENDING"));
        assertEquals(List.of("staging-context", "production-context", "development-context"), names(descending));
    }

    @Test
    public void paging_reports_the_total_of_all_matches() {
        SearchResults<ScheduledContextRecord> page = find(() -> dao.findByFilter(filter("paging", null), 3, 6, null,
            null));

        assertEquals(List.of("paging-6", "paging-7"), names(page));
        assertEquals(8, page.getTotalNumberOfResults());
    }

    @Test
    public void paging_convention() {
        PagingConvention.assertFollowed(8, (offset, limit) -> PagingConvention.page(
            find(() -> dao.findByFilter(filter("paging", null), limit, offset, null, null))));
    }

    @Test
    public void no_match() {
        SearchResults<ScheduledContextRecord> results = find(() -> dao.findByFilter(filter("nonexistent", null), -1,
            -1, null, null));

        assertEquals(List.of(), names(results));
        assertEquals(0, results.getTotalNumberOfResults());
    }

    private static SearchResults<ScheduledContextRecord> find(Supplier<SearchResults<ScheduledContextRecord>> call) {
        return AtlasLocalSupport.usingSearch(DB, call);
    }

    private static SolrScheduledContextSearchFilterImpl filter(String contextName, List<String> contextNames) {
        SolrScheduledContextSearchFilterImpl filter = new SolrScheduledContextSearchFilterImpl();
        filter.setContextName(contextName);
        filter.setContextNames(contextNames);
        return filter;
    }

    private static List<String> names(SearchResults<ScheduledContextRecord> results) {
        return results.getResultList().stream().map(ScheduledContextRecord::getContextName).toList();
    }

    private static MongoScheduledContextRecordImpl context(String name, long timestamp) {
        ContextTemplateImpl template = new ContextTemplateImpl();
        template.setName(name);
        template.setDescription("context " + name);

        MongoScheduledContextRecordImpl record = new MongoScheduledContextRecordImpl(name);
        record.setContext(template);
        record.setTimestamp(timestamp);
        return record;
    }
}
