package org.ikasan.job.orchestration.provision;

import org.ikasan.job.orchestration.provision.job.JobProvisionServiceImpl;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.client.ConfigurationService;
import org.ikasan.spec.module.client.MetaDataService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class JobOrchestrationProvisionAutoConfiguration {

    @Bean
    public JobProvisionServiceImpl jobProvisionService(SchedulerJobService schedulerJobService, ConfigurationService configurationRestService, ModuleControlService moduleControlRestService,
                                                       ModuleMetaDataService moduleMetadataService, MetaDataService metaDataRestService) {
        return new JobProvisionServiceImpl(schedulerJobService, configurationRestService, moduleControlRestService, moduleMetadataService,
            metaDataRestService);
    }


}