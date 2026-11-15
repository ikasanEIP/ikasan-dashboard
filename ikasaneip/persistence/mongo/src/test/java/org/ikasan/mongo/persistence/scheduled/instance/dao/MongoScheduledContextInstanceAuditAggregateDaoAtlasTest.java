package org.ikasan.mongo.persistence.scheduled.instance.dao;

import org.bson.Document;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.instance.ScheduledContextInstanceAuditAggregateImpl;
import org.ikasan.mongo.persistence.general.dao.AtlasLocalSupport;
import org.ikasan.mongo.persistence.general.dao.AtlasSearchQueryHelper;
import org.ikasan.mongo.persistence.general.dao.PagingConvention;
import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.mongo.persistence.scheduled.instance.model.MongoScheduledContextInstanceAuditAggregateRecordImpl;
import org.ikasan.mongo.persistence.scheduled.instance.repository.MongoScheduledContextInstanceAuditAggregateRepository;
import org.ikasan.spec.scheduled.event.model.SchedulerJobInitiationEvent;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateRecord;
import org.ikasan.spec.scheduled.instance.model.ScheduledContextInstanceAuditAggregateSearchFilter;
import org.ikasan.spec.search.SearchResults;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * {@link MongoScheduledContextInstanceAuditAggregateDao#findScheduledContextInstanceAuditAggregateRecordsByFilter}
 * against Atlas Local with the shipped search index: every filter is a case-insensitive substring match that must be
 * answered by {@code $search}, together with the sort and count.
 */
public class MongoScheduledContextInstanceAuditAggregateDaoAtlasTest {

    private static final String DB = "atlas_audit_aggregate";

    private static AtlasSearchQueryHelper helper;
    private static MongoScheduledContextInstanceAuditAggregateDao dao;

    @BeforeClass
    public static void seed() {
        MongoTemplate template = AtlasLocalSupport.freshDatabase(DB);
        helper = AtlasLocalSupport.searchHelper(template);
        dao = new MongoScheduledContextInstanceAuditAggregateDao(new MongoRepositoryFactory(template)
            .getRepository(MongoScheduledContextInstanceAuditAggregateRepository.class), template, 90, helper);

        dao.save(audit("billing-daily", "ci-aaaa-0001", "file-watcher", InstanceStatus.COMPLETE,
            "loader-job", "report-job"));
        dao.save(audit("billing-daily", "ci-aaaa-0002", "file-loader", InstanceStatus.ERROR, "report-job"));
        dao.save(audit("Billing-Monthly", "ci-bbbb-0003", "quartz-trigger", InstanceStatus.RUNNING, "archive-job"));
        dao.save(audit("payroll", "ci-cccc-0004", "payroll-trigger", InstanceStatus.COMPLETE));
        for (int i = 0; i < 8; i++) {
            dao.save(audit("paging-context", "ci-p-" + i, "paging-event-" + i, InstanceStatus.COMPLETE));
        }
        // a context instance in the shared collection that matches every filter: must never be returned
        template.getCollection(MongoConstants.IKASAN_COLLECTION_NAME).insertOne(new Document("type", "scheduledContextInstance")
            .append("moduleName", "billing-daily").append("flowName", "ci-aaaa-0001")
            .append("componentName", "file-watcher").append("status", "COMPLETE").append("event", "report-job")
            .append("timestamp", System.currentTimeMillis()));

        AtlasLocalSupport.applyShippedIndexes(DB);
        AtlasLocalSupport.awaitSearchable(DB, MongoConstants.IKASAN_COLLECTION_NAME, "type", 13);
    }

    @Before
    public void atlasSearchStillInUse() {
        assertTrue("Atlas Search must not have fallen back to regex", helper.isAtlasSearchAvailable());
    }

    @Test
    public void context_name_search_is_case_insensitive() {
        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results = find(f -> f.setContextName("BILLING"));

        assertEquals(List.of("file-loader", "file-watcher", "quartz-trigger"), eventNames(results));
        assertEquals(3, results.getTotalNumberOfResults());
    }

    @Test
    public void a_whole_context_instance_id_matches() {
        assertEquals(List.of("file-loader"), eventNames(find(f -> f.setContextInstanceId("ci-aaaa-0002"))));
    }

    @Test
    public void scheduled_process_event_name_search() {
        assertEquals(List.of("payroll-trigger", "quartz-trigger"),
            eventNames(find(f -> f.setScheduledProcessEventName("Trigger"))));
    }

    @Test
    public void status_search_combined_with_context_name() {
        assertEquals(List.of("file-watcher"), eventNames(find(f -> {
            f.setContextName("billing");
            f.setStatus("complete");
        })));
    }

    @Test
    public void raised_initiation_event_search_matches_a_whole_job_name() {
        assertEquals(List.of("file-loader", "file-watcher"),
            eventNames(find(f -> f.setRaisedInitiationEventName("report-job"))));
    }

    @Test
    public void raised_initiation_event_search_matches_across_job_names() {
        // the raised job names are stored as one space-separated value: "loader-job report-job"
        assertEquals(List.of("file-watcher"),
            eventNames(find(f -> f.setRaisedInitiationEventName("loader-job report"))));
    }

    @Test
    public void default_sort_is_answered_by_search() {
        ScheduledContextInstanceAuditAggregateSearchFilter filter = new ScheduledContextInstanceAuditAggregateSearchFilter();
        filter.setContextName("billing");

        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results = AtlasLocalSupport.usingSearch(DB,
            () -> dao.findScheduledContextInstanceAuditAggregateRecordsByFilter(filter, -1, -1, null, null));
        assertEquals(Set.of("file-loader", "file-watcher", "quartz-trigger"), results.getResultList().stream()
            .map(ScheduledContextInstanceAuditAggregateRecord::getScheduledProcessEventName)
            .collect(Collectors.toSet()));
    }

    @Test
    public void paging_reports_the_total_of_all_matches() {
        ScheduledContextInstanceAuditAggregateSearchFilter filter = new ScheduledContextInstanceAuditAggregateSearchFilter();
        filter.setContextName("paging");

        SearchResults<ScheduledContextInstanceAuditAggregateRecord> page = AtlasLocalSupport.usingSearch(DB,
            () -> dao.findScheduledContextInstanceAuditAggregateRecordsByFilter(filter, 3, 6, "componentName",
                "ASCENDING"));
        assertEquals(List.of("paging-event-6", "paging-event-7"), eventNames(page));
        assertEquals(8, page.getTotalNumberOfResults());

        PagingConvention.assertFollowed(8, (offset, limit) -> PagingConvention.page(AtlasLocalSupport.usingSearch(DB,
            () -> dao.findScheduledContextInstanceAuditAggregateRecordsByFilter(filter, limit, offset,
                "componentName", "ASCENDING"))));
    }

    @Test
    public void no_match() {
        SearchResults<ScheduledContextInstanceAuditAggregateRecord> results = find(f -> f.setContextName("nonexistent"));

        assertEquals(List.of(), eventNames(results));
        assertEquals(0, results.getTotalNumberOfResults());
    }

    /**
     * Runs the filter search sorted by scheduled process event name. Records are timestamped when saved, so the
     * default (newest first) order is not deterministic for records saved in the same millisecond.
     */
    private static SearchResults<ScheduledContextInstanceAuditAggregateRecord> find(Consumer<ScheduledContextInstanceAuditAggregateSearchFilter> spec) {
        ScheduledContextInstanceAuditAggregateSearchFilter filter = new ScheduledContextInstanceAuditAggregateSearchFilter();
        spec.accept(filter);
        return AtlasLocalSupport.usingSearch(DB, () -> dao.findScheduledContextInstanceAuditAggregateRecordsByFilter(
            filter, -1, -1, "componentName", "ASCENDING"));
    }

    private static List<String> eventNames(SearchResults<ScheduledContextInstanceAuditAggregateRecord> results) {
        return results.getResultList().stream()
            .map(ScheduledContextInstanceAuditAggregateRecord::getScheduledProcessEventName)
            .toList();
    }

    private static ScheduledContextInstanceAuditAggregateRecord audit(String contextName, String contextInstanceId,
                                                                      String processEventName, InstanceStatus status,
                                                                      String... raisedJobNames) {
        ScheduledContextInstanceAuditAggregateImpl aggregate = new ScheduledContextInstanceAuditAggregateImpl();
        if (raisedJobNames.length > 0) {
            aggregate.setSchedulerJobInitiationEvents(java.util.Arrays.stream(raisedJobNames)
                .map(jobName -> {
                    SchedulerJobInitiationEventImpl event = new SchedulerJobInitiationEventImpl();
                    event.setJobName(jobName);
                    return (SchedulerJobInitiationEvent) event;
                })
                .toList());
        }

        MongoScheduledContextInstanceAuditAggregateRecordImpl record = new MongoScheduledContextInstanceAuditAggregateRecordImpl();
        record.setContextName(contextName);
        record.setContextInstanceId(contextInstanceId);
        record.setScheduledProcessEventName(processEventName);
        record.setStatus(status.name());
        record.setJobType("");
        record.setScheduledContextInstanceAuditAggregate(aggregate);
        return record;
    }
}
