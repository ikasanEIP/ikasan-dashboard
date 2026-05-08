package org.ikasan.orchestration.service;

import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.job.orchestration.context.util.TimeService;
import org.ikasan.orchestration.service.context.JobLockCacheInitialisationServiceImpl;
import org.ikasan.orchestration.service.context.lifecycle.ContextInstanceEndServiceImpl;
import org.ikasan.orchestration.service.context.recovery.ContextInstanceRecoveryServiceImpl;
import org.ikasan.orchestration.service.context.register.ContextInstanceRegistrationServiceImpl;
import org.ikasan.orchestration.service.context.reset.ContextResetServiceImpl;
import org.ikasan.orchestration.service.context.status.ContextStatusServiceImpl;
import org.ikasan.orchestration.service.scheduled.context.ScheduledContextServiceImpl;
import org.ikasan.orchestration.service.scheduled.instance.ScheduledContextInstanceServiceImpl;
import org.ikasan.orchestration.service.scheduled.instance.SchedulerJobInstanceServiceImpl;
import org.ikasan.orchestration.service.scheduled.job.InternalEventDrivenJobRecordServiceImpl;
import org.ikasan.orchestration.service.scheduled.job.SchedulerJobServiceImpl;
import org.ikasan.orchestration.service.scheduled.joblock.JobLockCacheServiceImpl;
import org.ikasan.orchestration.service.scheduled.notification.EmailNotificationContextServiceImpl;
import org.ikasan.orchestration.service.scheduled.notification.EmailNotificationDetailsServiceImpl;
import org.ikasan.orchestration.service.scheduled.notification.NotificationSendAuditServiceImpl;
import org.ikasan.orchestration.service.scheduled.profile.ContextProfileServiceImpl;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextViewDao;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRecoveryService;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ContextInstanceSchedulerService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditAggregateDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceDao;
import org.ikasan.spec.scheduled.instance.dao.SchedulerJobInstanceDao;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.dao.*;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheAuditDao;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheDao;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationContextDao;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationDetailsDao;
import org.ikasan.spec.scheduled.notification.dao.NotificationSendAuditDao;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.notification.service.NotificationSendAuditService;
import org.ikasan.spec.scheduled.profile.dao.ContextProfileDao;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.ikasan.spec.systemevent.SystemEventService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.Map;

@Configuration
public class JobOrchestrationServiceAutoConfiguration {

    @Value("${scheduled.job.context.queue.directory:.}")
    private String queueDirectory;

    @Value("${is.ikasan.enterprise.scheduler.instance:true}")
    private boolean isIkasanEnterpriseSchedulerInstance;

    @Value("${context.machine.executor.wait.timeout.seconds:30}")
    private int contextMachineExecutorWaitTimeoutSeconds;

    @Value("${context.machine.blacklisted.message.max.retries:5}")
    private int contextMachineBlackListedMessageMaxRetries = 5;

    @Value("${solr.scheduler.instance.retention.days:90}")
    private int solrSchedulerInstanceRetentionDays;

    @Value("${solr.save.context.instance.audits:true}")
    private boolean saveContextInstanceAuditRecords;

    @Value("${solr.save.context.instance.audit.deltas:true}")
    private boolean saveContextInstanceAuditDeltaRecords;

    @Value("${solr.save.joblockcache.audits:true}")
    private boolean saveJobLockCacheAudits;

    @Value("#{${scheduler.job.execution.environment.label}}")
    private Map<String, String> schedulerJobExecutionEnvironmentLabel;

    @Value("${ikasan.enterprise.scheduler.use.legacy.job.status.count:false}")
    private boolean useLegacyJobStatusCount = false;

    @Bean
    public JobLockCacheInitialisationService jobLockCacheInitialisationService(JobLockCacheService jobLockCacheService) {
        return new JobLockCacheInitialisationServiceImpl(jobLockCacheService);
    }

    @Bean
    TimeService timeService() {
        return new TimeService();
    }

