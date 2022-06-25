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
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

    private void validateResults(SearchResults results, int expectedCount, String contextId) {
        assertEquals(expectedCount, results.getResultList().size());
        if (expectedCount > 0) {
            int resetCount = 0;
            for (int i = 0; i < results.getResultList().size(); i++) {
                SchedulerJobRecord job = (SolrSchedulerJobRecordImpl) results.getResultList().get(i);
                assertEquals(contextId + "agentName" + resetCount, job.getAgentName());
                assertEquals(contextId + "jobName" + resetCount, job.getJobName());
                assertEquals(contextId, job.getContextId());
                if (job.getJob() instanceof SolrFileEventDrivenJobImpl) {
                    FileEventDrivenJob fileJob = (FileEventDrivenJob) job.getJob();
                    assertEquals(job.getAgentName() + "_" + job.getJobName(), fileJob.getIdentifier());
                    assertEquals("cronExpression" + resetCount, fileJob.getCronExpression());
                    assertEquals("filePath" + resetCount, fileJob.getFilePath());
                }
                if (job.getJob() instanceof SolrInternalEventDrivenJobImpl) {
                    InternalEventDrivenJob internalEventDrivenJob = (InternalEventDrivenJob) job.getJob();
                    assertEquals(job.getAgentName() + "_" + job.getJobName(), internalEventDrivenJob.getIdentifier());
                    assertEquals("ls -al" + resetCount, internalEventDrivenJob.getCommandLine());
                }
                if (job.getJob() instanceof SolrQuartzScheduleDrivenJobImpl) {
                    QuartzScheduleDrivenJob quartzScheduleDrivenJob = (QuartzScheduleDrivenJob) job.getJob();
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
            solrFileEventDrivenJob.setJobName(contextId + "jobName" + i);
            solrFileEventDrivenJob.setIdentifier(solrFileEventDrivenJob.getAgentName() + "_" + solrFileEventDrivenJob.getJobName());
            solrFileEventDrivenJob.setContextId(contextId);
            solrFileEventDrivenJob.setCronExpression("cronExpression" + i);
            solrFileEventDrivenJob.setFilePath("filePath" + i);

            SolrInternalEventDrivenJobImpl solrInternalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
            solrInternalEventDrivenJob.setAgentName(contextId + "agentName" + i);
            solrInternalEventDrivenJob.setJobName(contextId + "jobName" + i);
            solrInternalEventDrivenJob.setIdentifier(solrInternalEventDrivenJob.getAgentName() + "_" + solrInternalEventDrivenJob.getJobName());
            solrInternalEventDrivenJob.setContextId(contextId);
            solrInternalEventDrivenJob.setCommandLine("ls -al" + i);

            SolrQuartzScheduleDrivenJobImpl solrQuartzScheduleDrivenJob = new SolrQuartzScheduleDrivenJobImpl();
            solrQuartzScheduleDrivenJob.setAgentName(contextId + "agentName" + i);
            solrQuartzScheduleDrivenJob.setJobName(contextId + "jobName" + i);
            solrQuartzScheduleDrivenJob.setIdentifier(solrQuartzScheduleDrivenJob.getAgentName() + "_" + solrQuartzScheduleDrivenJob.getJobName());
            solrQuartzScheduleDrivenJob.setContextId(contextId);
            solrQuartzScheduleDrivenJob.setCronExpression("cronExpression" + i);

            jobs.add(solrFileEventDrivenJob);
            jobs.add(solrInternalEventDrivenJob);
            jobs.add(solrQuartzScheduleDrivenJob);
        }

        return jobs;
    }
}