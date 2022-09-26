package org.ikasan.job.orchestration.rest.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.job.orchestration.rest.dashboard.util.TestSchedulerJobRecord;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.scheduled.job.model.SolrFileEventDrivenJobImpl;
import org.ikasan.scheduled.job.model.SolrInternalEventDrivenJobImpl;
import org.ikasan.scheduled.job.model.SolrQuartzScheduleDrivenJobImpl;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.model.JobConstants;
import org.ikasan.spec.scheduled.job.model.SchedulerJobRecord;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ContextExportControl.class)
@WebAppConfiguration
@EnableWebMvc
public class ContextExportControlTest extends AbstractRestMvcTest{

    protected MockMvc mvc;

    @Autowired
    WebApplicationContext webApplicationContext;

    @MockBean
    private ScheduledContextService scheduledContextService;

    @MockBean
    private SchedulerJobService schedulerJobService;

    private final ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    @Autowired
    @Before
    public void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void testNormal() throws Exception {
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String contextName = "HelloContext";
        String jsonContext = super.loadDataFile("/data/context.json");
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        record.setContext(context);
        record.setContextName(contextName);

        doReturn(record).when(scheduledContextService).findByName(contextName);

        SearchResultsImpl searchResults = new SearchResultsImpl(createListOfJobRecords(contextName), 0, 100);

        doReturn(searchResults).when(schedulerJobService).findByContext(contextName, 50, 0);


        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/export/context/" + contextName)).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
    }

    /**
     * Windows have some unsafe characters to create files, therefore this test is using the name "Context?Name"
     * with a ? which is not allowed to be used in windows
     * Will test both the context and the jobs as jobs are created dynamically in the test based on the name of the
     * context
     * @throws Exception - file the file we are loading into the test
     */
    @Test
    public void testContextWithNotSafeCharacters() throws Exception {
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String contextName = "Context*Name";
        String jsonContext = super.loadDataFile("/data/context.json");
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        record.setContext(context);
        record.setContextName(contextName);

        doReturn(record).when(scheduledContextService).findByName(contextName);

        SearchResultsImpl searchResults = new SearchResultsImpl(createListOfJobRecords(contextName), 0, 100);

        doReturn(searchResults).when(schedulerJobService).findByContext(contextName, 50, 0);


        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/export/context/" + contextName)).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
    }

    /**
     * Setup a Context in the system as HelloContext
     * However trying to extract the context, DOESNOTEXIST
     * Program should return error 500
     * @throws Exception - file the file we are loading into the test
     */
    @Test
    public void testWhenNoContextExist() throws Exception {
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String contextName = "HelloContext";
        String jsonContext = super.loadDataFile("/data/context.json");
        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        record.setContext(context);
        record.setContextName(contextName);

        doReturn(record).when(scheduledContextService).findByName(contextName);

        SearchResultsImpl searchResults = new SearchResultsImpl(createListOfJobRecords(contextName), 0, 100);

        doReturn(searchResults).when(schedulerJobService).findByContext(contextName, 50, 0);


        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/export/context/DOESNOTEXIST")).andReturn();

        // As the context DOESNOTEXIST is not there, return error 500
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), mvcResult.getResponse().getStatus());
    }

    /**
     * Helper method to create some Jobs based on the context name being tested
     * @param contextName String value
     * @return a list of SchedulerJobRecords
     */
    private static List<SchedulerJobRecord> createListOfJobRecords(String contextName) {
        List<SchedulerJobRecord> schedulerJobRecords = new ArrayList<>();

        // Creating 9 jobs, 3 files, 3 schedulers and 3 event base ones.
        for (int i = 0; i < 3; i++) {
            SolrFileEventDrivenJobImpl solrFileEventDrivenJob = new SolrFileEventDrivenJobImpl();
            solrFileEventDrivenJob.setAgentName(contextName + "agentName" + i);
            solrFileEventDrivenJob.setJobName(contextName + "jobName" + i);
            solrFileEventDrivenJob.setIdentifier(solrFileEventDrivenJob.getAgentName() + "_" + solrFileEventDrivenJob.getJobName());
            solrFileEventDrivenJob.setContextName(contextName);
            solrFileEventDrivenJob.setCronExpression("cronExpression" + i);
            solrFileEventDrivenJob.setFilePath("filePath" + i);

            TestSchedulerJobRecord fileRecord = new TestSchedulerJobRecord();
            fileRecord.setType(JobConstants.FILE_EVENT_DRIVEN_JOB);
            fileRecord.setJob(solrFileEventDrivenJob);
            fileRecord.setId(solrFileEventDrivenJob.getAgentName() + "_" + solrFileEventDrivenJob.getJobName());
            schedulerJobRecords.add(fileRecord);

            SolrInternalEventDrivenJobImpl solrInternalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
            solrInternalEventDrivenJob.setAgentName(contextName + "agentName" + i);
            solrInternalEventDrivenJob.setJobName(contextName + "jobName" + i);
            solrInternalEventDrivenJob.setIdentifier(solrInternalEventDrivenJob.getAgentName() + "_" + solrInternalEventDrivenJob.getJobName());
            solrInternalEventDrivenJob.setContextName(contextName);
            solrInternalEventDrivenJob.setCommandLine("ls -al" + i);

            TestSchedulerJobRecord eventRecord = new TestSchedulerJobRecord();
            eventRecord.setType(JobConstants.INTERNAL_EVENT_DRIVEN_JOB);
            eventRecord.setJob(solrInternalEventDrivenJob);
            eventRecord.setId(solrInternalEventDrivenJob.getAgentName() + "_" + solrInternalEventDrivenJob.getJobName());
            schedulerJobRecords.add(eventRecord);

            SolrQuartzScheduleDrivenJobImpl solrQuartzScheduleDrivenJob = new SolrQuartzScheduleDrivenJobImpl();
            solrQuartzScheduleDrivenJob.setAgentName(contextName + "agentName" + i);
            solrQuartzScheduleDrivenJob.setJobName(contextName + "jobName" + i);
            solrQuartzScheduleDrivenJob.setIdentifier(solrQuartzScheduleDrivenJob.getAgentName() + "_" + solrQuartzScheduleDrivenJob.getJobName());
            solrQuartzScheduleDrivenJob.setContextName(contextName);
            solrQuartzScheduleDrivenJob.setCronExpression("cronExpression" + i);

            TestSchedulerJobRecord quartzRecord = new TestSchedulerJobRecord();
            quartzRecord.setType(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB);
            quartzRecord.setJob(solrQuartzScheduleDrivenJob);
            quartzRecord.setId(solrQuartzScheduleDrivenJob.getAgentName() + "_" + solrQuartzScheduleDrivenJob.getJobName());
            schedulerJobRecords.add(quartzRecord);
        }
        return schedulerJobRecords;
    }
}
