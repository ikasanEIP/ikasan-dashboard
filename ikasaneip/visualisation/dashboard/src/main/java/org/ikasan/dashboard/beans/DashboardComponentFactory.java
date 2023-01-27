package org.ikasan.dashboard.beans;

import org.ikasan.bigqueue.BigQueueImpl;
import org.ikasan.bigqueue.IBigQueue;
import com.vaadin.flow.server.*;
import org.ikasan.business.stream.metadata.dao.SolrBusinessStreamMetadataDao;
import org.ikasan.business.stream.metadata.service.SolrBusinessStreamMetaDataServiceImpl;
import org.ikasan.configuration.metadata.dao.SolrComponentConfigurationMetadataDao;
import org.ikasan.configuration.metadata.service.SolrComponentConfigurationMetadataServiceImpl;
import org.ikasan.dashboard.cache.FlowStateCache;
import org.ikasan.dashboard.ui.scheduler.model.CalendarConfiguration;
import org.ikasan.dashboard.ui.util.DashboardCacheAdapter;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.ContextInstanceStateChangeEventBroadcasterImpl;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.JobLockCacheEventBroadcasterImpl;
import org.ikasan.dashboard.ui.visualisation.scheduler.service.SchedulerJobStateChangeEventBroadcasterImpl;
import org.ikasan.error.reporting.dao.SolrErrorReportingServiceDao;
import org.ikasan.error.reporting.service.SolrErrorReportingServiceImpl;
import org.ikasan.exclusion.dao.SolrExclusionEventDao;
import org.ikasan.exclusion.service.SolrExclusionServiceImpl;
import org.ikasan.hospital.dao.SolrHospitalDao;
import org.ikasan.hospital.service.SolrHospitalServiceImpl;
import org.ikasan.job.orchestration.context.cache.JobLockCacheImpl;
import org.ikasan.metrics.dao.SolrMetricsDao;
import org.ikasan.metrics.service.SolrMetricsServiceImpl;
import org.ikasan.module.metadata.dao.SolrModuleMetadataDao;
import org.ikasan.module.metadata.service.SolrModuleMetadataServiceImpl;
import org.ikasan.orchestration.service.context.global.GlobalEventServiceImpl;
import org.ikasan.replay.dao.SolrReplayAuditDao;
import org.ikasan.replay.dao.SolrReplayDao;
import org.ikasan.replay.service.SolrReplayAuditServiceImpl;
import org.ikasan.replay.service.SolrReplayServiceImpl;
import org.ikasan.scheduled.event.dao.SolrScheduledProcessEventDao;
import org.ikasan.scheduled.event.service.SolrScheduledProcessServiceImpl;
import org.ikasan.solr.dao.SolrGeneralDaoImpl;
import org.ikasan.solr.service.SolrGeneralServiceImpl;
import org.ikasan.spec.cache.FlowStateCacheAdapter;
import org.ikasan.spec.exclusion.ExclusionEvent;
import org.ikasan.spec.history.FlowInvocationMetric;
import org.ikasan.spec.hospital.service.HospitalAuditService;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;
import org.ikasan.spec.metadata.ModuleMetaDataProvider;
import org.ikasan.spec.metrics.MetricsService;
import org.ikasan.spec.module.client.ModuleControlService;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.replay.ReplayEvent;
import org.ikasan.spec.scheduled.context.model.JobLockCache;
import org.ikasan.spec.scheduled.event.service.ContextInstanceStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.JobLockCacheEventBroadcaster;
import org.ikasan.spec.scheduled.event.service.SchedulerJobStateChangeEventBroadcaster;
import org.ikasan.spec.scheduled.job.service.GlobalEventService;
import org.ikasan.spec.wiretap.WiretapEvent;
import org.ikasan.systemevent.dao.SolrSystemEventDao;
import org.ikasan.systemevent.service.SolrSystemEventServiceImpl;
import org.ikasan.topology.metadata.JsonFlowMetaDataProvider;
import org.ikasan.topology.metadata.JsonModuleMetaDataProvider;
import org.ikasan.wiretap.dao.SolrWiretapDao;
import org.ikasan.wiretap.service.SolrWiretapServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Configuration
public class DashboardComponentFactory
{
    @Value("${scheduled.job.context.queue.directory}")
    private String queueDirectory;

    @Resource
    private ModuleControlService moduleControlRestService;

    private static final String INBOUND_QUEUE = "dashboard-inbound-queue";

    @Bean
    @ConfigurationProperties(prefix = "scheduler.calendar")
    public CalendarConfiguration calendarConfiguration() {
        return new CalendarConfiguration();
    }

    @Bean
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
    public JobLockCacheEventBroadcaster jobLockCacheEventBroadcaster() {
        JobLockCacheEventBroadcaster broadcaster = new JobLockCacheEventBroadcasterImpl();
        JobLockCacheImpl.instance().setJobLockCacheEventBroadcaster(broadcaster);

        return broadcaster;
    }

    @Bean
    public ModuleMetaDataProvider<String> moduleMetaDataProvider() {
        return new JsonModuleMetaDataProvider(new JsonFlowMetaDataProvider());
    }

}
