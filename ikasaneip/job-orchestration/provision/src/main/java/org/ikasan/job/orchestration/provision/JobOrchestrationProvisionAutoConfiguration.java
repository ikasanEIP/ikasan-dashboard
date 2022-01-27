package org.ikasan.job.orchestration.provision;

import org.ikasan.job.orchestration.provision.job.JobProvisionServiceImpl;
import org.ikasan.job.orchestration.rest.JobProvisionModuleRestServiceImpl;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class JobOrchestrationProvisionAutoConfiguration {

    @Bean
    public JobProvisionServiceImpl jobProvisionService(SchedulerJobService schedulerJobService, ModuleMetaDataService moduleMetadataService,
                                                       JobProvisionModuleRestServiceImpl jobProvisionModuleRestService) {
        return new JobProvisionServiceImpl(schedulerJobService, moduleMetadataService,
            jobProvisionModuleRestService);
    }


}