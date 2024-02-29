package org.ikasan.dashboard.beans;

import com.vaadin.flow.server.*;
import org.ikasan.bigqueue.BigQueueImpl;
import org.ikasan.bigqueue.IBigQueue;
import org.ikasan.dashboard.cache.FlowStateCache;
import org.ikasan.dashboard.ui.scheduler.model.CalendarConfiguration;
import org.ikasan.dashboard.ui.scheduler.util.ContextInstanceSavedEventBroadcasterImpl;
import org.ikasan.dashboard.ui.util.DashboardCacheAdapter;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ContextInstanceStateChangeEventBroadcasterImpl;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.JobLockCacheEventBroadcasterImpl;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.SchedulerJobStateChangeEventBroadcasterImpl;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.job.orchestration.util.ContextHelper;
import org.ikasan.module.metadata.service.SolrModuleMetadataServiceImpl;
import org.ikasan.orchestration.service.context.global.GlobalEventServiceImpl;
import org.ikasan.spec.cache.FlowStateCacheAdapter;
import org.ikasan.spec.metadata.ModuleMetaDataProvider;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.scheduled.event.service.ContextInstanceSavedEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.ikasan.topology.metadata.JsonFlowMetaDataProvider;
import org.ikasan.topology.metadata.JsonModuleMetaDataProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Configuration
public class DashboardComponentFactory
{
    @Value("${scheduled.job.context.queue.directory:.}")
    private String queueDirectory;

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

    @Component
    private static final class IkasanSessionListener implements SessionInitListener, SessionDestroyListener {

        @Override
        public void sessionInit(SessionInitEvent event)
            throws ServiceException {
            // Nothing to do here
        }

        @Override
        public void sessionDestroy(SessionDestroyEvent event) {
            // Remove the authentication from the context holder
            SecurityContextHolder.getContext().setAuthentication(null);
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
    public FlowStateCache flowStateCache(SolrModuleMetadataServiceImpl moduleMetadataService)
    {
        FlowStateCache flowStateCache = FlowStateCache.instance();
        flowStateCache.setModuleControlRestService(this.moduleControlRestService);
        flowStateCache.setModuleMetaDataService(moduleMetadataService);
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

}
