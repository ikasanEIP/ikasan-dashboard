package org.ikasan.notification;

import org.ikasan.job.orchestration.model.notification.Monitor;
import org.ikasan.job.orchestration.model.notification.Notifier;
import org.ikasan.monitor.notifier.EmailNotifierConfiguration;
import org.ikasan.notification.monitor.ErrorMonitorImpl;
import org.ikasan.notification.monitor.OverdueFileMonitorImpl;
import org.ikasan.notification.notifier.EmailErrorNotifier;
import org.ikasan.notification.notifier.EmailOverdueFileNotifier;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class NotificationConfiguration {

    @Resource
    private SchedulerJobService schedulerJobService;

    @Value("${scheduler.notification.file.overdue.tolerance.minutes:30}")
    private Integer fileArrivalToleranceInMinutes;

    /** default executor service is a single thread executor */
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Bean
    public EmailErrorNotifier emailErrorNotifier() {
        EmailErrorNotifier emailNotifier = new EmailErrorNotifier();
        return emailNotifier;
    }

    @Bean
    public EmailOverdueFileNotifier emailOverdueFileNotifier() {
        EmailOverdueFileNotifier emailNotifier = new EmailOverdueFileNotifier();
        return emailNotifier;
    }

    @Bean
    public Monitor errorMonitor(List<Notifier> errorNotifiers) {
        Monitor monitor = new ErrorMonitorImpl(executorService);
        monitor.setNotifiers(errorNotifiers);
        return monitor;
    }

    @Bean
    public Monitor overdueFileMonitor(List<Notifier> overdueFileNotifiers) {
        Monitor monitor = new OverdueFileMonitorImpl(fileArrivalToleranceInMinutes, executorService, schedulerJobService);
        monitor.setNotifiers(overdueFileNotifiers);
        return monitor;
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
