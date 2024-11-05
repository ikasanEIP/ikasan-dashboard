package org.ikasan.notification.notifier;

import org.ikasan.job.orchestration.context.cache.ContextMachineCache;
import org.ikasan.job.orchestration.core.machine.ContextMachine;
import org.ikasan.job.orchestration.model.context.ContextTemplateImpl;
import org.ikasan.job.orchestration.model.instance.ContextInstanceImpl;
import org.ikasan.job.orchestration.model.notification.*;
import org.ikasan.monitor.notifier.EmailNotifierConfiguration;
import org.ikasan.notification.configuration.EmailNotificationParamsConfiguration;
import org.ikasan.scheduled.general.SearchResultsImpl;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationDetails;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationDetailsRecord;
import org.ikasan.scheduled.notification.model.SolrNotificationSendAudit;
import org.ikasan.scheduled.notification.model.SolrNotificationSendAuditRecord;
import org.ikasan.spec.scheduled.context.model.ContextTemplate;
import org.ikasan.spec.scheduled.instance.model.ContextInstance;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.notification.model.*;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.notification.service.NotificationSendAuditService;
import org.ikasan.spec.search.SearchResults;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.imposters.ByteBuddyClassImposteriser;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.*;

public class EmailNotifierTest {

    private Mockery mockery = new Mockery()
    {
        {
            setThreadingPolicy(new Synchroniser());
            setImposteriser(ByteBuddyClassImposteriser.INSTANCE);
        }
    };

    private static Logger logger = LoggerFactory.getLogger(EmailNotifierTest.class);

    /** in memory SMTP server */
    Wiser wiser;

    private EmailNotifier emailNotifier;
    private EmailNotificationContextService emailNotificationContextService = mockery.mock(EmailNotificationContextService.class);
    private EmailNotificationDetailsService emailNotificationDetailsService = mockery.mock(EmailNotificationDetailsService.class);
    private NotificationSendAuditService notificationSendAuditService = mockery.mock(NotificationSendAuditService.class);
    private EmailNotificationParamsConfiguration emailNotificationParamsConfiguration;

    @Before
    public void setup()
    {
        wiser = new Wiser();
        for(int count = 0; count < 5; count++)
            try {
                wiser.setPort(2500);
                logger.info(String.format("Attempting to start Wiser SMTP Server on port 2500"));
                wiser.start();
                break;
            } catch (RuntimeException re){
                logger.info("Failed to start Wiser SMTP server, sleeping for a couple of seconds", re);
                try {
                    Thread.sleep(2000l);
                } catch (InterruptedException e) {
                }
            }

        // Create configuration
        emailNotificationParamsConfiguration = this.emailNotificationParamsConfiguration();

        emailNotifier = new EmailNotifier(emailNotificationDetailsService, emailNotificationContextService, notificationSendAuditService, emailNotificationParamsConfiguration, emailTemplateEngine(), "http://localhost:9090/schedulerJobLogFile/");
        emailNotifier.setConfiguration(getConfiguration());

        ContextTemplate contextTemplate1 = new ContextTemplateImpl();
        contextTemplate1.setName("context-template-1");
        ContextInstance contextInstance1 = new ContextInstanceImpl();
        contextInstance1.setName("context-instance-1");
        ContextMachine contextMachine1 = new ContextMachine(contextTemplate1, contextInstance1, null, null, null
            , null, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(),"./target", null, null, null, null, null,
            null, null, null, null);
        if (ContextMachineCache.instance().getFirstByContextName("context-instance-1") == null) {
            ContextMachineCache.instance().put(contextMachine1);
        }
        logger.info("CONTEXT MACHINE CACHE SETUP : {}",ContextMachineCache.instance().toString());
    }

    @After
    public void teardown()
    {
        wiser.stop();
    }

    @Test
    public void test_with_no_record_from_config() {

        GenericNotificationDetails notificationDetails = new GenericNotificationDetails("agent-1", "context-instance-1", "context-id-1", "job-1", "context-instance-id-1", MonitorType.ERROR, InstanceStatus.ERROR);

        mockery.checking(new Expectations(){{
            oneOf(emailNotificationDetailsService).findByJobNameAndMonitorType("job-1","context-id-1","ERROR");
            will(returnValue(null));
            oneOf(emailNotificationContextService).findByContextName("context-instance-1", 50, 0);
            will(returnValue(null));
        }});

        emailNotifier.invoke(notificationDetails);

        List<WiserMessage> messages = wiser.getMessages();
        Assert.assertTrue("no messages should have been published", messages.size() == 0);

    }

