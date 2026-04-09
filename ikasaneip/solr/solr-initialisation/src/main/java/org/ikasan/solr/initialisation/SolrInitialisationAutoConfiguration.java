package org.ikasan.solr.initialisation;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.ikasan.business.stream.metadata.dao.SolrBusinessStreamMetadataDao;
import org.ikasan.business.stream.metadata.service.SolrBusinessStreamMetaDataServiceImpl;
import org.ikasan.configuration.metadata.dao.SolrComponentConfigurationMetadataDao;
import org.ikasan.configuration.metadata.service.SolrComponentConfigurationMetadataServiceImpl;
import org.ikasan.error.reporting.dao.SolrErrorReportingServiceDao;
import org.ikasan.error.reporting.service.SolrErrorReportingServiceImpl;
import org.ikasan.exclusion.dao.SolrExclusionEventDao;
import org.ikasan.exclusion.service.SolrExclusionServiceImpl;
import org.ikasan.hospital.dao.SolrHospitalDao;
import org.ikasan.hospital.service.SolrHospitalServiceImpl;
import org.ikasan.metrics.dao.SolrMetricsDao;
import org.ikasan.metrics.service.SolrMetricsServiceImpl;
import org.ikasan.module.metadata.dao.SolrModuleMetadataDao;
import org.ikasan.module.metadata.service.SolrModuleMetadataServiceImpl;
import org.ikasan.replay.dao.SolrReplayAuditDao;
import org.ikasan.replay.dao.SolrReplayDao;
import org.ikasan.replay.service.SolrReplayAuditServiceImpl;
import org.ikasan.replay.service.SolrReplayServiceImpl;
import org.ikasan.scheduled.context.dao.SolrScheduledContextDaoImpl;
import org.ikasan.scheduled.context.dao.SolrScheduledContextViewDaoImpl;
import org.ikasan.scheduled.context.service.SolrScheduledContextServiceImpl;
import org.ikasan.scheduled.event.dao.SolrScheduledProcessEventDao;
import org.ikasan.scheduled.event.service.SolrScheduledProcessServiceImpl;
import org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceAuditAggregateDaoImpl;
import org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceAuditDaoImpl;
import org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceDaoImpl;
import org.ikasan.scheduled.instance.dao.SolrSchedulerJobInstanceDaoImpl;
import org.ikasan.scheduled.instance.service.SolrScheduledContextInstanceServiceImpl;
import org.ikasan.scheduled.instance.service.SolrSchedulerJobInstanceServiceImpl;
import org.ikasan.scheduled.job.dao.*;
import org.ikasan.scheduled.job.service.SolrInternalEventDrivenJobRecordServiceImpl;
import org.ikasan.scheduled.job.service.SolrSchedulerJobServiceImpl;
import org.ikasan.scheduled.joblock.dao.SolrJobLockCacheAuditDaoImpl;
import org.ikasan.scheduled.joblock.dao.SolrJobLockCacheDaoImpl;
import org.ikasan.scheduled.joblock.service.SolrJobLockCacheServiceImpl;
import org.ikasan.scheduled.notification.dao.SolrEmailNotificationContextDaoImpl;
import org.ikasan.scheduled.notification.dao.SolrEmailNotificationDetailsDaoImpl;
import org.ikasan.scheduled.notification.dao.SolrNotificationSendAuditDaoImpl;
import org.ikasan.scheduled.notification.service.SolrEmailNotificationContextServiceImpl;
import org.ikasan.scheduled.notification.service.SolrEmailNotificationDetailsServiceImpl;
import org.ikasan.scheduled.notification.service.SolrNotificationSendAuditServiceImpl;
import org.ikasan.scheduled.profile.dao.SolrContextProfileDaoImpl;
import org.ikasan.scheduled.profile.service.SolrContextProfileServiceImpl;
import org.ikasan.security.dao.*;
import org.ikasan.security.service.SecurityServiceImpl;
import org.ikasan.security.service.UserServiceImpl;
import org.ikasan.setup.service.SetupService;
import org.ikasan.solr.dao.SolrGeneralDaoImpl;
import org.ikasan.solr.initialisation.core.SolrDataJob;
import org.ikasan.solr.initialisation.core.SolrDataJobException;
import org.ikasan.solr.initialisation.core.SolrDataJobManager;
import org.ikasan.solr.initialisation.security.BaselineSecurityDataLoader;
import org.ikasan.solr.service.SolrGeneralServiceImpl;
import org.ikasan.spec.exclusion.ExclusionEvent;
import org.ikasan.spec.history.FlowInvocationMetric;
import org.ikasan.spec.hospital.service.HospitalAuditService;
import org.ikasan.spec.metadata.BusinessStreamMetaDataService;
import org.ikasan.spec.metrics.MetricsService;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.replay.ReplayEvent;
import org.ikasan.spec.scheduled.context.service.ScheduledContextService;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditAggregateDao;
import org.ikasan.spec.scheduled.instance.service.ScheduledContextInstanceService;
import org.ikasan.spec.scheduled.instance.service.SchedulerJobInstanceService;
import org.ikasan.spec.scheduled.job.dao.InternalEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.service.InternalEventDrivenJobService;
import org.ikasan.spec.scheduled.job.service.SchedulerJobService;
import org.ikasan.spec.scheduled.joblock.service.JobLockCacheService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationContextService;
import org.ikasan.spec.scheduled.notification.service.EmailNotificationDetailsService;
import org.ikasan.spec.scheduled.notification.service.NotificationSendAuditService;
import org.ikasan.spec.scheduled.profile.service.ContextProfileService;
import org.ikasan.spec.security.service.SecurityService;
import org.ikasan.spec.security.service.UserService;
import org.ikasan.spec.solr.SolrDaoBase;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.ikasan.spec.wiretap.WiretapEvent;
import org.ikasan.systemevent.dao.SolrSystemEventDaoImpl;
import org.ikasan.systemevent.service.SolrSystemEventSearchServiceImpl;
import org.ikasan.systemevent.service.SolrSystemEventServiceImpl;
import org.ikasan.wiretap.dao.SolrWiretapDao;
import org.ikasan.wiretap.service.SolrWiretapServiceImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
public class SolrInitialisationAutoConfiguration implements ApplicationListener<ContextRefreshedEvent> {

    @Resource
    private SolrPolicyDaoImpl policyDao;
    @Resource
    private SolrRoleDaoImpl roleDao;
    @Resource
    private SolrIkasanPrincipalDaoImpl principalDao;
    @Resource
    private SolrUserDaoImpl userDao;
    @Resource
    private SetupService setupService;

    private SolrDataJobManager solrDataJobManager;

    @Bean
    public SolrDataJobManager solrDataJobManager(List<SolrDataJob> solrDataJobs) {
        this.solrDataJobManager = new SolrDataJobManager(this.setupService, solrDataJobs);
        return solrDataJobManager;
    }

    @Bean
    public SolrDataJob baselineSecurityDataLoader() {
        return new BaselineSecurityDataLoader(this.policyDao, this.roleDao, this.principalDao, this.userDao);
    }

    @Bean
    public List<SolrDataJob> solrDataJobs() {
        ArrayList<SolrDataJob> solrDataJobs = new ArrayList<>();
        solrDataJobs.add(this.baselineSecurityDataLoader());

        return solrDataJobs;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
//        try {
//            this.solrDataJobManager.execute();
//        } catch (SolrDataJobException e) {
//            throw new RuntimeException(e);
//        }
    }
}