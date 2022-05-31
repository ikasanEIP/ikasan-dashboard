package org.ikasan.job.orchestration;

import java.util.Map;

import org.ikasan.job.orchestration.context.parameters.ContextParametersFactory;
import org.ikasan.job.orchestration.context.parameters.ContextParametersInstanceServiceImpl;
import org.ikasan.job.orchestration.context.util.SchedulerOverrider;
import org.ikasan.module.service.ModuleActivatorDefaultImpl;
import org.ikasan.module.startup.dao.StartupControlDao;
import org.ikasan.job.orchestration.context.recovery.ContextInstanceRecoveryManager;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerService;
import org.ikasan.job.orchestration.integration.StartupApplicationListener;
import org.ikasan.job.orchestration.integration.module.InboundModuleFactory;
import org.ikasan.scheduler.CachingScheduledJobFactory;
import org.ikasan.scheduler.SchedulerFactory;
import org.ikasan.spec.configuration.ConfigurationService;
import org.ikasan.spec.dashboard.DashboardRestService;
import org.ikasan.spec.flow.Flow;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.Module;
import org.ikasan.spec.module.ModuleActivator;
import org.ikasan.spec.module.client.ContextParametersUpdateService;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * House keeping related configuration required by dashboard.
 * This autoconfig should be excluded from dashboard.
 */
@Configuration
@Import({InboundModuleFactory.class})
public class DashboardJobOrchestrationAutoConfiguration {

    @Value("${scheduled.job.context.queue.directory}")
    private String queueDirectory;

    @Resource
    ConfigurationService configurationService;

    @Resource
    Module<Flow> inboundFlowModule;

    @Resource
    InternalEventDrivenJobService internalEventDrivenJobService;

    @Value("${use.skip.jobs.flag:false}")
    private boolean useSkipJobs;

    @Value("#{${jobs.to.skip:{T(java.util.Collections).emptyMap()}}}")
    private Map<String, Map<String, Boolean>> jobsToSkip;

    @Value("${use.replace.context.params.flag:false}")
    private boolean replaceContextParams;

    @Value("#{${job.context.params.to.replace:{T(java.util.Collections).emptyMap()}}}")
    private Map<String, Map<String, String>> paramsToReplace;

    // TODO remove this feature flag when we are satisfied all recovery working
    @Value("${use.post.construct.ikasan.2097:false}")
    private boolean usePostConstructs;

    @Bean
    @DependsOn("contextParametersFactory")
    public ContextParametersInstanceService contextParametersInstanceService() {
        return new ContextParametersInstanceServiceImpl(contextParametersFactory());
    }

    @Bean
    @DependsOn("schedulerOverrider")
    public ContextParametersFactory contextParametersFactory() {
        return new ContextParametersFactory(schedulerOverrider());
    }

    @Bean
    public SchedulerOverrider schedulerOverrider() {
        return new SchedulerOverrider(useSkipJobs, jobsToSkip, replaceContextParams, paramsToReplace);
    }

    @Bean
    public ContextInstanceRecoveryManager contextInstanceRecoveryManager(ScheduledContextInstanceService scheduledContextInstanceService
        , ScheduledContextService scheduledContextService, InternalEventDrivenJobService internalEventDrivenJobRecordService
        , JobLockCacheService jobLockCacheService, ContextParametersInstanceService contextParametersInstanceService, SchedulerService schedulerService
        , ModuleMetaDataService moduleMetadataService, ContextParametersUpdateService contextParametersUpdateService) {
        return new ContextInstanceRecoveryManager(scheduledContextInstanceService, scheduledContextService, internalEventDrivenJobRecordService,
            queueDirectory, jobLockCacheService, contextParametersInstanceService, schedulerService,
            moduleMetadataService, contextParametersUpdateService, usePostConstructs);
    }

    @Bean
    @DependsOn("contextInstanceRecoveryManager")
    public ContextInstanceSchedulerService contextInstanceSchedulerService(ScheduledContextService scheduledContextService
        , ScheduledContextInstanceService scheduledContextInstanceService, SchedulerService schedulerService
        , JobLockCacheService jobLockCacheService, ContextParametersInstanceService contextParametersInstanceService
        , ModuleMetaDataService moduleMetadataService, ContextParametersUpdateService contextParametersUpdateService) {
        return new ContextInstanceSchedulerService(SchedulerFactory.getInstance().getScheduler()
            , CachingScheduledJobFactory.getInstance(), scheduledContextService, scheduledContextInstanceService, schedulerService
            , this.internalEventDrivenJobService, this.queueDirectory, jobLockCacheService, contextParametersInstanceService
            , moduleMetadataService, contextParametersUpdateService, usePostConstructs);
    }

    @Bean
    public ModuleActivator moduleActivator(StartupControlDao startupControlDao) {
        return new ModuleActivatorDefaultImpl(configurationService, startupControlDao);
    }

    @Bean
    public StartupApplicationListener startupApplicationListener(DashboardRestService moduleMetadataDashboardRestService,
                                                                 DashboardRestService configurationMetadataDashboardRestService,
                                                                 Module<Flow> inboundFlowModule) {
        return new StartupApplicationListener(moduleMetadataDashboardRestService,
            configurationMetadataDashboardRestService, inboundFlowModule);
    }


    @PostConstruct
    public void startInboundFlow() {
        Flow inboundFlow = inboundFlowModule.getFlow("Scheduled Process Event Inbound Flow");
        inboundFlow.start();
//        Flow outboundFlow = inboundFlowModule.getFlow("Job Initiation Event Outbound Flow");
//        outboundFlow.start();
    }
}