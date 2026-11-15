package org.ikasan.mongo.persistence.scheduled.event.dao;

import org.ikasan.job.orchestration.model.event.ContextualisedScheduledProcessEventImpl;
import org.ikasan.mongo.persistence.general.dao.AtlasLocalSupport;
import org.ikasan.mongo.persistence.general.dao.AtlasSearchQueryHelper;
import org.ikasan.mongo.persistence.general.dao.PagingConvention;
import org.ikasan.mongo.persistence.scheduled.event.repository.MongoScheduledProcessEventRepository;
import org.ikasan.spec.search.SearchResults;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * {@link MongoScheduledProcessEventDao#getScheduleProcessEvents(List, long, long, String, boolean, int, int, String)}
 * against Atlas Local with the shipped search index for the {@code contextualisedScheduledProcessEventImpl}
 * collection: the free-text filter, agent list, fire time window, failures-only flag, sort and count must all be
 * answered by {@code $search}.
 */
public class MongoScheduledProcessEventDaoAtlasTest {

    private static final String DB = "atlas_process_event";
    private static final String COLLECTION = "contextualisedScheduledProcessEventImpl";
    private static final long NOW = System.currentTimeMillis();
    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;
    private static final long FROM = NOW - 24 * HOUR;
    private static final long TO = NOW + HOUR;

    private static AtlasSearchQueryHelper helper;
    private static MongoScheduledProcessEventDao dao;

    @BeforeClass
    public static void seed() {
        MongoTemplate template = AtlasLocalSupport.freshDatabase(DB);
        helper = AtlasLocalSupport.searchHelper(template);
        dao = new MongoScheduledProcessEventDao(new MongoRepositoryFactory(template)
            .getRepository(MongoScheduledProcessEventRepository.class), template, 30, helper);

        dao.save(event("billing-agent", "billing", "file-watcher", NOW - 3 * HOUR, true,
            "/opt/scripts/watch.sh --dir /in", "processed 10 files", null));
        dao.save(event("billing-agent", "billing", "file-loader", NOW - 2 * HOUR, false,
            "/opt/scripts/load.sh", null, "Connection refused by host"));
        dao.save(event("payroll-agent", "Payroll", "payroll-run", NOW - HOUR, true,
            "/opt/payroll/run.sh", "payroll complete", null));
        dao.save(event("payroll-agent", "Payroll", "payroll-report", NOW, false,
            "/opt/payroll/report.sh", null, "Disk full"));
        for (int i = 0; i < 8; i++) {
            dao.save(event("paging-agent", "paging", "paging-job-" + i, NOW - 10 * HOUR - i, true, "paging", null,
                null));
        }

        AtlasLocalSupport.applyShippedIndexes(DB);
        AtlasLocalSupport.awaitSearchable(DB, COLLECTION, "fireTime", 12);
    }

    @Before
    public void atlasSearchStillInUse() {
        assertTrue("Atlas Search must not have fallen back to regex", helper.isAtlasSearchAvailable());
    }

    @Test
    public void filter_matches_part_of_the_command_line_newest_first() {
        SearchResults<ContextualisedScheduledProcessEventImpl> results = find(null, FROM, TO, "opt/scripts", false,
            "desc");

        assertEquals(List.of("file-loader", "file-watcher"), jobNames(results));
        assertEquals(2, results.getTotalNumberOfResults());
    }

    @Test
    public void filter_matches_agent_job_group_and_job_name_case_insensitively() {
        assertEquals(List.of("payroll-report", "payroll-run"), jobNames(find(null, FROM, TO, "PAYROLL", false, "desc")));
        assertEquals(List.of("file-loader", "file-watcher"), jobNames(find(null, FROM, TO, "Billing-Agent", false,
            "desc")));
    }

    @Test
    public void filter_matches_result_output_and_error() {
        assertEquals(List.of("file-loader"), jobNames(find(null, FROM, TO, "refused", false, "desc")));
        assertEquals(List.of("payroll-run"), jobNames(find(null, FROM, TO, "complete", false, "desc")));
    }

    @Test
    public void accessible_agents_filter() {
        assertEquals(List.of("file-loader", "file-watcher"),
            jobNames(find(List.of("billing-agent"), FROM, TO, "opt", false, "desc")));
        assertEquals(List.of(), jobNames(find(List.of(), FROM, TO, "opt", false, "desc")));
    }

    @Test
    public void failures_only_filter() {
        assertEquals(List.of("payroll-report", "file-loader"), jobNames(find(null, FROM, TO, "opt", true, "desc")));
    }

    @Test
    public void fire_time_window() {
        assertEquals(List.of("payroll-run", "file-loader"),
            jobNames(find(null, NOW - 150 * MINUTE, NOW - 1, "opt", false, "desc")));
    }

    @Test
    public void ascending_sort() {
        assertEquals(List.of("file-watcher", "file-loader", "payroll-run", "payroll-report"),
            jobNames(find(null, FROM, TO, "opt", false, "asc")));
    }

    @Test
    public void paging_reports_the_total_of_all_matches() {
        SearchResults<ContextualisedScheduledProcessEventImpl> page = AtlasLocalSupport.usingSearch(DB,
            () -> dao.getScheduleProcessEvents(null, FROM, TO, "paging", false, 6, 3, "desc"));
        assertEquals(List.of("paging-job-6", "paging-job-7"), jobNames(page));
        assertEquals(8, page.getTotalNumberOfResults());

        PagingConvention.assertFollowed(8, (offset, limit) -> PagingConvention.page(AtlasLocalSupport.usingSearch(DB,
            () -> dao.getScheduleProcessEvents(null, FROM, TO, "paging", false, offset, limit, "desc"))));
    }

    @Test
    public void no_match() {
        SearchResults<ContextualisedScheduledProcessEventImpl> results = find(null, FROM, TO, "nonexistent", false,
            "desc");

        assertEquals(List.of(), jobNames(results));
        assertEquals(0, results.getTotalNumberOfResults());
    }

    private static SearchResults<ContextualisedScheduledProcessEventImpl> find(List<String> agents, long from, long to,
                                                                             String filter, boolean failuresOnly,
                                                                             String sortOrder) {
        return AtlasLocalSupport.usingSearch(DB,
            () -> dao.getScheduleProcessEvents(agents, from, to, filter, failuresOnly, -1, -1, sortOrder));
    }

    private static List<String> jobNames(SearchResults<ContextualisedScheduledProcessEventImpl> results) {
        return results.getResultList().stream().map(ContextualisedScheduledProcessEventImpl::getJobName).toList();
    }

    private static ContextualisedScheduledProcessEventImpl event(String agentName, String jobGroup, String jobName,
                                                                 long fireTime, boolean successful, String commandLine,
                                                                 String resultOutput, String resultError) {
        ContextualisedScheduledProcessEventImpl event = new ContextualisedScheduledProcessEventImpl();
        event.setAgentName(agentName);
        event.setJobGroup(jobGroup);
        event.setJobName(jobName);
        event.setFireTime(fireTime);
        event.setSuccessful(successful);
        event.setOutcome(successful ? "EXECUTION_INVOKED" : "EXECUTION_FAILED");
        event.setCommandLine(commandLine);
        event.setResultOutput(resultOutput);
        event.setResultError(resultError);
        return event;
    }
}
