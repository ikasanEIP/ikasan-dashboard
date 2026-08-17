package org.ikasan.dashboard.beans;

import com.vaadin.flow.server.*;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.ikasan.bigqueue.BigQueueImpl;
import org.ikasan.bigqueue.IBigQueue;
import org.ikasan.dashboard.cache.FlowStateCache;
import org.ikasan.dashboard.cache.ModuleMetadataCache;
import org.ikasan.dashboard.ui.scheduler.model.CalendarConfiguration;
import org.ikasan.dashboard.ui.scheduler.service.AggregateStatusCollector;
import org.ikasan.dashboard.ui.scheduler.util.ContextInstanceSavedEventBroadcasterImpl;
import org.ikasan.dashboard.ui.util.DashboardCacheAdapter;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ContextInstanceStateChangeEventBroadcasterImpl;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.JobLockCacheEventBroadcasterImpl;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.SchedulerJobStateChangeEventBroadcasterImpl;
import org.ikasan.flow.configuration.FlowComponentInvokerSetupServiceConfiguration;
import org.ikasan.flow.configuration.FlowPersistentConfiguration;
import org.ikasan.harvesting.HarvestingAutoConfiguration;
import org.ikasan.harvesting.HarvestingSchedulerServiceImpl;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.module.SimpleModule;
import org.ikasan.orchestration.service.context.global.GlobalEventServiceImpl;
import org.ikasan.scheduler.CachingScheduledJobFactory;
import org.ikasan.scheduler.SchedulerFactory;
import org.ikasan.spec.cache.FlowStateCacheAdapter;
import org.ikasan.spec.harvest.HarvestingJob;
import org.ikasan.spec.harvest.HarvestingSchedulerService;
import org.ikasan.spec.metadata.ModuleMetaDataProvider;
import org.ikasan.spec.metadata.ModuleMetaDataService;
import org.ikasan.spec.module.Module;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.ikasan.topology.metadata.JsonFlowMetaDataProvider;
import org.ikasan.topology.metadata.JsonModuleMetaDataProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

@Configuration
@Import({HarvestingAutoConfiguration.class})
public class DashboardComponentFactory
{
    @Value("${scheduled.job.context.queue.directory:.}")
    private String queueDirectory;

    @Value("${org.atmosphere.cpr.broadcaster.use.externalised.configurations:false}")
    private String atmosphereBroadcasterUseExternalisedConfigurations;

    @Value("#{${org.atmosphere.cpr.configurations}}")
    private Map<String, String> atomosphereConfigurationMap;

    @Resource
    private ModuleControlService moduleControlRestService;

    private static final String INBOUND_QUEUE = "dashboard-inbound-queue";

    @Value("${context.export.context.token.replace.string:#{null}}")
    private String contextTokenReplacementString;

    @Value("${context.export.agent.token.replace.string:#{null}}")
    private String agentTokenReplacementString;

    @Value("${context.export.env.token.replace.string:#{null}}")
    private String envTokenReplacementString;

    @Value("${context.export.use.underscore.separated.context.name.convention:false}")
    private boolean useUnderscoreSeparatedContextNameConvention;

    @Value("${module.metadata.cache.expiry.seconds:60}")
    private int moduleMetadataCacheExpirySeconds;

    @Value("${module.name}")
    private String moduleName;

    @Value("${flow.state.cache.throttle.interval.millis:60000}")
    private long flowStateCacheThrottleIntervalMillis;

    @Value("${flow.state.cache.oscillation.window.millis:5000}")
    private long oscillationWindowMs;

    private static boolean invalidateNonLoginSessions = true;


    /**
     * This method is used to reset the state of the `invalidateNonLoginSessions` field to `false`.
     * It may be intended for preparing or restoring a specific context state within the
     * DashboardComponentFactory class.
     *
     * Note: The method does not accept parameters and has no return value.
     */
    public static void testContext() {
        invalidateNonLoginSessions = false;
    }

    @Bean
    public ContextHelper contextHelper() {
        ContextHelper contextHelper = new ContextHelper();

        if(contextTokenReplacementString != null && !contextTokenReplacementString.isEmpty()) {
            contextHelper.setContextNameReplacement(contextTokenReplacementString);
        }

        if(agentTokenReplacementString != null && !agentTokenReplacementString.isEmpty()) {
            contextHelper.setAgentNameReplacement(agentTokenReplacementString);
        }

        if(envTokenReplacementString != null && !envTokenReplacementString.isEmpty()) {
            contextHelper.setEnvNameReplacement(envTokenReplacementString);
        }

        contextHelper.setUseUnderscoreSeparatedContextNameConvention(useUnderscoreSeparatedContextNameConvention);

        return contextHelper;
    }

    @Bean
    @ConfigurationProperties(prefix = "scheduler.calendar")
    public CalendarConfiguration calendarConfiguration() {
        return new CalendarConfiguration();
    }

    @Bean
    @ConditionalOnProperty(value="is.ikasan.enterprise.scheduler.instance", havingValue = "true")
    public IBigQueue inboundQueue() throws IOException {
        return new BigQueueImpl(queueDirectory, INBOUND_QUEUE);
    }

    @Bean
    @ConditionalOnProperty(value="is.ikasan.enterprise.scheduler.instance", havingValue = "false")
    public Module module() throws IOException {
        return new SimpleModule(moduleName);
    }

    @Bean(name = "harvestingSchedulerService")
    @ConditionalOnProperty(value="is.ikasan.enterprise.scheduler.instance", havingValue = "false")
    public HarvestingSchedulerService harvestingSchedulerService(List<HarvestingJob> harvestingJobs)
    {
        HarvestingSchedulerService harvestingSchedulerService =  new HarvestingSchedulerServiceImpl
            (SchedulerFactory.getInstance().getScheduler(), CachingScheduledJobFactory.getInstance(), harvestingJobs);

        harvestingSchedulerService.registerJobs();
        return harvestingSchedulerService;
    }

