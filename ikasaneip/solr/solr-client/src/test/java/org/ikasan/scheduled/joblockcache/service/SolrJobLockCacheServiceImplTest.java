package org.ikasan.scheduled.joblockcache.service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.scheduled.cache.SolrJobLockCacheMachine;
import org.ikasan.scheduled.context.model.SolrJobLockImpl;
import org.ikasan.scheduled.job.model.SolrSchedulerJobImpl;
import org.ikasan.scheduled.joblockcache.dao.SolrJobLockCacheAuditDaoImpl;
import org.ikasan.scheduled.joblockcache.dao.SolrJobLockCacheDaoImpl;
import org.ikasan.scheduled.joblockcache.model.SolrJobLockCacheRecordImpl;
import org.ikasan.spec.scheduled.context.model.JobLock;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.context.model.JobLockHolder;
import org.ikasan.spec.scheduled.job.model.SchedulerJob;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheAuditRecord;
import org.ikasan.spec.scheduled.joblock.model.JobLockCacheRecord;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

public class SolrJobLockCacheServiceImplTest extends SolrTestCaseJ4 {

    private SolrJobLockCacheDaoImpl solrJobLockCacheDao;
    private SolrJobLockCacheAuditDaoImpl solrJobLockCacheAuditDao;
    private SolrJobLockCacheServiceImpl service;

    private NodeConfig config;
    private Path tmpPath;

    private EmbeddedSolrServer server;

    @Before
    public void setup() throws SolrServerException, IOException {
        tmpPath = createTempDir();
        config = new NodeConfig
            .NodeConfigBuilder("testnode", tmpPath)
            .setConfigSetBaseDirectory(Paths.get(getFile("solr/ikasan").getParent())
                .resolve("configsets").toString())
            .build();

        server = new EmbeddedSolrServer(config, "ikasan");
        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName("ikasan");
        createRequest.setConfigSet("minimal");
        server.request(createRequest);

        solrJobLockCacheDao = new SolrJobLockCacheDaoImpl();
        solrJobLockCacheDao.setSolrClient(server);

        solrJobLockCacheAuditDao = new SolrJobLockCacheAuditDaoImpl();
        solrJobLockCacheAuditDao.setSolrClient(server);

        service = new SolrJobLockCacheServiceImpl(solrJobLockCacheDao, solrJobLockCacheAuditDao);
    }

