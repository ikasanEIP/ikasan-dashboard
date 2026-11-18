package org.ikasan.data.synchronisation;

import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.security.dao.MongoAuthenticationMethodDaoImpl;
import org.ikasan.mongo.persistence.security.dao.MongoIkasanPrincipalDaoImpl;
import org.ikasan.mongo.persistence.security.dao.MongoPolicyDaoImpl;
import org.ikasan.mongo.persistence.security.dao.MongoRoleDaoImpl;
import org.ikasan.spec.metadata.dao.BusinessStreamMetadataDao;
import org.ikasan.spec.metadata.dao.ComponentConfigurationMetadataDao;
import org.ikasan.spec.metadata.dao.ModuleMetadataDao;
import org.ikasan.spec.security.dao.UserDao;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.lang.management.ManagementFactory;
import java.util.UUID;

/**
 * Synchronises writes to the dashboard's Mongo database into Solr. Enabled with
 * {@code ikasan.data-synchronisation.enabled=true}; nothing is created otherwise.
 *
 * <p>The Solr DAOs are created here rather than taken from {@code SolrClientAutoConfiguration}, whose bean names
 * clash with {@link MongoPersistenceAutoConfiguration}'s.
 */
@AutoConfiguration(after = MongoPersistenceAutoConfiguration.class)
@ConditionalOnProperty(prefix = DataSynchronisationProperties.PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties(DataSynchronisationProperties.class)
public class DataSynchronisationAutoConfiguration {

    /**
     * Registered with the Mongo client by {@link MongoPersistenceAutoConfiguration}; inactive unless the command
     * listener mode is chosen. It has no dependencies, so the Mongo client can be built before anything else here.
     */
    @Bean
    public static WriteCaptureListener dataSynchronisationWriteCaptureListener() {
        return new WriteCaptureListener();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public SolrDaos dataSynchronisationSolrDaos(Environment environment) {
        return SolrDaos.create(SolrSettings.from(environment));
    }

    @Bean
    public MongoLookups dataSynchronisationMongoLookups(@Qualifier("mongoUserDao") UserDao userDao,
                                                        MongoIkasanPrincipalDaoImpl principalDao,
                                                        MongoRoleDaoImpl roleDao,
                                                        MongoPolicyDaoImpl policyDao,
                                                        MongoAuthenticationMethodDaoImpl authenticationMethodDao,
                                                        @Qualifier("moduleMetadataDao")
                                                        ModuleMetadataDao moduleMetadataDao,
                                                        @Qualifier("businessStreamMetadataDao")
                                                        BusinessStreamMetadataDao businessStreamMetadataDao,
                                                        @Qualifier("componentConfigurationMetadataDao")
                                                        ComponentConfigurationMetadataDao componentConfigurationMetadataDao) {
        return new MongoLookups(userDao, principalDao, roleDao, policyDao, authenticationMethodDao, moduleMetadataDao,
            businessStreamMetadataDao, componentConfigurationMetadataDao);
    }

    @Bean
    public SyncPipeline dataSynchronisationPipeline(MongoTemplate mongoTemplate, SolrDaos solrDaos,
                                                    MongoLookups mongoLookups,
                                                    DataSynchronisationProperties properties) {
        return new SyncPipeline(SyncHandlers.create(mongoTemplate.getConverter(), solrDaos, mongoLookups),
            properties.getExcludedTypes(), solrDaos, properties.getRetryMaxBackoffMs());
    }

    @Bean
    public SyncStateStore dataSynchronisationStateStore(MongoTemplate mongoTemplate) {
        return new SyncStateStore(mongoTemplate);
    }

    @Bean
    public Reconciler dataSynchronisationReconciler(MongoTemplate mongoTemplate, SolrDaos solrDaos,
                                                    SyncPipeline pipeline, SyncStateStore stateStore,
                                                    DataSynchronisationProperties properties) {
        return new Reconciler(mongoTemplate, solrDaos, pipeline, stateStore, properties.getBatchSize());
    }

    @Bean
    public DataSynchronisationEngine dataSynchronisationEngine(DataSynchronisationProperties properties,
                                                               MongoTemplate mongoTemplate, SyncPipeline pipeline,
                                                               Reconciler reconciler, SyncStateStore stateStore,
                                                               WriteCaptureListener writeCaptureListener) {
        SyncLease lease = properties.getLease().isEnabled()
            ? new SyncLease(stateStore, instanceId(), properties.getLease().getDurationSeconds() * 1000)
            : null;
        return new DataSynchronisationEngine(properties, mongoTemplate, pipeline, reconciler, stateStore,
            writeCaptureListener, lease);
    }

    private static String instanceId() {
        // e.g. "12345@host" plus a random suffix, so two instances on one host never share an id
        return ManagementFactory.getRuntimeMXBean().getName() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