    @Test
    public void test_with_a_record_from_config() throws MessagingException, IOException {

        GenericNotificationDetails notificationDetails = new GenericNotificationDetails("agent-1", "ContextParent1", "context-id-1", "job-1", "context-instance-id-1", MonitorType.ERROR, InstanceStatus.ERROR);

        EmailNotificationDetails emailNotificationDetails = new SolrEmailNotificationDetails();
        emailNotificationDetails.setContextName("ContextParent1");
        emailNotificationDetails.setJobName("job-1");
        emailNotificationDetails.setMonitorType("ERROR");
        emailNotificationDetails.setEmailSendTo(Arrays.asList("to-1", "to-2"));
        emailNotificationDetails.setEmailSendCc(Arrays.asList("cc-1"));
        emailNotificationDetails.setEmailSendBcc(Arrays.asList("bcc-1"));
        emailNotificationDetails.setEmailBody("body-1");
        emailNotificationDetails.setEmailSubject("subject-1");
        emailNotificationDetails.setEmailBodyTemplate("src/main/resources/templates/notification-error-email-body-template.txt");
        emailNotificationDetails.setEmailSubjectTemplate("src/main/resources/templates/notification-error-email-subject-template.txt");
        emailNotificationDetails.setHtml(true);

        SolrEmailNotificationDetailsRecord emailNotificationDetailsRecord = new SolrEmailNotificationDetailsRecord();
        emailNotificationDetailsRecord.setEmailNotificationDetails(emailNotificationDetails);

        mockery.checking(new Expectations(){{
            oneOf(emailNotificationDetailsService).findByJobNameAndMonitorType("job-1", "context-id-1","ERROR");
            will(returnValue(emailNotificationDetailsRecord));
            oneOf(notificationSendAuditService).find("context-instance-id-1", "context-id-1","job-1", "ERROR", "EMAIL");
            will(returnValue(null));
            oneOf(notificationSendAuditService).save(with(any(NotificationSendAuditRecord.class)));
        }});

        emailNotifier.invoke(notificationDetails);


        List<WiserMessage> messages = wiser.getMessages();

        Assert.assertTrue("Should be four messages - one per addressee", messages.size() == 4);
        for(WiserMessage message:wiser.getMessages())
        {
            Assert.assertEquals("sender-1" , message.getEnvelopeSender());

            MimeMessage mimeMessage = message.getMimeMessage();
            MimeMultipart mimeMultipart = (MimeMultipart)mimeMessage.getContent();
            Assert.assertTrue("should be only 1 bodypart", mimeMultipart.getCount() == 1);
            BodyPart bodyPart = mimeMultipart.getBodyPart(0);
            String content = (String)bodyPart.getContent();
            Assert.assertTrue(content.contains("The job job-1 for the Context context-id-1 had issues."));
            Assert.assertEquals("[DEVELOPMENT] *** ERROR *** ContextParent1 for job context-id-1/job-1 has error!!!", mimeMessage.getSubject());
        }
    }

    @Test
    public void test_with_a_template() throws MessagingException, IOException {

        GenericNotificationDetails notificationDetails = new GenericNotificationDetails("agent-1", "ContextParent1", "context-id-1", "job-1", "context-instance-id-1", MonitorType.ERROR, InstanceStatus.ERROR);

        EmailNotificationDetails emailNotificationDetails = new SolrEmailNotificationDetails();
        emailNotificationDetails.setContextName("ContextParent1");
        emailNotificationDetails.setJobName("job-1");
        emailNotificationDetails.setMonitorType("ERROR");
        emailNotificationDetails.setEmailSendTo(Arrays.asList("to-1"));
        emailNotificationDetails.setEmailBody("body-1");
        emailNotificationDetails.setEmailSubject("subject-1");
        emailNotificationDetails.setEmailBodyTemplate("src/test/resources/email/notification-email-template.txt");
        emailNotificationDetails.setEmailSubjectTemplate("src/main/resources/templates/notification-error-email-subject-template.txt");
        emailNotificationDetails.setHtml(false);

        SolrEmailNotificationDetailsRecord record = new SolrEmailNotificationDetailsRecord();
        record.setEmailNotificationDetails(emailNotificationDetails);

        mockery.checking(new Expectations(){{
            oneOf(emailNotificationDetailsService).findByJobNameAndMonitorType("job-1", "context-id-1","ERROR");
            will(returnValue(record));
            oneOf(notificationSendAuditService).find("context-instance-id-1", "context-id-1","job-1", "ERROR", "EMAIL");
            will(returnValue(null));
            oneOf(notificationSendAuditService).save(with(any(NotificationSendAuditRecord.class)));
        }});

        emailNotifier.invoke(notificationDetails);


        List<WiserMessage> messages = wiser.getMessages();

        Assert.assertTrue("Should be only one message", messages.size() == 1);

        WiserMessage message = wiser.getMessages().get(0);

        Assert.assertEquals("sender-1" , message.getEnvelopeSender());

        MimeMessage mimeMessage = message.getMimeMessage();
        MimeMultipart mimeMultipart = (MimeMultipart)mimeMessage.getContent();
        Assert.assertTrue("should be only 1 bodypart", mimeMultipart.getCount() == 1);
        BodyPart bodyPart = mimeMultipart.getBodyPart(0);
        String content = (String)bodyPart.getContent();
        Assert.assertTrue(content.contains("from template: job-1"));
        Assert.assertEquals("[DEVELOPMENT] *** ERROR *** ContextParent1 for job context-id-1/job-1 has error!!!", mimeMessage.getSubject());

    }

