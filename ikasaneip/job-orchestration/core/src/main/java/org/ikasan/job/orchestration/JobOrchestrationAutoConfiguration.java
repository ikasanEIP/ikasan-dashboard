package org.ikasan.job.orchestration;

import jakarta.annotation.PostConstruct;
import org.ikasan.job.orchestration.configuration.JobContextParamsSetupConfiguration;
import org.ikasan.job.orchestration.configuration.JobContextParamsSetupFactory;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.context.parameters.ContextParametersFactory;
import org.ikasan.job.orchestration.context.parameters.ContextParametersInstanceServiceImpl;
import org.ikasan.job.orchestration.context.recovery.ContextInstanceRecoveryManager;
import org.ikasan.job.orchestration.context.register.ContextInstanceSchedulerServiceImpl;
import org.ikasan.job.orchestration.context.util.SchedulerContextParametersPropertiesProvider;
import org.ikasan.job.orchestration.context.util.TimeService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.*;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.jta.JtaTransactionManager;

import java.util.Map;

@Configuration
@Import({InboundModuleFactory.class, JobContextParamsSetupFactory.class})
public class JobOrchestrationAutoConfiguration implements ApplicationListener<ContextRefreshedEvent> {
    private Logger logger = LoggerFactory.getLogger(JobOrchestrationAutoConfiguration.class);

    public JobOrchestrationAutoConfiguration() {
        logger.info("Refreshing - JobOrchestrationAutoConfiguration");
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    JtaTransactionManager transactionManager;

    @Value("${context.lifecycle.active:true}")
    private boolean isContextLifeCycleActive;

    @Value("${is.ikasan.enterprise.scheduler.instance:true}")
    private boolean isIkasanEnterpriseSchedulerInstance;

    @Value("${scheduler.instance.registration.attempts:5}")
    private int registrationJobAttempts;

    @Value("${scheduler.instance.registration.retry.interval,milliseconds:1000}")
    private int registrationJobRetryInterval;

    @Autowired
    private JobContextParamsSetupConfiguration jobContextParamsSetupConfiguration;

    @Value("${job.lock.cache.executor.thread.pool.size:5}")
    private int executorThreadPoolSize;

    @PostConstruct
    public void configureJobLockCache() {
        JobLockCacheImpl.instance().setExecutorThreadPoolSize(executorThreadPoolSize);
    }

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
    @DependsOn("jobContextParamsSetupConfiguration")
    public SchedulerContextParametersPropertiesProvider schedulerOverrider() {
        return new SchedulerContextParametersPropertiesProvider(jobContextParamsSetupConfiguration, spelContextParamsCalculators);
    }

    @Bean
    @DependsOn({"stateChangeMonitor","overdueFileMonitor","monitorManagement"})
    public ContextInstanceRecoveryManager contextInstanceRecoveryManager(@Lazy ContextInstanceRecoveryService contextInstanceRecoveryService) {
        return new ContextInstanceRecoveryManager(contextInstanceRecoveryService, isContextLifeCycleActive, isIkasanEnterpriseSchedulerInstance);
    }

    @Bean
    @DependsOn("contextInstanceRecoveryManager")
    public ContextInstanceSchedulerServiceImpl contextInstanceSchedulerService(@Lazy @Qualifier("contextInstanceRegistrationService") ContextInstanceRegistrationService contextInstanceRegistrationService
        , ScheduledContextService scheduledContextService, TimeService timeService) {
        return new ContextInstanceSchedulerServiceImpl(SchedulerFactory.getInstance().getScheduler(),
            CachingScheduledJobFactory.getInstance(),
            scheduledContextService,
            contextInstanceRegistrationService,
            timeService,
            this.isContextLifeCycleActive,
            this.isIkasanEnterpriseSchedulerInstance,
            this.registrationJobAttempts,
            this.registrationJobRetryInterval);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ContextInstanceRecoveryManager contextInstanceRecoveryManager
            = (ContextInstanceRecoveryManager) event.getApplicationContext().getBean("contextInstanceRecoveryManager");

        contextInstanceRecoveryManager.recoverContextInstances();
    }

    @Bean
    public ModuleActivator moduleActivator(StartupControlDao startupControlDao) {
        return new ModuleActivatorDefaultImpl(configurationService, startupControlDao);
    }

    @Bean
    @ConditionalOnProperty(value="is.ikasan.enterprise.scheduler.instance", havingValue = "true")
    public StartupApplicationListener startupApplicationListener(@Qualifier("moduleMetadataDashboardRestService") DashboardRestService moduleMetadataDashboardRestService,
                                                                 @Qualifier("configurationMetadataDashboardRestService") DashboardRestService configurationMetadataDashboardRestService,
                                                                 Module<Flow> inboundFlowModule) {
        return new StartupApplicationListener(moduleMetadataDashboardRestService,
            configurationMetadataDashboardRestService, inboundFlowModule);
    }

    @Bean
    @ConditionalOnProperty(value="is.ikasan.enterprise.scheduler.instance", havingValue = "true")
    public StartupCompleteApplicationListener startupCompleteApplicationListener(Module<Flow> inboundFlowModule) {
        return new StartupCompleteApplicationListener(transactionManager, inboundFlowModule);
    }

    /**
     * Force refresh to the Refresh Scoped beans.
     * @param event
     */
    @EventListener
    public void onRefreshScopeRefreshed(final RefreshScopeRefreshedEvent event) {
        logger.info("Received Refresh event");
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            if(beanName.contains("scopedTarget")) {
                logger.info("Refreshing: bean name: " + beanName + " - Bean class: " + applicationContext.getBean(beanName).getClass());
                logger.debug("jobContextParamsSetupConfiguration = [{}]", jobContextParamsSetupConfiguration.getParamsToReplace());
            }
        }
    }
}