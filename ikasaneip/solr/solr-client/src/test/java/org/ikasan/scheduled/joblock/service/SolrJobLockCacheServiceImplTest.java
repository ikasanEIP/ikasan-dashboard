package org.ikasan.scheduled.joblock.service;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.job.orchestration.model.context.JobLockHolderImpl;
import org.ikasan.job.orchestration.model.event.ContextualisedSchedulerJobInitiationEventImpl;
import org.ikasan.job.orchestration.model.event.SchedulerJobInitiationEventImpl;
import org.ikasan.scheduled.context.model.SolrJobLockImpl;
import org.ikasan.scheduled.job.model.SolrSchedulerJobImpl;
import org.ikasan.scheduled.job.model.SolrSchedulerJobLockParticipantImpl;
import org.ikasan.scheduled.joblock.dao.SolrJobLockCacheAuditDaoImpl;
import org.ikasan.scheduled.joblock.dao.SolrJobLockCacheDaoImpl;
import org.ikasan.scheduled.joblock.model.SolrJobLockCacheDataImpl;
import org.ikasan.scheduled.joblock.model.SolrJobLockCacheRecordImpl;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.job.model.SchedulerJobLockParticipant;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheAuditRecord;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheData;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SolrJobLockCacheServiceImplTest extends SolrTestCaseJ4 {

    private SolrJobLockCacheDaoImpl jobLockCacheDao;
    private SolrJobLockCacheAuditDaoImpl jobLockCacheAuditDao;
    private JobLockCacheService service;

    private Path tmpPath;
    private EmbeddedSolrServer server;

    @Before
    public void setup() throws SolrServerException, IOException {
        tmpPath = createTempDir();
        NodeConfig config = new NodeConfig
            .NodeConfigBuilder("testnode", tmpPath)
            .setConfigSetBaseDirectory(Paths.get(getFile("solr/ikasan").getParent())
                .resolve("configsets").toString())
            .build();

        server = new EmbeddedSolrServer(config, "ikasan");
        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName("ikasan");
        createRequest.setConfigSet("minimal");
        server.request(createRequest);

        jobLockCacheDao = new SolrJobLockCacheDaoImpl();
        jobLockCacheDao.setSolrClient(server);

        jobLockCacheAuditDao = new SolrJobLockCacheAuditDaoImpl();
        jobLockCacheAuditDao.setSolrClient(server);

        service = new SolrJobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, true);
    }

    @After
    public void teardown() throws IOException {
        server.close();
        FileSystemUtils.deleteRecursively(tmpPath);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_should_throw_exception_if_job_lock_cache_dao_is_null() {
        service = new SolrJobLockCacheServiceImpl(null, jobLockCacheAuditDao, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_should_throw_exception_if_job_lock_cache_audit_dao_is_null() {
        service = new SolrJobLockCacheServiceImpl(jobLockCacheDao, null, true);
    }

    @Test
    public void test_save_and_get_job_cache_instance_null_environment() {
        // set the flag to false for save audit records
        ReflectionTestUtils.setField(service, "saveJobLockCacheAudits", Boolean.FALSE);

        SearchResults<JobLockCacheAuditRecord> audits = service.findAll(25, 0);
        assertEquals(0, audits.getResultList().size());

        List<JobLock> jobLocks = List.of(makeJobLock("TEST-LOCK", 3, 3)
            , makeJobLock("TEST-LOCK-1", 2, 2));

        JobLockCacheData jobLockCacheData = new SolrJobLockCacheDataImpl();
        this.addLocks(jobLockCacheData, jobLocks);

        JobLockCacheRecord record1 = new SolrJobLockCacheRecordImpl();
        record1.setJobLockCache(jobLockCacheData);

        service.save(record1);
        audits = service.findAll(25, 0);
        assertEquals(0, audits.getResultList().size());

        JobLockCacheRecord savedRecord = service.get(null);

        assertNotNull(savedRecord);
        assertEquals(JobLockCacheRecord.DEFAULT_ENVIRONMENT, savedRecord.getEnvironment());
        if (savedRecord.getId().equals(SolrJobLockCacheDaoImpl.JOB_LOCK_CACHE_ID)) {
            assertEquals(savedRecord.getId(), savedRecord.getId());
        }
        // make sure the timestamp is within the last couple of seconds
        assertTrue(savedRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && savedRecord.getTimestamp() <= System.currentTimeMillis());

        JobLockCacheData savedRecordJobLockCache = savedRecord.getJobLockCache();
        assertEquals(2, savedRecordJobLockCache.getJobLocksByLockName().size());
        assertEquals(5, savedRecordJobLockCache.getJobLocksByIdentifier().size());

        JobLockHolder jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK");
        assertEquals(3, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());

        jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK-1");
        assertEquals(2, jobLockHolder.getLockCount());
        assertEquals(2, jobLockHolder.getSchedulerJobs().size());

        jobLocks = List.of(makeJobLock("TEST-LOCK-3", 5, 1)
            , makeJobLock("TEST-LOCK-4", 20, 1));

        this.addLocks(jobLockCacheData, jobLocks);
        record1.setJobLockCache(jobLockCacheData);

        service.save(record1);
        audits = service.findAll(25, 0);
        assertEquals(0, audits.getResultList().size());

        savedRecord = service.get(null);

        assertNotNull(savedRecord);
        assertEquals(JobLockCacheRecord.DEFAULT_ENVIRONMENT, savedRecord.getEnvironment());
        if (savedRecord.getId().equals(SolrJobLockCacheDaoImpl.JOB_LOCK_CACHE_ID)) {
            assertEquals(savedRecord.getId(), savedRecord.getId());
        }
        // make sure the timestamp is within the last couple of seconds
        assertTrue(savedRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && savedRecord.getTimestamp() <= System.currentTimeMillis());

        savedRecordJobLockCache = savedRecord.getJobLockCache();
        assertEquals(4, savedRecordJobLockCache.getJobLocksByLockName().size());
        assertEquals(30, savedRecordJobLockCache.getJobLocksByIdentifier().size());

        jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK");
        assertEquals(3, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());

        jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK-1");
        assertEquals(2, jobLockHolder.getLockCount());
        assertEquals(2, jobLockHolder.getSchedulerJobs().size());

        jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK-3");
        assertEquals(1, jobLockHolder.getLockCount());
        assertEquals(5, jobLockHolder.getSchedulerJobs().size());

        jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK-4");
        assertEquals(1, jobLockHolder.getLockCount());
        assertEquals(20, jobLockHolder.getSchedulerJobs().size());
    }

    @Test
    public void test_save_and_get_job_cache_instance_non_null_environment() {
        // set the flag to false for save audit records
        ReflectionTestUtils.setField(service, "saveJobLockCacheAudits", Boolean.FALSE);

        SearchResults<JobLockCacheAuditRecord> audits = service.findAll(25, 0);
        assertEquals(0, audits.getResultList().size());

        List<JobLock> jobLocks = List.of(makeJobLock("TEST-LOCK", 3, 3)
            , makeJobLock("TEST-LOCK-1", 2, 2));

        JobLockCacheData jobLockCacheData = new SolrJobLockCacheDataImpl();
        this.addLocks(jobLockCacheData, jobLocks);

        JobLockCacheRecord record1 = new SolrJobLockCacheRecordImpl();
        record1.setEnvironment("environment");
        record1.setJobLockCache(jobLockCacheData);

        service.save(record1);
        audits = service.findAll(25, 0);
        assertEquals(0, audits.getResultList().size());

        JobLockCacheRecord savedRecord = service.get("environment");

        assertNotNull(savedRecord);
        assertEquals("environment", savedRecord.getEnvironment());
        if (savedRecord.getId().equals(SolrJobLockCacheDaoImpl.JOB_LOCK_CACHE_ID)) {
            assertEquals(savedRecord.getId(), savedRecord.getId());
        }
        // make sure the timestamp is within the last couple of seconds
        assertTrue(savedRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && savedRecord.getTimestamp() <= System.currentTimeMillis());

        JobLockCacheData savedRecordJobLockCache = savedRecord.getJobLockCache();
        assertEquals(2, savedRecordJobLockCache.getJobLocksByLockName().size());
        assertEquals(5, savedRecordJobLockCache.getJobLocksByIdentifier().size());

        JobLockHolder jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK");
        assertEquals(3, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());

        jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK-1");
        assertEquals(2, jobLockHolder.getLockCount());
        assertEquals(2, jobLockHolder.getSchedulerJobs().size());

        jobLocks = List.of(makeJobLock("TEST-LOCK-3", 5, 1)
            , makeJobLock("TEST-LOCK-4", 20, 1));

        this.addLocks(jobLockCacheData, jobLocks);
        record1.setJobLockCache(jobLockCacheData);

        service.save(record1);
        audits = service.findAll(25, 0);
        assertEquals(0, audits.getResultList().size());

        savedRecord = service.get("environment");

        assertNotNull(savedRecord);
        assertEquals("environment", savedRecord.getEnvironment());
        if (savedRecord.getId().equals(SolrJobLockCacheDaoImpl.JOB_LOCK_CACHE_ID)) {
            assertEquals(savedRecord.getId(), savedRecord.getId());
        }
        // make sure the timestamp is within the last couple of seconds
        assertTrue(savedRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && savedRecord.getTimestamp() <= System.currentTimeMillis());

        savedRecordJobLockCache = savedRecord.getJobLockCache();
        assertEquals(4, savedRecordJobLockCache.getJobLocksByLockName().size());
        assertEquals(30, savedRecordJobLockCache.getJobLocksByIdentifier().size());

        jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK");
        assertEquals(3, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());

        jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK-1");
        assertEquals(2, jobLockHolder.getLockCount());
        assertEquals(2, jobLockHolder.getSchedulerJobs().size());

        jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK-3");
        assertEquals(1, jobLockHolder.getLockCount());
        assertEquals(5, jobLockHolder.getSchedulerJobs().size());

        jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK-4");
        assertEquals(1, jobLockHolder.getLockCount());
        assertEquals(20, jobLockHolder.getSchedulerJobs().size());
    }

    @Test
    public void test_save_and_get_job_cache_instance_with_audit_null_environment() {

        SearchResults<JobLockCacheAuditRecord> audits = service.findAll(25, 0);
        assertEquals(0, audits.getResultList().size());

        List<JobLock> jobLocks = List.of(makeJobLock("TEST-LOCK", 3, 3)
            , makeJobLock("TEST-LOCK-1", 2, 2));

        JobLockCacheData jobLockCacheData = new SolrJobLockCacheDataImpl();
        this.addLocks(jobLockCacheData, jobLocks);

        JobLockCacheRecord record1 = new SolrJobLockCacheRecordImpl();
        record1.setJobLockCache(jobLockCacheData);

        service.save(record1);
        audits = service.findAll(25, 0);
        assertEquals(1, audits.getResultList().size());

        JobLockCacheRecord savedRecord = service.get(null);

        assertNotNull(savedRecord);
        assertEquals(JobLockCacheRecord.DEFAULT_ENVIRONMENT, savedRecord.getEnvironment());
        if (savedRecord.getId().equals(SolrJobLockCacheDaoImpl.JOB_LOCK_CACHE_ID)) {
            assertEquals(savedRecord.getId(), savedRecord.getId());
        }
        // make sure the timestamp is within the last couple of seconds
        assertTrue(savedRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && savedRecord.getTimestamp() <= System.currentTimeMillis());

        JobLockCacheData savedRecordJobLockCache = savedRecord.getJobLockCache();
        assertEquals(2, savedRecordJobLockCache.getJobLocksByLockName().size());
        assertEquals(5, savedRecordJobLockCache.getJobLocksByIdentifier().size());

        JobLockHolder jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK");
        assertEquals(3, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        assertEquals(3, jobLockHolder.getSchedulerJobInitiationEventWaitQueue().size());

        jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK-1");
        assertEquals(2, jobLockHolder.getLockCount());
        assertEquals(2, jobLockHolder.getSchedulerJobs().size());
        assertEquals(2, jobLockHolder.getSchedulerJobInitiationEventWaitQueue().size());

        jobLocks = List.of(makeJobLock("TEST-LOCK-3", 5, 1)
            , makeJobLock("TEST-LOCK-4", 20, 1));

        this.addLocks(jobLockCacheData, jobLocks);
        record1.setJobLockCache(jobLockCacheData);

        service.save(record1);
        audits = service.findAll(25, 0);
        assertEquals(2, audits.getResultList().size());

        savedRecord = service.get(null);

        assertNotNull(savedRecord);
        assertEquals(JobLockCacheRecord.DEFAULT_ENVIRONMENT, savedRecord.getEnvironment());
        if (savedRecord.getId().equals(SolrJobLockCacheDaoImpl.JOB_LOCK_CACHE_ID)) {
            assertEquals(savedRecord.getId(), savedRecord.getId());
        }
        // make sure the timestamp is within the last couple of seconds
        assertTrue(savedRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && savedRecord.getTimestamp() <= System.currentTimeMillis());

        savedRecordJobLockCache = savedRecord.getJobLockCache();
        assertEquals(4, savedRecordJobLockCache.getJobLocksByLockName().size());
        assertEquals(30, savedRecordJobLockCache.getJobLocksByIdentifier().size());

        jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK");
        assertEquals(3, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        assertEquals(3, jobLockHolder.getSchedulerJobInitiationEventWaitQueue().size());

        jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK-1");
        assertEquals(2, jobLockHolder.getLockCount());
        assertEquals(2, jobLockHolder.getSchedulerJobs().size());
        assertEquals(2, jobLockHolder.getSchedulerJobInitiationEventWaitQueue().size());

        jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK-3");
        assertEquals(1, jobLockHolder.getLockCount());
        assertEquals(5, jobLockHolder.getSchedulerJobs().size());
        assertEquals(5, jobLockHolder.getSchedulerJobInitiationEventWaitQueue().size());

        jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK-4");
        assertEquals(1, jobLockHolder.getLockCount());
        assertEquals(20, jobLockHolder.getSchedulerJobs().size());
        assertEquals(20, jobLockHolder.getSchedulerJobInitiationEventWaitQueue().size());
    }

    @Test
    public void test_save_and_get_job_cache_instance_with_audit_non_null_environment() {

        SearchResults<JobLockCacheAuditRecord> audits = service.findAll(25, 0);
        assertEquals(0, audits.getResultList().size());

        List<JobLock> jobLocks = List.of(makeJobLock("TEST-LOCK", 3, 3)
            , makeJobLock("TEST-LOCK-1", 2, 2));

        JobLockCacheData jobLockCacheData = new SolrJobLockCacheDataImpl();
        this.addLocks(jobLockCacheData, jobLocks);

        JobLockCacheRecord record1 = new SolrJobLockCacheRecordImpl();
        record1.setEnvironment("environment");
        record1.setJobLockCache(jobLockCacheData);

        service.save(record1);
        audits = service.findAll(25, 0);
        assertEquals(1, audits.getResultList().size());

        JobLockCacheRecord savedRecord = service.get("environment");

        assertNotNull(savedRecord);
        assertEquals("environment", savedRecord.getEnvironment());
        if (savedRecord.getId().equals(SolrJobLockCacheDaoImpl.JOB_LOCK_CACHE_ID)) {
            assertEquals(savedRecord.getId(), savedRecord.getId());
        }
        // make sure the timestamp is within the last couple of seconds
        assertTrue(savedRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && savedRecord.getTimestamp() <= System.currentTimeMillis());

        JobLockCacheData savedRecordJobLockCache = savedRecord.getJobLockCache();
        assertEquals(2, savedRecordJobLockCache.getJobLocksByLockName().size());
        assertEquals(5, savedRecordJobLockCache.getJobLocksByIdentifier().size());

        JobLockHolder jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK");
        assertEquals(3, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        assertEquals(3, jobLockHolder.getSchedulerJobInitiationEventWaitQueue().size());

        jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK-1");
        assertEquals(2, jobLockHolder.getLockCount());
        assertEquals(2, jobLockHolder.getSchedulerJobs().size());
        assertEquals(2, jobLockHolder.getSchedulerJobInitiationEventWaitQueue().size());

        jobLocks = List.of(makeJobLock("TEST-LOCK-3", 5, 1)
            , makeJobLock("TEST-LOCK-4", 20, 1));

        this.addLocks(jobLockCacheData, jobLocks);
        record1.setJobLockCache(jobLockCacheData);

        service.save(record1);
        audits = service.findAll(25, 0);
        assertEquals(2, audits.getResultList().size());

        savedRecord = service.get("environment");

        assertNotNull(savedRecord);
        assertEquals("environment", savedRecord.getEnvironment());
        if (savedRecord.getId().equals(SolrJobLockCacheDaoImpl.JOB_LOCK_CACHE_ID)) {
            assertEquals(savedRecord.getId(), savedRecord.getId());
        }
        // make sure the timestamp is within the last couple of seconds
        assertTrue(savedRecord.getTimestamp() >= System.currentTimeMillis() - 2000 && savedRecord.getTimestamp() <= System.currentTimeMillis());

        savedRecordJobLockCache = savedRecord.getJobLockCache();
        assertEquals(4, savedRecordJobLockCache.getJobLocksByLockName().size());
        assertEquals(30, savedRecordJobLockCache.getJobLocksByIdentifier().size());

        jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK");
        assertEquals(3, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        assertEquals(3, jobLockHolder.getSchedulerJobInitiationEventWaitQueue().size());

        jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK-1");
        assertEquals(2, jobLockHolder.getLockCount());
        assertEquals(2, jobLockHolder.getSchedulerJobs().size());
        assertEquals(2, jobLockHolder.getSchedulerJobInitiationEventWaitQueue().size());

        jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK-3");
        assertEquals(1, jobLockHolder.getLockCount());
        assertEquals(5, jobLockHolder.getSchedulerJobs().size());
        assertEquals(5, jobLockHolder.getSchedulerJobInitiationEventWaitQueue().size());

        jobLockHolder = savedRecordJobLockCache.getJobLocksByLockName().get("TEST-LOCK-4");
        assertEquals(1, jobLockHolder.getLockCount());
        assertEquals(20, jobLockHolder.getSchedulerJobs().size());
        assertEquals(20, jobLockHolder.getSchedulerJobInitiationEventWaitQueue().size());
    }

    private JobLock makeJobLock(String jobLockName, int count, int jobLockCount) {
        JobLock jobLock = new SolrJobLockImpl();
        jobLock.setName(jobLockName);
        jobLock.setLockCount(jobLockCount);
        Map<String, List<SchedulerJobLockParticipant>> jobs = new HashMap<>();
        for (int i = 0; i < count; i++) {
            jobs.put("contextName"+i, makeSchedulerJobLockParticipant(i, jobLockName));
        }
        jobLock.setJobs(jobs);
        return jobLock;
    }

    private List<SchedulerJob> makeSchedulerJob(int count, String jobLockName) {
        SchedulerJob job = new SolrSchedulerJobImpl();
        job.setAgentName("AgentName" + count);
        job.setJobName(jobLockName + "-" + "JobName" + count);
        job.setIdentifier(job.getAgentName() + "-" + job.getJobName());
        return List.of(job);
    }

    private List<SchedulerJobLockParticipant> makeSchedulerJobLockParticipant(int count, String jobLockName) {
        SchedulerJobLockParticipant job = new SolrSchedulerJobLockParticipantImpl();
        job.setAgentName("AgentName" + count);
        job.setJobName(jobLockName + "-" + "JobName" + count);
        job.setIdentifier(job.getAgentName() + "-" + job.getJobName());
        job.setLockCount(1);
        return List.of(job);
    }

    private JobLockCacheData addLocks(JobLockCacheData jobLockCacheData, List<JobLock> jobLocks) {
        jobLocks.forEach(lock -> this.addLock(lock, jobLockCacheData));

        return jobLockCacheData;
    }

    private JobLockCacheData addLock(JobLock jobLock, JobLockCacheData jobLockCacheData) {
        if (jobLock != null) {
            // we need jobLocksByLockName to create the global lock holder added later in jobLocksByIdentifier
            JobLockHolder jobLockHolder = jobLockCacheData.getJobLocksByLockName().get(jobLock.getName());
            if (jobLockHolder == null) {
                jobLockHolder = new JobLockHolderImpl();
                jobLockHolder.setLockName(jobLock.getName());
                jobLockHolder.setLockCount(jobLock.getLockCount());
                for (Map.Entry<String, List<SchedulerJobLockParticipant>> entry : jobLock.getJobs().entrySet()) {
                    jobLockHolder.addSchedulerJobs(entry.getKey(), entry.getValue());
                    SchedulerJobInitiationEventImpl schedulerJobInitiationEvent
                        = new SchedulerJobInitiationEventImpl();
                    schedulerJobInitiationEvent.setJobName(entry.getKey());

                    ContextualisedSchedulerJobInitiationEventImpl contextualisedSchedulerJobInitiationEvent
                        = new ContextualisedSchedulerJobInitiationEventImpl();
                    contextualisedSchedulerJobInitiationEvent.setContextName("contextName");
                    contextualisedSchedulerJobInitiationEvent.setSchedulerJobInitiationEvent(schedulerJobInitiationEvent);
                    jobLockHolder.getSchedulerJobInitiationEventWaitQueue().offer(contextualisedSchedulerJobInitiationEvent);
                }
            } else {
                for (Map.Entry<String, List<SchedulerJobLockParticipant>> entry : jobLock.getJobs().entrySet()) {
                    jobLockHolder.addSchedulerJobs(entry.getKey(), entry.getValue());
                }
            }
            jobLockCacheData.getJobLocksByLockName().put(jobLock.getName(), jobLockHolder);

            List<SchedulerJob> jobs = jobLockCacheData.getJobLocksByLockName().get(jobLock.getName())
                .getSchedulerJobs()
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

            for (SchedulerJob schedulerJob : jobs) {
                jobLockCacheData.getJobLocksByIdentifier().put(schedulerJob.getIdentifier(), jobLock.getName());
            }
        }

        return jobLockCacheData;
    }
}