    @Test
    public void test_with_a_template_2() throws MessagingException, IOException {

        GenericNotificationDetails notificationDetails = new GenericNotificationDetails("agent-1", "ContextParent1", "context-id-1", "job-1", "context-instance-id-1", MonitorType.ERROR, InstanceStatus.ERROR);

        EmailNotificationDetails emailNotificationDetails = new SolrEmailNotificationDetails();
        emailNotificationDetails.setContextName("ContextParent1");
        emailNotificationDetails.setJobName("job-1");
        emailNotificationDetails.setMonitorType("ERROR");
        emailNotificationDetails.setEmailSendTo(Arrays.asList("to-1"));
        emailNotificationDetails.setEmailBody("body-1");
        emailNotificationDetails.setEmailSubject("subject-1");
        emailNotificationDetails.setEmailBodyTemplate("src/main/resources/templates/notification-error-email-body-template.txt");
        emailNotificationDetails.setEmailSubjectTemplate("src/main/resources/templates/notification-error-email-subject-template.txt");
        emailNotificationDetails.setHtml(false);

        SolrEmailNotificationDetailsRecord record = new SolrEmailNotificationDetailsRecord();
        record.setEmailNotificationDetails(emailNotificationDetails);

        mockery.checking(new Expectations(){{
            oneOf(emailNotificationDetailsService).findByJobNameAndMonitorType("job-1", "context-id-1","ERROR");
            will(returnValue(record));
            oneOf(notificationSendAuditService).find("context-instance-id-1", "context-id-1","job-1", "ERROR", "EMAIL");
            will(returnValue(null));
            oneOf(notificationSendAuditService).save(with(any(NotificationSendAuditRecord.class)));
        }});

        emailNotifier.invoke(notificationDetails);


        List<WiserMessage> messages = wiser.getMessages();

        Assert.assertTrue("Should be only one message", messages.size() == 1);

        WiserMessage message = wiser.getMessages().get(0);

        Assert.assertEquals("sender-1" , message.getEnvelopeSender());

        MimeMessage mimeMessage = message.getMimeMessage();
        MimeMultipart mimeMultipart = (MimeMultipart)mimeMessage.getContent();
        Assert.assertTrue("should be only 1 bodypart", mimeMultipart.getCount() == 1);
        BodyPart bodyPart = mimeMultipart.getBodyPart(0);
        String content = (String)bodyPart.getContent();
        Assert.assertTrue(content.contains("Log file : http://localhost:9090/schedulerJobLogFile/context-instance-id-1:::context-id-1:::job-1:::false"));
        Assert.assertTrue(content.contains("Error log file : http://localhost:9090/schedulerJobLogFile/context-instance-id-1:::context-id-1:::job-1:::true"));
        Assert.assertEquals("[DEVELOPMENT] *** ERROR *** ContextParent1 for job context-id-1/job-1 has error!!!", mimeMessage.getSubject());

    }

