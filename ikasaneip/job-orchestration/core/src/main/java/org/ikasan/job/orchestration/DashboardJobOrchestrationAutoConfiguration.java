package org.ikasan.job.orchestration;

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
import org.ikasan.spec.module.Module;
import org.ikasan.spec.module.ModuleActivator;
import org.ikasan.spec.scheduled.SchedulerService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Bean
    public ContextInstanceRecoveryManager contextInstanceRecoveryManager(ScheduledContextInstanceService scheduledContextInstanceService
        , ScheduledContextService scheduledContextService, InternalEventDrivenJobService internalEventDrivenJobRecordService) {
        return new ContextInstanceRecoveryManager(scheduledContextInstanceService, scheduledContextService, internalEventDrivenJobRecordService,
            queueDirectory);
    }

    @Bean
    public ContextInstanceSchedulerService contextInstanceSchedulerService(ScheduledContextService scheduledContextService
        , ScheduledContextInstanceService scheduledContextInstanceService, SchedulerService schedulerService) {
        return new ContextInstanceSchedulerService(SchedulerFactory.getInstance().getScheduler()
            , CachingScheduledJobFactory.getInstance(), scheduledContextService, scheduledContextInstanceService, schedulerService
            , this.internalEventDrivenJobService, this.queueDirectory);
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