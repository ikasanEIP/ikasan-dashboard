package org.ikasan.data.synchronisation;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.ikasan.business.stream.metadata.dao.SolrBusinessStreamMetadataDaoImpl;
import org.ikasan.configuration.metadata.dao.SolrComponentConfigurationMetadataDaoImpl;
import org.ikasan.error.reporting.dao.SolrErrorReportingServiceDaoImpl;
import org.ikasan.exclusion.dao.SolrExclusionEventDao;
import org.ikasan.hospital.dao.SolrHospitalDao;
import org.ikasan.metrics.dao.SolrMetricsDaoImpl;
import org.ikasan.module.metadata.dao.SolrModuleMetadataDao;
import org.ikasan.replay.dao.SolrReplayDao;
import org.ikasan.scheduled.context.dao.SolrScheduledContextDaoImpl;
import org.ikasan.scheduled.context.dao.SolrScheduledContextViewDaoImpl;
import org.ikasan.scheduled.event.dao.SolrScheduledProcessEventDao;
import org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceAuditAggregateDaoImpl;
import org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceAuditDaoImpl;
import org.ikasan.scheduled.instance.dao.SolrScheduledContextInstanceDaoImpl;
import org.ikasan.scheduled.instance.dao.SolrSchedulerJobInstanceDaoImpl;
import org.ikasan.scheduled.job.dao.SolrBridgingJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrContextStartJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrContextTerminalJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrFileEventDrivenJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrGlobalEventJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrInternalEventDrivenJobDaoImpl;
import org.ikasan.scheduled.job.dao.SolrInternalEventDrivenJobTemplateDaoImpl;
import org.ikasan.scheduled.job.dao.SolrQuartzScheduleDrivenJobDaoImpl;
import org.ikasan.scheduled.joblock.dao.SolrJobLockCacheAuditDaoImpl;
import org.ikasan.scheduled.joblock.dao.SolrJobLockCacheDaoImpl;
import org.ikasan.scheduled.notification.dao.SolrEmailNotificationContextDaoImpl;
import org.ikasan.scheduled.notification.dao.SolrEmailNotificationDetailsDaoImpl;
import org.ikasan.scheduled.notification.dao.SolrNotificationSendAuditDaoImpl;
import org.ikasan.scheduled.profile.dao.SolrContextProfileDaoImpl;
import org.ikasan.security.dao.SolrAuthenticationMethodDaoImpl;
import org.ikasan.security.dao.SolrIkasanPrincipalDaoImpl;
import org.ikasan.security.dao.SolrPolicyDaoImpl;
import org.ikasan.security.dao.SolrRoleDaoImpl;
import org.ikasan.security.dao.SolrUserDaoImpl;
import org.ikasan.setup.dao.SolrSetupDaoImpl;
import org.ikasan.spec.solr.SolrDaoBase;
import org.ikasan.systemevent.dao.SolrSystemEventDaoImpl;
import org.ikasan.wiretap.dao.SolrWiretapDao;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * The Solr DAOs the synchronisation writes through.
 *
 * <p>They are deliberately not Spring beans: {@code SolrClientAutoConfiguration} declares beans with the same names
 * as {@code MongoPersistenceAutoConfiguration}, so it cannot be loaded alongside it. Each DAO gets the retention
 * {@code SolrClientAutoConfiguration} gives it, and all of them share one {@link SolrClient}.
 */
public class SolrDaos implements AutoCloseable {

    private final SolrClient solrClient;
    private final SolrSettings settings;

