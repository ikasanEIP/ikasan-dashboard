package org.ikasan.orchestration.service.context;

import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.job.orchestration.context.util.TimeService;
import org.ikasan.orchestration.service.context.lifecycle.ContextInstanceEndServiceImpl;
import org.ikasan.orchestration.service.context.recovery.ContextInstanceRecoveryServiceImpl;
import org.ikasan.orchestration.service.context.register.ContextInstanceRegistrationServiceImpl;
import org.ikasan.orchestration.service.context.reset.ContextResetServiceImpl;
import org.ikasan.orchestration.service.context.status.ContextStatusServiceImpl;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRecoveryService;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ContextInstanceSchedulerService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.job.service.JobUtilsService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.scheduled.provision.JobProvisionService;
import org.ikasan.spec.systemevent.SystemEventService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class ContextServiceAutoConfiguration {

    @Value("${scheduled.job.context.queue.directory:.}")
    private String queueDirectory;

    @Value("${is.ikasan.enterprise.scheduler.instance:true}")
    private boolean isIkasanEnterpriseSchedulerInstance;

    @Value("${context.machine.executor.wait.timeout.seconds:30}")
    private int contextMachineExecutorWaitTimeoutSeconds;

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
        ContextInstanceStateChangeEventBroadcaster contextInstanceStateChangeEventBroadcaster,
        SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster,
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
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
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
        ContextInstanceStateChangeEventBroadcaster contextInstanceStateChangeEventBroadcaster,
        SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster,
        JobLockCacheInitialisationService jobLockCacheInitialisationService,
        TimeService timeService,
        ContextInstanceSavedEventBroadcaster contextInstanceSavedEventBroadcaster,
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
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            jobLockCacheInitialisationService,
            timeService,
            contextInstanceSavedEventBroadcaster,
            systemEventService,
            jobUtilsService,
            jobProvisionService,
            schedulerJobService,
            this.isIkasanEnterpriseSchedulerInstance
        );
        contextInstanceRegistrationService.setContextMachineExecutorWaitTimeoutSeconds(this.contextMachineExecutorWaitTimeoutSeconds);

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
        ContextInstanceStateChangeEventBroadcaster contextInstanceStateChangeEventBroadcaster,
        SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster,
        JobLockCacheInitialisationService jobLockCacheInitialisationService,
        TimeService timeService,
        ContextInstanceSavedEventBroadcaster contextInstanceSavedEventBroadcaster,
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
            contextInstanceStateChangeEventBroadcaster,
            schedulerJobStateChangeEventBroadcaster,
            jobLockCacheInitialisationService,
            timeService,
            contextInstanceSavedEventBroadcaster,
            systemEventService,
            jobUtilsService,
            jobProvisionService,
            schedulerJobService,
            contextInstanceSchedulerService,
            this.isIkasanEnterpriseSchedulerInstance
        );
        contextInstanceEndService.setContextMachineExecutorWaitTimeoutSeconds(this.contextMachineExecutorWaitTimeoutSeconds);

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
}
