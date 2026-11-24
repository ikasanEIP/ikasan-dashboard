package org.ikasan.mongo.persistence.scheduled.instance.dao;

import org.bson.Document;
import org.ikasan.job.orchestration.model.instance.ContextStartJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.FileEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.InternalEventDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.QuartzScheduleDrivenJobInstanceImpl;
import org.ikasan.job.orchestration.model.instance.SchedulerJobInstanceSearchFilterImpl;
import org.ikasan.mongo.persistence.general.dao.AtlasLocalSupport;
import org.ikasan.mongo.persistence.general.dao.AtlasSearchQueryHelper;
import org.ikasan.mongo.persistence.general.dao.PagingConvention;
import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.mongo.persistence.scheduled.instance.model.MongoSchedulerJobInstanceRecordImpl;
import org.ikasan.mongo.persistence.scheduled.instance.repository.MongoSchedulerJobInstanceRecordRepository;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstance;
import org.ikasan.spec.scheduled.instance.model.SchedulerJobInstanceRecord;
import org.ikasan.spec.scheduled.job.model.JobConstants;
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
 * {@link MongoSchedulerJobInstanceDaoImpl#getScheduledContextInstancesByFilter} against Atlas Local with the shipped
 * search index: job name / display name search, job type, context, context instance, child context, status, boolean
 * flags, start and end time windows, sort and count must all be answered by {@code $search}.
 */
public class MongoSchedulerJobInstanceDaoImplAtlasTest {

    private static final String DB = "atlas_job_instance";
    private static final long NOW = System.currentTimeMillis();
    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;

    private static AtlasSearchQueryHelper helper;
    private static MongoSchedulerJobInstanceDaoImpl dao;

    @BeforeClass
    public static void seed() {
        MongoTemplate template = AtlasLocalSupport.freshDatabase(DB);
        helper = AtlasLocalSupport.searchHelper(template);
        dao = new MongoSchedulerJobInstanceDaoImpl(new MongoRepositoryFactory(template)
            .getRepository(MongoSchedulerJobInstanceRecordRepository.class), template, helper);

        MongoSchedulerJobInstanceRecordImpl watcher = job("file-watcher", "File Watcher", "billing", "ci-1",
            "billing-child", InstanceStatus.RUNNING, JobConstants.FILE_EVENT_DRIVEN_JOB_INSTANCE,
            new FileEventDrivenJobInstanceImpl(), NOW - 3 * HOUR, 0);
        watcher.setParticipatesInLock(true);
        dao.save(watcher);

        MongoSchedulerJobInstanceRecordImpl loader = job("file-loader", "File Loader", "billing", "ci-1",
            "billing-child", InstanceStatus.COMPLETE, JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE,
            new QuartzScheduleDrivenJobInstanceImpl(), NOW - 2 * HOUR, NOW - 100 * MINUTE);
        loader.setTargetResidingContextOnly(true);
        dao.save(loader);

        dao.save(job("report-file", "Monthly Report", "billing", "ci-2", "other-child", InstanceStatus.ERROR,
            JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE, new InternalEventDrivenJobInstanceImpl(),
            NOW - HOUR, NOW - 30 * MINUTE));
        dao.save(job("payroll-file", "Payroll", "payroll", "ci-3", "payroll-child", InstanceStatus.COMPLETE,
            JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE, new QuartzScheduleDrivenJobInstanceImpl(),
            NOW, NOW + MINUTE));
        dao.save(job("file-start", "Start", "billing", "ci-1", "billing-child", InstanceStatus.COMPLETE,
            JobConstants.CONTEXT_START_JOB_INSTANCE, new ContextStartJobInstanceImpl(), NOW - 4 * HOUR,
            NOW - 4 * HOUR));
        for (int i = 0; i < 8; i++) {
            dao.save(job("paging-job-" + i, "Paging " + i, "paging", "ci-p", "paging-child", InstanceStatus.COMPLETE,
                JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB_INSTANCE, new QuartzScheduleDrivenJobInstanceImpl(),
                NOW - 10 * HOUR - i, 0));
        }
        // a context instance in the shared collection that matches the name and context filters: never returned
        template.getCollection(MongoConstants.IKASAN_COLLECTION_NAME).insertOne(new Document("type", "scheduledContextInstance")
            .append("moduleName", "file-watcher").append("displayName", "File Watcher").append("flowName", "billing")
            .append("componentName", "ci-1").append("status", "RUNNING").append("startTime", NOW - 3 * HOUR));

        AtlasLocalSupport.applyShippedIndexes(DB);
        AtlasLocalSupport.awaitSearchable(DB, MongoConstants.IKASAN_COLLECTION_NAME, "type", 14);
    }

    @Before
    public void atlasSearchStillInUse() {
        assertTrue("Atlas Search must not have fallen back to regex", helper.isAtlasSearchAvailable());
    }

    @Test
    public void job_name_search_is_case_insensitive_and_includes_start_and_terminal_jobs_by_default() {
        SearchResults<SchedulerJobInstanceRecord> results = find(f -> f.setJobName("FILE"));

        assertEquals(List.of("payroll-file", "report-file", "file-loader", "file-watcher", "file-start"),
            jobNames(results));
        assertEquals(5, results.getTotalNumberOfResults());
    }

    @Test
    public void start_and_terminal_jobs_can_be_excluded() {
        assertEquals(List.of("payroll-file", "report-file", "file-loader", "file-watcher"), jobNames(find(f -> {
            f.setJobName("file");
            f.setIncludeStartAndTerminalJobsInSearchResults(false);
        })));
    }

    @Test
    public void job_type_filter() {
        assertEquals(List.of("report-file"), jobNames(find(f -> {
            f.setJobName("file");
            f.setJobType(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE);
        })));
    }

    @Test
    public void display_name_search() {
        assertEquals(List.of("file-loader", "file-watcher"), jobNames(find(f -> f.setDisplayNameFilter("file "))));
    }

    /**
     * A filter scoped to one context instance is answered by the collection, not Atlas Search (which lags behind
     * writes), with the same matching: job name as a case-insensitive substring, the rest exact.
     */
    @Test
    public void context_and_context_instance_filters() {
        SchedulerJobInstanceSearchFilterImpl filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setJobName("FILE");
        filter.setContextName("billing");
        filter.setContextInstanceId("ci-1");

        SearchResults<SchedulerJobInstanceRecord> results = AtlasLocalSupport.withoutSearch(DB,
            () -> dao.getScheduledContextInstancesByFilter(filter, -1, -1, "startTime", "DESC"));

        assertEquals(List.of("file-loader", "file-watcher", "file-start"), jobNames(results));
        assertEquals(3, results.getTotalNumberOfResults());
    }

    /**
     * The scheduler updates a job's record straight after the instance's records are created and expects to find it:
     * a context instance scoped query must see a record saved a moment ago (Atlas Search indexes it ~1s later).
     */
    @Test
    public void a_job_saved_a_moment_ago_is_found_within_its_context_instance() {
        String contextInstanceId = "ci-just-saved-" + System.nanoTime();
        dao.save(job("just-saved", "Just saved", "billing", contextInstanceId, "child", InstanceStatus.WAITING,
            JobConstants.INTERNAL_EVENT_DRIVEN_JOB_INSTANCE, new InternalEventDrivenJobInstanceImpl(), 1, 0));

        SchedulerJobInstanceSearchFilterImpl filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setContextInstanceId(contextInstanceId);
        filter.setJobName("just-saved");
        filter.setChildContextName("child");
        SearchResults<SchedulerJobInstanceRecord> results = dao.getScheduledContextInstancesByFilter(filter, 1, 0, null, null);

        assertEquals(List.of("just-saved"), jobNames(results));
        assertEquals(1, results.getTotalNumberOfResults());
    }

    @Test
    public void child_context_filter() {
        assertEquals(List.of("report-file"), jobNames(find(f -> {
            f.setJobName("file");
            f.setChildContextName("other-child");
        })));
    }

    @Test
    public void status_filter() {
        assertEquals(List.of("payroll-file", "file-loader", "file-start"), jobNames(find(f -> {
            f.setJobName("file");
            f.setStatus(InstanceStatus.COMPLETE.name());
        })));
    }

    @Test
    public void boolean_flag_filters() {
        assertEquals(List.of("file-loader"), jobNames(find(f -> {
            f.setJobName("file");
            f.setTargetResidingContextOnly(true);
        })));
        assertEquals(List.of("file-watcher"), jobNames(find(f -> {
            f.setJobName("file");
            f.setParticipatesInLock(true);
        })));
    }

    @Test
    public void start_and_end_time_windows() {
        assertEquals(List.of("report-file", "file-loader"), jobNames(find(f -> {
            f.setJobName("file");
            f.setStartTimeWindowStart(NOW - 150 * MINUTE);
            f.setStartTimeWindowEnd(NOW - 1);
        })));
        assertEquals(List.of("report-file", "file-loader"), jobNames(find(f -> {
            f.setJobName("file");
            f.setEndTimeWindowStart(NOW - 120 * MINUTE);
            f.setEndTimeWindowEnd(NOW);
        })));
    }

    @Test
    public void sort_by_job_name() {
        SchedulerJobInstanceSearchFilterImpl filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setJobName("file");

        assertEquals(List.of("file-loader", "file-start", "file-watcher", "payroll-file", "report-file"),
            jobNames(AtlasLocalSupport.usingSearch(DB,
                () -> dao.getScheduledContextInstancesByFilter(filter, -1, -1, "moduleName", "ASC"))));
    }

    @Test
    public void default_sort_by_modified_timestamp_is_answered_by_search() {
        SchedulerJobInstanceSearchFilterImpl filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setJobName("file");

        SearchResults<SchedulerJobInstanceRecord> results = AtlasLocalSupport.usingSearch(DB,
            () -> dao.getScheduledContextInstancesByFilter(filter, -1, -1, null, null));
        assertEquals(Set.of("payroll-file", "report-file", "file-loader", "file-watcher", "file-start"),
            results.getResultList().stream().map(SchedulerJobInstanceRecord::getJobName).collect(Collectors.toSet()));
    }

    @Test
    public void paging_reports_the_total_of_all_matches() {
        SchedulerJobInstanceSearchFilterImpl filter = new SchedulerJobInstanceSearchFilterImpl();
        filter.setJobName("paging");

        SearchResults<SchedulerJobInstanceRecord> page = AtlasLocalSupport.usingSearch(DB,
            () -> dao.getScheduledContextInstancesByFilter(filter, 3, 6, "startTime", "DESC"));
        assertEquals(List.of("paging-job-6", "paging-job-7"), jobNames(page));
        assertEquals(8, page.getTotalNumberOfResults());

        PagingConvention.assertFollowed(8, (offset, limit) -> PagingConvention.page(AtlasLocalSupport.usingSearch(DB,
            () -> dao.getScheduledContextInstancesByFilter(filter, limit, offset, "startTime", "DESC"))));
    }

    @Test
    public void no_match() {
        SearchResults<SchedulerJobInstanceRecord> results = find(f -> f.setJobName("nonexistent"));

        assertEquals(List.of(), jobNames(results));
        assertEquals(0, results.getTotalNumberOfResults());
    }

    /** Runs the filter search sorted by start time, newest first, so the expected order is deterministic. */
    private static SearchResults<SchedulerJobInstanceRecord> find(Consumer<SchedulerJobInstanceSearchFilterImpl> spec) {
        SchedulerJobInstanceSearchFilterImpl filter = new SchedulerJobInstanceSearchFilterImpl();
        spec.accept(filter);
        return AtlasLocalSupport.usingSearch(DB,
            () -> dao.getScheduledContextInstancesByFilter(filter, -1, -1, "startTime", "DESC"));
    }

    private static List<String> jobNames(SearchResults<SchedulerJobInstanceRecord> results) {
        return results.getResultList().stream().map(SchedulerJobInstanceRecord::getJobName).toList();
    }

    private static MongoSchedulerJobInstanceRecordImpl job(String jobName, String displayName, String contextName,
                                                           String contextInstanceId, String childContextName,
                                                           InstanceStatus status, String type,
                                                           SchedulerJobInstance jobInstance, long startTime,
                                                           long endTime) {
        jobInstance.setJobName(jobName);
        jobInstance.setDisplayName(displayName);
        jobInstance.setContextName(contextName);
        jobInstance.setContextInstanceId(contextInstanceId);
        jobInstance.setStatus(status);

        MongoSchedulerJobInstanceRecordImpl record = new MongoSchedulerJobInstanceRecordImpl(contextInstanceId);
        record.setId(contextInstanceId + "-" + jobName);
        record.setJobName(jobName);
        record.setDisplayName(displayName);
        record.setContextName(contextName);
        record.setContextInstanceId(contextInstanceId);
        record.setChildContextName(childContextName);
        record.setStatus(status.name());
        record.setType(type);
        record.setStartTime(startTime);
        record.setEndTime(endTime);
        record.setSchedulerJobInstance(jobInstance);
        return record;
    }
}