    @Test
    public void test_with_a_template_2_with_param_to_replace() throws MessagingException, IOException {

        GenericNotificationDetails notificationDetails = new GenericNotificationDetails("agent-1", "ContextParent1", "context-id-1", "job-1", "context-instance-id-1", MonitorType.ERROR, InstanceStatus.ERROR);

        EmailNotificationDetails emailNotificationDetails = new SolrEmailNotificationDetails();
        emailNotificationDetails.setContextName("ContextParent1");
        emailNotificationDetails.setJobName("job-1");
        emailNotificationDetails.setMonitorType("ERROR");
        emailNotificationDetails.setEmailSendTo(Arrays.asList("${EMAIL2}"));
        emailNotificationDetails.setEmailBody("body-1");
        emailNotificationDetails.setEmailSubject("subject-1 with replace = ${ABC}");
        emailNotificationDetails.setEmailBodyTemplate("src/main/resources/templates/notification-error-email-body-template.txt");
        emailNotificationDetails.setEmailSubjectTemplate("src/main/resources/templates/notification-error-email-subject-template.txt");
        emailNotificationDetails.setHtml(false);

        SolrEmailNotificationDetailsRecord record = new SolrEmailNotificationDetailsRecord();
        record.setEmailNotificationDetails(emailNotificationDetails);

        mockery.checking(new Expectations(){{
            oneOf(emailNotificationDetailsService).findByJobNameAndMonitorType("job-1", "context-id-1","ERROR");
            will(returnValue(record));
            oneOf(notificationSendAuditService).find("context-instance-id-1", "context-id-1","job-1", "ERROR", "EMAIL");
            will(returnValue(null));
            oneOf(notificationSendAuditService).save(with(any(NotificationSendAuditRecord.class)));
        }});

        emailNotifier.invoke(notificationDetails);


        List<WiserMessage> messages = wiser.getMessages();

        Assert.assertTrue("Should be three messages due to enrichment parameters", messages.size() == 3);

        for (WiserMessage w : messages) {
            Assert.assertTrue(w.getEnvelopeReceiver().equals("replace1@abc.com") ||
                w.getEnvelopeReceiver().equals("replace2@abc.com") ||
                w.getEnvelopeReceiver().equals("replace3@abc.com"));
        }

        // Just check the first one as all body should get the replacement
        WiserMessage message = wiser.getMessages().get(0);

        Assert.assertEquals("sender-1" , message.getEnvelopeSender());

        MimeMessage mimeMessage = message.getMimeMessage();
        MimeMultipart mimeMultipart = (MimeMultipart)mimeMessage.getContent();
        Assert.assertTrue("should be only 1 bodypart", mimeMultipart.getCount() == 1);
        BodyPart bodyPart = mimeMultipart.getBodyPart(0);
        String content = (String)bodyPart.getContent();
        Assert.assertTrue(content.contains("The job job-1 for the Context context-id-1 had issues."));
        Assert.assertTrue(content.contains("Log file : http://localhost:9090/schedulerJobLogFile/context-instance-id-1:::context-id-1:::job-1:::false"));
        Assert.assertTrue(content.contains("Error log file : http://localhost:9090/schedulerJobLogFile/context-instance-id-1:::context-id-1:::job-1:::true"));
        Assert.assertEquals("[DEVELOPMENT] *** ERROR *** ContextParent1 for job context-id-1/job-1 has error!!!", mimeMessage.getSubject());
    }

    @Test
    public void test_with_already_sent_before() throws MessagingException, IOException {

        GenericNotificationDetails notificationDetails = new GenericNotificationDetails("agent-1", "ContextParent1", "context-id-1", "job-1", "context-instance-id-1",MonitorType.ERROR, InstanceStatus.ERROR);

        EmailNotificationDetails emailNotificationDetails = new SolrEmailNotificationDetails();
        emailNotificationDetails.setContextName("ContextParent1");
        emailNotificationDetails.setJobName("job-1");
        emailNotificationDetails.setMonitorType("ERROR");
        emailNotificationDetails.setEmailSendTo(Arrays.asList("to-1", "to-2"));
        emailNotificationDetails.setEmailSendCc(Arrays.asList("cc-1"));
        emailNotificationDetails.setEmailSendBcc(Arrays.asList("bcc-1"));
        emailNotificationDetails.setEmailBody("body-1");
        emailNotificationDetails.setEmailSubject("subject-1");
        emailNotificationDetails.setHtml(true);

        EmailNotificationDetailsRecord emailNotificationDetailsRecord = new SolrEmailNotificationDetailsRecord();
        emailNotificationDetailsRecord.setEmailNotificationDetails(emailNotificationDetails);

        NotificationSendAudit notificationSendAudit = new SolrNotificationSendAudit();
        notificationSendAudit.setContextInstanceId("context-instance-id-1");
        notificationSendAudit.setJobName("job-1");
        notificationSendAudit.setMonitorType("ERROR");
        notificationSendAudit.setNotifierType("EMAIL");
        notificationSendAudit.setNotificationSend(true);

        NotificationSendAuditRecord notificationSendAuditRecord = new SolrNotificationSendAuditRecord();
        notificationSendAuditRecord.setNotificationSendAudit(notificationSendAudit);
        notificationSendAuditRecord.setTimestamp(new Date().getTime());

        mockery.checking(new Expectations(){{
            oneOf(emailNotificationDetailsService).findByJobNameAndMonitorType("job-1", "context-id-1","ERROR");
            will(returnValue(emailNotificationDetailsRecord));
            oneOf(notificationSendAuditService).find("context-instance-id-1", "context-id-1","job-1", "ERROR", "EMAIL");
            will(returnValue(notificationSendAuditRecord));
        }});

        emailNotifier.invoke(notificationDetails);

        List<WiserMessage> messages = wiser.getMessages();
        Assert.assertTrue("no messages should have been published", messages.size() == 0);
    }