    final SolrJobLockCacheDaoImpl jobLockCache = new SolrJobLockCacheDaoImpl();
    final SolrJobLockCacheAuditDaoImpl jobLockCacheAudit = new SolrJobLockCacheAuditDaoImpl();
    final SolrScheduledContextDaoImpl scheduledContext = new SolrScheduledContextDaoImpl();
    final SolrScheduledContextViewDaoImpl scheduledContextView = new SolrScheduledContextViewDaoImpl();
    final SolrScheduledContextInstanceDaoImpl scheduledContextInstance = new SolrScheduledContextInstanceDaoImpl();
    final SolrScheduledContextInstanceAuditDaoImpl scheduledContextInstanceAudit
        = new SolrScheduledContextInstanceAuditDaoImpl();
    final SolrScheduledContextInstanceAuditAggregateDaoImpl scheduledContextInstanceAuditAggregate
        = new SolrScheduledContextInstanceAuditAggregateDaoImpl();
    final SolrEmailNotificationContextDaoImpl emailNotificationContext = new SolrEmailNotificationContextDaoImpl();
    final SolrEmailNotificationDetailsDaoImpl emailNotificationDetails = new SolrEmailNotificationDetailsDaoImpl();
    final SolrNotificationSendAuditDaoImpl notificationSendAudit = new SolrNotificationSendAuditDaoImpl();
    final SolrSchedulerJobInstanceDaoImpl schedulerJobInstance = new SolrSchedulerJobInstanceDaoImpl();
    final SolrContextProfileDaoImpl contextProfile = new SolrContextProfileDaoImpl();
    final SolrFileEventDrivenJobDaoImpl fileEventDrivenJob = new SolrFileEventDrivenJobDaoImpl();
    final SolrInternalEventDrivenJobDaoImpl internalEventDrivenJob = new SolrInternalEventDrivenJobDaoImpl();
    final SolrInternalEventDrivenJobTemplateDaoImpl internalEventDrivenJobTemplate
        = new SolrInternalEventDrivenJobTemplateDaoImpl();
    final SolrQuartzScheduleDrivenJobDaoImpl quartzScheduleDrivenJob = new SolrQuartzScheduleDrivenJobDaoImpl();
    final SolrGlobalEventJobDaoImpl globalEventJob = new SolrGlobalEventJobDaoImpl();
    final SolrContextStartJobDaoImpl contextStartJob = new SolrContextStartJobDaoImpl();
    final SolrContextTerminalJobDaoImpl contextTerminalJob = new SolrContextTerminalJobDaoImpl();
    final SolrBridgingJobDaoImpl bridgingJob = new SolrBridgingJobDaoImpl();
    final SolrScheduledProcessEventDao scheduledProcessEvent = new SolrScheduledProcessEventDao();
    final SolrWiretapDao wiretap = new SolrWiretapDao();
    final SolrErrorReportingServiceDaoImpl error = new SolrErrorReportingServiceDaoImpl();
    final SolrExclusionEventDao exclusion = new SolrExclusionEventDao();
    final SolrHospitalDao hospital = new SolrHospitalDao();
    final SolrReplayDao replay = new SolrReplayDao();
    final SolrSystemEventDaoImpl systemEvent = new SolrSystemEventDaoImpl();
    final SolrMetricsDaoImpl metrics;
    final SolrModuleMetadataDao moduleMetadata = new SolrModuleMetadataDao();
    final SolrBusinessStreamMetadataDaoImpl businessStreamMetadata = new SolrBusinessStreamMetadataDaoImpl();
    final SolrComponentConfigurationMetadataDaoImpl componentConfigurationMetadata
        = new SolrComponentConfigurationMetadataDaoImpl();
    final SolrSetupDaoImpl setup = new SolrSetupDaoImpl();
    final SolrAuthenticationMethodDaoImpl authenticationMethod = new SolrAuthenticationMethodDaoImpl();
    final SolrPolicyDaoImpl policy = new SolrPolicyDaoImpl();
    final SolrRoleDaoImpl role;
    final SolrIkasanPrincipalDaoImpl principal;
    final SolrUserDaoImpl user;

    /**
     * Create the DAOs with a Solr client built from the settings.
     */
    public static SolrDaos create(SolrSettings settings) {
        return new SolrDaos(createClient(settings), settings);
    }

