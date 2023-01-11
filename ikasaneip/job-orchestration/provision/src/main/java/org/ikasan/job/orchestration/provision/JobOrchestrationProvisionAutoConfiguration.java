package org.ikasan.job.orchestration.provision;

import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerService;
import org.ikasan.job.orchestration.provision.context.ContextProvisionServiceImpl;
import org.ikasan.job.orchestration.provision.job.JobProvisionServiceImpl;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.service.JobProvisionModuleService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobOrchestrationProvisionAutoConfiguration {

    @Value("${ikasan.dashboard.unzip.and.provision.jobs:true}")
    private boolean uploadProvisionJobs;

    @Bean
    public JobProvisionServiceImpl jobProvisionService(SchedulerJobService schedulerJobService, ModuleMetaDataService moduleMetadataService,
                                                       JobProvisionModuleService jobProvisionModuleService) {
        return new JobProvisionServiceImpl(schedulerJobService, moduleMetadataService,
            jobProvisionModuleService);
    }

    @Bean
    public ContextProvisionServiceImpl contextUploadInitialisationService(
        ScheduledContextService scheduledContextService,
        ModuleMetaDataService moduleMetadataService,
        SchedulerJobService schedulerJobService,
        JobProvisionModuleService jobProvisionModuleRestService,
        ContextInstanceRegistrationService contextInstanceRegistrationService,
        ContextProfileService contextProfileService,
        EmailNotificationDetailsService emailNotificationDetailsService,
        EmailNotificationContextService emailNotificationContextService,
        ContextInstanceSchedulerService contextInstanceSchedulerService) {

        return new ContextProvisionServiceImpl(
            scheduledContextService,
            moduleMetadataService,
            schedulerJobService,
            jobProvisionModuleRestService,
            contextInstanceRegistrationService,
            contextProfileService,
            emailNotificationDetailsService,
            emailNotificationContextService,
            uploadProvisionJobs,
            contextInstanceSchedulerService
        );
    }
}