package org.ikasan.dashboard.notification.scheduler;

import org.ikasan.dashboard.notification.email.EmailNotification;
import org.ikasan.dashboard.notification.email.EmailNotifier;
import org.ikasan.dashboard.notification.scheduler.model.SchedulerNotification;
import org.ikasan.dashboard.notification.scheduler.service.SchedulerNotificationService;
import org.ikasan.dashboard.schedule.DashboardJob;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.spec.configuration.PlatformConfigurationService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Optional;

public class SchedulerNotificationJob implements DashboardJob {

    private static Logger logger = LoggerFactory.getLogger(SchedulerNotificationJob.class);

    public static final String LAST_RUN_TIMESTAMP = "-last-run-timestamp";

    private TemplateEngine templateEngine;
    private SchedulerNotification schedulerNotification;
    private SchedulerNotificationService schedulerNotificationService;
    private PlatformConfigurationService platformConfigurationService;
    private EmailNotifier emailNotifier;

    public SchedulerNotificationJob(TemplateEngine templateEngine, SchedulerNotification schedulerNotification,
                                    SchedulerNotificationService schedulerNotificationService,
                                    PlatformConfigurationService platformConfigurationService,
                                    EmailNotifier emailNotifier) {
        this.templateEngine = templateEngine;
        if(this.templateEngine == null) {
            throw new IllegalArgumentException("templateEngine cannot be null!");
        }
        this.schedulerNotification = schedulerNotification;
        if(this.schedulerNotification == null) {
            throw new IllegalArgumentException("schedulerNotification cannot be null!");
        }
        this.schedulerNotificationService = schedulerNotificationService;
        if(this.schedulerNotificationService == null) {
            throw new IllegalArgumentException("schedulerNotificationService cannot be null!");
        }
        this.platformConfigurationService = platformConfigurationService;
        if(this.platformConfigurationService == null) {
            throw new IllegalArgumentException("platformConfigurationService cannot be null!");
        }
        this.emailNotifier = emailNotifier;
        if(this.emailNotifier == null) {
            throw new IllegalArgumentException("emailNotifier cannot be null!");
        }
    }

    private void saveLastRunTimestamp()
    {
        this.platformConfigurationService.saveConfigurationValue(this.getJobName() + LAST_RUN_TIMESTAMP
            , String.valueOf(System.currentTimeMillis()));
    }

    private Long getLastRunTimestamp() {
        String lastRun = this.platformConfigurationService.getConfigurationValue(this.getJobName() + LAST_RUN_TIMESTAMP);

        if(lastRun == null || lastRun.isEmpty()) {
            return 0L;
        }

        return Long.valueOf(lastRun);
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        final Context ctx = new Context();

        try {
            long lastRunTimestamp = this.getLastRunTimestamp();


            Optional<List<IkasanSolrDocument>> failedScheduledJobs = this.schedulerNotificationService
                .getFailedScheduledJobs(this.schedulerNotification.getSchedulerAgentName(), lastRunTimestamp,
                    this.schedulerNotification.getResultSize());

            if(failedScheduledJobs.isPresent()) {
                ctx.setVariable("failedScheduledJobs", failedScheduledJobs.get());

                final String content = this.templateEngine.process(schedulerNotification.getEmailBodyTemplate(), ctx);
                final String subject = this.templateEngine.process(schedulerNotification.getEmailSubjectTemplate(), ctx);

                EmailNotification emailNotification = new EmailNotification(this.schedulerNotification.getRecipientList(),
                    subject, content, this.schedulerNotification.isHtml());

                this.emailNotifier.sendNotification(emailNotification);

            }
            else {
                logger.debug(String.format("No failed scheduled jobs to report on [%s].", this.schedulerNotification.getJobName()));
            }
        }
        catch (Exception e) {
            throw new JobExecutionException(e);
        }

        this.saveLastRunTimestamp();
    }

    @Override
    public String getJobName() {
        return this.schedulerNotification.getJobName();
    }

    @Override
    public String getCronExpression() {
        return this.schedulerNotification.getCronExpression();
    }
}
