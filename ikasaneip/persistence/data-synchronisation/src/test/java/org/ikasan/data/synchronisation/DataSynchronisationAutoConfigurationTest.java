package org.ikasan.data.synchronisation;

import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.spec.entity.EntityDao;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DataSynchronisationAutoConfigurationTest {

    private TestEnvironment env;
    private ApplicationContextRunner runner;

    @Before
    public void setUp() throws Exception {
        env = TestEnvironment.get();
        env.reset();
        runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MongoPersistenceAutoConfiguration.class,
                DataSynchronisationAutoConfiguration.class))
            .withPropertyValues("mongodb.uri=" + env.mongo.getReplicaSetUrl("test"), "mongodb.database=test")
            // the embedded Solr server is shared by the tests, so it must not be closed with the context
            .withBean(SolrDaos.class, () -> new SolrDaos(env.solrServer, TestEnvironment.SOLR_SETTINGS) {
                @Override
                public void close() {
                }
            });
    }

    @Test
    public void nothing_is_created_unless_enabled() {
        runner.run(context -> {
            assertEquals(0, context.getBeansOfType(DataSynchronisationEngine.class).size());
            assertEquals(0, context.getBeansOfType(WriteCaptureListener.class).size());
        });
    }

    @Test
    public void when_enabled_it_starts_alongside_the_mongo_persistence_and_synchronises() {
        runner.withPropertyValues("ikasan.data-synchronisation.enabled=true",
                "ikasan.data-synchronisation.batch-max-wait-ms=100")
            .run(context -> {
                assertTrue(context.getStartupFailure() == null);
                DataSynchronisationEngine engine = context.getBean(DataSynchronisationEngine.class);
                SyncTestSupport.await("synchroniser running",
                    () -> engine.synchroniser() != null && engine.synchroniser().isRunning());
                assertEquals(SyncMode.CHANGE_STREAM, engine.synchroniser().mode());

                new SampleData(env).wiretap("1");
                SyncTestSupport.await("write synchronised", () -> {
                    try {
                        return env.solrIds("type:\"" + EntityDao.WIRETAP_TYPE + "\"").size() == 1;
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                });
            });
    }

    @Test
    public void the_command_listener_mode_captures_writes_through_the_mongo_client() {
        runner.withPropertyValues("ikasan.data-synchronisation.enabled=true",
                "ikasan.data-synchronisation.mode=command-listener",
                "ikasan.data-synchronisation.batch-max-wait-ms=100")
            .run(context -> {
                DataSynchronisationEngine engine = context.getBean(DataSynchronisationEngine.class);
                SyncTestSupport.await("synchroniser running",
                    () -> engine.synchroniser() != null && engine.synchroniser().isRunning());
                assertEquals(SyncMode.COMMAND_LISTENER, engine.synchroniser().mode());
                SyncTestSupport.await("listener active",
                    () -> context.getBean(WriteCaptureListener.class).isActive());

                // write through this context's Mongo client, which has the listener registered
                context.getBean("wiretapEntityDao", EntityDao.class).save(wiretap());
                SyncTestSupport.await("write synchronised", () -> {
                    try {
                        return env.solrIds("type:\"" + EntityDao.WIRETAP_TYPE + "\"").size() == 1;
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                });
            });
    }

    private static org.ikasan.mongo.persistence.wiretap.model.MongoWiretapEventImpl wiretap() {
        org.ikasan.mongo.persistence.wiretap.model.MongoWiretapEventImpl event
            = new org.ikasan.mongo.persistence.wiretap.model.MongoWiretapEventImpl();
        event.setId("7");
        event.setModuleName("module");
        event.setFlowName("flow");
        event.setComponentName("component");
        event.setEventId("event-7");
        event.setTimestamp(System.currentTimeMillis());
        event.setEvent("{}");
        return event;
    }
}
