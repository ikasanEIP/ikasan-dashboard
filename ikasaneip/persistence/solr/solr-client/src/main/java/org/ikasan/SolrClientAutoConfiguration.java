package org.ikasan;

import org.ikasan.business.stream.metadata.dao.SolrBusinessStreamMetadataDaoImpl;
import org.ikasan.configuration.metadata.dao.SolrComponentConfigurationMetadataDaoImpl;
import org.ikasan.error.reporting.dao.SolrErrorReportingServiceDaoImpl;
import org.ikasan.exclusion.dao.SolrExclusionEventDao;
import org.ikasan.hospital.dao.SolrHospitalDao;
import org.ikasan.metrics.dao.SolrMetricsDaoImpl;
import org.ikasan.module.metadata.dao.SolrModuleMetadataDao;
import org.ikasan.replay.dao.SolrReplayAuditDao;
import org.ikasan.replay.dao.SolrReplayDao;
import org.ikasan.scheduled.context.dao.SolrScheduledContextDaoImpl;
import org.ikasan.scheduled.context.dao.SolrScheduledContextViewDaoImpl;
import org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceAuditAggregateDaoImpl;
import org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceAuditDaoImpl;
import org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceDaoImpl;
import org.ikasan.scheduled.instance.dao.SolrSchedulerJobInstanceDaoImpl;
import org.ikasan.scheduled.job.dao.*;
import org.ikasan.scheduled.joblock.dao.SolrJobLockCacheAuditDaoImpl;
import org.ikasan.scheduled.joblock.dao.SolrJobLockCacheDaoImpl;
import org.ikasan.scheduled.notification.dao.SolrEmailNotificationContextDaoImpl;
import org.ikasan.scheduled.notification.dao.SolrEmailNotificationDetailsDaoImpl;
import org.ikasan.scheduled.notification.dao.SolrNotificationSendAuditDaoImpl;
import org.ikasan.scheduled.profile.dao.SolrContextProfileDaoImpl;
import org.ikasan.security.dao.*;
import org.ikasan.setup.dao.SetupDao;
import org.ikasan.setup.dao.SolrSetupDaoImpl;
import org.ikasan.setup.service.SetupService;
import org.ikasan.setup.service.SolrSetupServiceImpl;
import org.ikasan.solr.dao.SolrGeneralDao;
import org.ikasan.solr.dao.SolrGeneralDaoImpl;
import org.ikasan.solr.service.SolrGeneralServiceImpl;
import org.ikasan.spec.entity.EntityDao;
import org.ikasan.spec.housekeeping.HousekeepService;
import org.ikasan.spec.metadata.dao.BusinessStreamMetadataDao;
import org.ikasan.spec.metadata.dao.ComponentConfigurationMetadataDao;
import org.ikasan.spec.metadata.dao.ModuleMetadataDao;
import org.ikasan.spec.metrics.MetricsDao;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextViewDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditAggregateDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceDao;
import org.ikasan.spec.scheduled.instance.dao.SchedulerJobInstanceDao;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheAuditDao;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheDao;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationContextDao;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationDetailsDao;
import org.ikasan.spec.scheduled.notification.dao.NotificationSendAuditDao;
import org.ikasan.spec.scheduled.profile.dao.ContextProfileDao;
import org.ikasan.spec.solr.SolrDaoBase;
import org.ikasan.spec.systemevent.SystemEventSearchDao;
import org.ikasan.systemevent.dao.SolrSystemEventDaoImpl;
import org.ikasan.wiretap.dao.SolrWiretapDao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SolrClientAutoConfiguration {

    @Value("${solr.mode:standalone}")
    private String solrMode;

    @Value("${solr.url:}")
    private String solrUrl;

    @Value("${solr.cloud.zk.hosts:}")
    private String solrCloudZkHosts;

    @Value("${solr.username:}")
    private String solrUsername;

    @Value("${solr.password:}")
    private String solrPassword;

    @Value("${solr.joblockcacheaudit.retention.days:30}")
    private int solrJobLockCacheAuditRetentionDays;

    @Value("${solr.retention.days:30}")
    private int solrRetentionDays;

    @Value("${solr.scheduler.instance.retention.days:90}")
    private int solrSchedulerInstanceRetentionDays;

    @Value("${solr.metrics.query.limit:200}")
    private int solrMetricsQueryLimit;

    @Value("${solr.connection.timeout.milli:15000}")
    private int solrConnectionTimeoutMilli;

    @Value("${notify.scheduled.events.batch.insert.listeners:false}")
    private boolean notifyBatchInsertListeners;

    @Bean("jobLockCacheDao")
    public JobLockCacheDao jobLockCacheDao() {
        SolrJobLockCacheDaoImpl solrJobLockCacheDao = new SolrJobLockCacheDaoImpl();
        initializeDao(solrJobLockCacheDao, SolrDaoBase.DO_NOT_EXPIRE);

        return solrJobLockCacheDao;
    }

    @Bean("jobLockCacheAuditDao")
    public JobLockCacheAuditDao jobLockCacheAuditDao() {
        SolrJobLockCacheAuditDaoImpl solrJobLockCacheAuditDao = new SolrJobLockCacheAuditDaoImpl();
        initializeDao(solrJobLockCacheAuditDao, this.solrJobLockCacheAuditRetentionDays);

        return solrJobLockCacheAuditDao;
    }

    @Bean("scheduledContextDao")
    public ScheduledContextDao scheduledContextDao() {
        SolrScheduledContextDaoImpl solrScheduledContextDao = new SolrScheduledContextDaoImpl();
        initializeDao(solrScheduledContextDao, SolrDaoBase.DO_NOT_EXPIRE);

        return solrScheduledContextDao;
    }

    @Bean("scheduledContextViewDao")
    public ScheduledContextViewDao scheduledContextViewDao() {
        SolrScheduledContextViewDaoImpl solrScheduledContextViewDao = new SolrScheduledContextViewDaoImpl();
        initializeDao(solrScheduledContextViewDao, SolrDaoBase.DO_NOT_EXPIRE);

        return solrScheduledContextViewDao;
    }

    @Bean("scheduledContextInstanceDao")
    public ScheduledContextInstanceDao scheduledContextInstanceDao() {
        SolrScheduledContextInstanceDaoImpl scheduledContextInstanceDao = new SolrScheduledContextInstanceDaoImpl();
        initializeDao(scheduledContextInstanceDao, this.solrSchedulerInstanceRetentionDays);

        return scheduledContextInstanceDao;
    }

    @Bean("scheduledContextInstanceAuditDao")
    public ScheduledContextInstanceAuditDao scheduledContextInstanceAuditDao() {
        SolrScheduledContextInstanceAuditDaoImpl scheduledContextInstanceAuditDao = new SolrScheduledContextInstanceAuditDaoImpl();
        initializeDao(scheduledContextInstanceAuditDao, this.solrSchedulerInstanceRetentionDays);

        return scheduledContextInstanceAuditDao;
    }

    @Bean("scheduledContextInstanceAuditAggregateDao")
    public ScheduledContextInstanceAuditAggregateDao scheduledContextInstanceAuditAggregateDao() {
        SolrScheduledContextInstanceAuditAggregateDaoImpl scheduledContextInstanceAuditAggregateDao = new SolrScheduledContextInstanceAuditAggregateDaoImpl();
        initializeDao(scheduledContextInstanceAuditAggregateDao, this.solrSchedulerInstanceRetentionDays);

        return scheduledContextInstanceAuditAggregateDao;
    }

    @Bean("emailNotificationContextDao")
    public EmailNotificationContextDao emailNotificationContextDao() {
        SolrEmailNotificationContextDaoImpl dao = new SolrEmailNotificationContextDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        return dao;
    }

    @Bean("systemEventSearchDao")
    public SystemEventSearchDao systemEventSearchDao() {
        SolrSystemEventDaoImpl dao = new SolrSystemEventDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        return dao;
    }

    @Bean("emailNotificationDetailsDao")
    public EmailNotificationDetailsDao emailNotificationDetailsDao() {
        SolrEmailNotificationDetailsDaoImpl dao = new SolrEmailNotificationDetailsDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        return dao;
    }

    @Bean("notificationSendAuditDao")
    public NotificationSendAuditDao notificationSendAuditDao() {
        SolrNotificationSendAuditDaoImpl dao = new SolrNotificationSendAuditDaoImpl();
        initializeDao(dao, this.solrRetentionDays);

        return dao;
    }

    @Bean("schedulerJobInstanceDao")
    public SchedulerJobInstanceDao schedulerJobInstanceDao() {
        SolrSchedulerJobInstanceDaoImpl schedulerJobInstanceDao = new SolrSchedulerJobInstanceDaoImpl();
        initializeDao(schedulerJobInstanceDao, this.solrSchedulerInstanceRetentionDays);

        return schedulerJobInstanceDao;
    }

    @Bean("contextProfileDao")
    public ContextProfileDao contextProfileDao() {
        SolrContextProfileDaoImpl solrContextProfileDao = new SolrContextProfileDaoImpl();
        initializeDao(solrContextProfileDao, SolrDaoBase.DO_NOT_EXPIRE);

        return solrContextProfileDao;
    }

    @Bean("fileEventDrivenJobRecordDao")
    public SolrFileEventDrivenJobDaoImpl fileEventDrivenJobRecordDao() {
        SolrFileEventDrivenJobDaoImpl dao = new SolrFileEventDrivenJobDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        return dao;
    }

    @Bean("internalEventDrivenJobRecordDao")
    public SolrInternalEventDrivenJobDaoImpl internalEventDrivenJobRecordDao() {
        SolrInternalEventDrivenJobDaoImpl dao = new SolrInternalEventDrivenJobDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        return dao;
    }

    @Bean("internalEventDrivenJobTemplateDao")
    public SolrInternalEventDrivenJobTemplateDaoImpl internalEventDrivenJobTemplateDao() {
        SolrInternalEventDrivenJobTemplateDaoImpl dao = new SolrInternalEventDrivenJobTemplateDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        return dao;
    }

    @Bean("quartzScheduleDrivenJobRecordDao")
    public SolrQuartzScheduleDrivenJobDaoImpl quartzScheduleDrivenJobRecordDao() {
        SolrQuartzScheduleDrivenJobDaoImpl dao = new SolrQuartzScheduleDrivenJobDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        return dao;
    }

    @Bean("globalEventJobRecordDao")
    public SolrGlobalEventJobDaoImpl globalEventJobRecordDao() {
        SolrGlobalEventJobDaoImpl dao = new SolrGlobalEventJobDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        return dao;
    }

    @Bean("contextStartJobDao")
    public SolrContextStartJobDaoImpl contextStartJobDao() {
        SolrContextStartJobDaoImpl dao = new SolrContextStartJobDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        return dao;
    }

    @Bean("contextTerminalJobDao")
    public SolrContextTerminalJobDaoImpl contextTerminalJobDao() {
        SolrContextTerminalJobDaoImpl dao = new SolrContextTerminalJobDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        return dao;
    }

    @Bean("schedulerJobRecordDao")
    public SolrSchedulerJobDaoImpl schedulerJobRecordDao() {
        SolrSchedulerJobDaoImpl dao = new SolrSchedulerJobDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        return dao;
    }

    @Bean(name = "esbSearchService")
    public SolrGeneralServiceImpl esbSearchService() {
        SolrGeneralDaoImpl dao = new SolrGeneralDaoImpl();
        initializeDao(dao, this.solrRetentionDays);
        SolrGeneralServiceImpl service = new SolrGeneralServiceImpl(dao);
        service.setSolrUsername(this.solrUsername);
        service.setSolrPassword(this.solrPassword);

        return service;
    }

    @Bean(name = "housekeepService")
    public HousekeepService housekeepService()
    {
        return esbSearchService();
    }

    @Bean("wiretapEntityDao")
    public EntityDao wiretapEntityDao() {
        SolrWiretapDao dao = new SolrWiretapDao();
        initializeDao(dao, this.solrRetentionDays);

        return dao;
    }

    @Bean("errorReportingServiceEntityDao")
    public EntityDao errorReportingServiceEntityDao() {
        SolrErrorReportingServiceDaoImpl dao = new SolrErrorReportingServiceDaoImpl();
        initializeDao(dao, this.solrRetentionDays);

        return dao;
    }

    @Bean("exclusionEventEntityDao")
    public EntityDao exclusionEventEntityDao() {
        SolrExclusionEventDao dao = new SolrExclusionEventDao();
        initializeDao(dao, this.solrRetentionDays);

        return dao;
    }

    @Bean("replayEntityDao")
    public EntityDao replayEntityDao() {
        SolrReplayDao dao = new SolrReplayDao();
        initializeDao(dao, this.solrRetentionDays);

        return dao;
    }

    @Bean("metricsDao")
    public MetricsDao metricsDao() {
        SolrMetricsDaoImpl dao = new SolrMetricsDaoImpl(this.solrMetricsQueryLimit);
        initializeDao(dao, this.solrRetentionDays);

        return dao;
    }

    @Bean("hospitalEntityDao")
    public EntityDao hospitalEntityDao() {
        SolrHospitalDao dao = new SolrHospitalDao();
        initializeDao(dao, this.solrRetentionDays);

        return dao;
    }

    @Bean("moduleMetadataDao")
    public ModuleMetadataDao moduleMetadataDao() {
        SolrModuleMetadataDao dao = new SolrModuleMetadataDao();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        return dao;
    }

    @Bean("businessStreamMetadataDao")
    public BusinessStreamMetadataDao businessStreamMetadataDao() {
        SolrBusinessStreamMetadataDaoImpl dao = new SolrBusinessStreamMetadataDaoImpl();
        initializeDao(dao, this.solrRetentionDays);

        return dao;
    }

    @Bean("replayAuditEntityDao")
    public EntityDao replayAuditEntityDao() {
        SolrReplayAuditDao dao = new SolrReplayAuditDao();
        initializeDao(dao, this.solrRetentionDays);

        return dao;
    }

    @Bean("componentConfigurationMetadataDao")
    public ComponentConfigurationMetadataDao componentConfigurationMetadataDao() {
        SolrComponentConfigurationMetadataDaoImpl dao = new SolrComponentConfigurationMetadataDaoImpl();
        initializeDao(dao, this.solrRetentionDays);

        return dao;
    }

    @Bean("systemEventEntityDao")
    public EntityDao systemEventEntityDao() {
        SolrSystemEventDaoImpl dao = new SolrSystemEventDaoImpl();
        initializeDao(dao, this.solrRetentionDays);

        return dao;
    }

    @Bean("solrAuthenticationMethodDao")
    public SolrAuthenticationMethodDaoImpl solrAuthenticationMethodDao() {
        SolrAuthenticationMethodDaoImpl dao = new SolrAuthenticationMethodDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);
        return dao;
    }

    @Bean("solrPolicyDao")
    public SolrPolicyDaoImpl solrPolicyDao() {
        SolrPolicyDaoImpl dao = new SolrPolicyDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);
        return dao;
    }

    @Bean("solrRoleDao")
    public SolrRoleDaoImpl solrRoleDao(SolrPolicyDaoImpl solrPolicyDao) {
        SolrRoleDaoImpl dao = new SolrRoleDaoImpl(solrPolicyDao);
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);
        return dao;
    }

    @Bean("solrIkasanPrincipalDao")
    public SolrIkasanPrincipalDaoImpl solrIkasanPrincipalDao(SolrRoleDaoImpl solrRoleDao) {
        SolrIkasanPrincipalDaoImpl dao = new SolrIkasanPrincipalDaoImpl(solrRoleDao);
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);
        return dao;
    }

    @Bean("solrUserDao")
    public SolrUserDaoImpl solrUserDao(SolrIkasanPrincipalDaoImpl solrIkasanPrincipalDao) {
        SolrUserDaoImpl dao = new SolrUserDaoImpl(solrIkasanPrincipalDao);
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);
        return dao;
    }

    @Bean("solrSecurityDao")
    public SolrSecurityDaoImpl solrSecurityDao(SolrIkasanPrincipalDaoImpl solrIkasanPrincipalDao,
                                                SolrPolicyDaoImpl solrPolicyDao,
                                                SolrRoleDaoImpl solrRoleDao,
                                                SolrAuthenticationMethodDaoImpl solrAuthenticationMethodDao,
                                                SolrUserDaoImpl solrUserDao) {
        return new SolrSecurityDaoImpl(solrIkasanPrincipalDao, solrPolicyDao, solrRoleDao,
            solrAuthenticationMethodDao, solrUserDao);
    }
    @Bean("setupDao")
    public SetupDao setupDao() {
        SolrSetupDaoImpl dao = new SolrSetupDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);
        return dao;
    }

    @Bean("setupService")
    public SetupService setupService(SetupDao setupDao) {
        SolrSetupServiceImpl service = new SolrSetupServiceImpl(setupDao);
        service.setSolrUsername(this.solrUsername);
        service.setSolrPassword(this.solrPassword);

        return service;
    }

    /**
     * Helper method to initialize a DAO based on the configured Solr mode.
     *
     * @param dao the DAO to initialize
     * @param daysToKeep retention days for the data
     */
    private void initializeDao(SolrDaoBase<?> dao, int daysToKeep) {
        if ("cloud".equalsIgnoreCase(this.solrMode)) {
            if (this.solrCloudZkHosts == null || this.solrCloudZkHosts.trim().isEmpty()) {
                throw new IllegalArgumentException("solr.cloud.zk.hosts must be configured when solr.mode=cloud");
            }
            List<String> zkHosts = Arrays.asList(solrCloudZkHosts.split(","));
            dao.initCloud(zkHosts, daysToKeep, this.solrConnectionTimeoutMilli);
        } else {
            if (solrUrl == null || solrUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("solr.url must be configured when solr.mode=standalone");
            }
            dao.initStandalone(this.solrUrl, daysToKeep, this.solrConnectionTimeoutMilli);
        }
        dao.setSolrUsername(this.solrUsername);
        dao.setSolrPassword(this.solrPassword);
    }
}