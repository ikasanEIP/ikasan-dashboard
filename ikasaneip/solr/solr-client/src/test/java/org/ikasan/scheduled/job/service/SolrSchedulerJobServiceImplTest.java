package org.ikasan.scheduled.job.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.scheduled.job.dao.SolrFileEventDrivenJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrInternalEventDrivenJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrQuartzScheduleDrivenJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrSchedulerJobDaoImpl;
import org.ikasan.scheduled.job.model.SolrFileEventDrivenJobImpl;
import org.ikasan.scheduled.job.model.SolrInternalEventDrivenJobImpl;
import org.ikasan.scheduled.job.model.SolrQuartzScheduleDrivenJobImpl;
import org.ikasan.scheduled.job.model.SolrSchedulerJobRecordImpl;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.search.SearchResults;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SolrSchedulerJobServiceImplTest extends SolrTestCaseJ4 {

    private SolrFileEventDrivenJobDaoImpl fileEventDrivenJobRecordDao;
    private SolrInternalEventDrivenJobDaoImpl internalEventDrivenJobRecordDao;
    private SolrQuartzScheduleDrivenJobDaoImpl quartzScheduleDrivenJobRecordDao;

    private SolrSchedulerJobDaoImpl schedulerJobRecordDao;

    private Path tmpPath;
    private EmbeddedSolrServer server;

    private SchedulerJobService service;

    @Before
    public void setup() throws SolrServerException, IOException {
        this.tmpPath = createTempDir();
        NodeConfig config = new NodeConfig
            .NodeConfigBuilder("testnode", tmpPath)
            .setConfigSetBaseDirectory(Paths.get(getFile("solr/ikasan").getParent())
                .resolve("configsets").toString())
            .build();

        this.server = new EmbeddedSolrServer(config, "ikasan");
        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName("ikasan");
        createRequest.setConfigSet("minimal");
        this.server.request(createRequest);

        this.fileEventDrivenJobRecordDao = new SolrFileEventDrivenJobDaoImpl();
        this.fileEventDrivenJobRecordDao.setSolrClient(server);

        this.internalEventDrivenJobRecordDao = new SolrInternalEventDrivenJobDaoImpl();
        this.internalEventDrivenJobRecordDao.setSolrClient(server);

        this.quartzScheduleDrivenJobRecordDao = new SolrQuartzScheduleDrivenJobDaoImpl();
        this.quartzScheduleDrivenJobRecordDao.setSolrClient(server);

        this.schedulerJobRecordDao = new SolrSchedulerJobDaoImpl();
        this.schedulerJobRecordDao.setSolrClient(server);


        this.service = new SolrSchedulerJobServiceImpl(
            this.fileEventDrivenJobRecordDao, this.internalEventDrivenJobRecordDao,
            this.quartzScheduleDrivenJobRecordDao, this.schedulerJobRecordDao
        );
    }

    @After
    public void teardown() throws IOException {
        server.close();
        FileSystemUtils.deleteRecursively(tmpPath);
    }

    @Test
    public void test_save_scheduler_records_null_records_should_not_npe() {
        service.save(null);
    }

    @Test
    public void test_save_scheduler_records_and_delete_by_context_name() {
        String contextId1 = "Context-" + RandomStringUtils.randomAlphanumeric(10);
        String contextId2 = "Context-" + RandomStringUtils.randomAlphanumeric(10);

        List<SchedulerJob> listOfRecords1 = createListOfRecords(contextId1);
        List<SchedulerJob> listOfRecords2 = createListOfRecords(contextId2);

        SearchResults results = service.findByContext(contextId1, 100, 0);
        assertEquals(0, results.getResultList().size());
        results = service.findByContext(contextId2, 100, 0);
        assertEquals(0, results.getResultList().size());

        service.save(listOfRecords1);
        service.save(listOfRecords2);

        results = service.findByContext(contextId1, 100, 0);
        validateResults(results, 9, contextId1);
        results = service.findByContext(contextId2, 100, 0);
        validateResults(results, 9, contextId2);

        service.deleteByContextName(contextId1);

        results = service.findByContext(contextId1, 100, 0);
        validateResults(results, 0, contextId1);
        results = service.findByContext(contextId2, 100, 0);
        validateResults(results, 9, contextId2);

        service.deleteByContextName(contextId2);

        results = service.findByContext(contextId1, 100, 0);
        validateResults(results, 0, contextId1);
        results = service.findByContext(contextId2, 100, 0);
        validateResults(results, 0, contextId2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_exception_skip_wrong_scheduler_job_type() {
        String contextId1 = "Context-" + RandomStringUtils.randomAlphanumeric(10);

        List<SchedulerJob> listOfRecords1 = createListOfRecords(contextId1);

        service.save(listOfRecords1);

        SearchResults<SchedulerJobRecord> results = this.service.findByContext(contextId1, -1, -1);

        List<SchedulerJobRecord> internalEventDrivenJobs = results.getResultList().stream()
            .filter(job -> job.getJob() instanceof QuartzScheduleDrivenJob)
            .collect(Collectors.toList());

        internalEventDrivenJobs.forEach(job ->
            this.service.skip(job, job.getJob().getChildContextNames(), "actor"));

        internalEventDrivenJobs.forEach(job -> {
            this.service.findByContextNameAndJobName(contextId1, job.getJobName());
        });
    }

    @Test
    public void test_skip_scheduler_job() {
        String contextId1 = "Context-" + RandomStringUtils.randomAlphanumeric(10);

        List<SchedulerJob> listOfRecords1 = createListOfRecords(contextId1);

        service.save(listOfRecords1);

        SearchResults<SchedulerJobRecord> results = this.service.findByContext(contextId1, -1, -1);

        List<SchedulerJobRecord> internalEventDrivenJobs = results.getResultList().stream()
            .filter(job -> job.getJob() instanceof InternalEventDrivenJob)
            .collect(Collectors.toList());

        internalEventDrivenJobs.forEach(job ->
            this.service.skip(job, job.getJob().getChildContextNames(), "actor"));

        internalEventDrivenJobs.forEach(job -> {
            SchedulerJobRecord schedulerJob = this.service.findByContextNameAndJobName(contextId1, job.getJobName());

            Assert.assertTrue(schedulerJob.isSkipped());
            Assert.assertFalse(schedulerJob.isHeld());
            Assert.assertEquals("actor", schedulerJob.getModifiedBy());
            Assert.assertTrue(schedulerJob.getJob().getSkippedContexts().get("child"));
        });
    }

    @Test
    public void test_skip_and_enable_scheduler_job() {
        String contextId1 = "Context-" + RandomStringUtils.randomAlphanumeric(10);

        List<SchedulerJob> listOfRecords1 = createListOfRecords(contextId1);

        service.save(listOfRecords1);

        SearchResults<SchedulerJobRecord> results = this.service.findByContext(contextId1, -1, -1);

        List<SchedulerJobRecord> internalEventDrivenJobs = results.getResultList().stream()
            .filter(job -> job.getJob() instanceof InternalEventDrivenJob)
            .collect(Collectors.toList());

        internalEventDrivenJobs.forEach(job ->
            this.service.skip(job, job.getJob().getChildContextNames(), "actor"));

        internalEventDrivenJobs.forEach(job -> {
            SchedulerJobRecord schedulerJob = this.service.findByContextNameAndJobName(contextId1, job.getJobName());

            Assert.assertTrue(schedulerJob.isSkipped());
            Assert.assertFalse(schedulerJob.isHeld());
            Assert.assertEquals("actor", schedulerJob.getModifiedBy());
            Assert.assertTrue(schedulerJob.getJob().getSkippedContexts().get("child"));
        });

        internalEventDrivenJobs.forEach(job ->
            this.service.enable(job, "actor"));

        internalEventDrivenJobs.forEach(job -> {
            SchedulerJobRecord schedulerJob = this.service.findByContextNameAndJobName(contextId1, job.getJobName());

            Assert.assertFalse(schedulerJob.isSkipped());
            Assert.assertFalse(schedulerJob.isHeld());
            Assert.assertEquals("actor", schedulerJob.getModifiedBy());
            Assert.assertTrue(schedulerJob.getJob().getSkippedContexts().isEmpty());
        });
    }

    @Test
    public void test_skip_and_enable_all_scheduler_job() {
        String contextId1 = "Context-" + RandomStringUtils.randomAlphanumeric(10);

        List<SchedulerJob> listOfRecords1 = createListOfRecords(contextId1);

        service.save(listOfRecords1);

        SearchResults<SchedulerJobRecord> results = this.service.findByContext(contextId1, -1, -1);

        List<SchedulerJobRecord> internalEventDrivenJobs = results.getResultList().stream()
            .filter(job -> job.getJob() instanceof InternalEventDrivenJob)
            .collect(Collectors.toList());

        internalEventDrivenJobs.forEach(job ->
            this.service.skip(job, job.getJob().getChildContextNames(), "actor"));

        internalEventDrivenJobs.forEach(job -> {
            SchedulerJobRecord schedulerJob = this.service.findByContextNameAndJobName(contextId1, job.getJobName());

            Assert.assertTrue(schedulerJob.isSkipped());
            Assert.assertFalse(schedulerJob.isHeld());
            Assert.assertEquals("actor", schedulerJob.getModifiedBy());
            Assert.assertTrue(schedulerJob.getJob().getSkippedContexts().get("child"));
        });

        this.service.enableAll(contextId1, "actor");

        internalEventDrivenJobs.forEach(job -> {
            SchedulerJobRecord schedulerJob = this.service.findByContextNameAndJobName(contextId1, job.getJobName());

            Assert.assertFalse(schedulerJob.isSkipped());
            Assert.assertFalse(schedulerJob.isHeld());
            Assert.assertEquals("actor", schedulerJob.getModifiedBy());
            Assert.assertTrue(schedulerJob.getJob().getSkippedContexts().isEmpty());
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_exception_hold_wrong_scheduler_job_type() {
        String contextId1 = "Context-" + RandomStringUtils.randomAlphanumeric(10);

        List<SchedulerJob> listOfRecords1 = createListOfRecords(contextId1);

        service.save(listOfRecords1);

        SearchResults<SchedulerJobRecord> results = this.service.findByContext(contextId1, -1, -1);

        List<SchedulerJobRecord> internalEventDrivenJobs = results.getResultList().stream()
            .filter(job -> job.getJob() instanceof FileEventDrivenJob)
            .collect(Collectors.toList());

        internalEventDrivenJobs.forEach(job ->
            this.service.hold(job, job.getJob().getChildContextNames(), "actor"));

        internalEventDrivenJobs.forEach(job -> {
            this.service.findByContextNameAndJobName(contextId1, job.getJobName());
        });
    }

    @Test
    public void test_hold_scheduler_job() {
        String contextId1 = "Context-" + RandomStringUtils.randomAlphanumeric(10);

        List<SchedulerJob> listOfRecords1 = createListOfRecords(contextId1);

        service.save(listOfRecords1);

        SearchResults<SchedulerJobRecord> results = this.service.findByContext(contextId1, -1, -1);

        List<SchedulerJobRecord> internalEventDrivenJobs = results.getResultList().stream()
            .filter(job -> job.getJob() instanceof InternalEventDrivenJob)
            .collect(Collectors.toList());

        internalEventDrivenJobs.forEach(job ->
            this.service.hold(job, job.getJob().getChildContextNames(), "actor"));

        internalEventDrivenJobs.forEach(job -> {
            SchedulerJobRecord schedulerJob = this.service.findByContextNameAndJobName(contextId1, job.getJobName());

            Assert.assertTrue(schedulerJob.isHeld());
            Assert.assertFalse(schedulerJob.isSkipped());
            Assert.assertEquals("actor", schedulerJob.getModifiedBy());
            Assert.assertTrue(schedulerJob.getJob().getHeldContexts().get("child"));
        });
    }

    @Test
    public void test_hold_and_release_scheduler_job() {
        String contextId1 = "Context-" + RandomStringUtils.randomAlphanumeric(10);

        List<SchedulerJob> listOfRecords1 = createListOfRecords(contextId1);

        service.save(listOfRecords1);

        SearchResults<SchedulerJobRecord> results = this.service.findByContext(contextId1, -1, -1);

        List<SchedulerJobRecord> internalEventDrivenJobs = results.getResultList().stream()
            .filter(job -> job.getJob() instanceof InternalEventDrivenJob)
            .collect(Collectors.toList());

        internalEventDrivenJobs.forEach(job ->
            this.service.hold(job, job.getJob().getChildContextNames(), "actor"));

        internalEventDrivenJobs.forEach(job -> {
            SchedulerJobRecord schedulerJob = this.service.findByContextNameAndJobName(contextId1, job.getJobName());

            Assert.assertTrue(schedulerJob.isHeld());
            Assert.assertFalse(schedulerJob.isSkipped());
            Assert.assertEquals("actor", schedulerJob.getModifiedBy());
            Assert.assertTrue(schedulerJob.getJob().getHeldContexts().get("child"));
        });

        internalEventDrivenJobs.forEach(job ->
            this.service.release(job, "actor"));

        internalEventDrivenJobs.forEach(job -> {
            SchedulerJobRecord schedulerJob = this.service.findByContextNameAndJobName(contextId1, job.getJobName());

            Assert.assertFalse(schedulerJob.isHeld());
            Assert.assertFalse(schedulerJob.isSkipped());
            Assert.assertEquals("actor", schedulerJob.getModifiedBy());
            Assert.assertTrue(schedulerJob.getJob().getHeldContexts().isEmpty());
        });
    }

    @Test
    public void test_hold_and_release_all_scheduler_job() {
        String contextId1 = "Context-" + RandomStringUtils.randomAlphanumeric(10);

        List<SchedulerJob> listOfRecords1 = createListOfRecords(contextId1);

        service.save(listOfRecords1);

        SearchResults<SchedulerJobRecord> results = this.service.findByContext(contextId1, -1, -1);

        List<SchedulerJobRecord> internalEventDrivenJobs = results.getResultList().stream()
            .filter(job -> job.getJob() instanceof InternalEventDrivenJob)
            .collect(Collectors.toList());

        internalEventDrivenJobs.forEach(job ->
            this.service.hold(job, job.getJob().getChildContextNames(), "actor"));

        internalEventDrivenJobs.forEach(job -> {
            SchedulerJobRecord schedulerJob = this.service.findByContextNameAndJobName(contextId1, job.getJobName());

            Assert.assertTrue(schedulerJob.isHeld());
            Assert.assertFalse(schedulerJob.isSkipped());
            Assert.assertEquals("actor", schedulerJob.getModifiedBy());
            Assert.assertTrue(schedulerJob.getJob().getHeldContexts().get("child"));
        });

        this.service.releaseAll(contextId1, "actor");

        internalEventDrivenJobs.forEach(job -> {
            SchedulerJobRecord schedulerJob = this.service.findByContextNameAndJobName(contextId1, job.getJobName());

            Assert.assertFalse(schedulerJob.isHeld());
            Assert.assertFalse(schedulerJob.isSkipped());
            Assert.assertEquals("actor", schedulerJob.getModifiedBy());
            Assert.assertTrue(schedulerJob.getJob().getHeldContexts().isEmpty());
        });
    }

    public void test_save_internal_event_driven_job() {
        String contextName = "contextName";
        InternalEventDrivenJob solrInternalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
        solrInternalEventDrivenJob.setAgentName(contextName + "agentName");
        solrInternalEventDrivenJob.setJobName(contextName + "jobNameInternal");
        solrInternalEventDrivenJob.setIdentifier(solrInternalEventDrivenJob.getAgentName() + "_" + solrInternalEventDrivenJob.getJobName());
        solrInternalEventDrivenJob.setContextName(contextName);
        solrInternalEventDrivenJob.setCommandLine("ls -al");
        solrInternalEventDrivenJob.setChildContextNames(List.of("child"));

        this.service.saveInternalEventDrivenJob(solrInternalEventDrivenJob, "tester");

        SchedulerJobRecord schedulerJobRecord = this.service
            .findByContextNameAndJobName("contextName", contextName + "jobNameInternal");
        InternalEventDrivenJob internalEventDrivenJob = (InternalEventDrivenJob) schedulerJobRecord.getJob();

        Assert.assertNotNull(internalEventDrivenJob);
        long modifiedTimestamp = schedulerJobRecord.getModifiedTimestamp();
        long timestamp = schedulerJobRecord.getTimestamp();
        Assert.assertTrue(schedulerJobRecord.getTimestamp() > 0);
        Assert.assertTrue(modifiedTimestamp > 0);
        Assert.assertEquals("tester", schedulerJobRecord.getModifiedBy());
        Assert.assertEquals("ls -al", internalEventDrivenJob.getCommandLine());

        internalEventDrivenJob.setCommandLine("pwd");

        this.service.saveInternalEventDrivenJob(internalEventDrivenJob, "tester2");

        schedulerJobRecord = this.service
            .findByContextNameAndJobName("contextName", contextName + "jobNameInternal");
        internalEventDrivenJob = (InternalEventDrivenJob) schedulerJobRecord.getJob();

        Assert.assertNotNull(internalEventDrivenJob);
        Assert.assertTrue(schedulerJobRecord.getTimestamp() > 0);
        Assert.assertTrue(schedulerJobRecord.getModifiedTimestamp() > 0);
        Assert.assertTrue(schedulerJobRecord.getModifiedTimestamp() > modifiedTimestamp);
        Assert.assertTrue(schedulerJobRecord.getTimestamp() == timestamp);
        Assert.assertEquals("tester2", schedulerJobRecord.getModifiedBy());
        Assert.assertEquals("pwd", internalEventDrivenJob.getCommandLine());
    }

    public void test_save_quartz_driven_job() {
        String contextName = "contextName";
        QuartzScheduleDrivenJob quartzScheduleDrivenJob = new SolrQuartzScheduleDrivenJobImpl();
        quartzScheduleDrivenJob.setAgentName(contextName + "agentName");
        quartzScheduleDrivenJob.setJobName(contextName + "jobName");
        quartzScheduleDrivenJob.setIdentifier(quartzScheduleDrivenJob.getAgentName() + "_" + quartzScheduleDrivenJob.getJobName());
        quartzScheduleDrivenJob.setContextName(contextName);
        quartzScheduleDrivenJob.setCronExpression("cronExpression");
        quartzScheduleDrivenJob.setChildContextNames(List.of("child"));

        this.service.saveQuartzScheduledJob(quartzScheduleDrivenJob, "tester");

        SchedulerJobRecord schedulerJobRecord = this.service
            .findByContextNameAndJobName("contextName", contextName + "jobName");
        QuartzScheduleDrivenJob found = (QuartzScheduleDrivenJob) schedulerJobRecord.getJob();

        Assert.assertNotNull(found);
        long modifiedTimestamp = schedulerJobRecord.getModifiedTimestamp();
        long timestamp = schedulerJobRecord.getTimestamp();
        Assert.assertTrue(schedulerJobRecord.getTimestamp() > 0);
        Assert.assertTrue(modifiedTimestamp > 0);
        Assert.assertEquals("tester", schedulerJobRecord.getModifiedBy());
        Assert.assertEquals("cronExpression", found.getCronExpression());

        found.setCronExpression("updatedCronExpression");

        this.service.saveQuartzScheduledJob(found, "tester2");

        schedulerJobRecord = this.service
            .findByContextNameAndJobName("contextName", contextName + "jobName");
        found = (QuartzScheduleDrivenJob) schedulerJobRecord.getJob();

        Assert.assertNotNull(found);
        Assert.assertTrue(schedulerJobRecord.getTimestamp() > 0);
        Assert.assertTrue(schedulerJobRecord.getModifiedTimestamp() > 0);
        Assert.assertTrue(schedulerJobRecord.getModifiedTimestamp() > modifiedTimestamp);
        Assert.assertTrue(schedulerJobRecord.getTimestamp() == timestamp);
        Assert.assertEquals("tester2", schedulerJobRecord.getModifiedBy());
        Assert.assertEquals("updatedCronExpression", found.getCronExpression());
    }

    public void test_save_file_driven_job() {
        String contextName = "contextName";
        FileEventDrivenJob fileEventDrivenJob = new SolrFileEventDrivenJobImpl();
        fileEventDrivenJob.setAgentName(contextName + "agentName");
        fileEventDrivenJob.setJobName(contextName + "jobName");
        fileEventDrivenJob.setIdentifier(fileEventDrivenJob.getAgentName() + "_" + fileEventDrivenJob.getJobName());
        fileEventDrivenJob.setContextName(contextName);
        fileEventDrivenJob.setCronExpression("cronExpression");
        fileEventDrivenJob.setChildContextNames(List.of("child"));

        this.service.saveFileEventDrivenJob(fileEventDrivenJob, "tester");

        SchedulerJobRecord schedulerJobRecord = this.service
            .findByContextNameAndJobName("contextName", contextName + "jobName");
        FileEventDrivenJob found = (FileEventDrivenJob) schedulerJobRecord.getJob();

        Assert.assertNotNull(found);
        long modifiedTimestamp = schedulerJobRecord.getModifiedTimestamp();
        long timestamp = schedulerJobRecord.getTimestamp();
        Assert.assertTrue(schedulerJobRecord.getTimestamp() > 0);
        Assert.assertTrue(modifiedTimestamp > 0);
        Assert.assertEquals("tester", schedulerJobRecord.getModifiedBy());
        Assert.assertEquals("cronExpression", found.getCronExpression());

        found.setCronExpression("updatedCronExpression");

        this.service.saveFileEventDrivenJob(found, "tester2");

        schedulerJobRecord = this.service
            .findByContextNameAndJobName("contextName", contextName + "jobName");
        found = (FileEventDrivenJob) schedulerJobRecord.getJob();

        Assert.assertNotNull(found);
        Assert.assertTrue(schedulerJobRecord.getTimestamp() > 0);
        Assert.assertTrue(schedulerJobRecord.getModifiedTimestamp() > 0);
        Assert.assertTrue(schedulerJobRecord.getModifiedTimestamp() > modifiedTimestamp);
        Assert.assertTrue(schedulerJobRecord.getTimestamp() == timestamp);
        Assert.assertEquals("tester2", schedulerJobRecord.getModifiedBy());
        Assert.assertEquals("updatedCronExpression", found.getCronExpression());
    }

    private void validateResults(SearchResults results, int expectedCount, String contextId) {
        assertEquals(expectedCount, results.getResultList().size());
        if (expectedCount > 0) {
            int resetCount = 0;
            for (int i = 0; i < results.getResultList().size(); i++) {
                SchedulerJobRecord job = (SolrSchedulerJobRecordImpl) results.getResultList().get(i);
                assertEquals(contextId + "agentName" + resetCount, job.getAgentName());
                assertEquals(contextId, job.getContextName());
                if (job.getJob() instanceof SolrFileEventDrivenJobImpl) {
                    FileEventDrivenJob fileJob = (FileEventDrivenJob) job.getJob();
                    assertEquals(contextId + "jobNameFile" + resetCount, job.getJobName());
                    assertEquals(job.getAgentName() + "_" + job.getJobName(), fileJob.getIdentifier());
                    assertEquals("cronExpression" + resetCount, fileJob.getCronExpression());
                    assertEquals("filePath" + resetCount, fileJob.getFilePath());
                }
                else if (job.getJob() instanceof SolrInternalEventDrivenJobImpl) {
                    InternalEventDrivenJob internalEventDrivenJob = (InternalEventDrivenJob) job.getJob();
                    assertEquals(contextId + "jobNameInternal" + resetCount, job.getJobName());
                    assertEquals(job.getAgentName() + "_" + job.getJobName(), internalEventDrivenJob.getIdentifier());
                    assertEquals("ls -al" + resetCount, internalEventDrivenJob.getCommandLine());
                }
                else if (job.getJob() instanceof SolrQuartzScheduleDrivenJobImpl) {
                    QuartzScheduleDrivenJob quartzScheduleDrivenJob = (QuartzScheduleDrivenJob) job.getJob();
                    assertEquals(contextId + "jobNameQuartz" + resetCount, job.getJobName());
                    assertEquals(job.getAgentName() + "_" + job.getJobName(), quartzScheduleDrivenJob.getIdentifier());
                    assertEquals("cronExpression" + resetCount, quartzScheduleDrivenJob.getCronExpression());
                }
                resetCount++;
                if (resetCount == 3) {
                    resetCount = 0;
                }
            }
        }
    }

    private List<SchedulerJob> createListOfRecords(String contextId) {
        List<SchedulerJob> jobs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            SolrFileEventDrivenJobImpl solrFileEventDrivenJob = new SolrFileEventDrivenJobImpl();
            solrFileEventDrivenJob.setAgentName(contextId + "agentName" + i);
            solrFileEventDrivenJob.setJobName(contextId + "jobNameFile" + i);
            solrFileEventDrivenJob.setIdentifier(solrFileEventDrivenJob.getAgentName() + "_" + solrFileEventDrivenJob.getJobName());
            solrFileEventDrivenJob.setContextName(contextId);
            solrFileEventDrivenJob.setCronExpression("cronExpression" + i);
            solrFileEventDrivenJob.setFilePath("filePath" + i);
            solrFileEventDrivenJob.setChildContextNames(List.of("child"));

            SolrInternalEventDrivenJobImpl solrInternalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
            solrInternalEventDrivenJob.setAgentName(contextId + "agentName" + i);
            solrInternalEventDrivenJob.setJobName(contextId + "jobNameInternal" + i);
            solrInternalEventDrivenJob.setIdentifier(solrInternalEventDrivenJob.getAgentName() + "_" + solrInternalEventDrivenJob.getJobName());
            solrInternalEventDrivenJob.setContextName(contextId);
            solrInternalEventDrivenJob.setCommandLine("ls -al" + i);
            solrInternalEventDrivenJob.setChildContextNames(List.of("child"));

            SolrQuartzScheduleDrivenJobImpl solrQuartzScheduleDrivenJob = new SolrQuartzScheduleDrivenJobImpl();
            solrQuartzScheduleDrivenJob.setAgentName(contextId + "agentName" + i);
            solrQuartzScheduleDrivenJob.setJobName(contextId + "jobNameQuartz" + i);
            solrQuartzScheduleDrivenJob.setIdentifier(solrQuartzScheduleDrivenJob.getAgentName() + "_" + solrQuartzScheduleDrivenJob.getJobName());
            solrQuartzScheduleDrivenJob.setContextName(contextId);
            solrQuartzScheduleDrivenJob.setCronExpression("cronExpression" + i);
            solrQuartzScheduleDrivenJob.setChildContextNames(List.of("child"));

            jobs.add(solrFileEventDrivenJob);
            jobs.add(solrInternalEventDrivenJob);
            jobs.add(solrQuartzScheduleDrivenJob);
        }

        return jobs;
    }
}