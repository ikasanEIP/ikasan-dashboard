package org.ikasan.job.orchestration;

import org.ikasan.job.orchestration.context.parameters.ContextParametersFactory;
import org.ikasan.job.orchestration.context.parameters.ContextParametersInstanceServiceImpl;
import org.ikasan.job.orchestration.context.recovery.ContextInstanceRecoveryManager;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerService;
import org.ikasan.job.orchestration.context.util.SchedulerContextParametersPropertiesProvider;
import org.ikasan.job.orchestration.integration.StartupApplicationListener;
import org.ikasan.job.orchestration.integration.StartupCompleteApplicationListener;
import org.ikasan.job.orchestration.integration.module.InboundModuleFactory;
import org.ikasan.module.service.ModuleActivatorDefaultImpl;
import org.ikasan.module.startup.dao.StartupControlDao;
import org.ikasan.scheduler.CachingScheduledJobFactory;
import org.ikasan.scheduler.SchedulerFactory;
import org.ikasan.spec.configuration.ConfigurationService;
import org.ikasan.spec.dashboard.DashboardRestService;
import org.ikasan.spec.flow.Flow;
import org.ikasan.spec.module.Module;
import org.ikasan.spec.module.ModuleActivator;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRecoveryService;
import org.ikasan.spec.scheduled.context.service.ContextInstanceRegistrationService;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.service.ContextParametersInstanceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.annotation.Resource;
import java.util.Map;

/**
 * House keeping related configuration required by dashboard.
 * This autoconfig should be excluded from dashboard.
 */
@Configuration
@Import({InboundModuleFactory.class})
public class DashboardJobOrchestrationAutoConfiguration {

    @Resource
    ConfigurationService configurationService;

    @Resource
    Module<Flow> inboundFlowModule;

    @Resource
    JtaTransactionManager transactionManager;

    @Value("${use.skip.jobs.flag:false}")
    private boolean useSkipJobs;

    @Value("#{${jobs.to.skip:{T(java.util.Collections).emptyMap()}}}")
    private Map<String, Map<String, Boolean>> jobsToSkip;

    @Value("${use.replace.context.params.flag:false}")
    private boolean replaceContextParams;

    @Value("#{${job.context.params.to.replace:{T(java.util.Collections).emptyMap()}}}")
    private Map<String, Map<String, String>> paramsToReplace;

    @Value("${context.lifecycle.active:true}")
    private boolean isContextLifeCycleActive;

    /**
     * This map with a String key that is an identifier for the spel expression.
     * The value String is the spel expression.
     *
     * E.g. in properties file
     *
     * job.context.params.to.spel.calculators={ \
     *   'BusinessDateCalculator':'T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).BASIC_ISO_DATE)' \
     *   }
     *
     *  Initialises to empty map if not set.
     */
    @Value("#{${job.context.params.to.spel.calculators:{T(java.util.Collections).emptyMap()}}}")
    private Map<String, String> spelContextParamsCalculators;

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
    public SchedulerContextParametersPropertiesProvider schedulerOverrider() {
        return new SchedulerContextParametersPropertiesProvider(useSkipJobs, jobsToSkip, replaceContextParams, paramsToReplace, spelContextParamsCalculators);
    }

    @Bean
    @DependsOn({"stateChangeMonitor","overdueFileMonitor","monitorManagement"})
    public ContextInstanceRecoveryManager contextInstanceRecoveryManager(ContextInstanceRecoveryService contextInstanceRecoveryService) {
        return new ContextInstanceRecoveryManager(contextInstanceRecoveryService, isContextLifeCycleActive);
    }

    @Bean
    @DependsOn("contextInstanceRecoveryManager")
    public ContextInstanceSchedulerService contextInstanceSchedulerService(ContextInstanceRegistrationService contextInstanceRegistrationService,
                                                                           ScheduledContextService scheduledContextService) {
        return new ContextInstanceSchedulerService(SchedulerFactory.getInstance().getScheduler()
            , CachingScheduledJobFactory.getInstance(), scheduledContextService, contextInstanceRegistrationService, isContextLifeCycleActive);
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

    @Bean
    public StartupCompleteApplicationListener startupCompleteApplicationListener(Module<Flow> inboundFlowModule) {
        return new StartupCompleteApplicationListener(transactionManager, inboundFlowModule);
    }
}