    /**
     * Create the DAOs around an existing client (tests pass an embedded Solr server).
     */
    public SolrDaos(SolrClient solrClient, SolrSettings settings) {
        this.solrClient = solrClient;
        this.settings = settings;

        this.metrics = new SolrMetricsDaoImpl(settings.metricsQueryLimit());
        this.role = new SolrRoleDaoImpl(policy);
        this.principal = new SolrIkasanPrincipalDaoImpl(role);
        this.user = new SolrUserDaoImpl(principal);

        // retention as configured by SolrClientAutoConfiguration
        init(jobLockCache, SolrDaoBase.DO_NOT_EXPIRE);
        init(jobLockCacheAudit, settings.jobLockCacheAuditRetentionDays());
        init(scheduledContext, SolrDaoBase.DO_NOT_EXPIRE);
        init(scheduledContextView, SolrDaoBase.DO_NOT_EXPIRE);
        init(scheduledContextInstance, settings.schedulerInstanceRetentionDays());
        init(scheduledContextInstanceAudit, settings.schedulerInstanceRetentionDays());
        init(scheduledContextInstanceAuditAggregate, settings.schedulerInstanceRetentionDays());
        init(emailNotificationContext, SolrDaoBase.DO_NOT_EXPIRE);
        init(emailNotificationDetails, SolrDaoBase.DO_NOT_EXPIRE);
        init(notificationSendAudit, settings.retentionDays());
        init(schedulerJobInstance, settings.schedulerInstanceRetentionDays());
        init(contextProfile, SolrDaoBase.DO_NOT_EXPIRE);
        init(fileEventDrivenJob, SolrDaoBase.DO_NOT_EXPIRE);
        init(internalEventDrivenJob, SolrDaoBase.DO_NOT_EXPIRE);
        init(internalEventDrivenJobTemplate, SolrDaoBase.DO_NOT_EXPIRE);
        init(quartzScheduleDrivenJob, SolrDaoBase.DO_NOT_EXPIRE);
        init(globalEventJob, SolrDaoBase.DO_NOT_EXPIRE);
        init(contextStartJob, SolrDaoBase.DO_NOT_EXPIRE);
        init(contextTerminalJob, SolrDaoBase.DO_NOT_EXPIRE);
        init(bridgingJob, SolrDaoBase.DO_NOT_EXPIRE);
        init(scheduledProcessEvent, settings.retentionDays());
        init(wiretap, settings.retentionDays());
        init(error, settings.retentionDays());
        init(exclusion, settings.retentionDays());
        init(hospital, settings.retentionDays());
        init(replay, settings.retentionDays());
        init(systemEvent, settings.retentionDays());
        init(metrics, settings.retentionDays());
        init(moduleMetadata, SolrDaoBase.DO_NOT_EXPIRE);
        init(businessStreamMetadata, settings.retentionDays());
        init(componentConfigurationMetadata, settings.retentionDays());
        init(setup, SolrDaoBase.DO_NOT_EXPIRE);
        init(authenticationMethod, SolrDaoBase.DO_NOT_EXPIRE);
        init(policy, SolrDaoBase.DO_NOT_EXPIRE);
        init(role, SolrDaoBase.DO_NOT_EXPIRE);
        init(principal, SolrDaoBase.DO_NOT_EXPIRE);
        init(user, SolrDaoBase.DO_NOT_EXPIRE);
    }

    public SolrClient solrClient() {
        return solrClient;
    }

    public SolrSettings settings() {
        return settings;
    }

    private void init(SolrDaoBase<?> dao, int daysToKeep) {
        dao.setSolrClient(solrClient);
        dao.setDaysToKeep(daysToKeep);
        dao.setSolrUsername(settings.username());
        dao.setSolrPassword(settings.password());
    }

    private static SolrClient createClient(SolrSettings settings) {
        if (settings.isCloud()) {
            if (settings.cloudZkHosts() == null || settings.cloudZkHosts().isBlank()) {
                throw new IllegalArgumentException("solr.cloud.zk.hosts must be configured when solr.mode=cloud");
            }
            CloudSolrClient client = new CloudSolrClient.Builder(Arrays.asList(settings.cloudZkHosts().split(",")),
                Optional.empty())
                .withZkConnectTimeout(settings.connectionTimeoutMillis(), TimeUnit.MILLISECONDS)
                .build();
            client.setDefaultCollection("ikasan");
            return client;
        }
        if (settings.url() == null || settings.url().isBlank()) {
            throw new IllegalArgumentException("solr.url must be configured when solr.mode=standalone");
        }
        return new Http2SolrClient.Builder(settings.url())
            .withConnectionTimeout(settings.connectionTimeoutMillis(), TimeUnit.MILLISECONDS)
            .build();
    }

    @Override
    public void close() throws IOException {
        solrClient.close();
    }
}
