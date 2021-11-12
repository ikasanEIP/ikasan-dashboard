package org.ikasan.dashboard.notification.scheduler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.SolrResourceLoader;
import org.ikasan.business.stream.metadata.dao.SolrBusinessStreamMetadataDao;
import org.ikasan.business.stream.metadata.service.SolrBusinessStreamMetaDataServiceImpl;
import org.ikasan.dashboard.notification.business.stream.BusinessStreamNotificationJob;
import org.ikasan.dashboard.notification.business.stream.model.BusinessStreamNotification;
import org.ikasan.dashboard.notification.business.stream.service.BusinessStreamNotificationService;
import org.ikasan.dashboard.notification.email.EmailNotification;
import org.ikasan.dashboard.notification.email.EmailNotifier;
import org.ikasan.dashboard.notification.scheduler.model.SchedulerNotification;
import org.ikasan.dashboard.notification.scheduler.service.SchedulerNotificationService;
import org.ikasan.error.reporting.dao.SolrErrorReportingServiceDao;
import org.ikasan.solr.dao.SolrGeneralDaoImpl;
import org.ikasan.solr.service.SolrGeneralServiceImpl;
import org.ikasan.spec.configuration.PlatformConfigurationService;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;


public class SchedulerNotificationJobTest extends SolrTestCaseJ4 {