    @Test
    public void test_with_not_sent_before() throws MessagingException, IOException {

        GenericNotificationDetails notificationDetails = new GenericNotificationDetails("agent-1", "ContextParent1", "context-id-1", "job-1","context-instance-id-1", MonitorType.ERROR, InstanceStatus.ERROR);

        EmailNotificationDetails emailNotificationDetails = new SolrEmailNotificationDetails();
        emailNotificationDetails.setContextName("ContextParent1");
        emailNotificationDetails.setJobName("job-1");
        emailNotificationDetails.setMonitorType("ERROR");
        emailNotificationDetails.setEmailSendTo(Arrays.asList("to-1", "to-2"));
        emailNotificationDetails.setEmailSendCc(Arrays.asList("cc-1"));
        emailNotificationDetails.setEmailSendBcc(Arrays.asList("bcc-1"));
        emailNotificationDetails.setEmailBody("body-1");
        emailNotificationDetails.setEmailSubject("subject-1");
        emailNotificationDetails.setEmailBodyTemplate("src/main/resources/templates/notification-error-email-body-template.txt");
        emailNotificationDetails.setEmailSubjectTemplate("src/main/resources/templates/notification-error-email-subject-template.txt");
        emailNotificationDetails.setHtml(true);

        EmailNotificationDetailsRecord emailNotificationDetailsRecord = new SolrEmailNotificationDetailsRecord();
        emailNotificationDetailsRecord.setEmailNotificationDetails(emailNotificationDetails);

        NotificationSendAudit notificationSendAudit = new SolrNotificationSendAudit();
        notificationSendAudit.setContextInstanceId("context-instance-id-1");
        notificationSendAudit.setJobName("job-1");
        notificationSendAudit.setMonitorType("ERROR");
        notificationSendAudit.setNotifierType("EMAIL");
        notificationSendAudit.setNotificationSend(false);

        NotificationSendAuditRecord notificationSendAuditRecord = new SolrNotificationSendAuditRecord();
        notificationSendAuditRecord.setNotificationSendAudit(notificationSendAudit);
        notificationSendAuditRecord.setTimestamp(new Date().getTime());

        mockery.checking(new Expectations(){{
            oneOf(emailNotificationDetailsService).findByJobNameAndMonitorType("job-1", "context-id-1","ERROR");
            will(returnValue(emailNotificationDetailsRecord));
            oneOf(notificationSendAuditService).find("context-instance-id-1", "context-id-1","job-1", "ERROR", "EMAIL");
            will(returnValue(notificationSendAuditRecord));
            oneOf(notificationSendAuditService).save(with(any(NotificationSendAuditRecord.class)));
        }});

        emailNotifier.invoke(notificationDetails);

        List<WiserMessage> messages = wiser.getMessages();

        Assert.assertTrue("Should be four messages - one per addressee", messages.size() == 4);
        for(WiserMessage message:wiser.getMessages())
        {
            Assert.assertEquals("sender-1" , message.getEnvelopeSender());

            MimeMessage mimeMessage = message.getMimeMessage();
            MimeMultipart mimeMultipart = (MimeMultipart)mimeMessage.getContent();
            Assert.assertTrue("should be only 1 bodypart", mimeMultipart.getCount() == 1);
            BodyPart bodyPart = mimeMultipart.getBodyPart(0);
            String content = (String)bodyPart.getContent();
            Assert.assertTrue(content.contains("The job job-1 for the Context context-id-1 had issues."));
            Assert.assertEquals("[DEVELOPMENT] *** ERROR *** ContextParent1 for job context-id-1/job-1 has error!!!", mimeMessage.getSubject());
        }
    }

