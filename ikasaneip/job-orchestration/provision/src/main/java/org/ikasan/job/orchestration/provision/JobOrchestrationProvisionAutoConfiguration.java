package org.ikasan.job.orchestration.provision;

import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.job.orchestration.provision.context.ContextProvisionServiceImpl;
import org.ikasan.job.orchestration.provision.job.JobProvisionServiceImpl;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.service.JobProvisionModuleService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.security.service.SecurityService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobOrchestrationProvisionAutoConfiguration {

    @Value("${scheduler.provision.jobs.on.upload:true}")
    private boolean uploadProvisionJobs;

    @Value("${job.plan.interval.multiple:3}")
    private int jobPlanIntervalMultiple;

    @Bean
    public JobProvisionServiceImpl jobProvisionService(SchedulerJobService schedulerJobService, @Qualifier("moduleMetadataService") ModuleMetaDataService moduleMetadataService,
                                                       JobProvisionModuleService jobProvisionModuleService) {
        return new JobProvisionServiceImpl(schedulerJobService, moduleMetadataService,
            jobProvisionModuleService);
    }

    @Bean
    public ContextProvisionServiceImpl contextUploadInitialisationService(
        ScheduledContextService scheduledContextService,
        @Qualifier("moduleMetadataService") ModuleMetaDataService moduleMetadataService,
        SchedulerJobService schedulerJobService,
        JobProvisionModuleService jobProvisionModuleRestService,
        @Qualifier("contextInstanceRegistrationService") ContextInstanceRegistrationService contextInstanceRegistrationService,
        ContextProfileService contextProfileService,
        EmailNotificationDetailsService emailNotificationDetailsService,
        EmailNotificationContextService emailNotificationContextService,
        ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService,
        ScheduledContextInstanceService scheduledContextInstanceService,
        SecurityService securityService) {

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
            contextInstanceSchedulerService,
            scheduledContextInstanceService,
            jobPlanIntervalMultiple,
            securityService
        );
    }
}