package org.ikasan.dashboard.notification;

import org.ikasan.dashboard.notification.business.stream.BusinessStreamNotificationJob;
import org.ikasan.dashboard.notification.business.stream.model.BusinessStreamNotification;
import org.ikasan.dashboard.notification.email.EmailNotifier;
import org.ikasan.dashboard.notification.business.stream.service.BusinessStreamNotificationSchedulerService;
import org.ikasan.dashboard.notification.business.stream.service.BusinessStreamNotificationService;

import org.ikasan.dashboard.notification.scheduler.SchedulerNotificationJob;
import org.ikasan.dashboard.notification.scheduler.model.SchedulerNotification;
import org.ikasan.dashboard.notification.scheduler.service.SchedulerNotificationSchedulerService;
import org.ikasan.dashboard.notification.scheduler.service.SchedulerNotificationService;
import org.ikasan.monitor.notifier.EmailNotifierConfiguration;
import org.ikasan.scheduler.CachingScheduledJobFactory;
import org.ikasan.scheduler.SchedulerFactory;
import org.ikasan.spec.configuration.PlatformConfigurationService;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;
import org.ikasan.spec.solr.SolrGeneralService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class NotificationConfiguration {


    @Bean
    public EmailNotifier emailNotifier(EmailNotifierConfiguration emailConfiguration) {
        EmailNotifier emailNotifier = new EmailNotifier();
        emailNotifier.setConfiguration(emailConfiguration);

        return emailNotifier;
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

    @Bean
    public List<BusinessStreamNotificationJob> businessStreamNotificationJobs(TemplateEngine emailTemplateEngine,
                                                                              List<BusinessStreamNotification> businessStreamNotifications,
                                                                              BusinessStreamNotificationService businessStreamNotificationService,
                                                                              PlatformConfigurationService platformConfigurationService,
                                                                              EmailNotifier emailNotifier) {
        return businessStreamNotifications.stream()
            .map(businessStreamNotification -> new BusinessStreamNotificationJob(emailTemplateEngine
                , businessStreamNotification, businessStreamNotificationService, platformConfigurationService, emailNotifier))
            .collect(Collectors.toList());
    }

    @Bean
    public List<SchedulerNotificationJob> schedulerNotificationJobs(TemplateEngine emailTemplateEngine,
                                                                              List<SchedulerNotification> schedulerNotifications,
                                                                              SchedulerNotificationService schedulerNotificationService,
                                                                              PlatformConfigurationService platformConfigurationService,
                                                                              EmailNotifier emailNotifier) {
        return schedulerNotifications.stream()
            .map(schedulerNotification -> new SchedulerNotificationJob(emailTemplateEngine
                , schedulerNotification, schedulerNotificationService, platformConfigurationService, emailNotifier))
            .collect(Collectors.toList());
    }

    @Bean
    @ConfigurationProperties(prefix = "dashboard.notification")
    public List<BusinessStreamNotification> businessStreamNotifications() {
        return new ArrayList<>();
    }

    @Bean
    @ConfigurationProperties(prefix = "scheduler.notification")
    public List<SchedulerNotification> schedulerNotifications() {
        return new ArrayList<>();
    }

    @Bean
    public BusinessStreamNotificationService businessStreamNotificationService(BusinessStreamMetaDataService businessStreamMetaDataService
        , SolrGeneralService solrGeneralService) {
            return new BusinessStreamNotificationService(businessStreamMetaDataService,
            solrGeneralService);
    }

    @Bean
    public SchedulerNotificationService schedulerNotificationService(SolrGeneralService solrGeneralService) {
        return new SchedulerNotificationService(solrGeneralService);
    }

    @Bean
    @DependsOn("dashboardSchedulerService")
    public BusinessStreamNotificationSchedulerService businessStreamNotificationSchedulerService(List<BusinessStreamNotificationJob> businessStreamNotificationJobs) {
        return new BusinessStreamNotificationSchedulerService(SchedulerFactory.getInstance().getScheduler()
            , CachingScheduledJobFactory.getInstance(), businessStreamNotificationJobs);
    }

    @Bean
    @DependsOn("schedulerNotificationService")
    public SchedulerNotificationSchedulerService schedulerNotificationSchedulerService(List<SchedulerNotificationJob> schedulerNotificationJobs) {
        return new SchedulerNotificationSchedulerService(SchedulerFactory.getInstance().getScheduler()
            , CachingScheduledJobFactory.getInstance(), schedulerNotificationJobs);
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