    @Test
    public void testParameterReplace() {
        EmailNotificationDetails details = new EmailNotificationDetailsImpl();
        details.setContextName("ContextParent1");
        Map<String, String> emailNotificationTemplateParameters = new HashMap<>();
        emailNotificationTemplateParameters.put("KEY1", "Something Not Replace");
        emailNotificationTemplateParameters.put("KEY2", "Something To Replace = ${ABC}");
        emailNotificationTemplateParameters.put("KEY3", "Something To Replace = ${DEF}");
        emailNotificationTemplateParameters.put("KEY4", "Something To Replace = ${ABC}+${DEF}");
        emailNotificationTemplateParameters.put("KEY5", "Something To Replace = ${ABC}+${ABC}");
        details.setEmailNotificationTemplateParameters(emailNotificationTemplateParameters);

        List<String> emailTo = new ArrayList<>();
        emailTo.add("${ABC}@abc.com");
        emailTo.add("${ABC}${DEF}@abc.com");
        emailTo.add("${EMAIL}");
        emailTo.add("${EMAIL2}");
        emailTo.add("donottouch@abc.com");
        details.setEmailSendTo(emailTo);

        List<String> emailCc = new ArrayList<>();
        emailCc.add("cc-dont-touch@abc.com");
        emailCc.add("${EMAIL2}");
        emailCc.add("${ABC}@ab-cc-carbon.com");
        details.setEmailSendCc(emailCc);

        List<String> emailBcc = new ArrayList<>();
        emailBcc.add("bcc-leave@abc.com");
        emailBcc.add("${EMAIL2}");
        emailBcc.add("${ABC}@bcc-test.com");
        details.setEmailSendBcc(emailBcc);

        details.setEmailSubject("Email subject for ${EMAIL} to review");
        details.setEmailSubjectTemplate("${URL}some_path/subject.txt");

        details.setEmailBody("Body with ${ABC} to replace and also ${ABC} and ${DEF} with ${EMAIL2}");
        details.setEmailBodyTemplate("${URL}some_path/body.txt");

        emailNotifier.parameterReplace(details, emailNotificationParamsConfiguration);
        logger.info("Enriched EmailNotificationDetails = {}", details);

        Assert.assertEquals("Something Not Replace", details.getEmailNotificationTemplateParameters().get("KEY1"));
        Assert.assertEquals("Something To Replace = Value1", details.getEmailNotificationTemplateParameters().get("KEY2"));
        Assert.assertEquals("Something To Replace = Value21", details.getEmailNotificationTemplateParameters().get("KEY3"));
        Assert.assertEquals("Something To Replace = Value1+Value21", details.getEmailNotificationTemplateParameters().get("KEY4"));
        Assert.assertEquals("Something To Replace = Value1+Value1", details.getEmailNotificationTemplateParameters().get("KEY5"));

        Assert.assertTrue(details.getEmailSendTo().contains("Value1@abc.com"));
        Assert.assertTrue(details.getEmailSendTo().contains("Value1Value21@abc.com"));
        Assert.assertTrue(details.getEmailSendTo().contains("replace0@abc.com"));
        Assert.assertTrue(details.getEmailSendTo().contains("replace1@abc.com"));
        Assert.assertTrue(details.getEmailSendTo().contains("replace2@abc.com"));
        Assert.assertTrue(details.getEmailSendTo().contains("replace3@abc.com"));
        Assert.assertTrue(details.getEmailSendTo().contains("donottouch@abc.com"));
        Assert.assertEquals(7, details.getEmailSendTo().size());

        Assert.assertTrue(details.getEmailSendCc().contains("cc-dont-touch@abc.com"));
        Assert.assertTrue(details.getEmailSendCc().contains("Value1@ab-cc-carbon.com"));
        Assert.assertTrue(details.getEmailSendCc().contains("replace1@abc.com"));
        Assert.assertTrue(details.getEmailSendCc().contains("replace2@abc.com"));
        Assert.assertTrue(details.getEmailSendCc().contains("replace3@abc.com"));
        Assert.assertEquals(5, details.getEmailSendCc().size());

        Assert.assertTrue(details.getEmailSendBcc().contains("bcc-leave@abc.com"));
        Assert.assertTrue(details.getEmailSendBcc().contains("Value1@bcc-test.com"));
        Assert.assertTrue(details.getEmailSendBcc().contains("replace1@abc.com"));
        Assert.assertTrue(details.getEmailSendBcc().contains("replace2@abc.com"));
        Assert.assertTrue(details.getEmailSendBcc().contains("replace3@abc.com"));
        Assert.assertEquals(5, details.getEmailSendBcc().size());

        Assert.assertEquals("Email subject for replace0@abc.com to review", details.getEmailSubject());
        Assert.assertEquals("/opt/config/some_path/subject.txt", details.getEmailSubjectTemplate());

        Assert.assertEquals("Body with Value1 to replace and also Value1 and Value21 with replace1@abc.com,replace2@abc.com,replace3@abc.com", details.getEmailBody());
        Assert.assertEquals("/opt/config/some_path/body.txt", details.getEmailBodyTemplate());

    }

