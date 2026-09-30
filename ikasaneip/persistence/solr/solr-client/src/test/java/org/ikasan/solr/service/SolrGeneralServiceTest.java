package org.ikasan.solr.service;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.NodeConfig;
import org.ikasan.solr.dao.SolrGeneralDao;
import org.ikasan.solr.dao.SolrGeneralDaoImpl;
import org.ikasan.solr.model.IkasanSolrDocument;
import org.ikasan.spec.search.model.IkasanDocumentSearchResults;
import org.ikasan.spec.search.model.IkasanESBDocument;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Ikasan Development Team on 04/08/2017.
 */
public class SolrGeneralServiceTest extends SolrTestCaseJ4
{
    SolrGeneralDaoImpl dao;

    @Test(expected = IllegalArgumentException.class)
    @DirtiesContext
    public void test_constructor_dao_null_exception()
    {
        new SolrGeneralServiceImpl(null);
    }

    @Test
    @DirtiesContext
    public void test_search_success() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
                .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
                .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "1");
            doc.addField("type", "type");
            doc.addField("payload", "test");
            doc.addField("expiry", 100l);
            doc.addField("timestamp", 100l);
            server.add("ikasan", doc);
            doc = new SolrInputDocument();
            doc.addField("id", "2");
            doc.addField("type", "type");
            doc.addField("payload", "test");
            doc.addField("expiry", 100l);
            doc.addField("timestamp", 100l);
            server.add("ikasan", doc);
            doc = new SolrInputDocument();
            doc.addField("id", "3");
            doc.addField("type", "type");
            doc.addField("payload", "test");
            doc.addField("timestamp", 100l);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);
            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            assertEquals(3, solrGeneralService.search(null, null, "test", 0, System.currentTimeMillis() + 100000000l, 100, false, null ,null ).getResultList().size());

        }
    }

    @Test
    @DirtiesContext
    public void test_search_with_offset_success() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "1");
            doc.addField("type", "type");
            doc.addField("payload", "test");
            doc.addField("expiry", 100l);
            doc.addField("timestamp", 100l);
            server.add("ikasan", doc);
            doc = new SolrInputDocument();
            doc.addField("id", "2");
            doc.addField("type", "type");
            doc.addField("payload", "test");
            doc.addField("expiry", 100l);
            doc.addField("timestamp", 100l);
            server.add("ikasan", doc);
            doc = new SolrInputDocument();
            doc.addField("id", "3");
            doc.addField("type", "type");
            doc.addField("payload", "blah");
            doc.addField("timestamp", 100l);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);
            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            assertEquals(1, solrGeneralService.search("test", 0, System.currentTimeMillis() + 100000000l, 1,100, null, false, null ,null ).getResultList().size());

        }
    }

    @Test
    @DirtiesContext
    public void test_search_entity_types_success() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
                .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
                .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "1");
            doc.addField("type", "type");
            doc.addField("payload", "test");
            doc.addField("expiry", 100l);
            doc.addField("timestamp", 100l);
            server.add("ikasan", doc);
            doc = new SolrInputDocument();
            doc.addField("id", "2");
            doc.addField("type", "type");
            doc.addField("payload", "test");
            doc.addField("expiry", 100l);
            doc.addField("timestamp", 100l);
            server.add("ikasan", doc);
            doc = new SolrInputDocument();
            doc.addField("id", "3");
            doc.addField("type", "type");
            doc.addField("payload", "test");
            doc.addField("timestamp", 100l);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);
            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            ArrayList<String> entityTypes = new ArrayList<>();
            entityTypes.add("type");

            assertEquals(3, solrGeneralService.search(null, null, "test", 0, System.currentTimeMillis() + 100000000l, 100, entityTypes, false, null ,null )
                    .getResultList().size());

        }
    }

    @Test
    @DirtiesContext
    public void test_search_entity_types_no_module_or_flow_success() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "1");
            doc.addField("type", "type");
            doc.addField("payload", "test");
            doc.addField("expiry", 100l);
            doc.addField("timestamp", 100l);
            server.add("ikasan", doc);
            doc = new SolrInputDocument();
            doc.addField("id", "2");
            doc.addField("type", "type");
            doc.addField("payload", "test");
            doc.addField("expiry", 100l);
            doc.addField("timestamp", 100l);
            server.add("ikasan", doc);
            doc = new SolrInputDocument();
            doc.addField("id", "3");
            doc.addField("type", "type");
            doc.addField("payload", "test");
            doc.addField("timestamp", 100l);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);
            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            ArrayList<String> entityTypes = new ArrayList<>();
            entityTypes.add("test");

            assertEquals(3, solrGeneralService.search("test", 0, System.currentTimeMillis() + 100000000l, 100, entityTypes, false, null ,null )
                .getResultList().size());

        }
    }

    @Test
    @DirtiesContext
    public void test_search_success_with_query_filter() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
                .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
                .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "1");
            doc.addField("type", "type");
            doc.addField("payload", "test");
            doc.addField("moduleName", "test");
            doc.addField("expiry", 100l);
            doc.addField("timestamp", 100l);
            server.add("ikasan", doc);
            doc = new SolrInputDocument();
            doc.addField("id", "2");
            doc.addField("type", "type");
            doc.addField("payload", "test");
            doc.addField("moduleName", "test");
            doc.addField("expiry", 100l);
            doc.addField("timestamp", 100l);
            server.add("ikasan", doc);
            doc = new SolrInputDocument();
            doc.addField("id", "3");
            doc.addField("type", "type");
            doc.addField("payload", "test");
            doc.addField("moduleName", "test");
            doc.addField("timestamp", 100l);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);
            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            Set<String> moduleNames = new HashSet<String>();
            moduleNames.add("test");

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            assertEquals(3, solrGeneralService.search(moduleNames, null, "test", 0, System.currentTimeMillis() + 100000000l, 100, false, null ,null ).getResultList().size());

        }
    }

    @Test
    @DirtiesContext
    public void test_save_document() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            IkasanSolrDocument doc = new IkasanSolrDocument();
            doc.setModuleName("test");
            doc.setExpiry(100l);
            doc.setTimeStamp(100l);
            doc.setEvent("test");
            doc.setId("1");

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            Set<String> moduleNames = new HashSet<String>();
            moduleNames.add("test");

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);
            solrGeneralService.saveOrUpdate(doc);


            assertEquals(1, solrGeneralService.search(moduleNames, null, "test", 0, System.currentTimeMillis() + 100000000l, 100, false, null ,null ).getResultList().size());

        }
    }

    @Test
    @DirtiesContext
    public void test_save_documents() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            IkasanSolrDocument doc1 = new IkasanSolrDocument();
            doc1.setModuleName("test");
            doc1.setId("1");
            doc1.setEvent("test");
            doc1.setExpiry(100l);
            doc1.setTimeStamp(100l);

            IkasanSolrDocument doc2 = new IkasanSolrDocument();
            doc2.setModuleName("test");
            doc2.setId("2");
            doc2.setEvent("test");
            doc2.setExpiry(100l);
            doc2.setTimeStamp(100l);

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            Set<String> moduleNames = new HashSet<String>();
            moduleNames.add("test");

            List<IkasanESBDocument> documents = new ArrayList<>();
            documents.add(doc1);
            documents.add(doc2);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);
            solrGeneralService.saveOrUpdate(documents);

            IkasanDocumentSearchResults results = solrGeneralService.search(moduleNames, null, "test", 0, System.currentTimeMillis() + 100000000l, 100, false, null ,null );

            assertEquals(2, solrGeneralService.search(moduleNames, null, "test", 0, System.currentTimeMillis() + 100000000l, 100, false, null ,null ).getResultList().size());

        }
    }

    @Test
    @DirtiesContext
    public void test_search_by_identifiers_success() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "id-1");
            doc.addField("type", "wiretap");
            doc.addField("payload", "test payload 1");
            doc.addField("expiry", 100l);
            doc.addField("timestamp", 100l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-2");
            doc.addField("type", "wiretap");
            doc.addField("payload", "test payload 2");
            doc.addField("expiry", 100l);
            doc.addField("timestamp", 200l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-3");
            doc.addField("type", "wiretap");
            doc.addField("payload", "test payload 3");
            doc.addField("timestamp", 300l);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-4");
            doc.addField("type", "wiretap");
            doc.addField("payload", "test payload 4");
            doc.addField("timestamp", 400l);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            Set<String> identifiers = new HashSet<>();
            identifiers.add("id-1");
            identifiers.add("id-3");

            IkasanDocumentSearchResults results = solrGeneralService.search(identifiers, 0, 10, null, null);

            assertEquals(2, results.getResultList().size());
            assertEquals(2, results.getTotalNumberOfResults());
        }
    }

    @Test
    @DirtiesContext
    public void test_search_by_identifiers_with_offset() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            for (int i = 1; i <= 10; i++) {
                SolrInputDocument doc = new SolrInputDocument();
                doc.addField("id", "id-" + i);
                doc.addField("type", "wiretap");
                doc.addField("payload", "test payload " + i);
                doc.addField("timestamp", (long) i * 100);
                doc.addField("expiry", System.currentTimeMillis() + 10000000l);
                server.add("ikasan", doc);
            }

            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            Set<String> identifiers = new HashSet<>();
            for (int i = 1; i <= 10; i++) {
                identifiers.add("id-" + i);
            }

            IkasanDocumentSearchResults results = solrGeneralService.search(identifiers, 3, 5, null, null);

            assertEquals(5, results.getResultList().size());
            assertEquals(10, results.getTotalNumberOfResults());
        }
    }

    @Test
    @DirtiesContext
    public void test_search_by_identifiers_with_result_size() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            for (int i = 1; i <= 10; i++) {
                SolrInputDocument doc = new SolrInputDocument();
                doc.addField("id", "id-" + i);
                doc.addField("type", "wiretap");
                doc.addField("payload", "test payload " + i);
                doc.addField("timestamp", (long) i * 100);
                doc.addField("expiry", System.currentTimeMillis() + 10000000l);
                server.add("ikasan", doc);
            }

            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            Set<String> identifiers = new HashSet<>();
            for (int i = 1; i <= 10; i++) {
                identifiers.add("id-" + i);
            }

            IkasanDocumentSearchResults results = solrGeneralService.search(identifiers, 0, 3, null, null);

            assertEquals(3, results.getResultList().size());
            assertEquals(10, results.getTotalNumberOfResults());
        }
    }

    @Test
    @DirtiesContext
    public void test_search_by_identifiers_sort_ascending() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "id-1");
            doc.addField("type", "wiretap");
            doc.addField("payload", "c payload");
            doc.addField("timestamp", 300l);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-2");
            doc.addField("type", "wiretap");
            doc.addField("payload", "a payload");
            doc.addField("timestamp", 100l);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-3");
            doc.addField("type", "wiretap");
            doc.addField("payload", "b payload");
            doc.addField("timestamp", 200l);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            Set<String> identifiers = new HashSet<>();
            identifiers.add("id-1");
            identifiers.add("id-2");
            identifiers.add("id-3");

            IkasanDocumentSearchResults results = solrGeneralService.search(identifiers, 0, 10, "payload", "ASCENDING");

            assertEquals(3, results.getResultList().size());
            assertEquals("id-2", results.getResultList().get(0).getId());
            assertEquals("id-3", results.getResultList().get(1).getId());
            assertEquals("id-1", results.getResultList().get(2).getId());
        }
    }

    @Test
    @DirtiesContext
    public void test_search_by_identifiers_sort_descending() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "id-1");
            doc.addField("type", "wiretap");
            doc.addField("payload", "c payload");
            doc.addField("timestamp", 300l);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-2");
            doc.addField("type", "wiretap");
            doc.addField("payload", "a payload");
            doc.addField("timestamp", 100l);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-3");
            doc.addField("type", "wiretap");
            doc.addField("payload", "b payload");
            doc.addField("timestamp", 200l);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            Set<String> identifiers = new HashSet<>();
            identifiers.add("id-1");
            identifiers.add("id-2");
            identifiers.add("id-3");

            IkasanDocumentSearchResults results = solrGeneralService.search(identifiers, 0, 10, "payload", "DESCENDING");

            assertEquals(3, results.getResultList().size());
            assertEquals("id-1", results.getResultList().get(0).getId());
            assertEquals("id-3", results.getResultList().get(1).getId());
            assertEquals("id-2", results.getResultList().get(2).getId());
        }
    }

    @Test
    @DirtiesContext
    public void test_search_by_identifiers_sort_default() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "id-1");
            doc.addField("type", "wiretap");
            doc.addField("payload", "payload 1");
            doc.addField("timestamp", 100l);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-2");
            doc.addField("type", "wiretap");
            doc.addField("payload", "payload 2");
            doc.addField("timestamp", 200l);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-3");
            doc.addField("type", "wiretap");
            doc.addField("payload", "payload 3");
            doc.addField("timestamp", 300l);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            Set<String> identifiers = new HashSet<>();
            identifiers.add("id-1");
            identifiers.add("id-2");
            identifiers.add("id-3");

            // When sort field and order are null, should default to timestamp descending
            IkasanDocumentSearchResults results = solrGeneralService.search(identifiers, 0, 10, null, null);

            assertEquals(3, results.getResultList().size());
            // Should be sorted by timestamp descending by default
            assertEquals("id-3", results.getResultList().get(0).getId());
            assertEquals("id-2", results.getResultList().get(1).getId());
            assertEquals("id-1", results.getResultList().get(2).getId());
        }
    }

    @Test
    @DirtiesContext
    public void test_search_by_identifiers_null_identifiers() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            IkasanDocumentSearchResults results = solrGeneralService.search(null, 0, 10, null, null);

            assertEquals(0, results.getResultList().size());
            assertEquals(0, results.getTotalNumberOfResults());
        }
    }

    @Test
    @DirtiesContext
    public void test_search_by_identifiers_empty_identifiers() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            Set<String> identifiers = new HashSet<>();
            IkasanDocumentSearchResults results = solrGeneralService.search(identifiers, 0, 10, null, null);

            assertEquals(0, results.getResultList().size());
            assertEquals(0, results.getTotalNumberOfResults());
        }
    }

    @Test
    @DirtiesContext
    public void test_search_by_identifiers_no_matching_ids() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "id-1");
            doc.addField("type", "wiretap");
            doc.addField("payload", "test payload");
            doc.addField("timestamp", 100l);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);
            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            Set<String> identifiers = new HashSet<>();
            identifiers.add("id-999");
            identifiers.add("id-888");

            IkasanDocumentSearchResults results = solrGeneralService.search(identifiers, 0, 10, null, null);

            assertEquals(0, results.getResultList().size());
            assertEquals(0, results.getTotalNumberOfResults());
        }
    }

    @Test
    @DirtiesContext
    public void test_search_by_identifiers_single_identifier() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "id-1");
            doc.addField("type", "wiretap");
            doc.addField("payload", "test payload 1");
            doc.addField("timestamp", 100l);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-2");
            doc.addField("type", "wiretap");
            doc.addField("payload", "test payload 2");
            doc.addField("timestamp", 200l);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            Set<String> identifiers = new HashSet<>();
            identifiers.add("id-1");

            IkasanDocumentSearchResults results = solrGeneralService.search(identifiers, 0, 10, null, null);

            assertEquals(1, results.getResultList().size());
            assertEquals("id-1", results.getResultList().get(0).getId());
        }
    }

    @Test
    @DirtiesContext
    public void test_search_by_identifiers_with_pagination() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            for (int i = 1; i <= 20; i++) {
                SolrInputDocument doc = new SolrInputDocument();
                doc.addField("id", "id-" + i);
                doc.addField("type", "wiretap");
                doc.addField("payload", "test payload " + i);
                doc.addField("timestamp", (long) i * 100);
                doc.addField("expiry", System.currentTimeMillis() + 10000000l);
                server.add("ikasan", doc);
            }

            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            Set<String> identifiers = new HashSet<>();
            for (int i = 1; i <= 20; i++) {
                identifiers.add("id-" + i);
            }

            // First page
            IkasanDocumentSearchResults page1 = solrGeneralService.search(identifiers, 0, 5, "timestamp", "ASCENDING");
            assertEquals(5, page1.getResultList().size());
            assertEquals(20, page1.getTotalNumberOfResults());

            // Second page
            IkasanDocumentSearchResults page2 = solrGeneralService.search(identifiers, 5, 5, "timestamp", "ASCENDING");
            assertEquals(5, page2.getResultList().size());
            assertEquals(20, page2.getTotalNumberOfResults());

            // Third page
            IkasanDocumentSearchResults page3 = solrGeneralService.search(identifiers, 10, 5, "timestamp", "ASCENDING");
            assertEquals(5, page3.getResultList().size());
            assertEquals(20, page3.getTotalNumberOfResults());

            // Last page
            IkasanDocumentSearchResults page4 = solrGeneralService.search(identifiers, 15, 5, "timestamp", "ASCENDING");
            assertEquals(5, page4.getResultList().size());
            assertEquals(20, page4.getTotalNumberOfResults());

            // Verify no overlap between pages
            assertEquals("id-1", page1.getResultList().get(0).getId());
            assertEquals("id-6", page2.getResultList().get(0).getId());
            assertEquals("id-11", page3.getResultList().get(0).getId());
            assertEquals("id-16", page4.getResultList().get(0).getId());
        }
    }

    @Test
    @DirtiesContext
    public void test_searchByHarvestReceivedTime_with_time_range() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            long now = System.currentTimeMillis();

            // Create documents with different harvest timestamps
            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "id-1");
            doc.addField("type", "type");
            doc.addField("payload", "test");
            doc.addField("harvestReceivedTimestamp", now - 5000); // Before range
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-2");
            doc.addField("type", "type");
            doc.addField("payload", "test");
            doc.addField("harvestReceivedTimestamp", now); // In range
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-3");
            doc.addField("type", "type");
            doc.addField("payload", "test");
            doc.addField("harvestReceivedTimestamp", now + 2000); // In range
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-4");
            doc.addField("type", "type");
            doc.addField("payload", "test");
            doc.addField("harvestReceivedTimestamp", now + 10000); // After range
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            IkasanDocumentSearchResults results = solrGeneralService.searchByHarvestReceivedTime(
                null, null, null, null, null, now - 1000, now + 5000,
                0, 10, null, false, null, null);

            assertEquals(2, results.getResultList().size());

            List<String> ids = results.getResultList().stream()
                .map(IkasanESBDocument::getId)
                .toList();

            assertTrue(ids.contains("id-2"));
            assertTrue(ids.contains("id-3"));
            assertFalse(ids.contains("id-1"));
            assertFalse(ids.contains("id-4"));
        }
    }

    @Test
    @DirtiesContext
    public void test_searchByHarvestReceivedTime_with_module_and_flow() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            long now = System.currentTimeMillis();

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "id-1");
            doc.addField("type", "type");
            doc.addField("moduleName", "module1");
            doc.addField("flowName", "flow1");
            doc.addField("harvestReceivedTimestamp", now);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-2");
            doc.addField("type", "type");
            doc.addField("moduleName", "module1");
            doc.addField("flowName", "flow2");
            doc.addField("harvestReceivedTimestamp", now + 1000);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-3");
            doc.addField("type", "type");
            doc.addField("moduleName", "module2");
            doc.addField("flowName", "flow1");
            doc.addField("harvestReceivedTimestamp", now + 2000);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            Set<String> moduleNames = new HashSet<>();
            moduleNames.add("module1");
            Set<String> flowNames = new HashSet<>();
            flowNames.add("flow1");

            IkasanDocumentSearchResults results = solrGeneralService.searchByHarvestReceivedTime(
                moduleNames, flowNames, null, null, null, now - 1000, now + 10000,
                0, 10, null, false, null, null);

            assertEquals(1, results.getResultList().size());
            assertEquals("module1", results.getResultList().get(0).getModuleName());
            assertEquals("flow1", results.getResultList().get(0).getFlowName());
        }
    }

    @Test
    @DirtiesContext
    public void test_searchByHarvestReceivedTime_with_entity_types() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            long now = System.currentTimeMillis();

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "id-1");
            doc.addField("type", "wiretap");
            doc.addField("harvestReceivedTimestamp", now);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-2");
            doc.addField("type", "error");
            doc.addField("harvestReceivedTimestamp", now + 1000);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-3");
            doc.addField("type", "replay");
            doc.addField("harvestReceivedTimestamp", now + 2000);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            List<String> entityTypes = new ArrayList<>();
            entityTypes.add("wiretap");
            entityTypes.add("error");

            IkasanDocumentSearchResults results = solrGeneralService.searchByHarvestReceivedTime(
                null, null, null, null, null, now - 1000, now + 10000,
                0, 10, entityTypes, false, null, null);

            assertEquals(2, results.getResultList().size());

            List<String> types = results.getResultList().stream()
                .map(IkasanESBDocument::getType)
                .toList();

            assertTrue(types.contains("wiretap"));
            assertTrue(types.contains("error"));
            assertFalse(types.contains("replay"));
        }
    }

    @Test
    @DirtiesContext
    public void test_searchByHarvestReceivedTime_with_pagination() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            long now = System.currentTimeMillis();

            // Create 5 documents
            for (int i = 0; i < 5; i++) {
                SolrInputDocument doc = new SolrInputDocument();
                doc.addField("id", "id-" + i);
                doc.addField("type", "type");
                doc.addField("harvestReceivedTimestamp", now + (i * 1000));
                doc.addField("expiry", System.currentTimeMillis() + 10000000l);
                server.add("ikasan", doc);
            }

            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            IkasanDocumentSearchResults results = solrGeneralService.searchByHarvestReceivedTime(
                null, null, null, null, null, now - 1000, now + 10000,
                2, 2, null, false, null, null);

            assertEquals(2, results.getResultList().size());
            assertEquals(5, results.getTotalNumberOfResults());
        }
    }

    @Test
    @DirtiesContext
    public void test_searchByHarvestReceivedTime_sort_ascending() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            long now = System.currentTimeMillis();

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "id-1");
            doc.addField("type", "type");
            doc.addField("harvestReceivedTimestamp", now + 3000);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-2");
            doc.addField("type", "type");
            doc.addField("harvestReceivedTimestamp", now + 1000);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-3");
            doc.addField("type", "type");
            doc.addField("harvestReceivedTimestamp", now + 2000);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            IkasanDocumentSearchResults results = solrGeneralService.searchByHarvestReceivedTime(
                null, null, null, null, null, now, now + 10000,
                0, 10, null, false, "harvestReceivedTimestamp", "ASCENDING");

            assertEquals(3, results.getResultList().size());
            assertEquals("id-2", results.getResultList().get(0).getId());
            assertEquals("id-3", results.getResultList().get(1).getId());
            assertEquals("id-1", results.getResultList().get(2).getId());
        }
    }

    @Test
    @DirtiesContext
    public void test_searchByHarvestReceivedTime_sort_descending() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            long now = System.currentTimeMillis();

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "id-1");
            doc.addField("type", "type");
            doc.addField("harvestReceivedTimestamp", now + 1000);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-2");
            doc.addField("type", "type");
            doc.addField("harvestReceivedTimestamp", now + 3000);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-3");
            doc.addField("type", "type");
            doc.addField("harvestReceivedTimestamp", now + 2000);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            IkasanDocumentSearchResults results = solrGeneralService.searchByHarvestReceivedTime(
                null, null, null, null, null, now, now + 10000,
                0, 10, null, false, "harvestReceivedTimestamp", "DESCENDING");

            assertEquals(3, results.getResultList().size());
            assertEquals("id-2", results.getResultList().get(0).getId());
            assertEquals("id-3", results.getResultList().get(1).getId());
            assertEquals("id-1", results.getResultList().get(2).getId());
        }
    }

    @Test
    @DirtiesContext
    public void test_searchByHarvestReceivedTime_no_results_outside_range() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            long now = System.currentTimeMillis();

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "id-1");
            doc.addField("type", "type");
            doc.addField("harvestReceivedTimestamp", now - 10000);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            IkasanDocumentSearchResults results = solrGeneralService.searchByHarvestReceivedTime(
                null, null, null, null, null, now, now + 5000,
                0, 10, null, false, null, null);

            assertEquals(0, results.getResultList().size());
            assertEquals(0, results.getTotalNumberOfResults());
        }
    }

    @Test
    @DirtiesContext
    public void test_searchByHarvestReceivedTime_with_all_parameters() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            long now = System.currentTimeMillis();

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "id-1");
            doc.addField("type", "wiretap");
            doc.addField("moduleName", "module1");
            doc.addField("flowName", "flow1");
            doc.addField("componentName", "component1");
            doc.addField("event", "event-123");
            doc.addField("harvestReceivedTimestamp", now);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            Set<String> moduleNames = new HashSet<>();
            moduleNames.add("module1");
            Set<String> flowNames = new HashSet<>();
            flowNames.add("flow1");
            Set<String> componentNames = new HashSet<>();
            componentNames.add("component1");
            List<String> entityTypes = new ArrayList<>();
            entityTypes.add("wiretap");

            IkasanDocumentSearchResults results = solrGeneralService.searchByHarvestReceivedTime(
                moduleNames, flowNames, componentNames, "event-123", null,
                now - 1000, now + 10000, 0, 10, entityTypes, false, null, null);

            assertEquals(1, results.getResultList().size());
            assertEquals("module1", results.getResultList().get(0).getModuleName());
            assertEquals("flow1", results.getResultList().get(0).getFlowName());
            assertEquals("component1", results.getResultList().get(0).getComponentName());
            assertEquals("event-123", results.getResultList().get(0).getEventId());
        }
    }

    @Test
    @DirtiesContext
    public void test_searchByHarvestReceivedTime_multiple_modules() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            long now = System.currentTimeMillis();

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "id-1");
            doc.addField("type", "type");
            doc.addField("moduleName", "module1");
            doc.addField("harvestReceivedTimestamp", now);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-2");
            doc.addField("type", "type");
            doc.addField("moduleName", "module2");
            doc.addField("harvestReceivedTimestamp", now + 1000);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-3");
            doc.addField("type", "type");
            doc.addField("moduleName", "module3");
            doc.addField("harvestReceivedTimestamp", now + 2000);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            Set<String> moduleNames = new HashSet<>();
            moduleNames.add("module1");
            moduleNames.add("module2");

            IkasanDocumentSearchResults results = solrGeneralService.searchByHarvestReceivedTime(
                moduleNames, null, null, null, null, now - 1000, now + 10000,
                0, 10, null, false, null, null);

            assertEquals(2, results.getResultList().size());

            List<String> modules = results.getResultList().stream()
                .map(IkasanESBDocument::getModuleName)
                .toList();

            assertTrue(modules.contains("module1"));
            assertTrue(modules.contains("module2"));
            assertFalse(modules.contains("module3"));
        }
    }

    @Test
    @DirtiesContext
    public void test_searchByHarvestReceivedTime_empty_filters() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            long now = System.currentTimeMillis();

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "id-1");
            doc.addField("type", "wiretap");
            doc.addField("harvestReceivedTimestamp", now);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            doc = new SolrInputDocument();
            doc.addField("id", "id-2");
            doc.addField("type", "error");
            doc.addField("harvestReceivedTimestamp", now + 1000);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            IkasanDocumentSearchResults results = solrGeneralService.searchByHarvestReceivedTime(
                null, null, null, null, null, now - 1000, now + 10000,
                0, 10, null, false, null, null);

            assertEquals(2, results.getResultList().size());
        }
    }

    @Test
    @DirtiesContext
    public void test_searchByHarvestReceivedTime_query_response_time() throws Exception {
        NodeConfig config = new NodeConfig.NodeConfigBuilder("testnode", createTempDir())
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString())
            .build();

        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan"))
        {
            CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
            createRequest.setCoreName("ikasan");
            createRequest.setConfigSet("minimal");
            server.request(createRequest);

            long now = System.currentTimeMillis();

            SolrInputDocument doc = new SolrInputDocument();
            doc.addField("id", "id-1");
            doc.addField("type", "type");
            doc.addField("harvestReceivedTimestamp", now);
            doc.addField("expiry", System.currentTimeMillis() + 10000000l);
            server.add("ikasan", doc);

            server.commit();

            dao = new SolrGeneralDaoImpl();
            dao.setSolrClient(server);

            SolrGeneralServiceImpl solrGeneralService = new SolrGeneralServiceImpl(dao);

            IkasanDocumentSearchResults results = solrGeneralService.searchByHarvestReceivedTime(
                null, null, null, null, null, now - 1000, now + 10000,
                0, 10, null, false, null, null);

            assertNotNull(results);
            assertTrue(results.getQueryResponseTime() >= 0);
        }
    }

    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
    }


}
