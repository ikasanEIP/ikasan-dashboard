package org.ikasan.orchestration.service.context.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.job.orchestration.model.profile.ContextProfileRecordImpl;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.orchestration.service.utils.TestSchedulerJobRecord;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.scheduled.job.model.SolrFileEventDrivenJobImpl;
import org.ikasan.scheduled.job.model.SolrGlobalEventJobImpl;
import org.ikasan.scheduled.job.model.SolrInternalEventDrivenJobImpl;
import org.ikasan.scheduled.job.model.SolrQuartzScheduleDrivenJobImpl;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationContextImpl;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationContextRecordImpl;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationDetails;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.model.*;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationContextRecord;
import org.ikasan.spec.scheduled.notification.model.EmailNotificationDetailsRecord;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.scheduled.profile.model.ContextProfileSearchFilter;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class ContextExportZipUtilsTest {

    @Mock
    private ScheduledContextService scheduledContextService;

    @Mock
    private SchedulerJobService schedulerJobService;

    @Mock
    private EmailNotificationDetailsService emailNotificationDetailsService;

    @Mock
    private EmailNotificationContextService emailNotificationContextService;

    @Mock
    private ContextProfileService contextProfileService;

    private final ObjectMapper objectMapper = ObjectMapperFactory.newInstance();


    @Test
    public void test_export_zip_with_tokens() throws Exception {
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String contextName = "HelloContext";
        String jsonContext = this.loadDataFile("/context.json");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        context.setName(contextName);
        record.setContext(context);
        record.setContextName(contextName);

        SearchResultsImpl searchResults = new SearchResultsImpl(createListOfJobRecords(contextName), 0, 100);

        doReturn(searchResults).when(schedulerJobService).findByContext(contextName, 1000, 0);

        //Email Notification
        searchResults = new SearchResultsImpl(createListOfEmailNotification(contextName), 0, 100);
        doReturn(searchResults).when(emailNotificationDetailsService).findByContextName(contextName, 1000, 0);

        //Email Notification Context
        searchResults = new SearchResultsImpl(createListOfEmailNotificationConext(contextName), 0, 100);
        doReturn(searchResults).when(emailNotificationContextService).findByContextName(contextName, 1000, 0);

        //ContextProfile
        searchResults = new SearchResultsImpl(createListOfContextProfileRecord(contextName),0, 100);
        doReturn(searchResults).when(contextProfileService).findByFilter(any(ContextProfileSearchFilter.class), eq(1000), eq(0), eq(null), eq(null));


        ByteArrayOutputStream result = ContextExportZipUtils.createZipFile(context, contextName, "downloadName", "."
            , schedulerJobService, this.emailNotificationDetailsService, this.emailNotificationContextService
            , this.contextProfileService, 1000, true);

        Assert.assertNotNull(result);

        ZipInputStream stream = this.resolveZipInputStream(result.toByteArray());

        this.assertDirectoryStructure(stream, List.of("downloadName/", "downloadName/context/",
            "downloadName/notification/", "downloadName/profiles/", "downloadName/jobs/", "downloadName/jobs/file/",
            "downloadName/jobs/internal/", "downloadName/jobs/quartz/", "downloadName/jobs/global/",
            "downloadName/notification_details/"));

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> internalJobs = this.getJobs(stream, "downloadName/jobs/internal/", InternalEventDrivenJob.class);
        Assert.assertEquals(3, internalJobs.size());
        this.assertJobsWithTokensCorrect(internalJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> quartzJobs = this.getJobs(stream, "downloadName/jobs/quartz/", QuartzScheduleDrivenJob.class);
        Assert.assertEquals(3, quartzJobs.size());
        this.assertJobsWithTokensCorrect(quartzJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> fileJobs = this.getJobs(stream, "downloadName/jobs/file/", FileEventDrivenJob.class);
        Assert.assertEquals(3, fileJobs.size());
        this.assertJobsWithTokensCorrect(fileJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> globalJobs = this.getJobs(stream, "downloadName/jobs/global/", GlobalEventJob.class);
        Assert.assertEquals(3, globalJobs.size());
        this.assertJobsWithTokensCorrect(globalJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        ContextTemplate contextTemplate = this.getContext(stream, "downloadName/context/download");
        Assert.assertNotNull(contextTemplate);

        JSONAssert.assertEquals(loadDataFile("/context-with-tokens.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextTemplate), JSONCompareMode.LENIENT);
    }

    @Test
    public void test_export_zip_without_tokens() throws Exception {
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        String contextName = "HelloContext";
        String jsonContext = this.loadDataFile("/context.json");

        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        context.setName(contextName);
        record.setContext(context);
        record.setContextName(contextName);

        SearchResultsImpl searchResults = new SearchResultsImpl(createListOfJobRecords(contextName), 0, 100);

        doReturn(searchResults).when(schedulerJobService).findByContext(contextName, 1000, 0);

        //Email Notification
        searchResults = new SearchResultsImpl(createListOfEmailNotification(contextName), 0, 100);
        doReturn(searchResults).when(emailNotificationDetailsService).findByContextName(contextName, 1000, 0);

        //Email Notification Context
        searchResults = new SearchResultsImpl(createListOfEmailNotificationConext(contextName), 0, 100);
        doReturn(searchResults).when(emailNotificationContextService).findByContextName(contextName, 1000, 0);

        //ContextProfile
        searchResults = new SearchResultsImpl(createListOfContextProfileRecord(contextName),0, 100);
        doReturn(searchResults).when(contextProfileService).findByFilter(any(ContextProfileSearchFilter.class), eq(1000), eq(0), eq(null), eq(null));


        ByteArrayOutputStream result = ContextExportZipUtils.createZipFile(context, contextName, "downloadName", "."
            , schedulerJobService, this.emailNotificationDetailsService, this.emailNotificationContextService
            , this.contextProfileService, 1000, false);

        Assert.assertNotNull(result);

        ZipInputStream stream = this.resolveZipInputStream(result.toByteArray());

        this.assertDirectoryStructure(stream, List.of("downloadName/", "downloadName/context/",
            "downloadName/notification/", "downloadName/profiles/", "downloadName/jobs/", "downloadName/jobs/file/",
            "downloadName/jobs/internal/", "downloadName/jobs/quartz/", "downloadName/jobs/global/",
            "downloadName/notification_details/"));

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> internalJobs = this.getJobs(stream, "downloadName/jobs/internal/", InternalEventDrivenJob.class);
        Assert.assertEquals(3, internalJobs.size());
        this.assertJobsWithNoTokensCorrect(internalJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> quartzJobs = this.getJobs(stream, "downloadName/jobs/quartz/", QuartzScheduleDrivenJob.class);
        Assert.assertEquals(3, quartzJobs.size());
        this.assertJobsWithNoTokensCorrect(quartzJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> fileJobs = this.getJobs(stream, "downloadName/jobs/file/", FileEventDrivenJob.class);
        Assert.assertEquals(3, fileJobs.size());
        this.assertJobsWithNoTokensCorrect(fileJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> globalJobs = this.getJobs(stream, "downloadName/jobs/global/", GlobalEventJob.class);
        Assert.assertEquals(3, globalJobs.size());
        this.assertJobsWithNoTokensCorrect(globalJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        ContextTemplate contextTemplate = this.getContext(stream, "downloadName/context/download");
        Assert.assertNotNull(contextTemplate);

        JSONAssert.assertEquals(loadDataFile("/context-without-tokens.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextTemplate), JSONCompareMode.LENIENT);
    }

    private ZipInputStream resolveZipInputStream(byte[] zipContents) {
        return new ZipInputStream(new ByteArrayInputStream(zipContents));
    }

    private void assertDirectoryStructure(ZipInputStream zipInputStream, List expected) throws IOException {
        List<String> directories = new ArrayList<>();
        ZipEntry entry;

        while((entry = zipInputStream.getNextEntry())!=null)
        {
            Assert.assertNotNull(entry);
            if(entry.isDirectory()) {
                directories.add(entry.getName());
            }
        }

        Assert.assertEquals(expected.size(), directories.size());
        Assert.assertTrue(directories.containsAll(expected));
        Assert.assertTrue(expected.containsAll(directories));
    }

    private void assertJobsWithTokensCorrect(Map<String, Object> jobs) {
        jobs.entrySet().forEach(entry -> {
            String jobname = this.getJobNameFromPath(entry.getKey());
            SchedulerJob job = (SchedulerJob) entry.getValue();

            Assert.assertEquals(jobname, job.getJobName());
            if(job instanceof GlobalEventJob == false){
                Assert.assertEquals("[[agent.name]]", job.getAgentName());
                Assert.assertTrue(job.getIdentifier().startsWith("[[agent.name]]"));
            }
            else {
                Assert.assertEquals("GLOBAL_EVENT", job.getAgentName());
                Assert.assertFalse(job.getIdentifier().startsWith("[[agent.name]]"));
            }
        });
    }

    private void assertJobsWithNoTokensCorrect(Map<String, Object> jobs) {
        jobs.entrySet().forEach(entry -> {
            String jobname = this.getJobNameFromPath(entry.getKey());
            SchedulerJob job = (SchedulerJob) entry.getValue();

            Assert.assertEquals(jobname, job.getJobName());
            if(job instanceof GlobalEventJob == false){
                Assert.assertNotEquals("[[agent.name]]", job.getAgentName());
                Assert.assertFalse(job.getIdentifier().startsWith("[[agent.name]]"));
            }
            else {
                Assert.assertEquals("GLOBAL_EVENT", job.getAgentName());
                Assert.assertFalse(job.getIdentifier().startsWith("[[agent.name]]"));
            }
        });
    }

    private String getJobNameFromPath(String path) {
        return path.substring(path.lastIndexOf("/")+1, path.indexOf("."));
    }

    private Map<String, Object> getJobs(ZipInputStream zipInputStream, String base, Class theClass) throws IOException {
        Map<String, Object> results = new HashMap<>();
        ZipEntry entry;

        while((entry = zipInputStream.getNextEntry())!=null) {
            Assert.assertNotNull(entry);
            if(!entry.isDirectory()) {
                if(entry.getName().contains(base)) {
                    byte[] file = zipInputStream.readAllBytes();
                    results.put(entry.getName(), objectMapper.readValue(file, theClass));
                }
            }
        }

        return results;
    }

    private ContextTemplate getContext(ZipInputStream zipInputStream, String base) throws IOException {
        ZipEntry entry;
        while((entry = zipInputStream.getNextEntry())!=null) {
            Assert.assertNotNull(entry);
            if(!entry.isDirectory()) {
                if(entry.getName().contains(base)) {
                    byte[] file = zipInputStream.readAllBytes();
                    return objectMapper.readValue(new String(file), ContextTemplateImpl.class);
                }
            }
        }

        return null;
    }

//    @Test
//    public void test_with_replacement_tokens() throws Exception {
//        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
//        String contextName = "HelloContext";
//        String jsonContext = this.loadDataFile("/data/context.json");
//        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");
//
//        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
//        record.setContext(context);
//        record.setContextName(contextName);
//
//        doReturn(record).when(scheduledContextService).findByName(contextName);
//
//        SearchResultsImpl searchResults = new SearchResultsImpl(createListOfJobRecords(contextName), 0, 100);
//
//        doReturn(searchResults).when(schedulerJobService).findByContext(contextName, 50, 0);
//
//        //Email Notification
//        searchResults = new SearchResultsImpl(createListOfEmailNotification(contextName), 0, 100);
//        doReturn(searchResults).when(emailNotificationDetailsService).findByContextName(contextName, 50, 0);
//
//        //Email Notification Context
//        searchResults = new SearchResultsImpl(createListOfEmailNotificationConext(contextName), 0, 100);
//        doReturn(searchResults).when(emailNotificationContextService).findByContextName(contextName, 50, 0);
//
//        //ContextProfile
//        searchResults = new SearchResultsImpl(createListOfContextProfileRecord(contextName),0, 100);
//        doReturn(searchResults).when(contextProfileService).findByFilter(any(ContextProfileSearchFilter.class), eq(50), eq(0), eq(null), eq(null));
//
//        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/export/context/tokens/" + contextName)).andReturn();
//
//        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
//    }
//
//    /**
//     * Windows have some unsafe characters to create files, therefore this test is using the name "Context?Name"
//     * with a ? which is not allowed to be used in windows
//     * Will test both the context and the jobs as jobs are created dynamically in the test based on the name of the
//     * context
//     * @throws Exception - file the file we are loading into the test
//     */
//    @Test
//    public void testContextWithNotSafeCharacters() throws Exception {
//        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
//        String contextName = "Context*Name";
//        String jsonContext = this.loadDataFile("/data/context.json");
//        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");
//
//        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
//        record.setContext(context);
//        record.setContextName(contextName);
//
//        doReturn(record).when(scheduledContextService).findByName(contextName);
//
//        SearchResultsImpl searchResults = new SearchResultsImpl(createListOfJobRecords(contextName), 0, 100);
//
//        doReturn(searchResults).when(schedulerJobService).findByContext(contextName, 50, 0);
//
//        //Email Notification Details
//        searchResults = new SearchResultsImpl(createListOfEmailNotification(contextName), 0, 100);
//        doReturn(searchResults).when(emailNotificationDetailsService).findByContextName(contextName, 50, 0);
//
//        //Email Notification Context test empty responds from solr
//        searchResults = new SearchResultsImpl(new ArrayList<EmailNotificationContext>(), 0, 100);
//        doReturn(searchResults).when(emailNotificationContextService).findByContextName(contextName, 50, 0);
//
//        //ContextProfile
//        searchResults = new SearchResultsImpl(createListOfContextProfileRecord(contextName),0, 100);
//        doReturn(searchResults).when(contextProfileService).findByFilter(any(ContextProfileSearchFilter.class), eq(50), eq(0), eq(null), eq(null));
//
//        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/export/context/" + contextName)).andReturn();
//
//        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus());
//    }
//
//    /**
//     * Setup a Context in the system as HelloContext
//     * However trying to extract the context, DOESNOTEXIST
//     * Program should return error 500
//     * @throws Exception - file the file we are loading into the test
//     */
//    @Test
//    public void testWhenNoContextExist() throws Exception {
//        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
//        String contextName = "HelloContext";
//        String jsonContext = this.loadDataFile("/data/context.json");
//        jsonContext = jsonContext.replace("\"name\": \"CONTEXT-1436221681\"", "\"name\" : \"" + contextName + "\"");
//
//        ContextTemplateImpl context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
//        record.setContext(context);
//        record.setContextName(contextName);
//
//        doReturn(record).when(scheduledContextService).findByName(contextName);
//
//        SearchResultsImpl searchResults = new SearchResultsImpl(createListOfJobRecords(contextName), 0, 100);
//
//        doReturn(searchResults).when(schedulerJobService).findByContext(contextName, 50, 0);
//
//        //Email Notification
//        searchResults = new SearchResultsImpl(createListOfEmailNotification(contextName), 0, 100);
//        doReturn(searchResults).when(emailNotificationDetailsService).findByContextName(contextName, 50, 0);
//
//        //ContextProfile
//        searchResults = new SearchResultsImpl(createListOfContextProfileRecord(contextName),0, 100);
//        doReturn(searchResults).when(contextProfileService).findByFilter(any(ContextProfileSearchFilter.class), eq(50), eq(0), eq(null), eq(null));
//
//        MvcResult mvcResult = mvc.perform(MockMvcRequestBuilders.get("/rest/export/context/DOESNOTEXIST")).andReturn();
//
//        // As the context DOESNOTEXIST is not there, return error 500
//        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), mvcResult.getResponse().getStatus());
//    }

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
            solrFileEventDrivenJob.setJobName(contextName + "jobName-fe" + i);
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
            solrInternalEventDrivenJob.setJobName(contextName + "jobName-ce" + i);
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
            solrQuartzScheduleDrivenJob.setJobName(contextName + "jobName-qe" + i);
            solrQuartzScheduleDrivenJob.setIdentifier(solrQuartzScheduleDrivenJob.getAgentName() + "_" + solrQuartzScheduleDrivenJob.getJobName());
            solrQuartzScheduleDrivenJob.setContextName(contextName);
            solrQuartzScheduleDrivenJob.setCronExpression("cronExpression" + i);

            TestSchedulerJobRecord quartzRecord = new TestSchedulerJobRecord();
            quartzRecord.setType(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB);
            quartzRecord.setJob(solrQuartzScheduleDrivenJob);
            quartzRecord.setId(solrQuartzScheduleDrivenJob.getAgentName() + "_" + solrQuartzScheduleDrivenJob.getJobName());
            schedulerJobRecords.add(quartzRecord);

            SolrGlobalEventJobImpl globalEventJob = new SolrGlobalEventJobImpl();
            globalEventJob.setJobName(contextName + "jobName-ge" + i);
            globalEventJob.setAgentName(contextName + "agentName" + i);
            globalEventJob.setIdentifier(solrQuartzScheduleDrivenJob.getAgentName() + "_" + solrQuartzScheduleDrivenJob.getJobName());
            globalEventJob.setContextName(contextName);

            TestSchedulerJobRecord globalRecord = new TestSchedulerJobRecord();
            globalRecord.setType(JobConstants.GLOBAL_EVENT_JOB);
            globalRecord.setJob(globalEventJob);
            globalRecord.setId(globalEventJob.getAgentName() + "_" + globalEventJob.getJobName());
            schedulerJobRecords.add(globalRecord);
        }
        return schedulerJobRecords;
    }

    private static List<EmailNotificationContextRecord> createListOfEmailNotificationConext(String contextName) {
        List<EmailNotificationContextRecord> records = new ArrayList<>();

        SolrEmailNotificationContextRecordImpl r = new SolrEmailNotificationContextRecordImpl();
        r.setId(contextName);
        r.setContextName(contextName);

        SolrEmailNotificationContextImpl email = new SolrEmailNotificationContextImpl();
        email.setContextName(contextName);
        r.setEmailNotificationContext(email);
        records.add(r);

        return records;
    }

    private static List<EmailNotificationDetailsRecord> createListOfEmailNotification(String contextName) {
        List<EmailNotificationDetailsRecord> records = new ArrayList<>();

        // Create 3 notification
        for (int i = 0; i < 3; i++) {
            SolrEmailNotificationDetailsRecord r = new SolrEmailNotificationDetailsRecord();
            r.setContextName(contextName);
            r.setJobName("jobName-" + i);
            r.setId("jobName-" + i + "-" + contextName + "-child-" + i + "-ERROR");
            r.setMonitorType("ERROR");

            SolrEmailNotificationDetails email = new SolrEmailNotificationDetails();
            email.setContextName(contextName);
            email.setChildContextName(contextName + "-child-" + i);
            email.setJobName("jobName-" + i);
            r.setEmailNotificationDetails(email);
            records.add(r);
        }
        return records;
    }

    private static List<ContextProfileRecord> createListOfContextProfileRecord(String contextName) {
        List<ContextProfileRecord> recordList = new ArrayList<>();
        ContextProfileRecord record = new ContextProfileRecordImpl();
        record.setProfileName("Profile-" + contextName);
        record.setContextName(contextName);
        recordList.add(record);
        return  recordList;
    }

    protected String loadDataFile(String fileName) throws IOException
    {
        String contentToSend = IOUtils.toString(loadDataFileStream(fileName), "UTF-8");

        return contentToSend;
    }

    protected InputStream loadDataFileStream(String fileName) throws IOException
    {
        return getClass().getResourceAsStream(fileName);
    }
}
