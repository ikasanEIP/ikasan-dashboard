package org.ikasan.data.synchronisation;

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.NodeConfig;
import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.security.dao.MongoAuthenticationMethodDaoImpl;
import org.ikasan.mongo.persistence.security.dao.MongoIkasanPrincipalDaoImpl;
import org.ikasan.mongo.persistence.security.dao.MongoPolicyDaoImpl;
import org.ikasan.mongo.persistence.security.dao.MongoRoleDaoImpl;
import org.ikasan.spec.metadata.dao.BusinessStreamMetadataDao;
import org.ikasan.spec.metadata.dao.ComponentConfigurationMetadataDao;
import org.ikasan.spec.metadata.dao.ModuleMetadataDao;
import org.ikasan.spec.security.dao.UserDao;
import org.ikasan.spec.solr.SolrConstants;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One Mongo replica set (Testcontainers), one Spring context with the real Mongo DAOs, and one embedded Solr core,
 * shared by all tests in the run; {@link #reset()} empties them between tests.
 */
final class TestEnvironment {

    static final SolrSettings SOLR_SETTINGS = new SolrSettings("standalone", "", "", "", "", 30, 90, 30, 200, 15000);

    private static TestEnvironment instance;

    final MongoDBContainer mongo;
    final AnnotationConfigApplicationContext context;
    final EmbeddedSolrServer solrServer;
    final WriteCaptureListener writeCaptureListener = new WriteCaptureListener();

    private TestEnvironment() throws Exception {
        mongo = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongo.start();

        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
            Map.of("mongodb.uri", mongo.getReplicaSetUrl("test"), "mongodb.database", "test")));
        // registered with the Mongo client through MongoPersistenceAutoConfiguration's CommandListener beans
        context.registerBean("writeCaptureListener", WriteCaptureListener.class, () -> writeCaptureListener);
        context.register(MongoPersistenceAutoConfiguration.class);
        context.refresh();

        Path solrHome = Files.createTempDirectory("ikasan-data-sync-solr");
        Path configSets = Paths.get(TestEnvironment.class.getResource("/solr/configsets").toURI());
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", solrHome)
            .setConfigSetBaseDirectory(configSets.toString())
            .build();
        solrServer = new EmbeddedSolrServer(config, SolrConstants.CORE);
        CoreAdminRequest.Create create = new CoreAdminRequest.Create();
        create.setCoreName(SolrConstants.CORE);
        create.setConfigSet("minimal");
        solrServer.request(create);
    }

    static synchronized TestEnvironment get() {
        if (instance == null) {
            try {
                instance = new TestEnvironment();
            } catch (Exception e) {
                throw new IllegalStateException("Could not start the test environment", e);
            }
        }
        return instance;
    }

    MongoTemplate mongoTemplate() {
        return context.getBean(MongoTemplate.class);
    }

    <T> T bean(String name, Class<T> type) {
        return context.getBean(name, type);
    }

    /** Solr DAOs on the embedded server. Not to be closed: the server is shared. */
    SolrDaos solrDaos() {
        return new SolrDaos(solrServer, SOLR_SETTINGS);
    }

    MongoLookups lookups() {
        return new MongoLookups(bean("mongoUserDao", UserDao.class),
            context.getBean(MongoIkasanPrincipalDaoImpl.class),
            context.getBean(MongoRoleDaoImpl.class),
            context.getBean(MongoPolicyDaoImpl.class),
            context.getBean(MongoAuthenticationMethodDaoImpl.class),
            bean("moduleMetadataDao", ModuleMetadataDao.class),
            bean("businessStreamMetadataDao", BusinessStreamMetadataDao.class),
            bean("componentConfigurationMetadataDao", ComponentConfigurationMetadataDao.class));
    }

    /** Empty Mongo and Solr. */
    void reset() throws Exception {
        writeCaptureListener.deactivate();
        MongoTemplate mongoTemplate = mongoTemplate();
        for (String collection : List.of(SyncCollections.IKASAN, SyncCollections.SCHEDULED_PROCESS_EVENT,
            SyncCollections.STATE)) {
            mongoTemplate.getCollection(collection).deleteMany(new org.bson.Document());
        }
        solrServer.deleteByQuery(SolrConstants.CORE, "*:*");
        solrServer.commit(SolrConstants.CORE);
    }

    /** @return the Solr documents matching the query */
    List<SolrDocument> solrQuery(String query) throws Exception {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("q", query);
        params.set("rows", 10000);
        return new ArrayList<>(new QueryRequest(params).process(solrServer, SolrConstants.CORE).getResults());
    }

    List<String> solrIds(String query) throws Exception {
        return solrQuery(query).stream().map(d -> String.valueOf(d.getFieldValue("id"))).sorted().toList();
    }
}