    @Test
    public void test_with_no_record_from_config_with_context_notification() throws MessagingException, IOException {

        GenericNotificationDetails notificationDetails = new GenericNotificationDetails("agent-1", "ContextParent1", "context-id-1", "job-1", "context-instance-id-1", MonitorType.ERROR, InstanceStatus.ERROR);

        NotificationSendAudit notificationSendAudit = new SolrNotificationSendAudit();
        notificationSendAudit.setContextInstanceId("context-instance-id-1");
        notificationSendAudit.setJobName("job-1");
        notificationSendAudit.setMonitorType("ERROR");
        notificationSendAudit.setNotifierType("EMAIL");
        notificationSendAudit.setNotificationSend(false);

        NotificationSendAuditRecord notificationSendAuditRecord = new SolrNotificationSendAuditRecord();
        notificationSendAuditRecord.setNotificationSendAudit(notificationSendAudit);
        notificationSendAuditRecord.setTimestamp(new Date().getTime());

        mockery.checking(new Expectations(){{
            oneOf(emailNotificationDetailsService).findByJobNameAndMonitorType("job-1","context-id-1","ERROR");
            will(returnValue(null));
            oneOf(emailNotificationContextService).findByContextName("ContextParent1", 50, 0);
            will(returnValue(emailNotificationContext("ContextParent1")));
            oneOf(notificationSendAuditService).find("context-instance-id-1", "context-id-1","job-1", "ERROR", "EMAIL");
            will(returnValue(notificationSendAuditRecord));
            oneOf(notificationSendAuditService).save(with(any(NotificationSendAuditRecord.class)));
        }});

        emailNotifier.invoke(notificationDetails);

        List<WiserMessage> messages = wiser.getMessages();

        Assert.assertTrue("Should be only one message", messages.size() == 1);

        WiserMessage message = wiser.getMessages().get(0);

        Assert.assertEquals("sender-1" , message.getEnvelopeSender());
        Assert.assertEquals("replace0@abc.com", message.getEnvelopeReceiver());

        MimeMessage mimeMessage = message.getMimeMessage();
        MimeMultipart mimeMultipart = (MimeMultipart)mimeMessage.getContent();
        Assert.assertTrue("should be only 1 bodypart", mimeMultipart.getCount() == 1);
        BodyPart bodyPart = mimeMultipart.getBodyPart(0);
        String content = (String)bodyPart.getContent();
        Assert.assertTrue(content.contains("Log file : http://localhost:9090/schedulerJobLogFile/context-instance-id-1:::context-id-1:::job-1:::false"));
        Assert.assertTrue(content.contains("Error log file : http://localhost:9090/schedulerJobLogFile/context-instance-id-1:::context-id-1:::job-1:::true"));
        Assert.assertEquals("[DEVELOPMENT] *** ERROR *** ContextParent1 for job context-id-1/job-1 has error!!!", mimeMessage.getSubject());

    }

    @Test
    public void test_with_no_record_from_config_with_context_notification_overdue() throws MessagingException, IOException {

        GenericNotificationDetails notificationDetails = new GenericNotificationDetails("agent-1", "ContextParent1", "context-id-1", "job-1", "context-instance-id-1", MonitorType.OVERDUE, InstanceStatus.ERROR);

        NotificationSendAudit notificationSendAudit = new SolrNotificationSendAudit();
        notificationSendAudit.setContextInstanceId("context-instance-id-1");
        notificationSendAudit.setJobName("job-1");
        notificationSendAudit.setMonitorType("OVERDUE");
        notificationSendAudit.setNotifierType("EMAIL");
        notificationSendAudit.setNotificationSend(false);

        NotificationSendAuditRecord notificationSendAuditRecord = new SolrNotificationSendAuditRecord();
        notificationSendAuditRecord.setNotificationSendAudit(notificationSendAudit);
        notificationSendAuditRecord.setTimestamp(new Date().getTime());

        mockery.checking(new Expectations(){{
            oneOf(emailNotificationDetailsService).findByJobNameAndMonitorType("job-1","context-id-1","OVERDUE");
            will(returnValue(null));
            oneOf(emailNotificationContextService).findByContextName("ContextParent1", 50, 0);
            will(returnValue(emailNotificationContext("ContextParent1")));
            oneOf(notificationSendAuditService).find("context-instance-id-1", "context-id-1","job-1", "OVERDUE", "EMAIL");
            will(returnValue(notificationSendAuditRecord));
            oneOf(notificationSendAuditService).save(with(any(NotificationSendAuditRecord.class)));
        }});

        emailNotifier.invoke(notificationDetails);

        List<WiserMessage> messages = wiser.getMessages();

        Assert.assertTrue("Should be only one message", messages.size() == 3);

        for(WiserMessage message:wiser.getMessages()) {

            Assert.assertEquals("sender-1", message.getEnvelopeSender());
            Assert.assertTrue(message.getEnvelopeReceiver().equals("replace1@abc.com") ||
                message.getEnvelopeReceiver().equals("replace2@abc.com") ||
                message.getEnvelopeReceiver().equals("replace3@abc.com"));

            MimeMessage mimeMessage = message.getMimeMessage();
            MimeMultipart mimeMultipart = (MimeMultipart) mimeMessage.getContent();
            Assert.assertTrue("should be only 1 bodypart", mimeMultipart.getCount() == 1);
            BodyPart bodyPart = mimeMultipart.getBodyPart(0);
            String content = (String) bodyPart.getContent();

            Assert.assertTrue(content.contains("File Path Location:"));
            Assert.assertTrue(content.contains("Files that may not have arrived:"));
            Assert.assertEquals("[DEVELOPMENT] *** OVERDUE FILE *** ContextParent1 for job context-id-1/job-1 file has not arrived!", mimeMessage.getSubject());
        }

    }

