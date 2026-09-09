package org.ikasan.mongo.persistence;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCompressor;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.ikasan.mongo.persistence.business.stream.metadata.dao.MongoBusinessStreamMetadataDaoImpl;
import org.ikasan.mongo.persistence.business.stream.metadata.repository.MongoBusinessStreamRepository;
import org.ikasan.mongo.persistence.configuration.metadata.dao.MongoComponentConfigurationMetadataDaoImpl;
import org.ikasan.mongo.persistence.configuration.metadata.repository.MongoComponentConfigurationMetadataRepository;
import org.ikasan.mongo.persistence.error.reporting.dao.MongoErrorReportingServiceDaoImpl;
import org.ikasan.mongo.persistence.error.reporting.repository.MongoErrorOccurrenceRepository;
import org.ikasan.mongo.persistence.exclusion.dao.MongoExclusionEventDaoImpl;
import org.ikasan.mongo.persistence.exclusion.repository.MongoExclusionEventRepository;
import org.ikasan.mongo.persistence.hospital.dao.MongoHospitalDaoImpl;
import org.ikasan.mongo.persistence.hospital.repository.MongoExclusionEventActionRepository;
import org.ikasan.mongo.persistence.metrics.dao.MongoMetricsDaoImpl;
import org.ikasan.mongo.persistence.metrics.repository.MongoFlowInvocationMetricRepository;
import org.ikasan.mongo.persistence.module.metadata.dao.MongoModuleMetadataDaoImpl;
import org.ikasan.mongo.persistence.module.metadata.repository.MongoModuleMetadataRepository;
import org.ikasan.mongo.persistence.replay.dao.MongoReplayAuditDaoImpl;
import org.ikasan.mongo.persistence.replay.dao.MongoReplayDaoImpl;
import org.ikasan.mongo.persistence.replay.repository.MongoReplayAuditEventRepository;
import org.ikasan.mongo.persistence.replay.repository.MongoReplayEventRepository;
import org.ikasan.mongo.persistence.scheduled.context.dao.MongoScheduledContextDaoImpl;
import org.ikasan.mongo.persistence.scheduled.context.dao.MongoScheduledContextViewDao;
import org.ikasan.mongo.persistence.scheduled.context.repository.MongoScheduledContextRecordRepository;
import org.ikasan.mongo.persistence.scheduled.context.repository.MongoScheduledContextViewRepository;
import org.ikasan.mongo.persistence.scheduled.profile.dao.MongoContextProfileDao;
import org.ikasan.mongo.persistence.scheduled.profile.repository.MongoContextProfileRepository;
import org.ikasan.mongo.persistence.scheduled.instance.dao.MongoScheduledContextInstanceAuditAggregateDao;
import org.ikasan.mongo.persistence.scheduled.instance.dao.MongoScheduledContextInstanceAuditDao;
import org.ikasan.mongo.persistence.scheduled.instance.dao.MongoScheduledContextInstanceDao;
import org.ikasan.mongo.persistence.scheduled.instance.dao.MongoSchedulerJobInstanceDaoImpl;
import org.ikasan.mongo.persistence.scheduled.instance.repository.MongoScheduledContextInstanceAuditAggregateRepository;
import org.ikasan.mongo.persistence.scheduled.instance.repository.MongoScheduledContextInstanceRepository;
import org.ikasan.mongo.persistence.scheduled.instance.repository.MongoSchedulerJobInstanceRecordRepository;
import org.ikasan.mongo.persistence.scheduled.job.dao.MongoBridgingJobDao;
import org.ikasan.mongo.persistence.scheduled.joblock.dao.MongoJobLockCacheAuditDao;
import org.ikasan.mongo.persistence.scheduled.joblock.dao.MongoJobLockCacheDao;
import org.ikasan.mongo.persistence.scheduled.joblock.repository.MongoJobLockCacheAuditRepository;
import org.ikasan.mongo.persistence.scheduled.joblock.repository.MongoJobLockCacheRepository;
import org.ikasan.mongo.persistence.scheduled.notification.dao.MongoEmailNotificationContextDao;
import org.ikasan.mongo.persistence.scheduled.notification.dao.MongoEmailNotificationDetailsDao;
import org.ikasan.mongo.persistence.scheduled.notification.dao.MongoNotificationSendAuditDao;
import org.ikasan.mongo.persistence.scheduled.notification.repository.MongoEmailNotificationContextRepository;
import org.ikasan.mongo.persistence.scheduled.notification.repository.MongoEmailNotificationDetailsRepository;
import org.ikasan.mongo.persistence.scheduled.notification.repository.MongoNotificationSendAuditRepository;
import org.ikasan.mongo.persistence.scheduled.job.dao.MongoContextStartJobDao;
import org.ikasan.mongo.persistence.scheduled.job.dao.MongoContextTerminalJobDao;
import org.ikasan.mongo.persistence.scheduled.job.dao.MongoFileEventDrivenJobDao;
import org.ikasan.mongo.persistence.scheduled.job.dao.MongoGlobalEventJobDao;
import org.ikasan.mongo.persistence.scheduled.job.dao.MongoInternalEventDrivenJobDao;
import org.ikasan.mongo.persistence.scheduled.job.dao.MongoInternalEventDrivenJobTemplateDao;
import org.ikasan.mongo.persistence.scheduled.job.dao.MongoQuartzScheduleDrivenJobDao;
import org.ikasan.mongo.persistence.scheduled.job.dao.MongoSchedulerJobDao;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoBridgingJobRepository;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoContextStartJobRepository;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoContextTerminalJobRepository;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoFileEventDrivenJobRepository;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoGlobalEventJobRepository;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoInternalEventDrivenJobRepository;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoQuartzScheduleDrivenJobRepository;
import org.ikasan.mongo.persistence.scheduled.job.repository.MongoSchedulerJobRepository;
import org.ikasan.mongo.persistence.security.dao.*;
import org.ikasan.mongo.persistence.security.repository.*;
import org.ikasan.mongo.persistence.systemevent.dao.MongoSystemEventDao;
import org.ikasan.mongo.persistence.systemevent.repository.MongoSystemEventRepository;
import org.ikasan.mongo.persistence.wiretap.dao.MongoWiretapDao;
import org.ikasan.mongo.persistence.wiretap.repository.MongoWiretapEventRepository;
import org.ikasan.mongo.persistence.scheduled.event.dao.MongoScheduledProcessEventDao;
import org.ikasan.mongo.persistence.scheduled.event.repository.MongoScheduledProcessEventRepository;
import org.ikasan.mongo.persistence.general.dao.MongoGeneralDaoImpl;
import org.ikasan.mongo.persistence.general.repository.MongoIkasanDocumentRepository;
import org.ikasan.mongo.persistence.general.service.MongoGeneralServiceImpl;
import org.ikasan.spec.housekeeping.HousekeepService;
import org.ikasan.spec.metadata.dao.BusinessStreamMetadataDao;
import org.ikasan.spec.metadata.dao.ComponentConfigurationMetadataDao;
import org.ikasan.spec.metadata.dao.ModuleMetadataDao;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextDao;
import org.ikasan.spec.scheduled.context.dao.ScheduledContextViewDao;
import org.ikasan.spec.scheduled.profile.dao.ContextProfileDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditAggregateDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceAuditDao;
import org.ikasan.spec.scheduled.instance.dao.ScheduledContextInstanceDao;
import org.ikasan.spec.scheduled.instance.dao.SchedulerJobInstanceDao;
import org.ikasan.spec.scheduled.job.dao.BridgingJobDao;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheAuditDao;
import org.ikasan.spec.scheduled.joblock.dao.JobLockCacheDao;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationContextDao;
import org.ikasan.spec.scheduled.notification.dao.EmailNotificationDetailsDao;
import org.ikasan.spec.scheduled.notification.dao.NotificationSendAuditDao;
import org.ikasan.spec.scheduled.job.dao.ContextStartJobDao;
import org.ikasan.spec.scheduled.job.dao.ContextTerminalJobDao;
import org.ikasan.spec.scheduled.job.dao.FileEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.dao.GlobalEventJobDao;
import org.ikasan.spec.scheduled.job.dao.InternalEventDrivenJobDao;
import org.ikasan.spec.scheduled.job.dao.QuartzScheduleDrivenJobDao;
import org.ikasan.spec.scheduled.job.dao.SchedulerJobDao;
import org.ikasan.spec.security.dao.SecurityDao;
import org.ikasan.spec.security.dao.UserDao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {
    "org.ikasan.mongo.persistence.scheduled.context.repository",
    "org.ikasan.mongo.persistence.scheduled.instance.repository",
    "org.ikasan.mongo.persistence.scheduled.job.repository",
    "org.ikasan.mongo.persistence.scheduled.joblock.repository",
    "org.ikasan.mongo.persistence.scheduled.notification.repository",
    "org.ikasan.mongo.persistence.business.stream.metadata.repository",
    "org.ikasan.mongo.persistence.module.metadata.repository",
    "org.ikasan.mongo.persistence.configuration.metadata.repository",
    "org.ikasan.mongo.persistence.error.reporting.repository",
    "org.ikasan.mongo.persistence.exclusion.repository",
    "org.ikasan.mongo.persistence.hospital.repository",
    "org.ikasan.mongo.persistence.metrics.repository",
    "org.ikasan.mongo.persistence.replay.repository",
    "org.ikasan.mongo.persistence.security.repository",
    "org.ikasan.mongo.persistence.systemevent.repository",
    "org.ikasan.mongo.persistence.wiretap.repository",
    "org.ikasan.mongo.persistence.scheduled.event.repository",
    "org.ikasan.mongo.persistence.general.repository",
    "org.ikasan.mongo.persistence.scheduled.profile.repository"
})
public class MongoPersistenceAutoConfiguration {

