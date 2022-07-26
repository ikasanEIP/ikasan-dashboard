package org.ikasan.notification;

import org.ikasan.monitor.notifier.EmailNotifierConfiguration;
import org.ikasan.job.orchestration.core.notification.MonitorManagement;
import org.ikasan.notification.monitor.JobRunningTimesMonitorImpl;
import org.ikasan.notification.monitor.StateChangeMonitorImpl;
import org.ikasan.notification.monitor.OverdueFileMonitorImpl;
import org.ikasan.notification.notifier.EmailNotifier;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.notification.model.Monitor;
import org.ikasan.spec.scheduled.notification.model.Notifier;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.notification.service.NotificationSendAuditService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class NotificationConfiguration {

    @Resource
    private SchedulerJobService schedulerJobService;

    @Resource
    private SchedulerJobInstanceService schedulerJobInstanceService;

    @Resource
    private InternalEventDrivenJobService internalEventDrivenJobService;

    @Resource
    private EmailNotificationDetailsService emailNotificationDetailsService;

    @Resource
    private NotificationSendAuditService notificationSendAuditService;

    @Value("${scheduler.notification.file.overdue.tolerance.minutes:30}")
    private Integer fileArrivalToleranceInMinutes;

    @Value("${mail.link.url}")
    private String mailLinkUrl;

    /** default executor service is a single thread executor */
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Bean
    public EmailNotifier notificationEmailNotifier(TemplateEngine emailTemplateEngine, EmailNotifierConfiguration emailConfiguration) {
        EmailNotifier emailNotifier = new EmailNotifier(emailNotificationDetailsService, notificationSendAuditService, emailTemplateEngine, mailLinkUrl);
        emailNotifier.setConfiguration(emailConfiguration);
        return emailNotifier;
    }

    @Bean
    public Monitor stateChangeMonitor(List<Notifier> stateChangeNotifiers) {
        Monitor monitor = new StateChangeMonitorImpl(executorService);
        monitor.setNotifiers(stateChangeNotifiers);
        return monitor;
    }

    @Bean
    public Monitor overdueFileMonitor(List<Notifier> overdueFileNotifiers) {
        Monitor monitor = new OverdueFileMonitorImpl(fileArrivalToleranceInMinutes, executorService, schedulerJobInstanceService);
        monitor.setNotifiers(overdueFileNotifiers);
        return monitor;
    }

    @Bean
    public Monitor jobRunningTimesMonitor(List<Notifier> jobRunningTimesNotifiers) {
        Monitor monitor = new JobRunningTimesMonitorImpl(executorService, schedulerJobInstanceService, internalEventDrivenJobService);
        monitor.setNotifiers(jobRunningTimesNotifiers);
        return monitor;
    }

    @Bean
    public List<Notifier> stateChangeNotifiers(EmailNotifier notificationEmailNotifier) {
        return Arrays.asList(notificationEmailNotifier);
    }

    @Bean
    public List<Notifier> jobRunningTimesNotifiers(EmailNotifier notificationEmailNotifier) {
        return Arrays.asList(notificationEmailNotifier);
    }

    @Bean
    public List<Notifier> overdueFileNotifiers(EmailNotifier notificationEmailNotifier) {
        return Arrays.asList(notificationEmailNotifier);
    }

    @Bean
    public MonitorManagement monitorManagement(Monitor stateChangeMonitor, Monitor overdueFileMonitor, Monitor jobRunningTimesMonitor) {
        MonitorManagement monitorManagement = new MonitorManagement();
        monitorManagement.registerMonitor(stateChangeMonitor);
        monitorManagement.registerMonitor(overdueFileMonitor);
        monitorManagement.registerMonitor(jobRunningTimesMonitor);
        return monitorManagement;
    }

    @Bean
    @ConfigurationProperties(prefix = "mail")
    private EmailNotifierConfiguration emailConfiguration() {
        return new EmailNotifierConfiguration();
    }

    @Bean
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
}