    @Bean
    @DependsOn({"stateChangeMonitor","overdueFileMonitor","monitorManagement"})
    public ContextInstanceRecoveryService contextInstanceRecoveryService(
        ScheduledContextInstanceService scheduledContextInstanceService,
        JobInitiationService jobInitiationService,
        @Qualifier("moduleMetadataService") ModuleMetaDataService moduleMetadataService,
        InternalEventDrivenJobService internalEventDrivenJobService,
        ContextParametersInstanceService contextParametersInstanceService,
        ContextInstancePublicationService contextInstancePublicationService,
        JobLockCacheService jobLockCacheService,
        ScheduledContextService scheduledContextService,
        SchedulerJobInstanceService schedulerJobInstanceService,
        JobLockCacheInitialisationService jobLockCacheInitialisationService,
        ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService,
        TimeService timeService,
        @Qualifier("contextInstanceRegistrationService") ContextInstanceRegistrationService contextInstanceRegistrationService,
        JobUtilsService jobUtilsService,
        JobProvisionService jobProvisionService,
        SchedulerJobService schedulerJobService) {

        ContextInstanceRecoveryServiceImpl contextInstanceRecoveryService =  new ContextInstanceRecoveryServiceImpl(queueDirectory,
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            schedulerJobInstanceService,
            jobLockCacheInitialisationService,
            contextInstanceSchedulerService,
            timeService,
            contextInstanceRegistrationService,
            jobUtilsService,
            jobProvisionService,
            schedulerJobService,
            this.isIkasanEnterpriseSchedulerInstance
        );
        contextInstanceRecoveryService.setContextMachineExecutorWaitTimeoutSeconds(this.contextMachineExecutorWaitTimeoutSeconds);
        contextInstanceRecoveryService.setBlackListedMessageMaxRetries(this.contextMachineBlackListedMessageMaxRetries);
        return contextInstanceRecoveryService;
    }

    @Bean("contextInstanceRegistrationService")
    public ContextInstanceRegistrationService contextInstanceRegistrationService(
        ScheduledContextInstanceService scheduledContextInstanceService,
        JobInitiationService jobInitiationService,
        @Qualifier("moduleMetadataService") ModuleMetaDataService moduleMetadataService,
        InternalEventDrivenJobService internalEventDrivenJobService,
        ContextParametersInstanceService contextParametersInstanceService,
        ContextInstancePublicationService contextInstancePublicationService,
        JobLockCacheService jobLockCacheService,
        ScheduledContextService scheduledContextService,
        SchedulerJobInstanceService schedulerJobInstanceService,
        JobLockCacheInitialisationService jobLockCacheInitialisationService,
        TimeService timeService,
        SystemEventService systemEventService,
        JobUtilsService jobUtilsService,
        JobProvisionService jobProvisionService,
        SchedulerJobService schedulerJobService) {

        ContextInstanceRegistrationServiceImpl contextInstanceRegistrationService =  new ContextInstanceRegistrationServiceImpl(queueDirectory,
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            schedulerJobInstanceService,
            jobLockCacheInitialisationService,
            timeService,
            systemEventService,
            jobUtilsService,
            jobProvisionService,
            schedulerJobService,
            this.isIkasanEnterpriseSchedulerInstance
        );
        contextInstanceRegistrationService.setContextMachineExecutorWaitTimeoutSeconds(this.contextMachineExecutorWaitTimeoutSeconds);
        contextInstanceRegistrationService.setBlackListedMessageMaxRetries(this.contextMachineBlackListedMessageMaxRetries);

        return contextInstanceRegistrationService;
    }

    @Bean("contextInstanceEndService")
    public ContextInstanceEndServiceImpl contextInstanceEndService(
        ScheduledContextInstanceService scheduledContextInstanceService,
        JobInitiationService jobInitiationService,
        @Qualifier("moduleMetadataService") ModuleMetaDataService moduleMetadataService,
        InternalEventDrivenJobService internalEventDrivenJobService,
        ContextParametersInstanceService contextParametersInstanceService,
        ContextInstancePublicationService contextInstancePublicationService,
        JobLockCacheService jobLockCacheService,
        ScheduledContextService scheduledContextService,
        SchedulerJobInstanceService schedulerJobInstanceService,
        JobLockCacheInitialisationService jobLockCacheInitialisationService,
        TimeService timeService,
        SystemEventService systemEventService,
        JobUtilsService jobUtilsService,
        JobProvisionService jobProvisionService,
        SchedulerJobService schedulerJobService,
        ContextInstanceSchedulerService contextInstanceSchedulerService) {

        ContextInstanceEndServiceImpl contextInstanceEndService =  new ContextInstanceEndServiceImpl(queueDirectory,
            scheduledContextInstanceService,
            jobInitiationService,
            moduleMetadataService,
            internalEventDrivenJobService,
            contextParametersInstanceService,
            contextInstancePublicationService,
            jobLockCacheService,
            scheduledContextService,
            schedulerJobInstanceService,
            jobLockCacheInitialisationService,
            timeService,
            systemEventService,
            jobUtilsService,
            jobProvisionService,
            schedulerJobService,
            contextInstanceSchedulerService,
            this.isIkasanEnterpriseSchedulerInstance
        );
        contextInstanceEndService.setContextMachineExecutorWaitTimeoutSeconds(this.contextMachineExecutorWaitTimeoutSeconds);
        contextInstanceEndService.setBlackListedMessageMaxRetries(this.contextMachineBlackListedMessageMaxRetries);

        return contextInstanceEndService;
    }