    @Test
    public void test_with_no_record_from_config_with_context_notification_complete_no_email() throws MessagingException, IOException {

        GenericNotificationDetails notificationDetails = new GenericNotificationDetails("agent-1", "ContextParent1", "context-id-1", "job-1", "context-instance-id-1", MonitorType.COMPLETE, InstanceStatus.COMPLETE);

        mockery.checking(new Expectations(){{
            oneOf(emailNotificationDetailsService).findByJobNameAndMonitorType("job-1","context-id-1","COMPLETE");
            will(returnValue(null));
            oneOf(emailNotificationContextService).findByContextName("ContextParent1", 50, 0);
            will(returnValue(emailNotificationContext("ContextParent1")));
        }});

        emailNotifier.invoke(notificationDetails);

        List<WiserMessage> messages = wiser.getMessages();

        Assert.assertTrue("Should be no message as no notification setup", messages.size() == 0);

    }

    private SearchResults<EmailNotificationContextRecord> emailNotificationContext(String contextName) {

        EmailNotificationContext notificationContext = new EmailNotificationContextImpl();
        notificationContext.setContextName(contextName);
        notificationContext.setMonitorTypes(List.of("ERROR", "OVERDUE"));
        notificationContext.setEmailSendTo(List.of("${EMAIL}"));
        notificationContext.setEmailSendToByMonitorType(Map.of("OVERDUE", List.of("${EMAIL2}")));
        notificationContext.setEmailSubjectNotificationTemplate(Map.of("ERROR", "src/main/resources/templates/notification-error-email-subject-template.txt",
            "OVERDUE", "src/main/resources/templates/notification-overdue-email-subject-template.txt"));
        notificationContext.setEmailBodyNotificationTemplate(Map.of("ERROR", "src/main/resources/templates/notification-error-email-body-template.txt",
            "OVERDUE", "src/main/resources/templates/notification-overdue-email-body-template.txt"));

        EmailNotificationContextRecord record = new EmailNotificationContextRecordImpl();
        record.setContextName(contextName);
        record.setId(contextName);
        record.setEmailNotificationContext(notificationContext);

        return new SearchResultsImpl<>(List.of(record), 1, 0);
    }

    private EmailNotificationParamsConfiguration emailNotificationParamsConfiguration() {
        EmailNotificationParamsConfiguration param = new EmailNotificationParamsConfiguration();
        Map<String, String> paramValues = new HashMap<>();
        paramValues.put("${ABC}", "Value1");
        paramValues.put("${DEF}", "Value21");
        paramValues.put("${EMAIL}", "replace0@abc.com");
        paramValues.put("${EMAIL2}", "replace1@abc.com,replace2@abc.com,replace3@abc.com");
        paramValues.put("${URL}", "/opt/config/");
        Map<String, Map<String, String>> paramMaps = new HashMap<>();
        paramMaps.put("ContextParent1", paramValues);
        param.setParamsToReplace(paramMaps);
        return param;
    }

    private TemplateEngine emailTemplateEngine() {
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

    private EmailNotifierConfiguration getConfiguration()
    {
        EmailNotifierConfiguration configuration = new EmailNotifierConfiguration();
        configuration.setMailHost("localhost");
        configuration.setMailSmtpPort(2500);
        configuration.setMailFrom("sender-1");
        configuration.setActive(true);

        Map<String,String> props = new HashMap<String,String>();
        configuration.setExtendedMailSessionProperties(props);
        configuration.setNotificationIntervalInSeconds(1L);

        return configuration;
    }


}
