package org.ikasan.esb.service;

import org.ikasan.esb.service.business.stream.metadata.BusinessStreamMetaDataServiceImpl;
import org.ikasan.esb.service.configuration.metadata.ConfigurationMetadataServiceImpl;
import org.ikasan.esb.service.error.reporting.ErrorReportingServiceImpl;
import org.ikasan.esb.service.exclusion.ExclusionServiceImpl;
import org.ikasan.esb.service.hospital.HospitalServiceImpl;
import org.ikasan.esb.service.metrics.MetricsServiceImpl;
import org.ikasan.esb.service.module.metadata.ModuleMetaDataServiceImpl;
import org.ikasan.esb.service.replay.ReplayAuditServiceImpl;
import org.ikasan.esb.service.replay.ReplayServiceImpl;
import org.ikasan.esb.service.systemevent.SystemEventSearchServiceImpl;
import org.ikasan.esb.service.systemevent.SystemEventServiceImpl;
import org.ikasan.esb.service.wiretap.WiretapServiceImpl;
import org.ikasan.spec.entity.EsbEntityDao;
import org.ikasan.spec.entity.EsbEntityService;
import org.ikasan.spec.error.reporting.ErrorOccurrence;
import org.ikasan.spec.exclusion.ExclusionEvent;
import org.ikasan.spec.history.FlowInvocationMetric;
import org.ikasan.spec.hospital.model.ExclusionEventAction;
import org.ikasan.spec.hospital.service.HospitalAuditService;
import org.ikasan.spec.metadata.dao.BusinessStreamMetadataDao;
import org.ikasan.spec.metadata.dao.ComponentConfigurationMetadataDao;
import org.ikasan.spec.metadata.dao.ModuleMetadataDao;
import org.ikasan.spec.metadata.service.BusinessStreamMetaDataService;
import org.ikasan.spec.metadata.service.ConfigurationMetaDataService;
import org.ikasan.spec.metadata.service.ModuleMetaDataService;
import org.ikasan.spec.metrics.MetricsDao;
import org.ikasan.spec.metrics.MetricsService;
import org.ikasan.spec.module.ModuleService;
import org.ikasan.spec.persistence.BatchInsert;
import org.ikasan.spec.replay.ReplayAuditEvent;
import org.ikasan.spec.replay.ReplayEvent;
import org.ikasan.spec.systemevent.SystemEvent;
import org.ikasan.spec.systemevent.SystemEventSearchDao;
import org.ikasan.spec.systemevent.SystemEventSearchService;
import org.ikasan.spec.wiretap.WiretapEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EsbServiceAutoConfiguration {

    @Bean
    public BusinessStreamMetaDataService businessStreamMetaDataService(BusinessStreamMetadataDao businessStreamMetadataDao) {
        return new BusinessStreamMetaDataServiceImpl(businessStreamMetadataDao);
    }

    @Bean("componentConfigurationMetadataEntityService")
    public ConfigurationMetaDataService componentConfigurationMetadataEntityService(ComponentConfigurationMetadataDao componentConfigurationMetadataDao) {
        return new ConfigurationMetadataServiceImpl(componentConfigurationMetadataDao);
    }

    @Bean("configurationMetadataBatchInsert")
    public BatchInsert configurationMetadataBatchInsert(ComponentConfigurationMetadataDao componentConfigurationMetadataDao) {
        return new ConfigurationMetadataServiceImpl(componentConfigurationMetadataDao);
    }

    @Bean
    public EsbEntityService<ErrorOccurrence> errorReportingEntityService(@Qualifier("errorReportingServiceEsbEntityDao") EsbEntityDao<ErrorOccurrence> errorReportingServiceDao) {
        return new ErrorReportingServiceImpl(errorReportingServiceDao);
    }

    @Bean("errorOccurrenceBatchInsert")
    public BatchInsert errorOccurrenceBatchInsert(@Qualifier("errorReportingServiceEsbEntityDao") EsbEntityDao<ErrorOccurrence> errorReportingServiceDao) {
        return new ErrorReportingServiceImpl(errorReportingServiceDao);
    }

    @Bean("exclusionEntityService")
    public EsbEntityService<ExclusionEvent> exclusionEntityService(@Qualifier("exclusionEventEntityDao")EsbEntityDao<ExclusionEvent> exclusionEventDao) {
        return new ExclusionServiceImpl(exclusionEventDao);
    }

    @Bean("exclusionEventBatchInsert")
    public BatchInsert<ExclusionEvent> exclusionEventBatchInsert(@Qualifier("exclusionEventEntityDao")EsbEntityDao<ExclusionEvent> exclusionEventDao) {
        return new ExclusionServiceImpl(exclusionEventDao);
    }

    @Bean("hospitalEntityService")
    public EsbEntityService<ExclusionEventAction> hospitalEntityService(@Qualifier("hospitalEntityDao") EsbEntityDao<ExclusionEventAction> hospitalDao) {
        return new HospitalServiceImpl(hospitalDao);
    }

    @Bean("hospitalAuditService")
    public HospitalAuditService hospitalAuditService(@Qualifier("hospitalEntityDao") EsbEntityDao<ExclusionEventAction> hospitalDao) {
        return new HospitalServiceImpl(hospitalDao);
    }

    @Bean("metricsEntityService")
    public MetricsService metricsEntityService(MetricsDao metricsDao) {
        return new MetricsServiceImpl(metricsDao);
    }

    @Bean("flowInvocationMetricBatchInsert")
    public BatchInsert<FlowInvocationMetric> flowInvocationMetricBatchInsert(MetricsDao metricsDao) {
        return new MetricsServiceImpl(metricsDao);
    }

    @Bean(name = {"moduleMetadataEntityService", "moduleMetadataService"})
    public ModuleMetaDataService moduleMetadataEntityService(ModuleMetadataDao moduleMetadataDao) {
        return new ModuleMetaDataServiceImpl(moduleMetadataDao);
    }

    @Bean("moduleMetadataBatchInsert")
    public BatchInsert moduleMetadataBatchInsert(ModuleMetadataDao moduleMetadataDao) {
        return new ModuleMetaDataServiceImpl(moduleMetadataDao);
    }

    @Bean("replayAuditService")
    public BatchInsert replayAuditService(@Qualifier("replayAuditEntityDao") EsbEntityDao<ReplayAuditEvent> replayAuditDao) {
        return new ReplayAuditServiceImpl(replayAuditDao);
    }

    @Bean("replayEntityService")
    public EsbEntityService<ReplayEvent> replayEntityService(@Qualifier("replayEntityDao") EsbEntityDao<ReplayEvent> replayDao) {
        return new ReplayServiceImpl(replayDao);
    }

    @Bean("replayEventBatchInsert")
    public BatchInsert<ReplayEvent> solrReplayService(@Qualifier("replayEntityDao") EsbEntityDao<ReplayEvent> replayDao) {
        return new ReplayServiceImpl(replayDao);
    }

    @Bean
    public SystemEventSearchService systemEventSearchService(@Qualifier("systemEventEntityDao") SystemEventSearchDao systemEventSearchDao) {
        return new SystemEventSearchServiceImpl(systemEventSearchDao);
    }

    @Bean("systemEventEntityService")
    public EsbEntityService<SystemEvent> systemEventEntityService(@Qualifier("systemEventEntityDao") EsbEntityDao<SystemEvent> systemEventDao) {
        return new SystemEventServiceImpl(systemEventDao);
    }

    @Bean("systemEventBatchInsert")
    public BatchInsert systemEventBatchInsertService(@Qualifier("systemEventEntityDao") EsbEntityDao<SystemEvent> systemEventDao) {
        return new SystemEventServiceImpl(systemEventDao);
    }

    @Bean("wiretapEventBatchInsert")
    public EsbEntityService<WiretapEvent> wiretapService(@Qualifier("wiretapEsbEntityDao") EsbEntityDao wiretapEsbEntityDao
        , ModuleService moduleService) {
        return new WiretapServiceImpl(wiretapEsbEntityDao, moduleService);
    }
}
