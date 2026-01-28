package org.ikasan.orchestration.service.context.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.context.ScheduledContextRecordImpl;
import org.ikasan.job.orchestration.model.profile.ContextProfileRecordImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    String contextName;
    ContextTemplateImpl context;

    public void setupFixture(boolean withGlobalEvents) throws IOException {
        ScheduledContextRecordImpl record = new ScheduledContextRecordImpl();
        contextName = "HelloContext";
        String jsonContext = this.loadDataFile("/context.json");

        context = objectMapper.readValue(jsonContext, ContextTemplateImpl.class);
        context.setName(contextName);
        record.setContext(context);
        record.setContextName(contextName);

        SearchResultsImpl searchResults = new SearchResultsImpl(createListOfJobRecords(context, withGlobalEvents), 0, 100);

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
    }

    @Test
    public void test_export_zip_with_tokens_no_global_events() throws Exception {
        setupFixture(false);

        ByteArrayOutputStream result = ContextExportZipUtils.createZipFile(context, contextName, "downloadName", "."
            , schedulerJobService, this.emailNotificationDetailsService, this.emailNotificationContextService
            , this.contextProfileService, 1000, true, false);

        Assert.assertNotNull(result);

        ZipInputStream stream = this.resolveZipInputStream(result.toByteArray());

        this.assertDirectoryStructure(stream, List.of("downloadName/", "downloadName/context/",
            "downloadName/notification/", "downloadName/profiles/", "downloadName/jobs/", "downloadName/jobs/file/",
            "downloadName/jobs/internal/", "downloadName/jobs/internalTemplate/", "downloadName/jobs/quartz/", "downloadName/jobs/global/",
            "downloadName/notification_details/"));

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> internalJobs = this.getJobs(stream, "downloadName/jobs/internal/", InternalEventDrivenJob.class);
        Assert.assertEquals(3, internalJobs.size());
        this.assertJobsWithTokensCorrect(internalJobs);
        this.assertJobsWithNoChildContexts(internalJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> quartzJobs = this.getJobs(stream, "downloadName/jobs/quartz/", QuartzScheduleDrivenJob.class);
        Assert.assertEquals(3, quartzJobs.size());
        this.assertJobsWithTokensCorrect(quartzJobs);
        this.assertJobsWithNoChildContexts(quartzJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> fileJobs = this.getJobs(stream, "downloadName/jobs/file/", FileEventDrivenJob.class);
        Assert.assertEquals(3, fileJobs.size());
        this.assertJobsWithTokensCorrect(fileJobs);
        this.assertJobsWithNoChildContexts(fileJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> globalJobs = this.getJobs(stream, "downloadName/jobs/global/", GlobalEventJob.class);
        Assert.assertEquals(0, globalJobs.size());
        this.assertJobsWithTokensCorrect(globalJobs);
        this.assertJobsWithNoChildContexts(globalJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        ContextTemplate contextTemplate = this.getContext(stream, "downloadName/context/downloadName.json");
        Assert.assertNotNull(contextTemplate);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> internalTemplateJobs = this.getJobs(stream, "downloadName/jobs/internalTemplate/", GlobalEventJob.class);
        Assert.assertEquals(3, internalTemplateJobs.size());
        this.assertJobsWithNoTokensCorrect(internalTemplateJobs);

        JSONAssert.assertEquals(loadDataFile("/context-with-tokens.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextTemplate), new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("**.ordinal", (o1, o2) -> true)
            ));
    }

    @Test
    public void test_export_zip_with_tokens_with_global_events() throws Exception {
        setupFixture(true);

        ByteArrayOutputStream result = ContextExportZipUtils.createZipFile(context, contextName, "downloadName", "."
            , schedulerJobService, this.emailNotificationDetailsService, this.emailNotificationContextService
            , this.contextProfileService, 1000, true, false);

        Assert.assertNotNull(result);

        ZipInputStream stream = this.resolveZipInputStream(result.toByteArray());

        this.assertDirectoryStructure(stream, List.of("downloadName/", "downloadName/context/",
            "downloadName/notification/", "downloadName/profiles/", "downloadName/jobs/", "downloadName/jobs/file/",
            "downloadName/jobs/internal/", "downloadName/jobs/internalTemplate/", "downloadName/jobs/quartz/", "downloadName/jobs/global/",
            "downloadName/notification_details/"));

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> internalJobs = this.getJobs(stream, "downloadName/jobs/internal/", InternalEventDrivenJob.class);
        Assert.assertEquals(3, internalJobs.size());
        this.assertJobsWithTokensCorrect(internalJobs);
        this.assertJobsWithNoChildContexts(internalJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> quartzJobs = this.getJobs(stream, "downloadName/jobs/quartz/", QuartzScheduleDrivenJob.class);
        Assert.assertEquals(3, quartzJobs.size());
        this.assertJobsWithTokensCorrect(quartzJobs);
        this.assertJobsWithNoChildContexts(quartzJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> fileJobs = this.getJobs(stream, "downloadName/jobs/file/", FileEventDrivenJob.class);
        Assert.assertEquals(3, fileJobs.size());
        this.assertJobsWithTokensCorrect(fileJobs);
        this.assertJobsWithNoChildContexts(fileJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> globalJobs = this.getJobs(stream, "downloadName/jobs/global/", GlobalEventJob.class);
        Assert.assertEquals(3, globalJobs.size());
        this.assertJobsWithTokensCorrect(globalJobs);
        this.assertJobsWithNoChildContexts(globalJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        ContextTemplate contextTemplate = this.getContext(stream, "downloadName/context/downloadName.json");
        Assert.assertNotNull(contextTemplate);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> internalTemplateJobs = this.getJobs(stream, "downloadName/jobs/internalTemplate/", GlobalEventJob.class);
        Assert.assertEquals(3, internalTemplateJobs.size());
        this.assertJobsWithNoTokensCorrect(internalTemplateJobs);
    }

    @Test
    public void test_export_zip_with_tokens_and_splitting_sub_contexts_no_global_events() throws Exception {
        setupFixture(false);

        ByteArrayOutputStream result = ContextExportZipUtils.createZipFile(context, contextName, "downloadName", "."
            , schedulerJobService, this.emailNotificationDetailsService, this.emailNotificationContextService
            , this.contextProfileService, 1000, true, true);

        Assert.assertNotNull(result);

        ZipInputStream stream = this.resolveZipInputStream(result.toByteArray());

        this.assertDirectoryStructure(stream, List.of("downloadName/", "downloadName/context/", "downloadName/context/HelloContext/",
            "downloadName/context/HelloContext/CONTEXT-1848727981/","downloadName/context/HelloContext/CONTEXT-1590773100/",
            "downloadName/context/HelloContext/CONTEXT-1182789380/","downloadName/context/HelloContext/CONTEXT-2139852148/",
            "downloadName/context/HelloContext/CONTEXT-195330380/", "downloadName/context/HelloContext/CONTEXT--715116816/",
            "downloadName/context/HelloContext/CONTEXT-1182789416/",
            "downloadName/notification/", "downloadName/profiles/", "downloadName/jobs/", "downloadName/jobs/file/",
            "downloadName/jobs/internal/", "downloadName/jobs/internalTemplate/", "downloadName/jobs/quartz/", "downloadName/jobs/global/",
            "downloadName/notification_details/"));

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> internalJobs = this.getJobs(stream, "downloadName/jobs/internal/", InternalEventDrivenJob.class);
        Assert.assertEquals(3, internalJobs.size());
        this.assertJobsWithTokensCorrect(internalJobs);
        this.assertJobsWithNoChildContexts(internalJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> quartzJobs = this.getJobs(stream, "downloadName/jobs/quartz/", QuartzScheduleDrivenJob.class);
        Assert.assertEquals(3, quartzJobs.size());
        this.assertJobsWithTokensCorrect(quartzJobs);
        this.assertJobsWithNoChildContexts(quartzJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> fileJobs = this.getJobs(stream, "downloadName/jobs/file/", FileEventDrivenJob.class);
        Assert.assertEquals(3, fileJobs.size());
        this.assertJobsWithTokensCorrect(fileJobs);
        this.assertJobsWithNoChildContexts(fileJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> globalJobs = this.getJobs(stream, "downloadName/jobs/global/", GlobalEventJob.class);
        Assert.assertEquals(0, globalJobs.size());
        this.assertJobsWithTokensCorrect(globalJobs);
        this.assertJobsWithNoChildContexts(globalJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> internalTemplateJobs = this.getJobs(stream, "downloadName/jobs/internalTemplate/", GlobalEventJob.class);
        Assert.assertEquals(3, internalTemplateJobs.size());
        this.assertJobsWithNoTokensCorrect(internalTemplateJobs);

        ContextTemplate helloContext = getContextTemplateForFile(result, stream, "downloadName/context/HelloContext.json");
        Assert.assertNotNull(helloContext);
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-195330380/CONTEXT--1250033421.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-1182789380/CONTEXT--663833459.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-1182789416/CONTEXT-613708632.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-1590773100/CONTEXT-1195088490.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-1590773100/CONTEXT-1195088490.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-1848727981/CONTEXT-774294372.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-2139852148/CONTEXT-1065418539.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT--715116816/CONTEXT-521366615.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-195330380.json"));

        JSONAssert.assertEquals(loadDataFile("/context-with-tokens-subcontext-split.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(helloContext), new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("**.ordinal", (o1, o2) -> true)
            ));
    }

    @Test
    public void test_export_zip_with_tokens_and_splitting_sub_contexts_with_global_events() throws Exception {
        setupFixture(true);

        ByteArrayOutputStream result = ContextExportZipUtils.createZipFile(context, contextName, "downloadName", "."
            , schedulerJobService, this.emailNotificationDetailsService, this.emailNotificationContextService
            , this.contextProfileService, 1000, true, true);

        Assert.assertNotNull(result);

        ZipInputStream stream = this.resolveZipInputStream(result.toByteArray());

        this.assertDirectoryStructure(stream, List.of("downloadName/", "downloadName/context/", "downloadName/context/HelloContext/",
            "downloadName/context/HelloContext/CONTEXT-1848727981/","downloadName/context/HelloContext/CONTEXT-1590773100/",
            "downloadName/context/HelloContext/CONTEXT-1182789380/","downloadName/context/HelloContext/CONTEXT-2139852148/",
            "downloadName/context/HelloContext/CONTEXT-195330380/", "downloadName/context/HelloContext/CONTEXT--715116816/",
            "downloadName/context/HelloContext/CONTEXT-1182789416/",
            "downloadName/notification/", "downloadName/profiles/", "downloadName/jobs/", "downloadName/jobs/file/",
            "downloadName/jobs/internal/", "downloadName/jobs/internalTemplate/", "downloadName/jobs/quartz/", "downloadName/jobs/global/",
            "downloadName/notification_details/"));

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> internalJobs = this.getJobs(stream, "downloadName/jobs/internal/", InternalEventDrivenJob.class);
        Assert.assertEquals(3, internalJobs.size());
        this.assertJobsWithTokensCorrect(internalJobs);
        this.assertJobsWithNoChildContexts(internalJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> quartzJobs = this.getJobs(stream, "downloadName/jobs/quartz/", QuartzScheduleDrivenJob.class);
        Assert.assertEquals(3, quartzJobs.size());
        this.assertJobsWithTokensCorrect(quartzJobs);
        this.assertJobsWithNoChildContexts(quartzJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> fileJobs = this.getJobs(stream, "downloadName/jobs/file/", FileEventDrivenJob.class);
        Assert.assertEquals(3, fileJobs.size());
        this.assertJobsWithTokensCorrect(fileJobs);
        this.assertJobsWithNoChildContexts(fileJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> globalJobs = this.getJobs(stream, "downloadName/jobs/global/", GlobalEventJob.class);
        Assert.assertEquals(3, globalJobs.size());
        this.assertJobsWithTokensCorrect(globalJobs);
        this.assertJobsWithNoChildContexts(globalJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> internalTemplateJobs = this.getJobs(stream, "downloadName/jobs/internalTemplate/", GlobalEventJob.class);
        Assert.assertEquals(3, internalTemplateJobs.size());
        this.assertJobsWithNoTokensCorrect(internalTemplateJobs);

        ContextTemplate helloContext = getContextTemplateForFile(result, stream, "downloadName/context/HelloContext.json");
        Assert.assertNotNull(helloContext);
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-195330380/CONTEXT--1250033421.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-1182789380/CONTEXT--663833459.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-1182789416/CONTEXT-613708632.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-1590773100/CONTEXT-1195088490.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-1590773100/CONTEXT-1195088490.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-1848727981/CONTEXT-774294372.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-2139852148/CONTEXT-1065418539.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT--715116816/CONTEXT-521366615.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-195330380.json"));
    }


    @Test
    public void test_export_zip_without_tokens_no_global_events() throws Exception {
        setupFixture(false);

        ByteArrayOutputStream result = ContextExportZipUtils.createZipFile(context, contextName, "downloadName", "."
            , schedulerJobService, this.emailNotificationDetailsService, this.emailNotificationContextService
            , this.contextProfileService, 1000, false, false);

        Assert.assertNotNull(result);

        ZipInputStream stream = this.resolveZipInputStream(result.toByteArray());

        this.assertDirectoryStructure(stream, List.of("downloadName/", "downloadName/context/",
            "downloadName/notification/", "downloadName/profiles/", "downloadName/jobs/", "downloadName/jobs/file/",
            "downloadName/jobs/internal/", "downloadName/jobs/internalTemplate/", "downloadName/jobs/quartz/", "downloadName/jobs/global/",
            "downloadName/notification_details/"));

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> internalJobs = this.getJobs(stream, "downloadName/jobs/internal/", InternalEventDrivenJob.class);
        Assert.assertEquals(3, internalJobs.size());
        this.assertJobsWithNoTokensCorrect(internalJobs);
        this.assertJobsWithNoChildContexts(internalJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> quartzJobs = this.getJobs(stream, "downloadName/jobs/quartz/", QuartzScheduleDrivenJob.class);
        Assert.assertEquals(3, quartzJobs.size());
        this.assertJobsWithNoTokensCorrect(quartzJobs);
        this.assertJobsWithNoChildContexts(quartzJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> fileJobs = this.getJobs(stream, "downloadName/jobs/file/", FileEventDrivenJob.class);
        Assert.assertEquals(3, fileJobs.size());
        this.assertJobsWithNoTokensCorrect(fileJobs);
        this.assertJobsWithNoChildContexts(fileJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> globalJobs = this.getJobs(stream, "downloadName/jobs/global/", GlobalEventJob.class);
        Assert.assertEquals(0, globalJobs.size());
        this.assertJobsWithNoTokensCorrect(globalJobs);
        this.assertJobsWithNoChildContexts(globalJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> internalTemplateJobs = this.getJobs(stream, "downloadName/jobs/internalTemplate/", GlobalEventJob.class);
        Assert.assertEquals(3, internalTemplateJobs.size());
        this.assertJobsWithNoTokensCorrect(internalTemplateJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        ContextTemplate contextTemplate = this.getContext(stream, "downloadName/context/download");
        Assert.assertNotNull(contextTemplate);

        JSONAssert.assertEquals(loadDataFile("/context-without-tokens.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(contextTemplate), new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("**.ordinal", (o1, o2) -> true)
            ));
    }

    @Test
    public void test_export_zip_without_tokens_with_global_events() throws Exception {
        setupFixture(true);

        ByteArrayOutputStream result = ContextExportZipUtils.createZipFile(context, contextName, "downloadName", "."
            , schedulerJobService, this.emailNotificationDetailsService, this.emailNotificationContextService
            , this.contextProfileService, 1000, false, false);

        Assert.assertNotNull(result);

        ZipInputStream stream = this.resolveZipInputStream(result.toByteArray());

        this.assertDirectoryStructure(stream, List.of("downloadName/", "downloadName/context/",
            "downloadName/notification/", "downloadName/profiles/", "downloadName/jobs/", "downloadName/jobs/file/",
            "downloadName/jobs/internal/", "downloadName/jobs/internalTemplate/", "downloadName/jobs/quartz/", "downloadName/jobs/global/",
            "downloadName/notification_details/"));

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> internalJobs = this.getJobs(stream, "downloadName/jobs/internal/", InternalEventDrivenJob.class);
        Assert.assertEquals(3, internalJobs.size());
        this.assertJobsWithNoTokensCorrect(internalJobs);
        this.assertJobsWithNoChildContexts(internalJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> quartzJobs = this.getJobs(stream, "downloadName/jobs/quartz/", QuartzScheduleDrivenJob.class);
        Assert.assertEquals(3, quartzJobs.size());
        this.assertJobsWithNoTokensCorrect(quartzJobs);
        this.assertJobsWithNoChildContexts(quartzJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> fileJobs = this.getJobs(stream, "downloadName/jobs/file/", FileEventDrivenJob.class);
        Assert.assertEquals(3, fileJobs.size());
        this.assertJobsWithNoTokensCorrect(fileJobs);
        this.assertJobsWithNoChildContexts(fileJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> globalJobs = this.getJobs(stream, "downloadName/jobs/global/", GlobalEventJob.class);
        Assert.assertEquals(3, globalJobs.size());
        this.assertJobsWithNoTokensCorrect(globalJobs);
        this.assertJobsWithNoChildContexts(globalJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> internalTemplateJobs = this.getJobs(stream, "downloadName/jobs/internalTemplate/", GlobalEventJob.class);
        Assert.assertEquals(3, internalTemplateJobs.size());
        this.assertJobsWithNoTokensCorrect(internalTemplateJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        ContextTemplate contextTemplate = this.getContext(stream, "downloadName/context/download");
        Assert.assertNotNull(contextTemplate);
    }

    @Test
    public void test_export_zip_without_tokens_and_splitting_sub_contexts_no_global_events() throws Exception {
        setupFixture(false);

        ByteArrayOutputStream result = ContextExportZipUtils.createZipFile(context, contextName, "downloadName", "."
            , schedulerJobService, this.emailNotificationDetailsService, this.emailNotificationContextService
            , this.contextProfileService, 1000, false, true);

        Assert.assertNotNull(result);

        ZipInputStream stream = this.resolveZipInputStream(result.toByteArray());

        this.assertDirectoryStructure(stream, List.of("downloadName/", "downloadName/context/", "downloadName/context/HelloContext/",
            "downloadName/context/HelloContext/CONTEXT-1848727981/","downloadName/context/HelloContext/CONTEXT-1590773100/",
            "downloadName/context/HelloContext/CONTEXT-1182789380/","downloadName/context/HelloContext/CONTEXT-2139852148/",
            "downloadName/context/HelloContext/CONTEXT-195330380/", "downloadName/context/HelloContext/CONTEXT--715116816/",
            "downloadName/context/HelloContext/CONTEXT-1182789416/",
            "downloadName/notification/", "downloadName/profiles/", "downloadName/jobs/", "downloadName/jobs/file/",
            "downloadName/jobs/internal/", "downloadName/jobs/internalTemplate/", "downloadName/jobs/quartz/", "downloadName/jobs/global/",
            "downloadName/notification_details/"));

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> internalJobs = this.getJobs(stream, "downloadName/jobs/internal/", InternalEventDrivenJob.class);
        Assert.assertEquals(3, internalJobs.size());
        this.assertJobsWithNoTokensCorrect(internalJobs);
        this.assertJobsWithNoChildContexts(internalJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> quartzJobs = this.getJobs(stream, "downloadName/jobs/quartz/", QuartzScheduleDrivenJob.class);
        Assert.assertEquals(3, quartzJobs.size());
        this.assertJobsWithNoTokensCorrect(quartzJobs);
        this.assertJobsWithNoChildContexts(quartzJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> fileJobs = this.getJobs(stream, "downloadName/jobs/file/", FileEventDrivenJob.class);
        Assert.assertEquals(3, fileJobs.size());
        this.assertJobsWithNoTokensCorrect(fileJobs);
        this.assertJobsWithNoChildContexts(fileJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> globalJobs = this.getJobs(stream, "downloadName/jobs/global/", GlobalEventJob.class);
        Assert.assertEquals(0, globalJobs.size());
        this.assertJobsWithNoTokensCorrect(globalJobs);
        this.assertJobsWithNoChildContexts(globalJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> internalTemplateJobs = this.getJobs(stream, "downloadName/jobs/internalTemplate/", GlobalEventJob.class);
        Assert.assertEquals(3, internalTemplateJobs.size());
        this.assertJobsWithNoTokensCorrect(internalTemplateJobs);

        ContextTemplate helloContext = getContextTemplateForFile(result, stream, "downloadName/context/HelloContext.json");
        Assert.assertNotNull(helloContext);
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-195330380/CONTEXT--1250033421.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-1182789380/CONTEXT--663833459.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-1182789416/CONTEXT-613708632.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-1590773100/CONTEXT-1195088490.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-1590773100/CONTEXT-1195088490.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-1848727981/CONTEXT-774294372.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-2139852148/CONTEXT-1065418539.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT--715116816/CONTEXT-521366615.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-195330380.json"));

        JSONAssert.assertEquals(loadDataFile("/context-without-tokens-subcontext-split.json")
            , objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(helloContext), new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("**.ordinal", (o1, o2) -> true)
            ));
    }

    @Test
    public void test_export_zip_without_tokens_and_splitting_sub_contexts_with_global_events() throws Exception {
        setupFixture(true);

        ByteArrayOutputStream result = ContextExportZipUtils.createZipFile(context, contextName, "downloadName", "."
            , schedulerJobService, this.emailNotificationDetailsService, this.emailNotificationContextService
            , this.contextProfileService, 1000, false, true);

        Assert.assertNotNull(result);

        ZipInputStream stream = this.resolveZipInputStream(result.toByteArray());

        this.assertDirectoryStructure(stream, List.of("downloadName/", "downloadName/context/", "downloadName/context/HelloContext/",
            "downloadName/context/HelloContext/CONTEXT-1848727981/","downloadName/context/HelloContext/CONTEXT-1590773100/",
            "downloadName/context/HelloContext/CONTEXT-1182789380/","downloadName/context/HelloContext/CONTEXT-2139852148/",
            "downloadName/context/HelloContext/CONTEXT-195330380/", "downloadName/context/HelloContext/CONTEXT--715116816/",
            "downloadName/context/HelloContext/CONTEXT-1182789416/",
            "downloadName/notification/", "downloadName/profiles/", "downloadName/jobs/", "downloadName/jobs/file/",
            "downloadName/jobs/internal/", "downloadName/jobs/internalTemplate/", "downloadName/jobs/quartz/", "downloadName/jobs/global/",
            "downloadName/notification_details/"));

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> internalJobs = this.getJobs(stream, "downloadName/jobs/internal/", InternalEventDrivenJob.class);
        Assert.assertEquals(3, internalJobs.size());
        this.assertJobsWithNoTokensCorrect(internalJobs);
        this.assertJobsWithNoChildContexts(internalJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> quartzJobs = this.getJobs(stream, "downloadName/jobs/quartz/", QuartzScheduleDrivenJob.class);
        Assert.assertEquals(3, quartzJobs.size());
        this.assertJobsWithNoTokensCorrect(quartzJobs);
        this.assertJobsWithNoChildContexts(quartzJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> fileJobs = this.getJobs(stream, "downloadName/jobs/file/", FileEventDrivenJob.class);
        Assert.assertEquals(3, fileJobs.size());
        this.assertJobsWithNoTokensCorrect(fileJobs);
        this.assertJobsWithNoChildContexts(fileJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> globalJobs = this.getJobs(stream, "downloadName/jobs/global/", GlobalEventJob.class);
        Assert.assertEquals(3, globalJobs.size());
        this.assertJobsWithNoTokensCorrect(globalJobs);
        this.assertJobsWithNoChildContexts(globalJobs);

        stream = this.resolveZipInputStream(result.toByteArray());
        Map<String, Object> internalTemplateJobs = this.getJobs(stream, "downloadName/jobs/internalTemplate/", GlobalEventJob.class);
        Assert.assertEquals(3, internalTemplateJobs.size());
        this.assertJobsWithNoTokensCorrect(internalTemplateJobs);

        ContextTemplate helloContext = getContextTemplateForFile(result, stream, "downloadName/context/HelloContext.json");
        Assert.assertNotNull(helloContext);
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-195330380/CONTEXT--1250033421.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-1182789380/CONTEXT--663833459.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-1182789416/CONTEXT-613708632.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-1590773100/CONTEXT-1195088490.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-1590773100/CONTEXT-1195088490.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-1848727981/CONTEXT-774294372.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-2139852148/CONTEXT-1065418539.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT--715116816/CONTEXT-521366615.json"));
        Assert.assertNotNull(getContextTemplateForFile(result, stream, "downloadName/context/HelloContext/CONTEXT-195330380.json"));
    }

    /**
     * Retrieves a ContextTemplate object for a specific file within a ZipInputStream.
     *
     * @param result The ByteArrayOutputStream containing the zip contents
     * @param stream The ZipInputStream pointing to the zip contents
     * @param fileName The name of the file to retrieve within the zip
     * @return A ContextTemplate object representing the specified file
     * @throws IOException If an I/O error occurs
     */
    private ContextTemplate getContextTemplateForFile(ByteArrayOutputStream result, ZipInputStream stream, String fileName) throws IOException {
        stream = this.resolveZipInputStream(result.toByteArray());
        return this.getContext(stream, fileName);
    }

    /**
     * Resolves a ZipInputStream object from the given zip contents byte array.
     *
     * @param zipContents The byte array containing the zip contents
     * @return A ZipInputStream object pointing to the zip contents
     */
    private ZipInputStream resolveZipInputStream(byte[] zipContents) {
        return new ZipInputStream(new ByteArrayInputStream(zipContents));
    }

    /**
     * Asserts the directory structure of a ZipInputStream against the expected list of directories.
     *
     * @param zipInputStream The ZipInputStream to assert directory structure on
     * @param expected A list of expected directories to be contained in the ZipInputStream
     * @throws IOException If an I/O error occurs while processing the ZipInputStream
     */
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

    /**
     * Asserts that the provided map of jobs contain correct tokens and values.
     * For each entry in the map, it validates the job name, agent name, and identifier based on the job type.
     *
     * @param jobs A map where the key is the job path and the value is a SchedulerJob object
     */
    private void assertJobsWithTokensCorrect(Map<String, Object> jobs) {
        jobs.entrySet().forEach(entry -> {
            String jobname = this.getJobNameFromPath(entry.getKey());
            SchedulerJob job = (SchedulerJob) entry.getValue();


            if(job instanceof GlobalEventJob == false){
                Assert.assertEquals(jobname, job.getJobName());
                Assert.assertEquals("[[agent.name]]", job.getAgentName());
                Assert.assertTrue(job.getIdentifier().startsWith("[[agent.name]]"));
            }
            else {
                Assert.assertEquals(jobname, job.getJobName().replace("_[[env.name]]", ""));
                Assert.assertEquals("GLOBAL_EVENT", job.getAgentName());
                Assert.assertFalse(job.getIdentifier().startsWith("[[agent.name]]"));
                Assert.assertTrue(job.getIdentifier().endsWith("_[[env.name]]"));
            }
        });
    }

    /**
     * Asserts that the provided map of jobs contain correct tokens and values.
     * For each entry in the map, it validates the job name, agent name, and identifier based on the job type.
     *
     * @param jobs A map where the key is the job path and the value is a SchedulerJob object
     */
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

    /**
     * Asserts that all jobs in the provided map have no child contexts.
     *
     * @param jobs A map where the key is the job path and the value is a SchedulerJob object
     */
    private void assertJobsWithNoChildContexts(Map<String, Object> jobs) {
        jobs.entrySet().forEach(entry -> {
            SchedulerJob job = (SchedulerJob) entry.getValue();
            Assert.assertTrue(job.getChildContextNames().isEmpty());
        });
    }

    /**
     * Retrieves the job name from the given path by extracting the substring between the last '/' character
     * and the '.' character.
     *
     * @param path The path to extract the job name from
     * @return The extracted job name
     */
    private String getJobNameFromPath(String path) {
        return path.substring(path.lastIndexOf("/")+1, path.indexOf("."));
    }

    /**
     * Retrieves a map of jobs from a ZipInputStream based on a specified base path and class.
     *
     * @param zipInputStream The ZipInputStream containing the zip contents
     * @param base The base path to filter the jobs from within the zip contents
     * @param theClass The class type to deserialize the job files into
     * @return A map containing the job names and deserialized objects as key-value pairs
     * @throws IOException If an I/O error occurs while processing the ZipInputStream
     */
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

    /**
     * Retrieves a ContextTemplate object for a specific file within a ZipInputStream.
     *
     * @param zipInputStream The ZipInputStream to retrieve the ContextTemplate from
     * @param base The base path to filter the jobs from within the zip contents
     * @return A ContextTemplate object representing the specified file, or null if not found
     * @throws IOException If an I/O error occurs
     */
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

    /**
     * Helper method to create some Jobs based on the context name being tested
     * @param contextTemplate we are getting jobs for
     * @return a list of SchedulerJobRecords
     */
    private static List<SchedulerJobRecord> createListOfJobRecords(ContextTemplate contextTemplate, boolean withGlobalEvents) {
        List<SchedulerJobRecord> schedulerJobRecords = new ArrayList<>();

        List<String> identifiers = ContextHelper.getAllJobs(contextTemplate).stream()
            .map(job -> job.getIdentifier()).collect(Collectors.toList());

        // Creating 9 jobs, 3 files, 3 schedulers and 3 event base ones.
        for (int i = 0; i < 3; i++) {
            SolrFileEventDrivenJobImpl solrFileEventDrivenJob = new SolrFileEventDrivenJobImpl();
            solrFileEventDrivenJob.setAgentName(contextTemplate.getName() + "agentName" + i);
            solrFileEventDrivenJob.setJobName(contextTemplate.getName() + "jobName-fe" + i);
            solrFileEventDrivenJob.setIdentifier(identifiers.get(i));
            solrFileEventDrivenJob.setContextName(contextTemplate.getName());
            solrFileEventDrivenJob.setChildContextNames(List.of("child"));
            solrFileEventDrivenJob.setCronExpression("cronExpression" + i);
            solrFileEventDrivenJob.setFilePath("filePath" + i);

            TestSchedulerJobRecord fileRecord = new TestSchedulerJobRecord();
            fileRecord.setType(JobConstants.FILE_EVENT_DRIVEN_JOB);
            fileRecord.setJob(solrFileEventDrivenJob);
            fileRecord.setId(solrFileEventDrivenJob.getAgentName() + "_" + solrFileEventDrivenJob.getJobName());
            schedulerJobRecords.add(fileRecord);

            SolrInternalEventDrivenJobImpl solrInternalEventDrivenJob = new SolrInternalEventDrivenJobImpl();
            solrInternalEventDrivenJob.setAgentName(contextTemplate.getName() + "agentName" + i);
            solrInternalEventDrivenJob.setJobName(contextTemplate.getName() + "jobName-ce" + i);
            solrInternalEventDrivenJob.setChildContextNames(List.of("child"));
            solrInternalEventDrivenJob.setIdentifier(identifiers.get(i+10));
            solrInternalEventDrivenJob.setContextName(contextTemplate.getName());
            solrInternalEventDrivenJob.setCommandLine("ls -al" + i);

            TestSchedulerJobRecord eventRecord = new TestSchedulerJobRecord();
            eventRecord.setType(JobConstants.INTERNAL_EVENT_DRIVEN_JOB);
            eventRecord.setJob(solrInternalEventDrivenJob);
            eventRecord.setId(solrInternalEventDrivenJob.getAgentName() + "_" + solrInternalEventDrivenJob.getJobName());
            schedulerJobRecords.add(eventRecord);

            SolrInternalEventDrivenJobImpl solrInternalEventDrivenJobTemplate = new SolrInternalEventDrivenJobImpl();
            solrInternalEventDrivenJobTemplate.setAgentName(contextTemplate.getName() + "agentName" + i);
            solrInternalEventDrivenJobTemplate.setJobName(contextTemplate.getName() + "jobName-cet" + i);
            solrInternalEventDrivenJobTemplate.setIdentifier(identifiers.get(i));
            solrInternalEventDrivenJobTemplate.setContextName(contextTemplate.getName());
            solrInternalEventDrivenJobTemplate.setCommandLine("ls -al" + i);
            solrInternalEventDrivenJobTemplate.setTemplateJob(true);

            TestSchedulerJobRecord templateRecord = new TestSchedulerJobRecord();
            templateRecord.setType(JobConstants.INTERNAL_EVENT_DRIVEN_JOB_TEMPLATE);
            templateRecord.setJob(solrInternalEventDrivenJobTemplate);
            templateRecord.setId(solrInternalEventDrivenJobTemplate.getAgentName() + "_" + solrInternalEventDrivenJobTemplate.getJobName());
            schedulerJobRecords.add(templateRecord);

            SolrQuartzScheduleDrivenJobImpl solrQuartzScheduleDrivenJob = new SolrQuartzScheduleDrivenJobImpl();
            solrQuartzScheduleDrivenJob.setAgentName(contextTemplate.getName() + "agentName" + i);
            solrQuartzScheduleDrivenJob.setJobName(contextTemplate.getName() + "jobName-qe" + i);
            solrQuartzScheduleDrivenJob.setIdentifier(identifiers.get(i+20));
            solrQuartzScheduleDrivenJob.setContextName(contextTemplate.getName());
            solrQuartzScheduleDrivenJob.setChildContextNames(List.of("child"));
            solrQuartzScheduleDrivenJob.setCronExpression("cronExpression" + i);

            TestSchedulerJobRecord quartzRecord = new TestSchedulerJobRecord();
            quartzRecord.setType(JobConstants.QUARTZ_SCHEDULE_DRIVEN_JOB);
            quartzRecord.setJob(solrQuartzScheduleDrivenJob);
            quartzRecord.setId(solrQuartzScheduleDrivenJob.getAgentName() + "_" + solrQuartzScheduleDrivenJob.getJobName());
            schedulerJobRecords.add(quartzRecord);

            SolrGlobalEventJobImpl globalEventJob = new SolrGlobalEventJobImpl();
            globalEventJob.setJobName(contextTemplate.getName() + "jobName-ge" + i);
            globalEventJob.setAgentName(contextTemplate.getName() + "agentName" + i);
            globalEventJob.setChildContextNames(List.of("child"));
            globalEventJob.setIdentifier(JobConstants.GLOBAL_EVENT + "_" + solrQuartzScheduleDrivenJob.getJobName());
            globalEventJob.setContextName(contextTemplate.getName());

            if(withGlobalEvents) {
                globalEventJob.setIdentifier(identifiers.get(i+30));
                contextTemplate.getScheduledJobs().add(globalEventJob);
            }

            TestSchedulerJobRecord globalRecord = new TestSchedulerJobRecord();
            globalRecord.setType(JobConstants.GLOBAL_EVENT_JOB);
            globalRecord.setJob(globalEventJob);
            globalRecord.setId(globalEventJob.getAgentName() + "_" + globalEventJob.getJobName());
            schedulerJobRecords.add(globalRecord);
        }
        return schedulerJobRecords;
    }

    /**
     * Creates a list of EmailNotificationContextRecord objects with the given context name.
     *
     * @param contextName The name of the context to be used for the EmailNotificationContextRecord objects
     * @return A List of EmailNotificationContextRecord objects with the specified context name
     */
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

    /**
     * Creates a list of EmailNotificationDetailsRecord objects for a specific context name.
     *
     * @param contextName The name of the context to create notifications for
     * @return A list of EmailNotificationDetailsRecord objects associated with the context name
     */
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

    /**
     * Creates a list of ContextProfileRecord objects with the specified context name.
     *
     * @param contextName The name of the context to be used for the ContextProfileRecord objects
     * @return A List of ContextProfileRecord objects with the specified context name
     */
    private static List<ContextProfileRecord> createListOfContextProfileRecord(String contextName) {
        List<ContextProfileRecord> recordList = new ArrayList<>();
        ContextProfileRecord record = new ContextProfileRecordImpl();
        record.setProfileName("Profile-" + contextName);
        record.setContextName(contextName);
        recordList.add(record);
        return  recordList;
    }

    /**
     * Loads the content of a data file as a String.
     *
     * @param fileName The name of the data file to load
     * @return The content of the data file as a String
     * @throws IOException If an I/O error occurs while loading the data file
     */
    protected String loadDataFile(String fileName) throws IOException
    {
        String contentToSend = IOUtils.toString(loadDataFileStream(fileName), StandardCharsets.UTF_8);

        return contentToSend;
    }

    /**
     * Retrieves an InputStream for a specified data file.
     *
     * @param fileName The name of the data file
     * @return An InputStream of the data file
     */
    protected InputStream loadDataFileStream(String fileName)
    {
        return getClass().getResourceAsStream(fileName);
    }
}
