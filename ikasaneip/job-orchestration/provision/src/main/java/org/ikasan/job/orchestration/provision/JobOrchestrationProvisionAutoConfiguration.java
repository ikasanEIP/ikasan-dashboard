package org.ikasan.job.orchestration.provision;

import org.ikasan.job.orchestration.provision.context.ContextProvisionServiceImpl;
import org.ikasan.job.orchestration.provision.job.JobProvisionServiceImpl;
import org.ikasan.scheduler.CachingScheduledJobFactory;
import org.ikasan.scheduler.SchedulerFactory;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.job.service.JobProvisionModuleService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
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
        ContextInstanceRegistrationService contextInstanceRegistrationService) {

        return new ContextProvisionServiceImpl(
            SchedulerFactory.getInstance().getScheduler(),
            CachingScheduledJobFactory.getInstance(),
            scheduledContextService,
            moduleMetadataService,
            schedulerJobService,
            jobProvisionModuleRestService,
            contextInstanceRegistrationService,
            uploadProvisionJobs
        );
    }
}