    @After
    public void teardown() throws IOException {
        server.close();
        FileSystemUtils.deleteRecursively(tmpPath);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfJobLockCacheDaoIsNull() {
        service = new SolrJobLockCacheServiceImpl(null, solrJobLockCacheAuditDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfJobLockCacheAuditDaoIsNull() {
        service = new SolrJobLockCacheServiceImpl(solrJobLockCacheDao, null);
    }

    @Test
    public void testSaveAndGetJobCacheInstanceAnfAuditHistory() {
        JobLockCache jlc = SolrJobLockCacheMachine.instance();
        List<JobLock> jobLocks = List.of(makeJobLock("TEST-LOCK", 3, 3), makeJobLock("TEST-LOCK-1", 2, 2));
        jlc.addLocks(jobLocks);
        jlc.lock("AgentName1-TEST-LOCK-JobName1");
        jlc.lock("AgentName1-TEST-LOCK-1-JobName1");

        long timestamp1 = System.currentTimeMillis() - 3000;
        JobLockCacheRecord record1 = new SolrJobLockCacheRecordImpl();
        record1.setJobLockCache(jlc);
        record1.setTimestamp(timestamp1);

        service.save(record1);
        validatedSavedRecord(record1, service.get(), true, true, "jockLockCacheRecordInstanceID");

        jlc.release("AgentName1-TEST-LOCK-JobName1");
        JobLockCacheRecord record2 = new SolrJobLockCacheRecordImpl();
        record2.setJobLockCache(jlc);
        long timestamp2 = System.currentTimeMillis() - 2000;
        record2.setTimestamp(timestamp2);

        service.save(record2);
        validatedSavedRecord(record2, service.get(), false, true, "jockLockCacheRecordInstanceID");

        jlc.release("AgentName1-TEST-LOCK-1-JobName1");

        JobLockCacheRecord record3 = new SolrJobLockCacheRecordImpl();
        record3.setJobLockCache(jlc);
        long timestamp3 = System.currentTimeMillis() - 1000;
        record3.setTimestamp(timestamp3);

        service.save(record3);
        validatedSavedRecord(record3, service.get(), false, false, "jockLockCacheRecordInstanceID");

        SearchResults<JobLockCacheAuditRecord> audits = service.findAll(25, 0);
        assertEquals(3, audits.getResultList().size());
        validatedSavedRecord(record1, audits.getResultList().get(0), true, true, "jockLockCacheRecordAuditID_");
        validatedSavedRecord(record2, audits.getResultList().get(1), false, true, "jockLockCacheRecordAuditID_");
        validatedSavedRecord(record3, audits.getResultList().get(2), false, false, "jockLockCacheRecordAuditID_");

        audits = service.findAll(25, 1);
        assertEquals(2, audits.getResultList().size());
        validatedSavedRecord(record2, audits.getResultList().get(0), false, true, "jockLockCacheRecordAuditID_");
        validatedSavedRecord(record3, audits.getResultList().get(1), false, false, "jockLockCacheRecordAuditID_");

        audits = service.findAll(25, 2);
        assertEquals(1, audits.getResultList().size());
        validatedSavedRecord(record3, audits.getResultList().get(0), false, false, "jockLockCacheRecordAuditID_");

        audits = service.findAll(25, 3);
        assertEquals(0, audits.getResultList().size());
    }

    private void validatedSavedRecord(JobLockCacheRecord record, JobLockCacheRecord savedRecord, boolean validateHolder1, boolean validateHolder2, String id) {
        assertNotNull(savedRecord);
        if (id.equals("jockLockCacheRecordInstanceID")) {
            assertEquals(id, savedRecord.getId());
        } else {
            assertTrue(savedRecord.getId().startsWith(id));
        }
        assertEquals(record.getTimestamp(), savedRecord.getTimestamp());
        SolrJobLockCacheMachine savedJlc = (SolrJobLockCacheMachine) savedRecord.getJobLockCache();
        assertEquals(2, savedJlc.getJobLocksByLockName().size());
        assertEquals(5, savedJlc.getJobLocksByIdentifier().size());

        JobLockHolder jobLockHolder = savedJlc.getJobLocksByLockName().get("TEST-LOCK");
        assertEquals(3, jobLockHolder.getLockCount());
        assertEquals(3, jobLockHolder.getSchedulerJobs().size());
        assertEquals("AgentName0-TEST-LOCK-JobName0", jobLockHolder.getSchedulerJobs().get(0).getIdentifier());
        assertEquals("AgentName1-TEST-LOCK-JobName1", jobLockHolder.getSchedulerJobs().get(1).getIdentifier());
        assertEquals("AgentName2-TEST-LOCK-JobName2", jobLockHolder.getSchedulerJobs().get(2).getIdentifier());
        if (validateHolder1) {
            assertEquals(1, jobLockHolder.getLockHolders().size());
            assertTrue(jobLockHolder.getLockHolders().contains("AgentName1-TEST-LOCK-JobName1"));
        } else {
            assertEquals(0, jobLockHolder.getLockHolders().size());
        }

        jobLockHolder = savedJlc.getJobLocksByLockName().get("TEST-LOCK-1");
        assertEquals(2, jobLockHolder.getLockCount());
        assertEquals(2, jobLockHolder.getSchedulerJobs().size());
        assertEquals("AgentName0-TEST-LOCK-1-JobName0", jobLockHolder.getSchedulerJobs().get(0).getIdentifier());
        assertEquals("AgentName1-TEST-LOCK-1-JobName1", jobLockHolder.getSchedulerJobs().get(1).getIdentifier());
        if (validateHolder2) {
            assertEquals(1, jobLockHolder.getLockHolders().size());
            assertTrue(jobLockHolder.getLockHolders().contains("AgentName1-TEST-LOCK-1-JobName1"));
        } else {
            assertEquals(0, jobLockHolder.getLockHolders().size());
        }
    }

    private JobLock makeJobLock(String jobLockName, int count, long jobLockCount) {
        JobLock jobLock = new SolrJobLockImpl();
        jobLock.setName(jobLockName);
        jobLock.setLockCount(jobLockCount);
        List<SchedulerJob> jobs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            jobs.add(makeSchedulerJob(i, jobLockName));
        }
        jobLock.setJobs(jobs);
        return jobLock;
    }

    private SchedulerJob makeSchedulerJob(int count, String jobLockName) {
        SchedulerJob job = new SolrSchedulerJobImpl();
        job.setAgentName("AgentName" + count);
        job.setJobName(jobLockName + "-" + "JobName" + count);
        job.setIdentifier(job.getAgentName() + "-" + job.getJobName());
        return job;
    }
}