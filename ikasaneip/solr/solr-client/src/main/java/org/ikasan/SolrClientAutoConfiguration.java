package org.ikasan;

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
import org.ikasan.solr.dao.SolrGeneralDaoImpl;
import org.ikasan.solr.service.SolrGeneralServiceImpl;
import org.ikasan.spec.exclusion.ExclusionEvent;
import org.ikasan.spec.history.FlowInvocationMetric;
import org.ikasan.spec.hospital.service.HospitalAuditService;
import org.ikasan.spec.metadata.service.BusinessStreamMetaDataService;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    @Value("${solr.socket.timeout.milli:15000}")
    private int solrSocketTimeoutMilli;

    @Value("${solr.save.context.instance.audits:true}")
    private boolean saveContextInstanceAuditRecords;

    @Value("${solr.save.context.instance.audit.deltas:true}")
    private boolean saveContextInstanceAuditDeltaRecords;

    @Value("${solr.save.joblockcache.audits:true}")
    private boolean saveJobLockCacheAudits;

    @Value("#{${scheduler.job.execution.environment.label}}")
    private Map<String, String> schedulerJobExecutionEnvironmentLabel;

    @Value("${notify.scheduled.events.batch.insert.listeners:false}")
    private boolean notifyBatchInsertListeners;

    @Value("${ikasan.enterprise.scheduler.use.legacy.job.status.count:false}")
    private boolean useLegacyJobStatusCount = false;

    @Bean
    public JobLockCacheService jobLockCacheService() {
        SolrJobLockCacheDaoImpl solrJobLockCacheDao = new SolrJobLockCacheDaoImpl();
        initializeDao(solrJobLockCacheDao, SolrDaoBase.DO_NOT_EXPIRE);

        SolrJobLockCacheAuditDaoImpl solrJobLockCacheAuditDao = new SolrJobLockCacheAuditDaoImpl();
        initializeDao(solrJobLockCacheAuditDao, this.solrJobLockCacheAuditRetentionDays);

        return new SolrJobLockCacheServiceImpl(solrJobLockCacheDao, solrJobLockCacheAuditDao, this.saveJobLockCacheAudits);
    }

    @Bean
    public ScheduledContextService scheduledContextService() {
        SolrScheduledContextDaoImpl solrScheduledContextDao = new SolrScheduledContextDaoImpl();
        initializeDao(solrScheduledContextDao, SolrDaoBase.DO_NOT_EXPIRE);

        SolrScheduledContextViewDaoImpl solrScheduledContextViewDao = new SolrScheduledContextViewDaoImpl();
        initializeDao(solrScheduledContextViewDao, SolrDaoBase.DO_NOT_EXPIRE);

        return new SolrScheduledContextServiceImpl(solrScheduledContextDao, solrScheduledContextViewDao);
    }

    @Bean("scheduledProcessEventBatchInsert")
    public SolrScheduledProcessServiceImpl solrScheduledProcessEventService()
    {
        SolrScheduledProcessEventDao dao = new SolrScheduledProcessEventDao();
        initializeDao(dao, 30);

        SolrModuleMetadataDao solrModuleMetadataDao = new SolrModuleMetadataDao();
        initializeDao(solrModuleMetadataDao, 30);

        SolrComponentConfigurationMetadataDao solrComponentConfigurationMetadataDao = new SolrComponentConfigurationMetadataDao();
        initializeDao(solrComponentConfigurationMetadataDao, 30);

        SolrBusinessStreamMetadataDao solrBusinessStreamMetadataDao = new SolrBusinessStreamMetadataDao();
        initializeDao(solrBusinessStreamMetadataDao, 30);

        SolrScheduledProcessServiceImpl service = new SolrScheduledProcessServiceImpl(dao, solrModuleMetadataDao
            , solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, this.notifyBatchInsertListeners);
        service.setSolrUsername(this.solrUsername);
        service.setSolrPassword(this.solrPassword);

        return service;
    }

    @Bean
    public ScheduledContextInstanceService scheduledContextInstanceService(ScheduledContextInstanceAuditAggregateDao scheduledContextInstanceAuditAggregateDao) {
        SolrScheduledContextInstanceDaoImpl scheduledContextInstanceDao = new SolrScheduledContextInstanceDaoImpl();
        initializeDao(scheduledContextInstanceDao, this.solrSchedulerInstanceRetentionDays);

        SolrScheduledContextInstanceAuditDaoImpl scheduledContextInstanceAuditDao = new SolrScheduledContextInstanceAuditDaoImpl();
        initializeDao(scheduledContextInstanceAuditDao, this.solrSchedulerInstanceRetentionDays);

        return new SolrScheduledContextInstanceServiceImpl(scheduledContextInstanceDao, scheduledContextInstanceAuditDao, scheduledContextInstanceAuditAggregateDao
            , this.saveContextInstanceAuditRecords, this.saveContextInstanceAuditDeltaRecords);
    }

    @Bean
    public ScheduledContextInstanceAuditAggregateDao scheduledContextInstanceAuditAggregateDao() {
        SolrScheduledContextInstanceAuditAggregateDaoImpl scheduledContextInstanceAuditAggregateDao = new SolrScheduledContextInstanceAuditAggregateDaoImpl();
        initializeDao(scheduledContextInstanceAuditAggregateDao, this.solrSchedulerInstanceRetentionDays);

        return scheduledContextInstanceAuditAggregateDao;
    }

    @Bean
    public EmailNotificationContextService emailNotificationContextService() {
        SolrEmailNotificationContextDaoImpl dao = new SolrEmailNotificationContextDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        SolrEmailNotificationContextServiceImpl service = new SolrEmailNotificationContextServiceImpl(dao);
        service.setSolrPassword(this.solrPassword);
        service.setSolrUsername(this.solrUsername);
        return service;
    }

    @Bean
    public SystemEventSearchService systemEventSearchService() {
        SolrSystemEventDaoImpl dao = new SolrSystemEventDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        SolrSystemEventSearchServiceImpl service = new SolrSystemEventSearchServiceImpl(dao);
        service.setSolrPassword(this.solrPassword);
        service.setSolrUsername(this.solrUsername);
        return service;
    }

    @Bean
    public EmailNotificationDetailsService emailNotificationDetailsService() {
        SolrEmailNotificationDetailsDaoImpl dao = new SolrEmailNotificationDetailsDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        SolrEmailNotificationDetailsServiceImpl service = new SolrEmailNotificationDetailsServiceImpl(dao);
        service.setSolrPassword(this.solrPassword);
        service.setSolrUsername(this.solrUsername);
        return service;
    }

    @Bean
    public NotificationSendAuditService notificationSendAuditService() {
        SolrNotificationSendAuditDaoImpl dao = new SolrNotificationSendAuditDaoImpl();
        initializeDao(dao, this.solrRetentionDays);

        SolrNotificationSendAuditServiceImpl service = new SolrNotificationSendAuditServiceImpl(dao);
        service.setSolrPassword(this.solrPassword);
        service.setSolrUsername(this.solrUsername);
        return service;
    }

    @Bean
    public SchedulerJobService solrSchedulerJobService(SolrFileEventDrivenJobDaoImpl fileEventDrivenJobDao
        , @Qualifier("internalEventDrivenJobRecordDao") SolrInternalEventDrivenJobDaoImpl internalEventDrivenJobDao, SolrQuartzScheduleDrivenJobDaoImpl quartzScheduleDrivenJobDao
        , SolrGlobalEventJobDaoImpl globalEventJobRecordDao, SolrContextStartJobDaoImpl contextStartJobDao
        , SolrContextTerminalJobDaoImpl contextTerminalJobDao, SolrSchedulerJobDaoImpl schedulerJobDao
        , @Qualifier("internalEventDrivenJobTemplateDao") SolrInternalEventDrivenJobTemplateDaoImpl internalEventDrivenJobTemplateDao) {
        return new SolrSchedulerJobServiceImpl(fileEventDrivenJobDao
            , internalEventDrivenJobDao, quartzScheduleDrivenJobDao
            , globalEventJobRecordDao, contextStartJobDao, contextTerminalJobDao, schedulerJobDao
            , internalEventDrivenJobTemplateDao);
    }

    @Bean
    public SchedulerJobInstanceService schedulerJobInstanceService(SolrSchedulerJobDaoImpl schedulerJobDao,
                                                                   ScheduledContextInstanceAuditAggregateDao solrScheduledContextInstanceAuditAggregateDao,
                                                                   ScheduledContextInstanceService scheduledContextInstanceService) {
        SolrSchedulerJobInstanceDaoImpl scheduledContextInstanceDao = new SolrSchedulerJobInstanceDaoImpl();
        initializeDao(scheduledContextInstanceDao, this.solrSchedulerInstanceRetentionDays);

        return new SolrSchedulerJobInstanceServiceImpl(scheduledContextInstanceDao
            , (SolrScheduledContextInstanceAuditAggregateDaoImpl) solrScheduledContextInstanceAuditAggregateDao
            , schedulerJobDao, scheduledContextInstanceService, this.schedulerJobExecutionEnvironmentLabel
            , this.useLegacyJobStatusCount);
    }

    @Bean
    public InternalEventDrivenJobService internalEventDrivenJobService(@Qualifier("internalEventDrivenJobRecordDao") InternalEventDrivenJobDao internalEventDrivenJobDao) {
        return new SolrInternalEventDrivenJobRecordServiceImpl(internalEventDrivenJobDao);
    }

    @Bean
    public ContextProfileService contextProfileService() {
        SolrContextProfileDaoImpl solrContextProfileDao = new SolrContextProfileDaoImpl();
        initializeDao(solrContextProfileDao, SolrDaoBase.DO_NOT_EXPIRE);

        return new SolrContextProfileServiceImpl(solrContextProfileDao);
    }

    @Bean
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

    @Bean
    public SolrQuartzScheduleDrivenJobDaoImpl quartzScheduleDrivenJobRecordDao() {
        SolrQuartzScheduleDrivenJobDaoImpl dao = new SolrQuartzScheduleDrivenJobDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        return dao;
    }

    @Bean
    public SolrGlobalEventJobDaoImpl globalEventJobRecordDao() {
        SolrGlobalEventJobDaoImpl dao = new SolrGlobalEventJobDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        return dao;
    }

    @Bean
    public SolrContextStartJobDaoImpl contextStartJobDao() {
        SolrContextStartJobDaoImpl dao = new SolrContextStartJobDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        return dao;
    }

    @Bean
    public SolrContextTerminalJobDaoImpl contextTerminalJobDao() {
        SolrContextTerminalJobDaoImpl dao = new SolrContextTerminalJobDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        return dao;
    }

    @Bean
    public SolrSchedulerJobDaoImpl schedulerJobRecordDao() {
        SolrSchedulerJobDaoImpl dao = new SolrSchedulerJobDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        return dao;
    }

    @Bean(name = "solrSearchService")
    public SolrGeneralServiceImpl solrSearchService()
    {
        SolrGeneralDaoImpl dao = new SolrGeneralDaoImpl();
        initializeDao(dao, this.solrRetentionDays);
        SolrGeneralServiceImpl service = new SolrGeneralServiceImpl(dao);
        service.setSolrUsername(this.solrUsername);
        service.setSolrPassword(this.solrPassword);

        return service;
    }

    @Bean("wiretapEventBatchInsert")
    public BatchInsert<WiretapEvent> solrWiretapService()
    {
        SolrWiretapDao dao = new SolrWiretapDao();
        initializeDao(dao, this.solrRetentionDays);
        SolrWiretapServiceImpl service = new SolrWiretapServiceImpl(dao);
        service.setSolrUsername(this.solrUsername);
        service.setSolrPassword(this.solrPassword);

        return service;
    }

    @Bean("errorOccurrenceBatchInsert")
    public SolrErrorReportingServiceImpl solrErrorReportingService()
    {
        SolrErrorReportingServiceDao dao = new SolrErrorReportingServiceDao();
        initializeDao(dao, this.solrRetentionDays);
        SolrErrorReportingServiceImpl service = new SolrErrorReportingServiceImpl(dao);
        service.setSolrUsername(this.solrUsername);
        service.setSolrPassword(this.solrPassword);

        return service;
    }

    @Bean("exclusionEventBatchInsert")
    public BatchInsert<ExclusionEvent> solrExclusionService()
    {
        SolrExclusionEventDao dao = new SolrExclusionEventDao();
        initializeDao(dao, this.solrRetentionDays);
        SolrExclusionServiceImpl service = new SolrExclusionServiceImpl(dao);
        service.setSolrUsername(this.solrUsername);
        service.setSolrPassword(this.solrPassword);

        return service;
    }

    @Bean("replayEventBatchInsert")
    public BatchInsert<ReplayEvent> solrReplayService()
    {
        SolrReplayDao dao = new SolrReplayDao();
        initializeDao(dao, this.solrRetentionDays);

        SolrReplayServiceImpl service = new SolrReplayServiceImpl(dao);
        service.setSolrUsername(this.solrUsername);
        service.setSolrPassword(this.solrPassword);

        return service;
    }

    @Bean("flowInvocationMetricBatchInsert")
    public BatchInsert<FlowInvocationMetric> solrMetricsBatchInsert()
    {
        SolrMetricsDao dao = new SolrMetricsDao(this.solrMetricsQueryLimit);
        initializeDao(dao, this.solrRetentionDays);

        SolrMetricsServiceImpl service = new SolrMetricsServiceImpl(dao);
        service.setSolrUsername(this.solrUsername);
        service.setSolrPassword(this.solrPassword);

        return service;
    }


    @Bean("replayAuditService")
    public BatchInsert replayAuditService()
    {
        return createSolrReplayAuditServiceImpl();
    }

    @Bean("configurationMetadataBatchInsert")
    public BatchInsert configurationMetadataBatchInsert()
    {
        return createSolrComponentConfigurationMetadataServiceImpl();
    }

    public MetricsService solrMetricsService()
    {
        SolrMetricsDao dao = new SolrMetricsDao(this.solrMetricsQueryLimit);
        initializeDao(dao, this.solrRetentionDays);

        SolrMetricsServiceImpl service = new SolrMetricsServiceImpl(dao);
        service.setSolrUsername(this.solrUsername);
        service.setSolrPassword(this.solrPassword);

        return service;
    }

    @Bean
    public HospitalAuditService hospitalAuditService()
    {
        SolrHospitalDao dao = new SolrHospitalDao();
        initializeDao(dao, this.solrRetentionDays);

        SolrHospitalServiceImpl service = new SolrHospitalServiceImpl(dao);
        service.setSolrUsername(this.solrUsername);
        service.setSolrPassword(this.solrPassword);

        return service;
    }

    @Bean("moduleMetadataBatchInsert")
    public BatchInsert moduleMetadataBatchInsert()
    {
        return this.createSolrModuleMetadataServiceImpl();
    }

    @Bean("moduleMetadataService")
    public SolrModuleMetadataServiceImpl moduleMetadataService()
    {
        return this.createSolrModuleMetadataServiceImpl();
    }

    private SolrModuleMetadataServiceImpl createSolrModuleMetadataServiceImpl()
    {
        SolrModuleMetadataDao dao = new SolrModuleMetadataDao();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);

        SolrModuleMetadataServiceImpl service = new SolrModuleMetadataServiceImpl(dao);
        service.setSolrUsername(this.solrUsername);
        service.setSolrPassword(this.solrPassword);

        return service;
    }

    @Bean
    public BusinessStreamMetaDataService businessStreamMetaDataService()
    {
        SolrBusinessStreamMetadataDao dao = new SolrBusinessStreamMetadataDao();
        initializeDao(dao, this.solrRetentionDays);

        SolrBusinessStreamMetaDataServiceImpl service = new SolrBusinessStreamMetaDataServiceImpl(dao);
        service.setSolrUsername(this.solrUsername);
        service.setSolrPassword(this.solrPassword);

        return service;
    }

    @Bean
    public BatchInsert systemEventBatchInsert()
    {
        return this.createSolrSystemEventServiceImpl();
    }

    @Bean
    public SolrComponentConfigurationMetadataServiceImpl configurationMetadataService()
    {
        return createSolrComponentConfigurationMetadataServiceImpl();
    }

    private SolrReplayAuditServiceImpl createSolrReplayAuditServiceImpl()
    {
        SolrReplayAuditDao dao = new SolrReplayAuditDao();
        initializeDao(dao, this.solrRetentionDays);

        SolrReplayAuditServiceImpl service = new SolrReplayAuditServiceImpl(dao);
        service.setSolrUsername(this.solrUsername);
        service.setSolrPassword(this.solrPassword);

        return service;
    }

    private SolrComponentConfigurationMetadataServiceImpl createSolrComponentConfigurationMetadataServiceImpl()
    {
        SolrComponentConfigurationMetadataDao dao = new SolrComponentConfigurationMetadataDao();
        initializeDao(dao, this.solrRetentionDays);

        SolrComponentConfigurationMetadataServiceImpl service = new SolrComponentConfigurationMetadataServiceImpl(dao);
        service.setSolrUsername(this.solrUsername);
        service.setSolrPassword(this.solrPassword);

        return service;
    }

    private SolrSystemEventServiceImpl createSolrSystemEventServiceImpl()
    {
        SolrSystemEventDaoImpl dao = new SolrSystemEventDaoImpl();
        initializeDao(dao, this.solrRetentionDays);

        SolrSystemEventServiceImpl service = new SolrSystemEventServiceImpl(dao);
        service.setSolrUsername(this.solrUsername);
        service.setSolrPassword(this.solrPassword);

        return service;
    }

    @Bean
    public SecurityService securityService(SolrSecurityDaoImpl solrSecurityDao)
    {
        return new SecurityServiceImpl(solrSecurityDao);
    }

    @Bean
    public UserService userService(SolrUserDaoImpl solrUserDao, SecurityService securityService)
    {
        return new UserServiceImpl(solrUserDao, securityService, passwordEncoder(), false);
    }

    @Bean
    public SolrAuthenticationMethodDaoImpl solrAuthenticationMethodDao() {
        SolrAuthenticationMethodDaoImpl dao = new SolrAuthenticationMethodDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);
        return dao;
    }

    @Bean
    public SolrPolicyDaoImpl solrPolicyDao() {
        SolrPolicyDaoImpl dao = new SolrPolicyDaoImpl();
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);
        return dao;
    }

    @Bean
    public SolrRoleDaoImpl solrRoleDao(SolrPolicyDaoImpl solrPolicyDao) {
        SolrRoleDaoImpl dao = new SolrRoleDaoImpl(solrPolicyDao);
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);
        return dao;
    }

    @Bean
    public SolrIkasanPrincipalDaoImpl solrIkasanPrincipalDao(SolrRoleDaoImpl solrRoleDao) {
        SolrIkasanPrincipalDaoImpl dao = new SolrIkasanPrincipalDaoImpl(solrRoleDao);
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);
        return dao;
    }

    @Bean
    public SolrUserDaoImpl solrUserDao(SolrIkasanPrincipalDaoImpl solrIkasanPrincipalDao) {
        SolrUserDaoImpl dao = new SolrUserDaoImpl(solrIkasanPrincipalDao);
        initializeDao(dao, SolrDaoBase.DO_NOT_EXPIRE);
        return dao;
    }

    @Bean
    public SolrSecurityDaoImpl solrSecurityDao(SolrIkasanPrincipalDaoImpl solrIkasanPrincipalDao,
                                                SolrPolicyDaoImpl solrPolicyDao,
                                                SolrRoleDaoImpl solrRoleDao,
                                                SolrAuthenticationMethodDaoImpl solrAuthenticationMethodDao,
                                                SolrUserDaoImpl solrUserDao) {
        return new SolrSecurityDaoImpl(solrIkasanPrincipalDao, solrPolicyDao, solrRoleDao,
            solrAuthenticationMethodDao, solrUserDao);
    }

    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
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