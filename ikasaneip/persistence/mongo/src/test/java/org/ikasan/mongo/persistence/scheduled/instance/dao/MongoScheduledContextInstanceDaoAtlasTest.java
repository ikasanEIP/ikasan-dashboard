package org.ikasan.mongo.persistence.scheduled.instance.dao;

import org.bson.Document;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceSearchFilterImpl;
import org.ikasan.mongo.persistence.general.dao.AtlasLocalSupport;
import org.ikasan.mongo.persistence.general.dao.AtlasSearchQueryHelper;
import org.ikasan.mongo.persistence.general.dao.PagingConvention;
import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.mongo.persistence.scheduled.instance.model.MongoScheduledContextInstanceRecordImpl;
import org.ikasan.mongo.persistence.scheduled.instance.repository.MongoScheduledContextInstanceRepository;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * {@link MongoScheduledContextInstanceDao#getScheduledContextInstancesByFilter} against Atlas Local with the shipped
 * search index: name / instance id search, status, created day, start and end time ranges, sort and count must all be
 * answered by {@code $search}.
 */
public class MongoScheduledContextInstanceDaoAtlasTest {

    private static final String DB = "atlas_context_instance";
    private static final long NOW = System.currentTimeMillis();
    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;
    private static final long THREE_DAYS_AGO = NOW - 72 * HOUR;

    private static AtlasSearchQueryHelper helper;
    private static MongoScheduledContextInstanceDao dao;

    @BeforeClass
    public static void seed() {
        MongoTemplate template = AtlasLocalSupport.freshDatabase(DB);
        helper = AtlasLocalSupport.searchHelper(template);
        dao = new MongoScheduledContextInstanceDao(new MongoRepositoryFactory(template)
            .getRepository(MongoScheduledContextInstanceRepository.class), template, 90, helper);

        dao.save(instance("inst-aaaa-0001", "billing-daily", InstanceStatus.RUNNING, NOW - 3 * HOUR, 0));
        dao.save(instance("inst-aaaa-0002", "billing-daily", InstanceStatus.COMPLETE, NOW - 2 * HOUR, NOW - 90 * MINUTE));
        dao.save(instance("inst-bbbb-0003", "Billing-Monthly", InstanceStatus.ERROR, NOW - HOUR, NOW - 30 * MINUTE));
        dao.save(instance("inst-cccc-0004", "payroll", InstanceStatus.COMPLETE, NOW, NOW + MINUTE));
        dao.save(instance("inst-dddd-0005", "billing-daily", InstanceStatus.COMPLETE, THREE_DAYS_AGO,
            THREE_DAYS_AGO + HOUR));
        for (int i = 0; i < 8; i++) {
            dao.save(instance("paging-" + i, "paging-context", InstanceStatus.COMPLETE, NOW - 10 * HOUR - i, 0));
        }
        // a job instance of the same context instance in the shared collection: must never be returned
        template.getCollection(MongoConstants.IKASAN_COLLECTION_NAME).insertOne(new Document("type", "schedulerJobInstance")
            .append("moduleName", "billing-daily").append("componentName", "inst-aaaa-0001")
            .append("status", "COMPLETE").append("timestamp", NOW).append("startTime", NOW).append("endTime", NOW));

        AtlasLocalSupport.applyShippedIndexes(DB);
        AtlasLocalSupport.awaitSearchable(DB, MongoConstants.IKASAN_COLLECTION_NAME, "type", 14);
    }

    @Before
    public void atlasSearchStillInUse() {
        assertTrue("Atlas Search must not have fallen back to regex", helper.isAtlasSearchAvailable());
    }

    @Test
    public void context_name_search_is_case_insensitive_and_sorted_newest_first_by_default() {
        SearchResults<ScheduledContextInstanceRecord> results = find(f -> f.setContextSearchFilter("BILLING"));

        assertEquals(List.of("inst-bbbb-0003", "inst-aaaa-0002", "inst-aaaa-0001", "inst-dddd-0005"), ids(results));
        assertEquals(4, results.getTotalNumberOfResults());
    }

    @Test
    public void context_name_and_instance_id_searches_must_both_match() {
        assertEquals(List.of("inst-aaaa-0002", "inst-aaaa-0001"), ids(find(f -> {
            f.setContextSearchFilter("billing");
            f.setContextInstanceId("AAAA");
        })));
    }

    @Test
    public void a_whole_instance_id_matches() {
        assertEquals(List.of("inst-bbbb-0003"), ids(find(f -> f.setContextInstanceId("inst-bbbb-0003"))));
    }

    @Test
    public void status_filter() {
        assertEquals(List.of("inst-aaaa-0002", "inst-dddd-0005"), ids(find(f -> {
            f.setContextSearchFilter("billing");
            f.setStatus(InstanceStatus.COMPLETE.name());
        })));
    }

    @Test
    public void context_name_list_filter() {
        assertEquals(List.of("inst-aaaa-0002", "inst-aaaa-0001", "inst-dddd-0005"), ids(find(f -> {
            f.setContextInstanceId("inst");
            f.setContextInstanceNames(List.of("billing-daily"));
        })));
    }

    @Test
    public void created_day_filter() {
        assertEquals(List.of("inst-dddd-0005"), ids(find(f -> {
            f.setContextSearchFilter("billing");
            f.setCreatedTimestamp(THREE_DAYS_AGO);
        })));
    }

    @Test
    public void start_time_range_filter() {
        assertEquals(List.of("inst-bbbb-0003", "inst-aaaa-0002"), ids(find(f -> {
            f.setContextSearchFilter("billing");
            f.setStartTimeStart(NOW - 150 * MINUTE);
            f.setStartTimeEnd(NOW);
        })));
    }

    @Test
    public void end_time_range_filter() {
        assertEquals(List.of("inst-bbbb-0003", "inst-aaaa-0002"), ids(find(f -> {
            f.setContextSearchFilter("billing");
            f.setEndTimeStart(NOW - 100 * MINUTE);
            f.setEndTimeEnd(NOW);
        })));
    }

    @Test
    public void sort_by_instance_id() {
        ContextInstanceSearchFilterImpl filter = new ContextInstanceSearchFilterImpl();
        filter.setContextSearchFilter("billing");

        SearchResults<ScheduledContextInstanceRecord> ascending = AtlasLocalSupport.usingSearch(DB,
            () -> dao.getScheduledContextInstancesByFilter(filter, -1, -1, "componentName", "ASCENDING"));
        assertEquals(List.of("inst-aaaa-0001", "inst-aaaa-0002", "inst-bbbb-0003", "inst-dddd-0005"), ids(ascending));

        SearchResults<ScheduledContextInstanceRecord> byStatus = AtlasLocalSupport.usingSearch(DB,
            () -> dao.getScheduledContextInstancesByFilter(filter, -1, -1, "status", "DESCENDING"));
        assertEquals(List.of("RUNNING", "ERROR", "COMPLETE", "COMPLETE"),
            byStatus.getResultList().stream().map(ScheduledContextInstanceRecord::getStatus).toList());
    }

    @Test
    public void paging_reports_the_total_of_all_matches() {
        ContextInstanceSearchFilterImpl filter = new ContextInstanceSearchFilterImpl();
        filter.setContextSearchFilter("paging");

        SearchResults<ScheduledContextInstanceRecord> page = AtlasLocalSupport.usingSearch(DB,
            () -> dao.getScheduledContextInstancesByFilter(filter, 3, 6, null, null));
        assertEquals(List.of("paging-6", "paging-7"), ids(page));
        assertEquals(8, page.getTotalNumberOfResults());

        PagingConvention.assertFollowed(8, (offset, limit) -> PagingConvention.page(AtlasLocalSupport.usingSearch(DB,
            () -> dao.getScheduledContextInstancesByFilter(filter, limit, offset, null, null))));
    }

    @Test
    public void no_match() {
        SearchResults<ScheduledContextInstanceRecord> results = find(f -> f.setContextSearchFilter("nonexistent"));

        assertEquals(List.of(), ids(results));
        assertEquals(0, results.getTotalNumberOfResults());
    }

    private static SearchResults<ScheduledContextInstanceRecord> find(Consumer<ContextInstanceSearchFilterImpl> spec) {
        ContextInstanceSearchFilterImpl filter = new ContextInstanceSearchFilterImpl();
        spec.accept(filter);
        return AtlasLocalSupport.usingSearch(DB, () -> dao.getScheduledContextInstancesByFilter(filter, -1, -1, null,
            null));
    }

    private static List<String> ids(SearchResults<ScheduledContextInstanceRecord> results) {
        return results.getResultList().stream()
            .map(record -> ((MongoScheduledContextInstanceRecordImpl) record).getContextInstanceId())
            .toList();
    }

    private static ScheduledContextInstanceRecord instance(String id, String contextName, InstanceStatus status,
                                                           long startTime, long endTime) {
        ContextInstanceImpl contextInstance = new ContextInstanceImpl();
        contextInstance.setId(id);
        contextInstance.setName(contextName);
        contextInstance.setStartTime(startTime);
        contextInstance.setEndTime(endTime);

        MongoScheduledContextInstanceRecordImpl record = new MongoScheduledContextInstanceRecordImpl();
        record.setContextName(contextName);
        record.setContextInstanceId(id);
        record.setStatus(status.name());
        record.setTimestamp(startTime);
        record.setContextInstance(contextInstance);
        return record;
    }
}