    private Mockery mockery = new Mockery()
    {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
            setThreadingPolicy(new Synchroniser());
        }
    };

    public static final String BUSINESS_STREAM_PAYLOAD = "/data/graph/wriggle3.json";

    private SolrGeneralDaoImpl dao;

    private NodeConfig config;

    private TemplateEngine templateEngine = mockery.mock(TemplateEngine.class);
    private SchedulerNotification schedulerNotification = mockery.mock(SchedulerNotification.class);
    private SchedulerNotificationService schedulerNotificationService = mockery.mock(SchedulerNotificationService.class);
    private PlatformConfigurationService platformConfigurationService = mockery.mock(PlatformConfigurationService.class);
    private EmailNotifier emailNotifier = mockery.mock(EmailNotifier.class);
    private JobExecutionContext jobExecutionContext = mockery.mock(JobExecutionContext.class);

    @Test
    public void test_job_success_no_exclusions_found() throws JobExecutionException {
        mockery.checking(new Expectations(){{
            oneOf(schedulerNotification).getSchedulerAgentName();
            will(returnValue("schedulerAgent1"));
            oneOf(schedulerNotification).getJobName();
            will(returnValue("jobName"));
            oneOf(platformConfigurationService).getConfigurationValue(with(any(String.class)));
            will(returnValue("0"));
            oneOf(schedulerNotification).getResultSize();
            will(returnValue(1000));
            oneOf(schedulerNotificationService).getFailedScheduledJobs("schedulerAgent1", 0L, 1000);
            will(returnValue(Optional.empty()));
            exactly(2).of(schedulerNotification).getJobName();
            will(returnValue("jobName"));
            oneOf(platformConfigurationService).saveConfigurationValue(with(any(String.class)), with(any(String.class)));
        }});

        SchedulerNotificationJob notification = new SchedulerNotificationJob(emailTemplateEngine(), schedulerNotification
            , schedulerNotificationService, platformConfigurationService, emailNotifier);

        notification.execute(jobExecutionContext);

        mockery.assertIsSatisfied();
    }

    @Test
    public void test_job_success_failed_scheduled_events_found() throws JobExecutionException {
        mockery.checking(new Expectations(){{
            ignoring(templateEngine);
            oneOf(schedulerNotification).getSchedulerAgentName();
            will(returnValue("schedulerAgent1"));
            oneOf(schedulerNotification).getJobName();
            will(returnValue("jobName"));
            oneOf(platformConfigurationService).getConfigurationValue(with(any(String.class)));
            will(returnValue("0"));
            oneOf(schedulerNotification).getResultSize();
            will(returnValue(1000));
            oneOf(schedulerNotification).getEmailBodyTemplate();
            will(returnValue("./src/test/resources/email/scheduler-notification-email.html"));
            oneOf(schedulerNotification).getEmailSubjectTemplate();
            will(returnValue("./src/test/resources/email/scheduler-notification-email-subject.txt"));
            oneOf(schedulerNotification).getRecipientList();
            will(returnValue(Arrays.asList("ikasan@ikasan.com")));
            oneOf(schedulerNotification).isHtml();
            will(returnValue(true));
            oneOf(emailNotifier).sendNotification(with(any(EmailNotification.class)));
            oneOf(schedulerNotification).getJobName();
            will(returnValue("jobName"));
            oneOf(platformConfigurationService).saveConfigurationValue(with(any(String.class)), with(any(String.class)));
        }});

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            this.initialiseOneSucceessOneFailData(server);

            SchedulerNotificationService schedulerNotificationService = this.initialiseService(server);

            SchedulerNotificationJob notification = new SchedulerNotificationJob(emailTemplateEngine(), schedulerNotification
                , schedulerNotificationService, platformConfigurationService, emailNotifier);

            notification.execute(jobExecutionContext);
        }
        catch (SolrServerException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        mockery.assertIsSatisfied();
    }

    @Test
    public void test_job_success_failed_scheduled_no_events_found_due_to_last_tun_timestamp() throws JobExecutionException {
        mockery.checking(new Expectations(){{
            ignoring(templateEngine);
            oneOf(schedulerNotification).getSchedulerAgentName();
            will(returnValue("schedulerAgent1"));
            oneOf(schedulerNotification).getJobName();
            will(returnValue("jobName"));
            oneOf(platformConfigurationService).getConfigurationValue(with(any(String.class)));
            will(returnValue(Long.toString(System.currentTimeMillis() + 10000000L)));
            oneOf(schedulerNotification).getResultSize();
            will(returnValue(1000));
            oneOf(schedulerNotification).getJobName();
            will(returnValue("jobName"));
            oneOf(schedulerNotification).getJobName();
            will(returnValue("jobName"));
            oneOf(platformConfigurationService).saveConfigurationValue(with(any(String.class)), with(any(String.class)));
        }});

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            init(server);

            this.initialiseOneSucceessOneFailData(server);

            SchedulerNotificationService schedulerNotificationService = this.initialiseService(server);

            SchedulerNotificationJob notification = new SchedulerNotificationJob(emailTemplateEngine(), schedulerNotification
                , schedulerNotificationService, platformConfigurationService, emailNotifier);

            notification.execute(jobExecutionContext);
        }
        catch (SolrServerException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        mockery.assertIsSatisfied();
    }

    public TemplateEngine emailTemplateEngine() {
        final SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.addTemplateResolver(textTemplateResolver());
        templateEngine.addTemplateResolver(htmlTemplateResolver());

        return templateEngine;
    }

    private ITemplateResolver textTemplateResolver() {
        final FileTemplateResolver templateResolver = new FileTemplateResolver();
        templateResolver.setOrder(Integer.valueOf(1));
        templateResolver.setSuffix(".txt");
        templateResolver.setTemplateMode(TemplateMode.TEXT);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);
        return templateResolver;
    }

    private ITemplateResolver htmlTemplateResolver() {
        final FileTemplateResolver templateResolver = new FileTemplateResolver();
        templateResolver.setOrder(Integer.valueOf(2));
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);
        return templateResolver;
    }

    @Before
    public void setup()
    {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.WARN);

        Path path = createTempDir();

        SolrResourceLoader loader = new SolrResourceLoader(path);
        config = new NodeConfig.NodeConfigBuilder("testnode", loader)
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString()).build();


    }

    private void init(EmbeddedSolrServer server) throws IOException, SolrServerException
    {
        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName("ikasan");
        createRequest.setConfigSet("minimal");
        server.request(createRequest);

        dao = new SolrGeneralDaoImpl();
        dao.setSolrClient(server);
    }

    private SchedulerNotificationService initialiseService(EmbeddedSolrServer server) {
        SolrGeneralDaoImpl solrGeneralDao = new SolrGeneralDaoImpl();
        solrGeneralDao.setSolrClient(server);

        SolrBusinessStreamMetadataDao solrBusinessStreamMetadataDao = new SolrBusinessStreamMetadataDao();
        solrBusinessStreamMetadataDao.setSolrClient(server);

        SolrErrorReportingServiceDao solrErrorReportingServiceDao = new SolrErrorReportingServiceDao();
        solrErrorReportingServiceDao.setSolrClient(server);

        SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(solrGeneralDao);

        return new SchedulerNotificationService(solrGeneralService);
    }


    private void initialiseOneSucceessOneFailData(EmbeddedSolrServer server) throws IOException, SolrServerException {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", "scheduledEvent1");
        doc.addField("type", "scheduledProcessEvent");
        doc.addField("moduleName", "schedulerAgent1");
        doc.addField("flowName", "Wriggle Customer HTTP Request Flow");
        doc.addField("payload", loadDataFile("/solr/data/scheduledProcessEventSuccess.json"));
        doc.addField("expiry", System.currentTimeMillis() + 10000000l);
        doc.addField("timestamp", System.currentTimeMillis() - 10000000l);
        server.add("ikasan", doc);

        doc = new SolrInputDocument();
        doc.addField("id", "scheduledEvent2");
        doc.addField("type", "scheduledProcessEvent");
        doc.addField("moduleName", "schedulerAgent1");
        doc.addField("flowName", "Wriggle Customer HTTP Request Flow");
        doc.addField("payload", loadDataFile("/solr/data/scheduledProcessEventSuccessFail.json"));
        doc.addField("expiry", System.currentTimeMillis() + 10000000l);
        doc.addField("timestamp", System.currentTimeMillis() - 10000000l);
        server.add("ikasan", doc);

        server.commit();
    }

    private void initialiseNoFailData(EmbeddedSolrServer server) throws IOException, SolrServerException {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", "scheduledEvent1");
        doc.addField("type", "scheduledProcessEvent");
        doc.addField("moduleName", "schedulerAgent1");
        doc.addField("flowName", "Wriggle Customer HTTP Request Flow");
        doc.addField("payload", loadDataFile("/solr/data/scheduledProcessEventSuccess.json"));
        doc.addField("expiry", System.currentTimeMillis() + 10000000l);
        doc.addField("timestamp", System.currentTimeMillis() - 10000000l);
        server.add("ikasan", doc);

        doc = new SolrInputDocument();
        doc.addField("id", "scheduledEvent2");
        doc.addField("type", "scheduledProcessEvent");
        doc.addField("moduleName", "schedulerAgent1");
        doc.addField("flowName", "Wriggle Customer HTTP Request Flow");
        doc.addField("payload", loadDataFile("/solr/data/scheduledProcessEventSuccess.json"));
        doc.addField("expiry", System.currentTimeMillis() + 10000000l);
        doc.addField("timestamp", System.currentTimeMillis() - 10000000l);
        server.add("ikasan", doc);

        server.commit();
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

    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
    }
}
