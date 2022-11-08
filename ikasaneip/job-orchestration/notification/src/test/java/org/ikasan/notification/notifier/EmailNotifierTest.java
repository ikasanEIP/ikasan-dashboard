package org.ikasan.notification.notifier;

import org.ikasan.job.orchestration.model.notification.GenericNotificationDetails;
import org.ikasan.job.orchestration.model.notification.MonitorType;
import org.ikasan.monitor.notifier.EmailNotifierConfiguration;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationDetails;
import org.ikasan.scheduled.notification.model.SolrEmailNotificationDetailsRecord;
import org.ikasan.scheduled.notification.model.SolrNotificationSendAudit;
import org.ikasan.scheduled.notification.model.SolrNotificationSendAuditRecord;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;
import org.ikasan.spec.scheduled.notification.model.*;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.notification.service.NotificationSendAuditService;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
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
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static Logger logger = LoggerFactory.getLogger(EmailNotifierTest.class);

    /** in memory SMTP server */
    Wiser wiser;

    private EmailNotifier emailNotifier;
    private EmailNotificationDetailsService emailNotificationDetailsService = mockery.mock(EmailNotificationDetailsService.class);
    private NotificationSendAuditService notificationSendAuditService = mockery.mock(NotificationSendAuditService.class);

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

        emailNotifier = new EmailNotifier(emailNotificationDetailsService, notificationSendAuditService, emailTemplateEngine(), "http://localhost:9090/schedulerJobLogFile/");
        emailNotifier.setConfiguration(getConfiguration());
    }

    @After
    public void teardown()
    {
        wiser.stop();
    }

    @Test
    public void test_with_no_record_from_config() {

        GenericNotificationDetails notificationDetails = new GenericNotificationDetails("context-id-1", "job-1", "context-instance-id-1", MonitorType.ERROR, InstanceStatus.ERROR);

        mockery.checking(new Expectations(){{
            oneOf(emailNotificationDetailsService).findByJobNameAndMonitorType("job-1","context-id-1","ERROR");
            will(returnValue(null));
        }});

        emailNotifier.invoke(notificationDetails);

        List<WiserMessage> messages = wiser.getMessages();
        Assert.assertTrue("no messages should have been published", messages.size() == 0);

    }

    @Test
    public void test_with_a_record_from_config() throws MessagingException, IOException {

        GenericNotificationDetails notificationDetails = new GenericNotificationDetails("context-id-1", "job-1", "context-instance-id-1", MonitorType.ERROR, InstanceStatus.ERROR);

        EmailNotificationDetails emailNotificationDetails = new SolrEmailNotificationDetails();
        emailNotificationDetails.setJobName("job-1");
        emailNotificationDetails.setMonitorType("ERROR");
        emailNotificationDetails.setEmailSendTo(Arrays.asList("to-1", "to-2"));
        emailNotificationDetails.setEmailSendCc(Arrays.asList("cc-1"));
        emailNotificationDetails.setEmailSendBcc(Arrays.asList("bcc-1"));
        emailNotificationDetails.setEmailBody("body-1");
        emailNotificationDetails.setEmailSubject("subject-1");
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
            Assert.assertTrue(content.contains("body-1"));
            Assert.assertEquals("subject-1", mimeMessage.getSubject());
        }
    }

    @Test
    public void test_with_a_template() throws MessagingException, IOException {

        GenericNotificationDetails notificationDetails = new GenericNotificationDetails("context-id-1", "job-1", "context-instance-id-1", MonitorType.ERROR, InstanceStatus.ERROR);

        EmailNotificationDetails emailNotificationDetails = new SolrEmailNotificationDetails();
        emailNotificationDetails.setJobName("job-1");
        emailNotificationDetails.setMonitorType("ERROR");
        emailNotificationDetails.setEmailSendTo(Arrays.asList("to-1"));
        emailNotificationDetails.setEmailBody("body-1");
        emailNotificationDetails.setEmailSubject("subject-1");
        emailNotificationDetails.setEmailBodyTemplate("src/test/resources/email/notification-email-template.txt");
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
        Assert.assertEquals("subject-1", mimeMessage.getSubject());

    }

    @Test
    public void test_with_a_template_2() throws MessagingException, IOException {

        GenericNotificationDetails notificationDetails = new GenericNotificationDetails("context-id-1", "job-1", "context-instance-id-1", MonitorType.ERROR, InstanceStatus.ERROR);

        EmailNotificationDetails emailNotificationDetails = new SolrEmailNotificationDetails();
        emailNotificationDetails.setJobName("job-1");
        emailNotificationDetails.setMonitorType("ERROR");
        emailNotificationDetails.setEmailSendTo(Arrays.asList("to-1"));
        emailNotificationDetails.setEmailBody("body-1");
        emailNotificationDetails.setEmailSubject("subject-1");
        emailNotificationDetails.setEmailBodyTemplate("src/main/resources/templates/notification-error-email-body-template.txt");
        emailNotificationDetails.setHtml(false);

        Map<String,String> templateParams = new HashMap<>();
        templateParams.put(EmailNotificationTemplateParameters.EMAIL_BODY_TEXT.name(), "from template body text!");

        emailNotificationDetails.setEmailNotificationTemplateParameters(templateParams);

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
        Assert.assertTrue(content.contains("from template body text!"));
        Assert.assertTrue(content.contains("You can access to log file : http://localhost:9090/schedulerJobLogFile/context-instance-id-1:::context-id-1:::job-1:::false"));
        Assert.assertTrue(content.contains("Error log file : http://localhost:9090/schedulerJobLogFile/context-instance-id-1:::context-id-1:::job-1:::true"));
        Assert.assertEquals("subject-1", mimeMessage.getSubject());

    }

    @Test
    public void test_with_already_sent_before() throws MessagingException, IOException {

        GenericNotificationDetails notificationDetails = new GenericNotificationDetails("context-id-1", "job-1", "context-instance-id-1",MonitorType.ERROR, InstanceStatus.ERROR);

        EmailNotificationDetails emailNotificationDetails = new SolrEmailNotificationDetails();
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

        GenericNotificationDetails notificationDetails = new GenericNotificationDetails("context-id-1", "job-1","context-instance-id-1", MonitorType.ERROR, InstanceStatus.ERROR);

        EmailNotificationDetails emailNotificationDetails = new SolrEmailNotificationDetails();
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
            Assert.assertTrue(content.contains("body-1"));
            Assert.assertEquals("subject-1", mimeMessage.getSubject());
        }
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
