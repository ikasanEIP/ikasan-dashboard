package org.ikasan.orchestration.service.context;

import org.ikasan.component.endpoint.bigqueue.service.BigQueueDirectoryManagementServiceImpl;
import org.ikasan.orchestration.service.context.recovery.ContextInstanceRecoveryServiceImpl;
import org.ikasan.orchestration.service.context.register.ContextInstanceRegistrationServiceImpl;
import org.ikasan.orchestration.service.context.reset.ContextResetServiceImpl;
import org.ikasan.orchestration.service.context.status.ContextStatusServiceImpl;
import org.ikasan.spec.bigqueue.service.BigQueueDirectoryManagementService;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.instance.service.ContextInstancePublicationService;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.job.service.JobInitiationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheInitialisationService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContextServiceAutoConfiguration {

    @Value("${scheduled.job.context.queue.directory}")
    private String queueDirectory;

    @Value("${ikasan.dashboard.unzip.and.provision.jobs:true}")
    private boolean uploadProvisionJobs;

    @Bean
    public BigQueueDirectoryManagementService bigQueueDirectoryManagementService() {
        return new BigQueueDirectoryManagementServiceImpl(queueDirectory);
    }

    @Bean
    public JobLockCacheInitialisationService jobLockCacheInitialisationService(JobLockCacheService jobLockCacheService) {
        return new JobLockCacheInitialisationServiceImpl(jobLockCacheService);
    }

    @Bean
    public ContextInstanceRecoveryServiceImpl contextInstanceRecoveryService(
        ScheduledContextInstanceService scheduledContextInstanceService,
        JobInitiationService jobInitiationService,
        ModuleMetaDataService moduleMetadataService,
        InternalEventDrivenJobService internalEventDrivenJobService,
        ContextParametersInstanceService contextParametersInstanceService,
        ContextInstancePublicationService contextInstancePublicationService,
        JobLockCacheService jobLockCacheService,
        ScheduledContextService scheduledContextService,
        SchedulerJobInstanceService schedulerJobInstanceService,
        ContextInstanceStateChangeEventBroadcaster contextInstanceStateChangeEventBroadcaster,
        SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster,
        JobLockCacheInitialisationService jobLockCacheInitialisationService) {

        return new ContextInstanceRecoveryServiceImpl(queueDirectory,
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
            jobLockCacheInitialisationService
        );
    }

    @Bean
    public ContextInstanceRegistrationServiceImpl contextInstanceRegistrationService(
        ScheduledContextInstanceService scheduledContextInstanceService,
        JobInitiationService jobInitiationService,
        ModuleMetaDataService moduleMetadataService,
        InternalEventDrivenJobService internalEventDrivenJobService,
        ContextParametersInstanceService contextParametersInstanceService,
        ContextInstancePublicationService contextInstancePublicationService,
        JobLockCacheService jobLockCacheService,
        ScheduledContextService scheduledContextService,
        SchedulerJobInstanceService schedulerJobInstanceService,
        ContextInstanceStateChangeEventBroadcaster contextInstanceStateChangeEventBroadcaster,
        SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster,
        JobLockCacheInitialisationService jobLockCacheInitialisationService) {

        return new ContextInstanceRegistrationServiceImpl(queueDirectory,
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
            jobLockCacheInitialisationService
        );
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