    @Value("${entity.retention.days:30}")
    private int entityRetentionDays;

    @Value("${scheduler.instance.entity.retention.days:90}")
    private int schedulerInstanceEntityRetentionDays;

    @Bean
    public MongoClient mongoClient(
            @Value("${spring.data.mongodb.uri}") String connectionString,
            @Value("${spring.data.mongodb.username:#{null}}") String username,
            @Value("${spring.data.mongodb.password:#{null}}") String password,
            @Value("${spring.data.mongodb.authentication-database:admin}") String authenticationDatabase,
            @Value("${spring.data.mongodb.auth-mechanism:#{null}}") String authMechanism,
            @Value("${spring.data.mongodb.ssl.enabled:false}") boolean sslEnabled,
            @Value("${spring.data.mongodb.ssl.invalid-hostname-allowed:false}") boolean invalidHostnameAllowed,
            @Value("${spring.data.mongodb.connection.pool.min-size:10}") int minConnectionPoolSize,
            @Value("${spring.data.mongodb.connection.pool.max-size:100}") int maxConnectionPoolSize,
            @Value("${spring.data.mongodb.connection.pool.max-wait-time-ms:120000}") long maxWaitTimeMs,
            @Value("${spring.data.mongodb.connection.pool.max-connection-life-time-ms:0}") long maxConnectionLifeTimeMs,
            @Value("${spring.data.mongodb.connection.pool.max-connection-idle-time-ms:0}") long maxConnectionIdleTimeMs,
            @Value("${spring.data.mongodb.connection.pool.maintenance-initial-delay-ms:0}") long maintenanceInitialDelayMs,
            @Value("${spring.data.mongodb.connection.pool.maintenance-frequency-ms:60000}") long maintenanceFrequencyMs,
            @Value("${spring.data.mongodb.socket.connect-timeout-ms:10000}") int connectTimeoutMs,
            @Value("${spring.data.mongodb.socket.read-timeout-ms:0}") int readTimeoutMs,
            @Value("${spring.data.mongodb.socket.receive-buffer-size:0}") int receiveBufferSize,
            @Value("${spring.data.mongodb.socket.send-buffer-size:0}") int sendBufferSize,
            @Value("${spring.data.mongodb.server.heartbeat-frequency-ms:10000}") long serverHeartbeatFrequencyMs,
            @Value("${spring.data.mongodb.server.min-heartbeat-frequency-ms:500}") long minHeartbeatFrequencyMs,
            @Value("${spring.data.mongodb.server.selection-timeout-ms:30000}") long serverSelectionTimeoutMs,
            @Value("${spring.data.mongodb.cluster.local-threshold-ms:15}") long localThresholdMs,
            @Value("${spring.data.mongodb.retry.writes:true}") boolean retryWrites,
            @Value("${spring.data.mongodb.retry.reads:true}") boolean retryReads,
            @Value("${spring.data.mongodb.compression.enabled:false}") boolean compressionEnabled,
            @Value("${spring.data.mongodb.compression.compressors:snappy,zlib,zstd}") String compressors) {

        MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString));

        // Configure authentication if username/password provided (overrides connection string auth)
        if (username != null && password != null) {
            MongoCredential credential;
            if (authMechanism != null) {
                // Specific auth mechanism requested
                switch (authMechanism.toUpperCase()) {
                    case "SCRAM-SHA-1":
                        credential = MongoCredential.createScramSha1Credential(
                                username, authenticationDatabase, password.toCharArray());
                        break;
                    case "SCRAM-SHA-256":
                        credential = MongoCredential.createScramSha256Credential(
                                username, authenticationDatabase, password.toCharArray());
                        break;
                    case "GSSAPI":
                        credential = MongoCredential.createGSSAPICredential(username);
                        break;
                    case "PLAIN":
                        credential = MongoCredential.createPlainCredential(
                                username, authenticationDatabase, password.toCharArray());
                        break;
                    case "MONGODB-X509":
                        credential = MongoCredential.createMongoX509Credential(username);
                        break;
                    default:
                        // Default to SCRAM-SHA-256 (most secure, MongoDB 4.0+)
                        credential = MongoCredential.createScramSha256Credential(
                                username, authenticationDatabase, password.toCharArray());
                }
            } else {
                // No mechanism specified, use SCRAM-SHA-256 (MongoDB 4.0+ default)
                credential = MongoCredential.createScramSha256Credential(
                        username, authenticationDatabase, password.toCharArray());
            }
            settingsBuilder.credential(credential);
        }

        // Configure SSL/TLS
        if (sslEnabled) {
            settingsBuilder.applyToSslSettings(builder -> builder
                    .enabled(true)
                    .invalidHostNameAllowed(invalidHostnameAllowed));
        }

        settingsBuilder.applyToConnectionPoolSettings(builder -> builder
                        .minSize(minConnectionPoolSize)
                        .maxSize(maxConnectionPoolSize)
                        .maxWaitTime(maxWaitTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .maxConnectionLifeTime(maxConnectionLifeTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .maxConnectionIdleTime(maxConnectionIdleTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .maintenanceInitialDelay(maintenanceInitialDelayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .maintenanceFrequency(maintenanceFrequencyMs, java.util.concurrent.TimeUnit.MILLISECONDS))
                .applyToSocketSettings(builder -> builder
                        .connectTimeout(connectTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .readTimeout(readTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .receiveBufferSize(receiveBufferSize)
                        .sendBufferSize(sendBufferSize))
                .applyToServerSettings(builder -> builder
                        .heartbeatFrequency(serverHeartbeatFrequencyMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .minHeartbeatFrequency(minHeartbeatFrequencyMs, java.util.concurrent.TimeUnit.MILLISECONDS))
                .applyToClusterSettings(builder -> builder
                        .serverSelectionTimeout(serverSelectionTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .localThreshold(localThresholdMs, java.util.concurrent.TimeUnit.MILLISECONDS))
                .retryWrites(retryWrites)
                .retryReads(retryReads);

        if (compressionEnabled) {
            List<MongoCompressor> compressorList = new ArrayList<>();
            for (String compressor : compressors.split(",")) {
                switch (compressor.trim().toLowerCase()) {
                    case "snappy":
                        compressorList.add(MongoCompressor.createSnappyCompressor());
                        break;
                    case "zlib":
                        compressorList.add(MongoCompressor.createZlibCompressor());
                        break;
                    case "zstd":
                        compressorList.add(MongoCompressor.createZstdCompressor());
                        break;
                }
            }
            if (!compressorList.isEmpty()) {
                settingsBuilder.compressorList(compressorList);
            }
        }

        return MongoClients.create(settingsBuilder.build());
    }

    @Bean("mongoTemplate")
    public MongoTemplate mongoTemplate(MongoClient mongoClient,
                                        @Value("${spring.data.mongodb.database:test}") String databaseName) {
        return new MongoTemplate(mongoClient, databaseName);
    }

    @Bean("scheduledContextDao")
    public ScheduledContextDao scheduledContextDao(MongoScheduledContextRecordRepository repository,
                                                    MongoTemplate mongoTemplate) {
        return new MongoScheduledContextDaoImpl(repository, mongoTemplate);
    }

    @Bean("scheduledContextViewDao")
    public ScheduledContextViewDao scheduledContextViewDao(MongoScheduledContextViewRepository repository,
                                                            MongoTemplate mongoTemplate) {
        return new MongoScheduledContextViewDao(repository, mongoTemplate);
    }

    @Bean("contextProfileDao")
    public ContextProfileDao contextProfileDao(MongoContextProfileRepository repository,
                                                MongoTemplate mongoTemplate) {
        return new MongoContextProfileDao(repository, mongoTemplate);
    }

    @Bean("jobLockCacheDao")
    public JobLockCacheDao jobLockCacheDao(MongoJobLockCacheRepository repository,
                                           MongoTemplate mongoTemplate) {
        return new MongoJobLockCacheDao(repository, mongoTemplate);
    }

    @Bean("jobLockCacheAuditDao")
    public JobLockCacheAuditDao jobLockCacheAuditDao(MongoJobLockCacheAuditRepository repository,
                                                     MongoTemplate mongoTemplate) {
        return new MongoJobLockCacheAuditDao(repository, mongoTemplate, this.schedulerInstanceEntityRetentionDays);
    }

    @Bean("emailNotificationContextDao")
    public EmailNotificationContextDao emailNotificationContextDao(MongoEmailNotificationContextRepository repository,
                                                                   MongoTemplate mongoTemplate) {
        return new MongoEmailNotificationContextDao(repository, mongoTemplate);
    }

    @Bean("emailNotificationDetailsDao")
    public EmailNotificationDetailsDao emailNotificationDetailsDao(MongoEmailNotificationDetailsRepository repository,
                                                                   MongoTemplate mongoTemplate) {
        return new MongoEmailNotificationDetailsDao(repository, mongoTemplate);
    }

    @Bean("notificationSendAuditDao")
    public NotificationSendAuditDao notificationSendAuditDao(MongoNotificationSendAuditRepository repository,
                                                             MongoTemplate mongoTemplate) {
        return new MongoNotificationSendAuditDao(repository, mongoTemplate);
    }

    @Bean("schedulerJobInstanceDao")
    public SchedulerJobInstanceDao schedulerJobInstanceDao(MongoSchedulerJobInstanceRecordRepository repository,
                                                           MongoTemplate mongoTemplate) {
        return new MongoSchedulerJobInstanceDaoImpl(repository, mongoTemplate);
    }

    @Bean("scheduledContextInstanceDao")
    public ScheduledContextInstanceDao scheduledContextInstanceDao(
            MongoScheduledContextInstanceRepository repository,
            MongoTemplate mongoTemplate) {
        return new MongoScheduledContextInstanceDao(repository, mongoTemplate, schedulerInstanceEntityRetentionDays);
    }

    @Bean("scheduledContextInstanceAuditDao")
    public ScheduledContextInstanceAuditDao scheduledContextInstanceAuditDao(
            MongoScheduledContextInstanceRepository repository) {
        return new MongoScheduledContextInstanceAuditDao(repository, this.schedulerInstanceEntityRetentionDays);
    }

    @Bean("scheduledContextInstanceAuditAggregateDao")
    public ScheduledContextInstanceAuditAggregateDao scheduledContextInstanceAuditAggregateDao(
            MongoScheduledContextInstanceAuditAggregateRepository repository,
            MongoTemplate mongoTemplate) {
        return new MongoScheduledContextInstanceAuditAggregateDao(repository, mongoTemplate
            , this.schedulerInstanceEntityRetentionDays);
    }

    @Bean("schedulerJobRecordDao")
    public SchedulerJobDao schedulerJobDao(
            MongoSchedulerJobRepository repository,
            MongoTemplate mongoTemplate) {
        return new MongoSchedulerJobDao(repository, mongoTemplate);
    }

    @Bean("quartzScheduleDrivenJobRecordDao")
    public QuartzScheduleDrivenJobDao quartzScheduleDrivenJobDao(
            MongoQuartzScheduleDrivenJobRepository repository,
            MongoTemplate mongoTemplate) {
        return new MongoQuartzScheduleDrivenJobDao(repository, mongoTemplate);
    }

    @Bean("bridgingJobRecordDao")
    public BridgingJobDao bridgingJobDao(
            MongoBridgingJobRepository repository,
            MongoTemplate mongoTemplate) {
        return new MongoBridgingJobDao(repository, mongoTemplate);
    }

    @Bean("contextStartJobDao")
    public ContextStartJobDao contextStartJobDao(
            MongoContextStartJobRepository repository,
            MongoTemplate mongoTemplate) {
        return new MongoContextStartJobDao(repository, mongoTemplate);
    }

    @Bean("contextTerminalJobDao")
    public ContextTerminalJobDao contextTerminalJobDao(
            MongoContextTerminalJobRepository repository,
            MongoTemplate mongoTemplate) {
        return new MongoContextTerminalJobDao(repository, mongoTemplate);
    }

    @Bean("fileEventDrivenJobRecordDao")
    public FileEventDrivenJobDao fileEventDrivenJobDao(
            MongoFileEventDrivenJobRepository repository,
            MongoTemplate mongoTemplate) {
        return new MongoFileEventDrivenJobDao(repository, mongoTemplate);
    }

    @Bean("globalEventJobRecordDao")
    public GlobalEventJobDao globalEventJobDao(
            MongoGlobalEventJobRepository repository,
            MongoTemplate mongoTemplate) {
        return new MongoGlobalEventJobDao(repository, mongoTemplate);
    }

    @Bean("internalEventDrivenJobRecordDao")
    public InternalEventDrivenJobDao internalEventDrivenJobDao(
            MongoInternalEventDrivenJobRepository repository,
            MongoTemplate mongoTemplate) {
        return new MongoInternalEventDrivenJobDao(repository, mongoTemplate);
    }

    @Bean("internalEventDrivenJobTemplateDao")
    public InternalEventDrivenJobDao internalEventDrivenJobTemplateDao(
            MongoInternalEventDrivenJobRepository repository,
            MongoTemplate mongoTemplate) {
        return new MongoInternalEventDrivenJobTemplateDao(repository, mongoTemplate);
    }

    @Bean("businessStreamMetadataDao")
    public BusinessStreamMetadataDao businessStreamMetadataDao(MongoBusinessStreamRepository repository,
                                                               MongoTemplate mongoTemplate) {
        return new MongoBusinessStreamMetadataDaoImpl(repository, mongoTemplate);
    }

    @Bean("moduleMetadataDao")
    public ModuleMetadataDao moduleMetadataDao(MongoModuleMetadataRepository repository,
                                               MongoTemplate mongoTemplate) {
        return new MongoModuleMetadataDaoImpl(repository, mongoTemplate);
    }

    @Bean("componentConfigurationMetadataDao")
    public ComponentConfigurationMetadataDao componentConfigurationMetadataDao(
            MongoComponentConfigurationMetadataRepository repository,
            MongoTemplate mongoTemplate) {
        return new MongoComponentConfigurationMetadataDaoImpl(repository, mongoTemplate);
    }

    @Bean("errorReportingServiceEntityDao")
    public MongoErrorReportingServiceDaoImpl errorReportingServiceDao(
            MongoErrorOccurrenceRepository repository,
            MongoTemplate mongoTemplate) {
        return new MongoErrorReportingServiceDaoImpl(repository, mongoTemplate, this.entityRetentionDays);
    }

    @Bean("exclusionEventEntityDao")
    public MongoExclusionEventDaoImpl exclusionEventDao(
            MongoExclusionEventRepository repository,
            MongoTemplate mongoTemplate) {
        return new MongoExclusionEventDaoImpl(repository, mongoTemplate, this.entityRetentionDays);
    }

    @Bean("hospitalEntityDao")
    public MongoHospitalDaoImpl hospitalDao(
            MongoExclusionEventActionRepository repository,
            MongoTemplate mongoTemplate) {
        return new MongoHospitalDaoImpl(repository, mongoTemplate, this.entityRetentionDays);
    }

    @Bean("metricsDao")
    public MongoMetricsDaoImpl metricsDao(
            MongoFlowInvocationMetricRepository repository,
            MongoTemplate mongoTemplate,
            @Value("${mongo.metrics.query.limit:1000}") int metricsQueryLimit) {
        return new MongoMetricsDaoImpl(repository, mongoTemplate, metricsQueryLimit);
    }

    @Bean("replayEntityDao")
    public MongoReplayDaoImpl replayDao(
            MongoReplayEventRepository repository,
            MongoTemplate mongoTemplate) {
        return new MongoReplayDaoImpl(repository, mongoTemplate);
    }

    @Bean("replayAuditEntityDao")
    public MongoReplayAuditDaoImpl replayAuditDao(
            MongoReplayAuditEventRepository repository,
            MongoTemplate mongoTemplate,
            @Value("${mongo.replay.audit.days.to.keep:7}") int daysToKeep) {
        return new MongoReplayAuditDaoImpl(repository, mongoTemplate, daysToKeep);
    }

    @Bean("systemEventEntityDao")
    public MongoSystemEventDao systemEventDao(
            MongoSystemEventRepository repository,
            MongoTemplate mongoTemplate) {
        return new MongoSystemEventDao(repository, mongoTemplate, this.entityRetentionDays);
    }

    @Bean("wiretapEntityDao")
    public MongoWiretapDao wiretapDao(
            MongoWiretapEventRepository repository,
            MongoTemplate mongoTemplate,
            @Value("${wiretap.housekeep.daysToKeep:90}") int daysToKeep) {
        return new MongoWiretapDao(repository, mongoTemplate, daysToKeep);
    }

    @Bean("scheduledProcessEventDao")
    public MongoScheduledProcessEventDao scheduledProcessEventDao(
            MongoScheduledProcessEventRepository repository,
            MongoTemplate mongoTemplate,
            @Value("${scheduled.process.event.housekeep.daysToKeep:90}") int daysToKeep) {
        return new MongoScheduledProcessEventDao(repository, mongoTemplate, daysToKeep);
    }

    // Security DAOs

    @Bean("mongoPolicyDao")
    public MongoPolicyDaoImpl mongoPolicyDao(
            MongoPolicyRepository repository,
            MongoTemplate mongoTemplate) {
        return new MongoPolicyDaoImpl(repository, mongoTemplate);
    }

    @Bean("mongoRoleDao")
    public MongoRoleDaoImpl mongoRoleDao(
            MongoRoleRepository repository,
            MongoTemplate mongoTemplate,
            MongoPolicyDaoImpl mongoPolicyDaoImpl) {
        MongoRoleDaoImpl roleDao = new MongoRoleDaoImpl(repository, mongoTemplate, mongoPolicyDaoImpl);
        // Set circular dependency
        mongoPolicyDaoImpl.setMongoRoleDao(roleDao);
        return roleDao;
    }

    @Bean("mongoIkasanPrincipalDao")
    public MongoIkasanPrincipalDaoImpl mongoIkasanPrincipalDao(
            MongoIkasanPrincipalRepository repository,
            MongoTemplate mongoTemplate,
            MongoRoleDaoImpl mongoRoleDaoImpl) {
        return new MongoIkasanPrincipalDaoImpl(repository, mongoTemplate, mongoRoleDaoImpl);
    }

    @Bean("mongoAuthenticationMethodDao")
    public MongoAuthenticationMethodDaoImpl mongoAuthenticationMethodDao(
            MongoAuthenticationMethodRepository repository,
            MongoTemplate mongoTemplate) {
        return new MongoAuthenticationMethodDaoImpl(repository, mongoTemplate);
    }

    @Bean("mongoUserDao")
    public UserDao mongoUserDao(
            MongoUserRepository repository,
            MongoTemplate mongoTemplate,
            MongoIkasanPrincipalDaoImpl mongoIkasanPrincipalDaoImpl) {
        return new MongoUserDaoImpl(repository, mongoTemplate, mongoIkasanPrincipalDaoImpl);
    }

    @Bean("mongoSecurityDao")
    public SecurityDao mongoSecurityDao(
            MongoIkasanPrincipalDaoImpl mongoIkasanPrincipalDaoImpl,
            MongoPolicyDaoImpl mongoPolicyDaoImpl,
            MongoRoleDaoImpl mongoRoleDaoImpl,
            MongoAuthenticationMethodDaoImpl mongoAuthenticationMethodDaoImpl,
            UserDao mongoUserDao) {
        return new MongoSecurityDaoImpl(
            mongoIkasanPrincipalDaoImpl,
            mongoPolicyDaoImpl,
            mongoRoleDaoImpl,
            mongoAuthenticationMethodDaoImpl,
            mongoUserDao
        );
    }

    // General DAO

    @Bean("mongoGeneralDao")
    public MongoGeneralDaoImpl mongoGeneralDao(
            MongoIkasanDocumentRepository repository,
            MongoTemplate mongoTemplate) {
        return new MongoGeneralDaoImpl(repository, mongoTemplate);
    }

    // General Service

    @Bean("esbSearchService")
    public MongoGeneralServiceImpl esbSearchService(MongoGeneralDaoImpl mongoGeneralDao) {
        return new MongoGeneralServiceImpl(mongoGeneralDao);
    }

    @Bean(name = "housekeepService")
    public HousekeepService housekeepService(MongoGeneralDaoImpl mongoGeneralDao)
    {
        return esbSearchService(mongoGeneralDao);
    }

}
