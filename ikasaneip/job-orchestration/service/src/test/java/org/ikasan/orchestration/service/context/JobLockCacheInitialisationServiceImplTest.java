package org.ikasan.orchestration.service.context;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.service.ContextService;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class JobLockCacheInitialisationServiceImplTest {

    @Mock
    JobLockCacheService jobLockCacheService;

    private String jsonContext;
    private String jsonContext2;
    private String jsonContextJobsAddedToLocks;
    private String jsonContextJobsRemovedFromLocks;

    private ContextService contextService;

    @Before
    public void setUp() throws IOException {
        jsonContext = new String(new ClassPathResource("context.json").getInputStream().readAllBytes());
        jsonContext2 = new String(new ClassPathResource("context2.json").getInputStream().readAllBytes());
        jsonContextJobsAddedToLocks = new String(new ClassPathResource("context-jobs-added-to-locks.json").getInputStream().readAllBytes());
        jsonContextJobsRemovedFromLocks = new String(new ClassPathResource("context-jobs-removed-from-locks.json").getInputStream().readAllBytes());

        this.contextService = new ContextService();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_exception_job_lock_cache_service_null() {
        new JobLockCacheInitialisationServiceImpl(null);
    }

    @Test
    public void initialise_job_lock_cache_success() throws JsonProcessingException {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);

        JobLockCacheInitialisationService service = new JobLockCacheInitialisationServiceImpl(this.jobLockCacheService);
        service.initialiseJobLockCache(context);

        this.assertOriginalJobLockCache();
    }

    @Test
    public void initialise_job_lock_cache_success_then_updated_with_context_with_more_jobs_in_locks() throws JsonProcessingException {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);

        JobLockCacheInitialisationService service = new JobLockCacheInitialisationServiceImpl(this.jobLockCacheService);
        service.initialiseJobLockCache(context);

        this.assertOriginalJobLockCache();

        context = this.contextService.getContextTemplate(this.jsonContextJobsAddedToLocks);

        service.initialiseJobLockCache(context);

        this.assertJobLockCacheWithJobsAdded();
    }

    @Test
    public void initialise_job_lock_cache_success_then_updated_with_context_with_less_jobs_in_locks() throws JsonProcessingException {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);

        JobLockCacheInitialisationService service = new JobLockCacheInitialisationServiceImpl(this.jobLockCacheService);
        service.initialiseJobLockCache(context);

        this.assertOriginalJobLockCache();

        context = this.contextService.getContextTemplate(this.jsonContextJobsRemovedFromLocks);

        service.initialiseJobLockCache(context);

        this.assertJobLockCacheWithLocksRemovedFromContext();
    }

    @Test
    public void initialise_job_lock_cache_success_then_updated_with_context_jobs_added_to_locks_in_context_then_removed() throws JsonProcessingException {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);

        JobLockCacheInitialisationService service = new JobLockCacheInitialisationServiceImpl(this.jobLockCacheService);
        service.initialiseJobLockCache(context);

        this.assertOriginalJobLockCache();

        context = this.contextService.getContextTemplate(this.jsonContextJobsAddedToLocks);

        service.initialiseJobLockCache(context);

        this.assertJobLockCacheWithJobsAdded();

        context = this.contextService.getContextTemplate(this.jsonContextJobsRemovedFromLocks);

        service.initialiseJobLockCache(context);

        this.assertJobLockCacheWithLocksRemovedFromContext();
    }

    @Test
    public void initialise_job_lock_cache_success_with_two_contexts_containing_locks() throws JsonProcessingException {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);

        JobLockCacheInitialisationService service = new JobLockCacheInitialisationServiceImpl(this.jobLockCacheService);
        service.initialiseJobLockCache(context);

        context = this.contextService.getContextTemplate(jsonContext2);

        service.initialiseJobLockCache(context);

        this.assertOriginalJobLockCache();
        this.assertOriginalJobLockCacheForSecondContext();
    }

    @Test
    public void initialise_job_lock_cache_success_with_two_contexts_containing_locks_then_remove_context() throws JsonProcessingException {
        ContextTemplate context1 = this.contextService.getContextTemplate(jsonContext);

        JobLockCacheInitialisationService service = new JobLockCacheInitialisationServiceImpl(this.jobLockCacheService);
        service.initialiseJobLockCache(context1);

        ContextTemplate context2 = this.contextService.getContextTemplate(jsonContext2);

        service.initialiseJobLockCache(context2);

        this.assertOriginalJobLockCache();
        this.assertOriginalJobLockCacheForSecondContext();

        service.removeJobLocksFromCache(context1);

        this.assertOriginalJobLockCacheRemoved();
        this.assertOriginalJobLockCacheForSecondContext();

        service.initialiseJobLockCache(context1);

        this.assertOriginalJobLockCache();
        this.assertOriginalJobLockCacheForSecondContext();
    }

    @Test
    public void initialise_job_lock_cache_success_same_context_added_multiple_times() throws JsonProcessingException {
        ContextTemplate context = this.contextService.getContextTemplate(jsonContext);

        JobLockCacheInitialisationService service = new JobLockCacheInitialisationServiceImpl(this.jobLockCacheService);
        service.initialiseJobLockCache(context);
        service.initialiseJobLockCache(context);

        context = this.contextService.getContextTemplate(jsonContext2);

        service.initialiseJobLockCache(context);
        service.initialiseJobLockCache(context);

        this.assertOriginalJobLockCache();
        this.assertOriginalJobLockCacheForSecondContext();
    }

    private void assertOriginalJobLockCache() {
        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData());
        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName());
        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByIdentifier());

        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK"));
        Assert.assertEquals(5, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context2").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context3").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context3").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context4").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context4").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context5").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context5").get(0).getJobName());

        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK"));
        Assert.assertEquals(5, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("-505061472", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("744167903", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context2").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context3").size());
        Assert.assertEquals("1226061027", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context3").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context4").size());
        Assert.assertEquals("-213937305", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context4").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context5").size());
        Assert.assertEquals("1651431039", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context5").get(0).getJobName());

        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK"));
        Assert.assertEquals(2, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("-131863702", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("-131863702", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context2").get(0).getJobName());

        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK"));
        Assert.assertEquals(2, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("97656185", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("97656185", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context2").get(0).getJobName());
    }

    private void assertOriginalJobLockCacheRemoved() {
        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData());
        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName());
        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByIdentifier());

        Assert.assertNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK"));
        Assert.assertNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK"));
        Assert.assertNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK"));
        Assert.assertNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK"));
    }

    private void assertOriginalJobLockCacheForSecondContext() {
        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData());
        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName());
        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByIdentifier());

        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK"));
        Assert.assertEquals(4, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().get("AC_CHAIN_Reuters_1900").size());
        Assert.assertEquals("2127081258", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().get("AC_CHAIN_Reuters_1900").get(0).getJobName());
        Assert.assertEquals(2, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().get("AC_CHAIN_Reuters_1500").size());
        Assert.assertEquals("346378239", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().get("AC_CHAIN_Reuters_1500").get(0).getJobName());
        Assert.assertEquals("2127077414", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().get("AC_CHAIN_Reuters_1500").get(1).getJobName());
        Assert.assertEquals(2, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().get("AC_CHAIN_Reuters_1600").size());
        Assert.assertEquals("346379200", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().get("AC_CHAIN_Reuters_1600").get(0).getJobName());
        Assert.assertEquals("2127078375", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().get("AC_CHAIN_Reuters_1600").get(1).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().get("AC_CHAIN_Reuters_1100").size());
        Assert.assertEquals("2127073570", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_RT_LK").getSchedulerJobs().get("AC_CHAIN_Reuters_1100").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceValidation_LK").getSchedulerJobs().size());
        Assert.assertEquals(6, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceValidation_LK").getSchedulerJobs().get("AC_CHAIN_Validation_CheckSuspect").size());
        Assert.assertEquals("-1898950882", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceValidation_LK").getSchedulerJobs().get("AC_CHAIN_Validation_CheckSuspect").get(0).getJobName());
        Assert.assertEquals("-1898950882", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceValidation_LK").getSchedulerJobs().get("AC_CHAIN_Validation_CheckSuspect").get(1).getJobName());
        Assert.assertEquals("-1898950882", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceValidation_LK").getSchedulerJobs().get("AC_CHAIN_Validation_CheckSuspect").get(2).getJobName());
        Assert.assertEquals("-1898950882", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceValidation_LK").getSchedulerJobs().get("AC_CHAIN_Validation_CheckSuspect").get(3).getJobName());
        Assert.assertEquals("-1898950882", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceValidation_LK").getSchedulerJobs().get("AC_CHAIN_Validation_CheckSuspect").get(4).getJobName());
        Assert.assertEquals("-1898950882", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceValidation_LK").getSchedulerJobs().get("AC_CHAIN_Validation_CheckSuspect").get(5).getJobName());
        Assert.assertEquals(6, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOIETF_1500").size());
        Assert.assertEquals("1106729113", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOIETF_1500").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_Refresh_CDWAnvilSOIBondEUClose").size());
        Assert.assertEquals("-131863702", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_Refresh_CDWAnvilSOIBondEUClose").get(0).getJobName());
        Assert.assertEquals(3, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_CDW").size());
        Assert.assertEquals("31552297", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_CDW").get(0).getJobName());
        Assert.assertEquals("1122403215", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_CDW").get(1).getJobName());
        Assert.assertEquals("-1243791495", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_CDW").get(2).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_Refresh_CDWAnvilSOI").size());
        Assert.assertEquals("-131863702", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_Refresh_CDWAnvilSOI").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOI_1500").size());
        Assert.assertEquals("1106729113", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOI_1500").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOICB_1700").size());
        Assert.assertEquals("1106729113", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_CDW_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOICB_1700").get(0).getJobName());
        Assert.assertEquals(15, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_CapAndFloor_1500").size());
        Assert.assertEquals("-996303677", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_CapAndFloor_1500").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FGRT4PM_1500").size());
        Assert.assertEquals("-996303677", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FGRT4PM_1500").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FGEQ_1500").size());
        Assert.assertEquals("-996303677", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FGEQ_1500").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOIETF_1500").size());
        Assert.assertEquals("-996303677", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOIETF_1500").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FXVolatility_1500").size());
        Assert.assertEquals("-996303677", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FXVolatility_1500").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FGFX_1500").size());
        Assert.assertEquals("-996303677", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FGFX_1500").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOI_2100").size());
        Assert.assertEquals("-996303677", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOI_2100").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_RTCU_1500").size());
        Assert.assertEquals("-996303677", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_RTCU_1500").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SCPR_1500").size());
        Assert.assertEquals("-996303677", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SCPR_1500").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOI_1500").size());
        Assert.assertEquals("-996303677", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOI_1500").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FGRT2PM_1500").size());
        Assert.assertEquals("-996303677", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FGRT2PM_1500").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOICB_1700").size());
        Assert.assertEquals("-996303677", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOICB_1700").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FXSpot_1500").size());
        Assert.assertEquals("-996303677", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_FXSpot_1500").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_CDS_1500").size());
        Assert.assertEquals("-996303677", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_CDS_1500").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_RTSH_1500").size());
        Assert.assertEquals("-996303677", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_MX_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_RTSH_1500").get(0).getJobName());
        Assert.assertEquals(13, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_CurrencyFixing_1900").size());
        Assert.assertEquals("-2035100479", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_CurrencyFixing_1900").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_AnvilBondNYClose_0700").size());
        Assert.assertEquals("-505061472", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_AnvilBondNYClose_0700").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_AnvilEquityEUClose_2100").size());
        Assert.assertEquals("1651431039", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_AnvilEquityEUClose_2100").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_AnvilBondUKClose_1730").size());
        Assert.assertEquals("-213937305", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_AnvilBondUKClose_1730").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Index_1400").size());
        Assert.assertEquals("-931627624", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Index_1400").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_AnvilBondEUClose_1600").size());
        Assert.assertEquals("1226061027", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_AnvilBondEUClose_1600").get(0).getJobName());
        Assert.assertEquals(7, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Numerix_1600").size());
        Assert.assertEquals("185916817", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Numerix_1600").get(0).getJobName());
        Assert.assertEquals("-532050073", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Numerix_1600").get(1).getJobName());
        Assert.assertEquals("131944233", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Numerix_1600").get(2).getJobName());
        Assert.assertEquals("2074200534", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Numerix_1600").get(3).getJobName());
        Assert.assertEquals("959606163", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Numerix_1600").get(4).getJobName());
        Assert.assertEquals("-144197246", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Numerix_1600").get(5).getJobName());
        Assert.assertEquals("-1352189442", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Numerix_1600").get(6).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_CurrencyFixing_1100").size());
        Assert.assertEquals("-2035108167", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_CurrencyFixing_1100").get(0).getJobName());
        Assert.assertEquals(19, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").size());
        Assert.assertEquals("-920342791", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(0).getJobName());
        Assert.assertEquals("-920341830", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(1).getJobName());
        Assert.assertEquals("99965605", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(2).getJobName());
        Assert.assertEquals("423129297", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(3).getJobName());
        Assert.assertEquals("-2035103362", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(4).getJobName());
        Assert.assertEquals("-1556294089", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(5).getJobName());
        Assert.assertEquals("25247679", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(6).getJobName());
        Assert.assertEquals("-1109364553", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(7).getJobName());
        Assert.assertEquals("1366695836", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(8).getJobName());
        Assert.assertEquals("1302826201", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(9).getJobName());
        Assert.assertEquals("1912575368", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(10).getJobName());
        Assert.assertEquals("-1897845819", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(11).getJobName());
        Assert.assertEquals("-931625702", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(12).getJobName());
        Assert.assertEquals("339423874", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(13).getJobName());
        Assert.assertEquals("1199790357", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(14).getJobName());
        Assert.assertEquals("1426838181", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(15).getJobName());
        Assert.assertEquals("-181370770", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(16).getJobName());
        Assert.assertEquals("-920340869", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(17).getJobName());
        Assert.assertEquals("-1556292167", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(18).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_AnvilEquityAsiaClose_0830").size());
        Assert.assertEquals("744167903", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_AnvilEquityAsiaClose_0830").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Index_1900").size());
        Assert.assertEquals("-931622819", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Index_1900").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Index_0900").size());
        Assert.assertEquals("-931652610", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Index_0900").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Futures_2200").size());
        Assert.assertEquals("1366721783", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_PriceConsolidation_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_Futures_2200").get(0).getJobName());
        Assert.assertEquals(2, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_REPOMTM_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_REPOMTM_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_RP_1600").size());
        Assert.assertEquals("415472823", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_REPOMTM_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_RP_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_REPOMTM_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").size());
        Assert.assertEquals("2066850730", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_REPOMTM_LK").getSchedulerJobs().get("AC_CHAIN_PriceConsolidate_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_IONBONDANDFUTURE_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_IONBONDANDFUTURE_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_IO_1600").size());
        Assert.assertEquals("-117936788", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_IONBONDANDFUTURE_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_IO_1600").get(0).getJobName());
        Assert.assertEquals(9, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOIETF_1500").size());
        Assert.assertEquals("97656185", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOIETF_1500").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_AnvilSOI_1545").size());
        Assert.assertEquals("97656185", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_AnvilSOI_1545").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOI_2100").size());
        Assert.assertEquals("97656185", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOI_2100").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_NumerixSOI_1530").size());
        Assert.assertEquals("97656185", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_NumerixSOI_1530").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_AnvilSOI_1530").size());
        Assert.assertEquals("97656185", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_AnvilSOI_1530").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOI_1500").size());
        Assert.assertEquals("97656185", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOI_1500").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_CDS_1500").size());
        Assert.assertEquals("97656185", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_CDS_1500").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_RTSH_1500").size());
        Assert.assertEquals("97656185", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_RTSH_1500").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOICB_1700").size());
        Assert.assertEquals("97656185", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ANVIL_LK").getSchedulerJobs().get("AC_CHAIN_MurexRequest_SOICB_1700").get(0).getJobName());
        Assert.assertEquals(2, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ULTUMUS_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ULTUMUS_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_UL_1200").size());
        Assert.assertEquals("-66035914", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ULTUMUS_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_UL_1200").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ULTUMUS_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_UL_1700").size());
        Assert.assertEquals("-66035914", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_ULTUMUS_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_UL_1700").get(0).getJobName());
        Assert.assertEquals(34, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixBasisSwap_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixBasisSwap_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_BondGetSnap_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_BondGetSnap_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixIndex_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixIndex_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_AnvilBondEUClose_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_AnvilBondEUClose_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Fixing_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Fixing_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_AnvilEquityEUClose_2100").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_AnvilEquityEUClose_2100").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_2200").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_2200").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Futures_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Futures_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixFXSpot_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixFXSpot_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixFXVolatility_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixFXVolatility_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixSwaptionVolatility_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixSwaptionVolatility_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_AnvilEquityAsiaClose_0830").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_AnvilEquityAsiaClose_0830").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Index_0900").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Index_0900").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixDeposit_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixDeposit_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_BondGetData_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_BondGetData_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_AnvilBondUKClose_1730").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_AnvilBondUKClose_1730").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_FXForward_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_FXForward_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BT_1600").size());
        Assert.assertEquals("-1838795665", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BT_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Equity_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Equity_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixInterestRateSwap_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixInterestRateSwap_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Bond_1800").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Bond_1800").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Equity_1800").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Equity_1800").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixCapVolatility_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixCapVolatility_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_AnvilBondNYClose_0700").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_AnvilBondNYClose_0700").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_CurrencyFixing_1900").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_CurrencyFixing_1900").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Index_1400").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Index_1400").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixFRA_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_NumerixFRA_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_BondUnderlier_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_BondUnderlier_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_InterestRateSwap_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_InterestRateSwap_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Exotic_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Exotic_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Index_1900").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_Index_1900").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_FRA_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_FRA_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_CreditDefaultSwap_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_CreditDefaultSwap_1600").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_FXSpot_1600").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("%Partition%.AC_BB_LK").getSchedulerJobs().get("AC_CHAIN_PriceLoad_BB_FXSpot_1600").get(0).getJobName());
    }

    private void assertJobLockCacheWithJobsAdded() {
        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData());
        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName());
        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByIdentifier());

        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK"));
        Assert.assertEquals(6, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context2").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context3").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context3").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context4").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context4").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context5").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context5").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context6").size());
        Assert.assertEquals("-1409548854", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context6").get(0).getJobName());

        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK"));
        Assert.assertEquals(7, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().size());
        Assert.assertEquals(2, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("-505061472", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals("-98367158", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context1").get(1).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("744167903", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context2").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context3").size());
        Assert.assertEquals("1226061027", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context3").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context4").size());
        Assert.assertEquals("-213937305", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context4").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context5").size());
        Assert.assertEquals("1651431039", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context5").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context6").size());
        Assert.assertEquals("1651431039", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context6").get(0).getJobName());
        Assert.assertEquals(2, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context7").size());
        Assert.assertEquals("1009789918", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context7").get(0).getJobName());
        Assert.assertEquals("-1349296895", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context7").get(1).getJobName());

        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK"));
        Assert.assertEquals(2, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("-131863702", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("-131863702", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context2").get(0).getJobName());

        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK"));
        Assert.assertEquals(2, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("97656185", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(2, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("97656185", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context2").get(0).getJobName());
        Assert.assertEquals("-1352045846", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context2").get(1).getJobName());
    }

    private void assertJobLockCacheWithLocksRemovedFromContext() {
        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData());
        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName());
        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByIdentifier());

        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK"));
        Assert.assertEquals(4, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context2").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context3").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context3").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context5").size());
        Assert.assertEquals("1164721449", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_BB_LK").getSchedulerJobs().get("context5").get(0).getJobName());

        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK"));
        Assert.assertEquals(4, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("-505061472", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("744167903", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context2").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context3").size());
        Assert.assertEquals("1226061027", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context3").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context5").size());
        Assert.assertEquals("1651431039", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_PriceConsolidation_LK").getSchedulerJobs().get("context5").get(0).getJobName());

        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK"));
        Assert.assertEquals(2, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("-131863702", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context1").get(0).getJobName());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context2").size());
        Assert.assertEquals("-131863702", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_CDW_LK").getSchedulerJobs().get("context2").get(0).getJobName());

        Assert.assertNotNull(JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK"));
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().size());
        Assert.assertEquals(1, JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context1").size());
        Assert.assertEquals("97656185", JobLockCacheImpl.instance().getJobLockCacheData().getJobLocksByLockName().get("AC_DEV4.AC_ANVIL_LK").getSchedulerJobs().get("context1").get(0).getJobName());
    }
}
