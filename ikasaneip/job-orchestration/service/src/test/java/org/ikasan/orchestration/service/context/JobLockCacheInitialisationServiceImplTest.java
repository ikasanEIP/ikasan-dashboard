package org.ikasan.orchestration.service.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.model.cache.JobLockCacheDataImpl;
import org.ikasan.job.orchestration.model.cache.JobLockCacheRecordImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.event.model.JobLockCacheEvent;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.awaitility.Awaitility.with;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class JobLockCacheInitialisationServiceImplTest {

    @Mock
    JobLockCacheService jobLockCacheService;

    private String jsonContext;
    private String jsonContextWithSameJobLocks;
    private String jsonContext2;
    private String jsonContextJobsAddedToLocks;
    private String jsonContextJobsRemovedFromLocks;

    private ContextService contextService;

    @Before
    public void setUp() throws IOException {
        jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContextWithSameJobLocks = new String(new ClassPathResource("context-wtih-same-job-locks.json").getInputStream().readAllBytes());
        jsonContext2 = new String(new ClassPathResource("context2.json").getInputStream().readAllBytes());
        jsonContextJobsAddedToLocks = new String(new ClassPathResource("context-jobs-added-to-locks.json").getInputStream().readAllBytes());
        jsonContextJobsRemovedFromLocks = new String(new ClassPathResource("context-jobs-removed-from-locks.json").getInputStream().readAllBytes());

        this.contextService = new ContextService();
    }

    @After
    public void teardown() {
        JobLockCacheImpl.instance().setJobLockCacheService(null);
        Mockito.clearAllCaches();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_exception_job_lock_cache_service_null() {
        new JobLockCacheInitialisationServiceImpl(null);
    }

    @Test
    public void initialise_job_lock_cache_success() throws JsonProcessingException {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);

        JobLockCacheInitialisationService service = new JobLockCacheInitialisationServiceImpl(this.jobLockCacheService);
        service.initialiseJobLockCache(context, true);

        this.assertOriginalJobLockCache();
    }

    @Test
    public void initialise_job_lock_cache_success_different_threads_job_locks_shared_across_contexts() throws JsonProcessingException, InterruptedException {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);
        ContextTemplate contextWithSameLocks = this.contextService.getContextTemplate(jsonContextWithSameJobLocks);

        ReflectionTestUtils.setField(JobLockCacheImpl.instance(), "jobLockCacheService", null);

        JobLockCacheInitialisationService service = new JobLockCacheInitialisationServiceImpl(this.jobLockCacheService);

        JobLockCacheRecord jobLockCacheRecord = new JobLockCacheRecordImpl();
        jobLockCacheRecord.setJobLockCache(new JobLockCacheDataImpl());
        when(this.jobLockCacheService.get(anyString())).thenReturn(jobLockCacheRecord);

        AtomicBoolean contextOneInitComplete = new AtomicBoolean(false);
        Executors.newSingleThreadExecutor().execute(() -> {
            for(int i=0; i<100; i++) {
                service.initialiseJobLockCache(context, true);
            }
            contextOneInitComplete.set(true);
        });

        AtomicBoolean contextTwoInitComplete = new AtomicBoolean(false);
        Executors.newSingleThreadExecutor().execute(() -> {
            for(int i=0; i<100; i++) {
                service.initialiseJobLockCache(contextWithSameLocks, true);
            }
            contextTwoInitComplete.set(true);
        });

        with().pollInterval(1, TimeUnit.SECONDS).and().with().pollDelay(1, TimeUnit.SECONDS).await()
            .atMost(15, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                Assert.assertTrue(contextOneInitComplete.get() && contextTwoInitComplete.get());
            });

        this.assertTwoContextsSameJobsJobLockCache();

        service.removeJobLocksFromCache(contextWithSameLocks);

        verify(this.jobLockCacheService, times(200)).get(anyString());
        verify(this.jobLockCacheService, times(1001)).save(any(JobLockCacheRecord.class));

        verifyNoMoreInteractions(this.jobLockCacheService);

        this.assertOriginalJobLockCache();
    }

    @Test
    public void initialise_job_lock_cache_success_then_updated_with_context_with_more_jobs_in_locks() throws JsonProcessingException {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);

        JobLockCacheInitialisationService service = new JobLockCacheInitialisationServiceImpl(this.jobLockCacheService);
        service.initialiseJobLockCache(context, true);

        this.assertOriginalJobLockCache();

        context = this.contextService.getContextTemplate(this.jsonContextJobsAddedToLocks);

        service.initialiseJobLockCache(context, true);

        this.assertJobLockCacheWithJobsAdded();
    }

    @Test
    public void initialise_job_lock_cache_success_then_updated_with_context_with_less_jobs_in_locks() throws JsonProcessingException {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);

        JobLockCacheInitialisationService service = new JobLockCacheInitialisationServiceImpl(this.jobLockCacheService);
        service.initialiseJobLockCache(context, true);

        this.assertOriginalJobLockCache();

        context = this.contextService.getContextTemplate(this.jsonContextJobsRemovedFromLocks);

        service.initialiseJobLockCache(context, true);

        this.assertJobLockCacheWithLocksRemovedFromContext();
    }

    @Test
    public void initialise_job_lock_cache_success_then_updated_with_context_jobs_added_to_locks_in_context_then_removed() throws JsonProcessingException {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);

        JobLockCacheInitialisationService service = new JobLockCacheInitialisationServiceImpl(this.jobLockCacheService);
        service.initialiseJobLockCache(context, true);

        this.assertOriginalJobLockCache();

        context = this.contextService.getContextTemplate(this.jsonContextJobsAddedToLocks);

        service.initialiseJobLockCache(context, true);

        this.assertJobLockCacheWithJobsAdded();

        context = this.contextService.getContextTemplate(this.jsonContextJobsRemovedFromLocks);

        service.initialiseJobLockCache(context, true);

        this.assertJobLockCacheWithLocksRemovedFromContext();
    }

    @Test
    public void initialise_job_lock_cache_success_with_two_contexts_containing_locks() throws JsonProcessingException {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);

        JobLockCacheInitialisationService service = new JobLockCacheInitialisationServiceImpl(this.jobLockCacheService);
        service.initialiseJobLockCache(context, true);

        context = this.contextService.getContextTemplate(jsonContext2);

        service.initialiseJobLockCache(context, true);

        this.assertOriginalJobLockCache();
        this.assertOriginalJobLockCacheForSecondContext();
    }

    @Test
    public void initialise_job_lock_cache_success_with_two_contexts_containing_locks_then_remove_context() throws JsonProcessingException {
        ContextTemplate context1 = this.contextService.getContextTemplate(jsonContext);

        JobLockCacheInitialisationService service = new JobLockCacheInitialisationServiceImpl(this.jobLockCacheService);
        service.initialiseJobLockCache(context1, true);

        ContextTemplate context2 = this.contextService.getContextTemplate(jsonContext2);

        service.initialiseJobLockCache(context2, true);

        this.assertOriginalJobLockCache();
        this.assertOriginalJobLockCacheForSecondContext();

        service.removeJobLocksFromCache(context1);

        this.assertOriginalJobLockCacheRemoved();
        this.assertOriginalJobLockCacheForSecondContext();

        service.initialiseJobLockCache(context1, true);

        this.assertOriginalJobLockCache();
        this.assertOriginalJobLockCacheForSecondContext();
    }

    @Test
    public void initialise_job_lock_cache_success_same_context_added_multiple_times() throws JsonProcessingException {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);

        JobLockCacheInitialisationService service = new JobLockCacheInitialisationServiceImpl(this.jobLockCacheService);
        service.initialiseJobLockCache(context, true);
        service.initialiseJobLockCache(context, true);

        context = this.contextService.getContextTemplate(jsonContext2);

        service.initialiseJobLockCache(context, true);
        service.initialiseJobLockCache(context, true);

        this.assertOriginalJobLockCache();
        this.assertOriginalJobLockCacheForSecondContext();
    }

    private void assertOriginalJobLockCache() {
        Assert.assertNotNull(this.getJobLockCacheData());
        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName());
        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByIdentifier());

        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK"));
        Assert.assertEquals(5, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context2").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context3").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context3").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context4").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context4").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context5").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context5").get(0).getJobName());

        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK"));
        Assert.assertEquals(5, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("-505061472", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("744167903", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context2").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context3").size());
        Assert.assertEquals("1226061027", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context3").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context4").size());
        Assert.assertEquals("-213937305", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context4").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context5").size());
        Assert.assertEquals("1651431039", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context5").get(0).getJobName());

        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK"));
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("-131863702", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("-131863702", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context2").get(0).getJobName());

        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK"));
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("97656185", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("97656185", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context2").get(0).getJobName());
    }

    private void assertTwoContextsSameJobsJobLockCache() {
        Assert.assertNotNull(this.getJobLockCacheData());
        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName());
        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByIdentifier());

        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK"));
        Assert.assertEquals(5, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().size());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context2").get(0).getJobName());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context3").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context3").get(0).getJobName());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context4").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context4").get(0).getJobName());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context5").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context5").get(0).getJobName());

        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK"));
        Assert.assertEquals(5, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().size());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("-505061472", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("744167903", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context2").get(0).getJobName());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context3").size());
        Assert.assertEquals("1226061027", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context3").get(0).getJobName());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context4").size());
        Assert.assertEquals("-213937305", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context4").get(0).getJobName());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context5").size());
        Assert.assertEquals("1651431039", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context5").get(0).getJobName());

        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK"));
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().size());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("-131863702", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("-131863702", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context2").get(0).getJobName());

        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK"));
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().size());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("97656185", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("97656185", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context2").get(0).getJobName());
    }

    private void assertOriginalJobLockCacheRemoved() {
        Assert.assertNotNull(this.getJobLockCacheData());
        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName());
        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByIdentifier());

        Assert.assertNull(this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK"));
        Assert.assertNull(this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK"));
        Assert.assertNull(this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK"));
        Assert.assertNull(this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK"));
    }

    private void assertOriginalJobLockCacheForSecondContext() {
        Assert.assertNotNull(this.getJobLockCacheData());
        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName());
        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByIdentifier());

        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK"));
        Assert.assertEquals(4, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().get("AC_CHAIN_Reuters_1900").size());
        Assert.assertEquals("2127081258", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().get("AC_CHAIN_Reuters_1900").get(0).getJobName());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().get("AC_CHAIN_Reuters_1500").size());
        Assert.assertEquals("346378239", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().get("AC_CHAIN_Reuters_1500").get(0).getJobName());
        Assert.assertEquals("2127077414", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().get("AC_CHAIN_Reuters_1500").get(1).getJobName());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().get("AC_CHAIN_Reuters_1600").size());
        Assert.assertEquals("346379200", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().get("AC_CHAIN_Reuters_1600").get(0).getJobName());
        Assert.assertEquals("2127078375", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().get("AC_CHAIN_Reuters_1600").get(1).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().get("AC_CHAIN_Reuters_1100").size());
        Assert.assertEquals("2127073570", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().get("AC_CHAIN_Reuters_1100").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceValidation_LK").getSchedulerJobs().size());
        Assert.assertEquals(6, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceValidation_LK").getSchedulerJobs().get("AC_CHAIN_Validation_CheckSuspect").size());
        Assert.assertEquals("-1898950882", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceValidation_LK").getSchedulerJobs().get("AC_CHAIN_Validation_CheckSuspect").get(0).getJobName());
        Assert.assertEquals("-1898950882", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceValidation_LK").getSchedulerJobs().get("AC_CHAIN_Validation_CheckSuspect").get(1).getJobName());
        Assert.assertEquals("-1898950882", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceValidation_LK").getSchedulerJobs().get("AC_CHAIN_Validation_CheckSuspect").get(2).getJobName());
        Assert.assertEquals("-1898950882", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceValidation_LK").getSchedulerJobs().get("AC_CHAIN_Validation_CheckSuspect").get(3).getJobName());
        Assert.assertEquals("-1898950882", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceValidation_LK").getSchedulerJobs().get("AC_CHAIN_Validation_CheckSuspect").get(4).getJobName());
        Assert.assertEquals("-1898950882", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceValidation_LK").getSchedulerJobs().get("AC_CHAIN_Validation_CheckSuspect").get(5).getJobName());
        Assert.assertEquals(6, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOIETF_1500").size());
        Assert.assertEquals("1106729113", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOIETF_1500").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_Refresh_CDWAnvilSOIBondEUClose").size());
        Assert.assertEquals("-131863702", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_Refresh_CDWAnvilSOIBondEUClose").get(0).getJobName());
        Assert.assertEquals(3, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_CDW").size());
        Assert.assertEquals("31552297", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_CDW").get(0).getJobName());
        Assert.assertEquals("1122403215", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_CDW").get(1).getJobName());
        Assert.assertEquals("-1243791495", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_CDW").get(2).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_Refresh_CDWAnvilSOI").size());
        Assert.assertEquals("-131863702", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_Refresh_CDWAnvilSOI").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOI_1500").size());
        Assert.assertEquals("1106729113", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOI_1500").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOICB_1700").size());
        Assert.assertEquals("1106729113", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOICB_1700").get(0).getJobName());
        Assert.assertEquals(15, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_CapAndFloor_1500").size());
        Assert.assertEquals("-996303677", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_CapAndFloor_1500").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FGRT4PM_1500").size());
        Assert.assertEquals("-996303677", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FGRT4PM_1500").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FGEQ_1500").size());
        Assert.assertEquals("-996303677", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FGEQ_1500").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOIETF_1500").size());
        Assert.assertEquals("-996303677", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOIETF_1500").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FXVolatility_1500").size());
        Assert.assertEquals("-996303677", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FXVolatility_1500").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FGFX_1500").size());
        Assert.assertEquals("-996303677", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FGFX_1500").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOI_2100").size());
        Assert.assertEquals("-996303677", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOI_2100").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_RTCU_1500").size());
        Assert.assertEquals("-996303677", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_RTCU_1500").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SCPR_1500").size());
        Assert.assertEquals("-996303677", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SCPR_1500").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOI_1500").size());
        Assert.assertEquals("-996303677", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOI_1500").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FGRT2PM_1500").size());
        Assert.assertEquals("-996303677", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FGRT2PM_1500").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOICB_1700").size());
        Assert.assertEquals("-996303677", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOICB_1700").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FXSpot_1500").size());
        Assert.assertEquals("-996303677", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FXSpot_1500").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_CDS_1500").size());
        Assert.assertEquals("-996303677", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_CDS_1500").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_RTSH_1500").size());
        Assert.assertEquals("-996303677", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_RTSH_1500").get(0).getJobName());
        Assert.assertEquals(13, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_CurrencyFixing_1900").size());
        Assert.assertEquals("-2035100479", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_CurrencyFixing_1900").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_AnvilBondNYClose_0700").size());
        Assert.assertEquals("-505061472", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_AnvilBondNYClose_0700").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_AnvilEquityEUClose_2100").size());
        Assert.assertEquals("1651431039", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_AnvilEquityEUClose_2100").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_AnvilBondUKClose_1730").size());
        Assert.assertEquals("-213937305", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_AnvilBondUKClose_1730").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Index_1400").size());
        Assert.assertEquals("-931627624", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Index_1400").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_AnvilBondEUClose_1600").size());
        Assert.assertEquals("1226061027", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_AnvilBondEUClose_1600").get(0).getJobName());
        Assert.assertEquals(7, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Numerix_1600").size());
        Assert.assertEquals("185916817", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Numerix_1600").get(0).getJobName());
        Assert.assertEquals("-532050073", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Numerix_1600").get(1).getJobName());
        Assert.assertEquals("131944233", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Numerix_1600").get(2).getJobName());
        Assert.assertEquals("2074200534", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Numerix_1600").get(3).getJobName());
        Assert.assertEquals("959606163", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Numerix_1600").get(4).getJobName());
        Assert.assertEquals("-144197246", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Numerix_1600").get(5).getJobName());
        Assert.assertEquals("-1352189442", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Numerix_1600").get(6).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_CurrencyFixing_1100").size());
        Assert.assertEquals("-2035108167", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_CurrencyFixing_1100").get(0).getJobName());
        Assert.assertEquals(19, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").size());
        Assert.assertEquals("-920342791", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(0).getJobName());
        Assert.assertEquals("-920341830", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(1).getJobName());
        Assert.assertEquals("99965605", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(2).getJobName());
        Assert.assertEquals("423129297", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(3).getJobName());
        Assert.assertEquals("-2035103362", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(4).getJobName());
        Assert.assertEquals("-1556294089", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(5).getJobName());
        Assert.assertEquals("25247679", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(6).getJobName());
        Assert.assertEquals("-1109364553", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(7).getJobName());
        Assert.assertEquals("1366695836", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(8).getJobName());
        Assert.assertEquals("1302826201", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(9).getJobName());
        Assert.assertEquals("1912575368", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(10).getJobName());
        Assert.assertEquals("-1897845819", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(11).getJobName());
        Assert.assertEquals("-931625702", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(12).getJobName());
        Assert.assertEquals("339423874", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(13).getJobName());
        Assert.assertEquals("1199790357", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(14).getJobName());
        Assert.assertEquals("1426838181", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(15).getJobName());
        Assert.assertEquals("-181370770", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(16).getJobName());
        Assert.assertEquals("-920340869", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(17).getJobName());
        Assert.assertEquals("-1556292167", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(18).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_AnvilEquityAsiaClose_0830").size());
        Assert.assertEquals("744167903", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_AnvilEquityAsiaClose_0830").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Index_1900").size());
        Assert.assertEquals("-931622819", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Index_1900").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Index_0900").size());
        Assert.assertEquals("-931652610", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Index_0900").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Futures_2200").size());
        Assert.assertEquals("1366721783", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Futures_2200").get(0).getJobName());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_REPOMTM_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_REPOMTM_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_RP_1600").size());
        Assert.assertEquals("415472823", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_REPOMTM_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_RP_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_REPOMTM_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").size());
        Assert.assertEquals("2066850730", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_REPOMTM_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_IONBONDANDFUTURE_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_IONBONDANDFUTURE_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_IO_1600").size());
        Assert.assertEquals("-117936788", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_IONBONDANDFUTURE_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_IO_1600").get(0).getJobName());
        Assert.assertEquals(9, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOIETF_1500").size());
        Assert.assertEquals("97656185", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOIETF_1500").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_AnvilSOI_1545").size());
        Assert.assertEquals("97656185", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_AnvilSOI_1545").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOI_2100").size());
        Assert.assertEquals("97656185", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOI_2100").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_NumerixSOI_1530").size());
        Assert.assertEquals("97656185", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_NumerixSOI_1530").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_AnvilSOI_1530").size());
        Assert.assertEquals("97656185", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_AnvilSOI_1530").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOI_1500").size());
        Assert.assertEquals("97656185", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOI_1500").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_CDS_1500").size());
        Assert.assertEquals("97656185", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_CDS_1500").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_RTSH_1500").size());
        Assert.assertEquals("97656185", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_RTSH_1500").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOICB_1700").size());
        Assert.assertEquals("97656185", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOICB_1700").get(0).getJobName());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ULTUMUS_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ULTUMUS_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_UL_1200").size());
        Assert.assertEquals("-66035914", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ULTUMUS_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_UL_1200").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ULTUMUS_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_UL_1700").size());
        Assert.assertEquals("-66035914", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ULTUMUS_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_UL_1700").get(0).getJobName());
        Assert.assertEquals(34, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixBasisSwap_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixBasisSwap_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_BondGetSnap_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_BondGetSnap_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixIndex_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixIndex_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_AnvilBondEUClose_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_AnvilBondEUClose_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Fixing_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Fixing_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_AnvilEquityEUClose_2100").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_AnvilEquityEUClose_2100").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_2200").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_2200").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Futures_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Futures_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixFXSpot_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixFXSpot_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixFXVolatility_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixFXVolatility_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixSwaptionVolatility_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixSwaptionVolatility_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_AnvilEquityAsiaClose_0830").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_AnvilEquityAsiaClose_0830").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Index_0900").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Index_0900").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixDeposit_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixDeposit_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_BondGetData_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_BondGetData_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_AnvilBondUKClose_1730").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_AnvilBondUKClose_1730").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_FXForward_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_FXForward_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BT_1600").size());
        Assert.assertEquals("-1838795665", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BT_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Equity_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Equity_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixInterestRateSwap_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixInterestRateSwap_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Bond_1800").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Bond_1800").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Equity_1800").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Equity_1800").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixCapVolatility_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixCapVolatility_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_AnvilBondNYClose_0700").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_AnvilBondNYClose_0700").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_CurrencyFixing_1900").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_CurrencyFixing_1900").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Index_1400").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Index_1400").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixFRA_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixFRA_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_BondUnderlier_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_BondUnderlier_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_InterestRateSwap_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_InterestRateSwap_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Exotic_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Exotic_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Index_1900").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Index_1900").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_FRA_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_FRA_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_CreditDefaultSwap_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_CreditDefaultSwap_1600").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_FXSpot_1600").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_FXSpot_1600").get(0).getJobName());
    }

    private void assertJobLockCacheWithJobsAdded() {
        Assert.assertNotNull(this.getJobLockCacheData());
        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName());
        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByIdentifier());

        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK"));
        Assert.assertEquals(6, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context2").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context3").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context3").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context4").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context4").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context5").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context5").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context6").size());
        Assert.assertEquals("-1409548854", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context6").get(0).getJobName());

        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK"));
        Assert.assertEquals(7, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().size());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("-505061472", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals("-98367158", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context1").get(1).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("744167903", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context2").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context3").size());
        Assert.assertEquals("1226061027", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context3").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context4").size());
        Assert.assertEquals("-213937305", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context4").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context5").size());
        Assert.assertEquals("1651431039", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context5").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context6").size());
        Assert.assertEquals("1651431039", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context6").get(0).getJobName());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context7").size());
        Assert.assertEquals("1009789918", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context7").get(0).getJobName());
        Assert.assertEquals("-1349296895", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context7").get(1).getJobName());

        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK"));
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("-131863702", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("-131863702", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context2").get(0).getJobName());

        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK"));
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("97656185", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("97656185", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context2").get(0).getJobName());
        Assert.assertEquals("-1352045846", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context2").get(1).getJobName());
    }

    private void assertJobLockCacheWithLocksRemovedFromContext() {
        Assert.assertNotNull(this.getJobLockCacheData());
        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName());
        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByIdentifier());

        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK"));
        Assert.assertEquals(4, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context2").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context3").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context3").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context5").size());
        Assert.assertEquals("1164721449", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context5").get(0).getJobName());

        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK"));
        Assert.assertEquals(4, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("-505061472", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("744167903", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context2").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context3").size());
        Assert.assertEquals("1226061027", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context3").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context5").size());
        Assert.assertEquals("1651431039", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context5").get(0).getJobName());

        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK"));
        Assert.assertEquals(2, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("-131863702", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("-131863702", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context2").get(0).getJobName());

        Assert.assertNotNull(this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK"));
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("97656185", this.getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context1").get(0).getJobName());
    }

    /**
     * Retrieves a specific JobLockCacheData object from the jobLockCacheDataMap based on the key "environment".
     * This method accesses the underlying ConcurrentHashMap containing JobLockCacheData objects via reflection.
     *
     * Note that this method is protected and should be used according to the access control restrictions of the class.
     */
    protected JobLockCacheData getJobLockCacheData() {
        ConcurrentHashMap<String, JobLockCacheData> jobLockCacheDataMap
            = (ConcurrentHashMap)ReflectionTestUtils.getField(JobLockCacheImpl.instance(), "jobLockCacheDataMap");
        return jobLockCacheDataMap.get("environment");
    }
}
