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
import org.ikasan.scheduled.instance.model.SolrContextInstanceAggregateJobStatusImpl;
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
import org.ikasan.solr.dao.SolrGeneralDaoImpl;
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
import org.ikasan.spec.solr.SolrDaoBase;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.ikasan.spec.wiretap.WiretapEvent;
import org.ikasan.systemevent.dao.SolrSystemEventDaoImpl;
import org.ikasan.systemevent.service.SolrSystemEventSearchServiceImpl;
import org.ikasan.systemevent.service.SolrSystemEventServiceImpl;
import org.ikasan.wiretap.dao.SolrWiretapDao;
import org.ikasan.wiretap.service.SolrWiretapServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class SolrClientAutoConfiguration {

    @Value("${solr.url}")
    private String solrUrl;

    @Value("${solr.username}")
    private String solrUsername;

    @Value("${solr.password}")
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
        solrJobLockCacheDao.initStandalone(solrUrl, SolrDaoBase.DO_NOT_EXPIRE, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        solrJobLockCacheDao.setSolrUsername(solrUsername);
        solrJobLockCacheDao.setSolrPassword(solrPassword);

        SolrJobLockCacheAuditDaoImpl solrJobLockCacheAuditDao = new SolrJobLockCacheAuditDaoImpl();
        solrJobLockCacheAuditDao.initStandalone(solrUrl, solrJobLockCacheAuditRetentionDays, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        solrJobLockCacheAuditDao.setSolrUsername(solrUsername);
        solrJobLockCacheAuditDao.setSolrPassword(solrPassword);

        return new SolrJobLockCacheServiceImpl(solrJobLockCacheDao, solrJobLockCacheAuditDao, saveJobLockCacheAudits);
    }

    @Bean
    public ScheduledContextService scheduledContextService() {
        SolrScheduledContextDaoImpl solrScheduledContextDao = new SolrScheduledContextDaoImpl();
        solrScheduledContextDao.initStandalone(solrUrl, SolrDaoBase.DO_NOT_EXPIRE, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        solrScheduledContextDao.setSolrUsername(solrUsername);
        solrScheduledContextDao.setSolrPassword(solrPassword);

        SolrScheduledContextViewDaoImpl solrScheduledContextViewDao = new SolrScheduledContextViewDaoImpl();
        solrScheduledContextViewDao.initStandalone(solrUrl, SolrDaoBase.DO_NOT_EXPIRE, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        solrScheduledContextViewDao.setSolrUsername(solrUsername);
        solrScheduledContextViewDao.setSolrPassword(solrPassword);

        return new SolrScheduledContextServiceImpl(solrScheduledContextDao, solrScheduledContextViewDao);
    }

    @Bean("scheduledProcessEventBatchInsert")
    public SolrScheduledProcessServiceImpl solrScheduledProcessEventService()
    {
        SolrScheduledProcessEventDao dao = new SolrScheduledProcessEventDao();
        dao.initStandalone(solrUrl, 30, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        SolrModuleMetadataDao solrModuleMetadataDao = new SolrModuleMetadataDao();
        solrModuleMetadataDao.initStandalone(solrUrl, 30, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        solrModuleMetadataDao.setSolrUsername(solrUsername);
        solrModuleMetadataDao.setSolrPassword(solrPassword);

        SolrComponentConfigurationMetadataDao solrComponentConfigurationMetadataDao = new SolrComponentConfigurationMetadataDao();
        solrComponentConfigurationMetadataDao.initStandalone(solrUrl, 30, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        solrComponentConfigurationMetadataDao.setSolrUsername(solrUsername);
        solrComponentConfigurationMetadataDao.setSolrPassword(solrPassword);

        SolrBusinessStreamMetadataDao solrBusinessStreamMetadataDao = new SolrBusinessStreamMetadataDao();
        solrBusinessStreamMetadataDao.initStandalone(solrUrl, 30, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        solrBusinessStreamMetadataDao.setSolrUsername(solrUsername);
        solrBusinessStreamMetadataDao.setSolrPassword(solrPassword);

        SolrScheduledProcessServiceImpl service = new SolrScheduledProcessServiceImpl(dao, solrModuleMetadataDao
            , solrComponentConfigurationMetadataDao, solrBusinessStreamMetadataDao, notifyBatchInsertListeners);
        service.setSolrUsername(solrUsername);
        service.setSolrPassword(solrPassword);

        return service;
    }

    @Bean
    public ScheduledContextInstanceService scheduledContextInstanceService(ScheduledContextInstanceAuditAggregateDao scheduledContextInstanceAuditAggregateDao) {
        SolrScheduledContextInstanceDaoImpl scheduledContextInstanceDao = new SolrScheduledContextInstanceDaoImpl();
        scheduledContextInstanceDao.initStandalone(solrUrl, solrSchedulerInstanceRetentionDays, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        scheduledContextInstanceDao.setSolrUsername(solrUsername);
        scheduledContextInstanceDao.setSolrPassword(solrPassword);

        SolrScheduledContextInstanceAuditDaoImpl scheduledContextInstanceAuditDao = new SolrScheduledContextInstanceAuditDaoImpl();
        scheduledContextInstanceAuditDao.initStandalone(solrUrl, solrSchedulerInstanceRetentionDays, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        scheduledContextInstanceAuditDao.setSolrUsername(solrUsername);
        scheduledContextInstanceAuditDao.setSolrPassword(solrPassword);

        return new SolrScheduledContextInstanceServiceImpl(scheduledContextInstanceDao, scheduledContextInstanceAuditDao, scheduledContextInstanceAuditAggregateDao
            , this.saveContextInstanceAuditRecords, this.saveContextInstanceAuditDeltaRecords);
    }

    @Bean
    public ScheduledContextInstanceAuditAggregateDao scheduledContextInstanceAuditAggregateDao() {
        SolrScheduledContextInstanceAuditAggregateDaoImpl scheduledContextInstanceAuditAggregateDao = new SolrScheduledContextInstanceAuditAggregateDaoImpl();
        scheduledContextInstanceAuditAggregateDao.initStandalone(solrUrl, solrSchedulerInstanceRetentionDays, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        scheduledContextInstanceAuditAggregateDao.setSolrUsername(solrUsername);
        scheduledContextInstanceAuditAggregateDao.setSolrPassword(solrPassword);

        return scheduledContextInstanceAuditAggregateDao;
    }

    @Bean
    public EmailNotificationContextService emailNotificationContextService() {
        SolrEmailNotificationContextDaoImpl dao = new SolrEmailNotificationContextDaoImpl();
        dao.initStandalone(solrUrl, SolrDaoBase.DO_NOT_EXPIRE, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        SolrEmailNotificationContextServiceImpl service = new SolrEmailNotificationContextServiceImpl(dao);
        service.setSolrPassword(solrPassword);
        service.setSolrUsername(solrUsername);
        return service;
    }

    @Bean
    public SystemEventSearchService systemEventSearchService() {
        SolrSystemEventDaoImpl dao = new SolrSystemEventDaoImpl();
        dao.initStandalone(solrUrl, SolrDaoBase.DO_NOT_EXPIRE, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        SolrSystemEventSearchServiceImpl service = new SolrSystemEventSearchServiceImpl(dao);
        service.setSolrPassword(solrPassword);
        service.setSolrUsername(solrUsername);
        return service;
    }

    @Bean
    public EmailNotificationDetailsService emailNotificationDetailsService() {
        SolrEmailNotificationDetailsDaoImpl dao = new SolrEmailNotificationDetailsDaoImpl();
        dao.initStandalone(solrUrl, SolrDaoBase.DO_NOT_EXPIRE, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        SolrEmailNotificationDetailsServiceImpl service = new SolrEmailNotificationDetailsServiceImpl(dao);
        service.setSolrPassword(solrPassword);
        service.setSolrUsername(solrUsername);
        return service;
    }

    @Bean
    public NotificationSendAuditService notificationSendAuditService() {
        SolrNotificationSendAuditDaoImpl dao = new SolrNotificationSendAuditDaoImpl();
        dao.initStandalone(solrUrl, solrRetentionDays, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        SolrNotificationSendAuditServiceImpl service = new SolrNotificationSendAuditServiceImpl(dao);
        service.setSolrPassword(solrPassword);
        service.setSolrUsername(solrUsername);
        return service;
    }

    @Bean
    public SchedulerJobService solrSchedulerJobService(SolrFileEventDrivenJobDaoImpl fileEventDrivenJobDao
        , SolrInternalEventDrivenJobDaoImpl internalEventDrivenJobDao, SolrQuartzScheduleDrivenJobDaoImpl quartzScheduleDrivenJobDao
        , SolrGlobalEventJobDaoImpl globalEventJobRecordDao, SolrSchedulerJobDaoImpl schedulerJobDao) {
        return new SolrSchedulerJobServiceImpl(fileEventDrivenJobDao
            ,internalEventDrivenJobDao, quartzScheduleDrivenJobDao
            ,globalEventJobRecordDao, schedulerJobDao);
    }

    @Bean
    public SchedulerJobInstanceService schedulerJobInstanceService(SolrSchedulerJobDaoImpl schedulerJobDao,
                                                                   ScheduledContextInstanceAuditAggregateDao solrScheduledContextInstanceAuditAggregateDao,
                                                                   ScheduledContextInstanceService scheduledContextInstanceService) {
        SolrSchedulerJobInstanceDaoImpl scheduledContextInstanceDao = new SolrSchedulerJobInstanceDaoImpl();
        scheduledContextInstanceDao.initStandalone(solrUrl, solrSchedulerInstanceRetentionDays, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        scheduledContextInstanceDao.setSolrUsername(solrUsername);
        scheduledContextInstanceDao.setSolrPassword(solrPassword);

        return new SolrSchedulerJobInstanceServiceImpl(scheduledContextInstanceDao
            , (SolrScheduledContextInstanceAuditAggregateDaoImpl) solrScheduledContextInstanceAuditAggregateDao
            , schedulerJobDao, scheduledContextInstanceService, schedulerJobExecutionEnvironmentLabel
            , useLegacyJobStatusCount);
    }

    @Bean
    public InternalEventDrivenJobService internalEventDrivenJobService(InternalEventDrivenJobDao internalEventDrivenJobDao) {
        return new SolrInternalEventDrivenJobRecordServiceImpl(internalEventDrivenJobDao);
    }

    @Bean
    public ContextProfileService contextProfileService() {
        SolrContextProfileDaoImpl solrContextProfileDao = new SolrContextProfileDaoImpl();
        solrContextProfileDao.initStandalone(solrUrl, SolrDaoBase.DO_NOT_EXPIRE, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        solrContextProfileDao.setSolrUsername(solrUsername);
        solrContextProfileDao.setSolrPassword(solrPassword);

        return new SolrContextProfileServiceImpl(solrContextProfileDao);
    }

    @Bean
    public SolrFileEventDrivenJobDaoImpl fileEventDrivenJobRecordDao() {
        SolrFileEventDrivenJobDaoImpl dao = new SolrFileEventDrivenJobDaoImpl();
        dao.initStandalone(solrUrl, SolrDaoBase.DO_NOT_EXPIRE, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        return dao;
    }

    @Bean
    public SolrInternalEventDrivenJobDaoImpl internalEventDrivenJobRecordDao() {
        SolrInternalEventDrivenJobDaoImpl dao = new SolrInternalEventDrivenJobDaoImpl();
        dao.initStandalone(solrUrl, SolrDaoBase.DO_NOT_EXPIRE, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        return dao;
    }

    @Bean
    public SolrQuartzScheduleDrivenJobDaoImpl quartzScheduleDrivenJobRecordDao() {
        SolrQuartzScheduleDrivenJobDaoImpl dao = new SolrQuartzScheduleDrivenJobDaoImpl();
        dao.initStandalone(solrUrl, SolrDaoBase.DO_NOT_EXPIRE, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        return dao;
    }

    @Bean
    public SolrGlobalEventJobDaoImpl globalEventJobRecordDao() {
        SolrGlobalEventJobDaoImpl dao = new SolrGlobalEventJobDaoImpl();
        dao.initStandalone(solrUrl, SolrDaoBase.DO_NOT_EXPIRE, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        return dao;
    }

    @Bean
    public SolrSchedulerJobDaoImpl schedulerJobRecordDao() {
        SolrSchedulerJobDaoImpl dao = new SolrSchedulerJobDaoImpl();
        dao.initStandalone(solrUrl, SolrDaoBase.DO_NOT_EXPIRE, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        dao.setSolrUsername(solrUsername);
        dao.setSolrPassword(solrPassword);

        return dao;
    }

    @Bean(name = "solrSearchService")
    public SolrGeneralServiceImpl solrSearchService()
    {
        SolrGeneralDaoImpl dao = new SolrGeneralDaoImpl();
        dao.initStandalone(solrUrl, solrRetentionDays, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        SolrGeneralServiceImpl service = new SolrGeneralServiceImpl(dao);
        service.setSolrUsername(solrUsername);
        service.setSolrPassword(solrPassword);

        return service;
    }

    @Bean("wiretapEventBatchInsert")
    public BatchInsert<WiretapEvent> solrWiretapService()
    {
        SolrWiretapDao dao = new SolrWiretapDao();
        dao.initStandalone(solrUrl, solrRetentionDays, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        SolrWiretapServiceImpl service = new SolrWiretapServiceImpl(dao);
        service.setSolrUsername(solrUsername);
        service.setSolrPassword(solrPassword);

        return service;
    }

    @Bean("errorOccurrenceBatchInsert")
    public SolrErrorReportingServiceImpl solrErrorReportingService()
    {
        SolrErrorReportingServiceDao dao = new SolrErrorReportingServiceDao();
        dao.initStandalone(solrUrl, solrRetentionDays, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        SolrErrorReportingServiceImpl service = new SolrErrorReportingServiceImpl(dao);
        service.setSolrUsername(solrUsername);
        service.setSolrPassword(solrPassword);

        return service;
    }

    @Bean("exclusionEventBatchInsert")
    public BatchInsert<ExclusionEvent> solrExclusionService()
    {
        SolrExclusionEventDao dao = new SolrExclusionEventDao();
        dao.initStandalone(solrUrl, solrRetentionDays, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);
        SolrExclusionServiceImpl service = new SolrExclusionServiceImpl(dao);
        service.setSolrUsername(solrUsername);
        service.setSolrPassword(solrPassword);

        return service;
    }

    @Bean("replayEventBatchInsert")
    public BatchInsert<ReplayEvent> solrReplayService()
    {
        SolrReplayDao dao = new SolrReplayDao();
        dao.initStandalone(solrUrl, solrRetentionDays, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);

        SolrReplayServiceImpl service = new SolrReplayServiceImpl(dao);
        service.setSolrUsername(solrUsername);
        service.setSolrPassword(solrPassword);

        return service;
    }

    @Bean("flowInvocationMetricBatchInsert")
    public BatchInsert<FlowInvocationMetric> solrMetricsBatchInsert()
    {
        SolrMetricsDao dao = new SolrMetricsDao(this.solrMetricsQueryLimit);
        dao.initStandalone(solrUrl, solrRetentionDays, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);

        SolrMetricsServiceImpl service = new SolrMetricsServiceImpl(dao);
        service.setSolrUsername(solrUsername);
        service.setSolrPassword(solrPassword);

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
        dao.initStandalone(solrUrl, solrRetentionDays, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);

        SolrMetricsServiceImpl service = new SolrMetricsServiceImpl(dao);
        service.setSolrUsername(solrUsername);
        service.setSolrPassword(solrPassword);

        return service;
    }

    @Bean
    public HospitalAuditService hospitalAuditService()
    {
        SolrHospitalDao dao = new SolrHospitalDao();
        dao.initStandalone(solrUrl, solrRetentionDays, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);

        SolrHospitalServiceImpl service = new SolrHospitalServiceImpl(dao);
        service.setSolrUsername(solrUsername);
        service.setSolrPassword(solrPassword);

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
        dao.initStandalone(solrUrl, SolrDaoBase.DO_NOT_EXPIRE, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);

        SolrModuleMetadataServiceImpl service = new SolrModuleMetadataServiceImpl(dao);
        service.setSolrUsername(solrUsername);
        service.setSolrPassword(solrPassword);

        return service;
    }

    @Bean
    public BusinessStreamMetaDataService businessStreamMetaDataService()
    {
        SolrBusinessStreamMetadataDao dao = new SolrBusinessStreamMetadataDao();
        dao.initStandalone(solrUrl, solrRetentionDays, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);

        SolrBusinessStreamMetaDataServiceImpl service = new SolrBusinessStreamMetaDataServiceImpl(dao);
        service.setSolrUsername(solrUsername);
        service.setSolrPassword(solrPassword);

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
        dao.initStandalone(solrUrl, solrRetentionDays, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);

        SolrReplayAuditServiceImpl service = new SolrReplayAuditServiceImpl(dao);
        service.setSolrUsername(solrUsername);
        service.setSolrPassword(solrPassword);

        return service;
    }

    private SolrComponentConfigurationMetadataServiceImpl createSolrComponentConfigurationMetadataServiceImpl()
    {
        SolrComponentConfigurationMetadataDao dao = new SolrComponentConfigurationMetadataDao();
        dao.initStandalone(solrUrl, solrRetentionDays, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);

        SolrComponentConfigurationMetadataServiceImpl service = new SolrComponentConfigurationMetadataServiceImpl(dao);
        service.setSolrUsername(solrUsername);
        service.setSolrPassword(solrPassword);

        return service;
    }

    private SolrSystemEventServiceImpl createSolrSystemEventServiceImpl()
    {
        SolrSystemEventDaoImpl dao = new SolrSystemEventDaoImpl();
        dao.initStandalone(solrUrl, solrRetentionDays, solrSocketTimeoutMilli,
            solrConnectionTimeoutMilli);

        SolrSystemEventServiceImpl service = new SolrSystemEventServiceImpl(dao);
        service.setSolrUsername(solrUsername);
        service.setSolrPassword(solrPassword);

        return service;
    }
}