    @Bean
    public ContextStatusServiceImpl contextStatusService() {
        return new ContextStatusServiceImpl();
    }

    @Bean
    public ContextResetServiceImpl contextResetService() {
        return new ContextResetServiceImpl();
    }

    // Pulling in job orchestration service beans below here
    @Bean
    public JobLockCacheService jobLockCacheService(JobLockCacheDao jobLockCacheDao,
                                                   JobLockCacheAuditDao jobLockCacheAuditDao) {
        return new JobLockCacheServiceImpl(jobLockCacheDao, jobLockCacheAuditDao, this.saveJobLockCacheAudits);
    }

    @Bean
    public ScheduledContextService scheduledContextService(ScheduledContextDao scheduledContextDao,
                                                           ScheduledContextViewDao scheduledContextViewDao) {
        return new ScheduledContextServiceImpl(scheduledContextDao, scheduledContextViewDao);
    }

    @Bean
    public ScheduledContextInstanceService scheduledContextInstanceService(ScheduledContextInstanceAuditAggregateDao scheduledContextInstanceAuditAggregateDao,
                                                                           ScheduledContextInstanceDao scheduledContextInstanceDao,
                                                                           ScheduledContextInstanceAuditDao scheduledContextInstanceAuditDao) {
        return new ScheduledContextInstanceServiceImpl(scheduledContextInstanceDao, scheduledContextInstanceAuditDao, scheduledContextInstanceAuditAggregateDao
            , this.saveContextInstanceAuditRecords, this.saveContextInstanceAuditDeltaRecords);
    }

    @Bean
    public EmailNotificationContextService emailNotificationContextService(EmailNotificationContextDao emailNotificationContextDao) {
        return new EmailNotificationContextServiceImpl(emailNotificationContextDao);
    }

    @Bean
    public EmailNotificationDetailsService emailNotificationDetailsService(EmailNotificationDetailsDao emailNotificationDetailsDao) {
        return new EmailNotificationDetailsServiceImpl(emailNotificationDetailsDao);
    }

    @Bean
    public NotificationSendAuditService notificationSendAuditService(NotificationSendAuditDao notificationSendAuditDao) {
        return new NotificationSendAuditServiceImpl(notificationSendAuditDao);
    }

    @Bean
    public SchedulerJobService solrSchedulerJobService(FileEventDrivenJobDao fileEventDrivenJobDao
        , @Qualifier("internalEventDrivenJobRecordDao") InternalEventDrivenJobDao internalEventDrivenJobDao
        , QuartzScheduleDrivenJobDao quartzScheduleDrivenJobDao
        , GlobalEventJobDao globalEventJobRecordDao, ContextStartJobDao contextStartJobDao
        , ContextTerminalJobDao contextTerminalJobDao, SchedulerJobDao schedulerJobDao
        , @Qualifier("internalEventDrivenJobTemplateDao") InternalEventDrivenJobDao internalEventDrivenJobTemplateDao) {
        return new SchedulerJobServiceImpl(fileEventDrivenJobDao
            , internalEventDrivenJobDao, quartzScheduleDrivenJobDao
            , globalEventJobRecordDao, contextStartJobDao, contextTerminalJobDao, schedulerJobDao
            , internalEventDrivenJobTemplateDao);
    }

    @Bean
    public SchedulerJobInstanceService schedulerJobInstanceService(SchedulerJobDao schedulerJobDao,
                                                                   ScheduledContextInstanceAuditAggregateDao scheduledContextInstanceAuditAggregateDao,
                                                                   ScheduledContextInstanceService scheduledContextInstanceService,
                                                                   SchedulerJobInstanceDao scheduledContextInstanceDao) {
        return new SchedulerJobInstanceServiceImpl(scheduledContextInstanceDao
            , scheduledContextInstanceAuditAggregateDao
            , schedulerJobDao, scheduledContextInstanceService, this.schedulerJobExecutionEnvironmentLabel
            , this.useLegacyJobStatusCount);
    }

    @Bean
    public InternalEventDrivenJobService internalEventDrivenJobService(@Qualifier("internalEventDrivenJobRecordDao") InternalEventDrivenJobDao internalEventDrivenJobDao) {
        return new InternalEventDrivenJobRecordServiceImpl(internalEventDrivenJobDao);
    }

    @Bean
    public ContextProfileService contextProfileService(ContextProfileDao contextProfileDao) {
        return new ContextProfileServiceImpl(contextProfileDao);
    }
}