    @Component
    public static final class IkasanSessionListener implements SessionInitListener, SessionDestroyListener {
        private static final Set<VaadinSession> ACTIVE_SESSIONS =
            Collections.synchronizedSet(new HashSet<>());

        @Override
        public void sessionInit(SessionInitEvent event)
            throws ServiceException {
            String pathInfo = event.getRequest().getPathInfo();
            if (pathInfo == null || !pathInfo.endsWith("/login")) {
                if(invalidateNonLoginSessions) {
                    event.getSession().access(() -> {
                        event.getSession().getSession().invalidate();
                        event.getSession().close();
                    });
                }
                return;
            }

            ACTIVE_SESSIONS.add(event.getSession());
        }

        @Override
        public void sessionDestroy(SessionDestroyEvent event) {
            ACTIVE_SESSIONS.remove(event.getSession());
        }

        /**
         * Retrieves the currently active Vaadin sessions stored in a ConcurrentHashMap.
         *
         * @return A ConcurrentHashMap containing active Vaadin sessions, with session ID as key and VaadinSession as value.
         */
        public static Set<VaadinSession> getActiveSessions() {
            return new HashSet<>(ACTIVE_SESSIONS);
        }
    }

    @Component
    private static class IkasanServiceInitListener implements VaadinServiceInitListener {

        private final IkasanSessionListener sessionListener;

        private IkasanServiceInitListener(IkasanSessionListener sessionListener) {
            this.sessionListener = sessionListener;
        }

        @Override
        public void serviceInit(ServiceInitEvent event) {
            event.getSource().addSessionInitListener(sessionListener);
            event.getSource().addSessionDestroyListener(sessionListener);
        }
    }

    @Component
    private class AtmosphereInitialiser implements ServletContextInitializer {

        @Override
        public void onStartup(ServletContext servletContext) throws ServletException {
            servletContext.setInitParameter("org.atmosphere.cpr.AtmosphereConfig.getInitParameter"
                , atmosphereBroadcasterUseExternalisedConfigurations);

            if(atmosphereBroadcasterUseExternalisedConfigurations.equals("true")) {
                atomosphereConfigurationMap.entrySet().forEach(entry -> {
                    servletContext.setInitParameter(entry.getKey(), entry.getValue());
                });
            }
        }
    }

    @Bean
    public AggregateStatusCollector aggregateStatusCollector(SchedulerJobInstanceService schedulerJobInstanceService) {
        AggregateStatusCollector.init(schedulerJobInstanceService);

        return AggregateStatusCollector.instance();
    }

    @Bean
    public ModuleMetadataCache moduleMetadataCache(@Qualifier("moduleMetadataService")ModuleMetaDataService moduleMetaDataService) {
        ModuleMetadataCache.init(moduleMetaDataService, moduleMetadataCacheExpirySeconds);

        return ModuleMetadataCache.instance();
    }

    @Bean
    public GlobalEventService globalEventService() {
        return new GlobalEventServiceImpl();
    }

    @Bean
    public FlowStateCacheAdapter dashboardCacheAdapter()
    {
        return new DashboardCacheAdapter();
    }

    @Bean
    public FlowStateCache flowStateCache(@Qualifier("moduleMetadataService") ModuleMetaDataService moduleMetadataService)
    {
        FlowStateCache flowStateCache = FlowStateCache.instance();
        flowStateCache.setModuleControlRestService(this.moduleControlRestService);
        flowStateCache.setModuleMetaDataService(moduleMetadataService);
        flowStateCache.setThrottleIntervalMs(this.flowStateCacheThrottleIntervalMillis);
        flowStateCache.setOscillationWindowMs(this.oscillationWindowMs);

        return flowStateCache;
    }

    @Bean
    public ContextInstanceStateChangeEventBroadcaster contextInstanceStateChangeEventBroadcaster() {
        return new ContextInstanceStateChangeEventBroadcasterImpl();
    }

    @Bean
    public SchedulerJobStateChangeEventBroadcaster schedulerJobStateChangeEventBroadcaster() {
        return new SchedulerJobStateChangeEventBroadcasterImpl();
    }

    @Bean
    public ContextInstanceSavedEventBroadcaster contextInstanceSavedEventBroadcaster() {
        return new ContextInstanceSavedEventBroadcasterImpl();
    }

    @Bean
    public JobLockCacheEventBroadcaster jobLockCacheEventBroadcaster() {
        JobLockCacheEventBroadcaster broadcaster = new JobLockCacheEventBroadcasterImpl();
        JobLockCacheImpl.instance().setJobLockCacheEventBroadcaster(broadcaster);

        return broadcaster;
    }

    @Bean
    @Primary
    public ModuleMetaDataProvider<String> moduleMetaDataProvider() {
        return new JsonModuleMetaDataProvider(new JsonFlowMetaDataProvider());
    }


    @Bean(name = "flowConfigurations")
    @ConfigurationProperties(prefix = "ikasan.flow.configuration")
    public Map<String, FlowPersistentConfiguration> flowConfigurations(){
        return new HashMap<>();
    }

    @Bean(name = "flowComponentInvokerConfigurations")
    @ConfigurationProperties(prefix = "ikasan.flow.component.invoker.configuration")
    public FlowComponentInvokerSetupServiceConfiguration componentInvokerConfiguration(){
        return new FlowComponentInvokerSetupServiceConfiguration();
    }

}
