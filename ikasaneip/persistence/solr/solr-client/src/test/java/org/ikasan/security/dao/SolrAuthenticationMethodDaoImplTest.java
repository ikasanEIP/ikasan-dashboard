package org.ikasan.security.dao;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.security.model.SolrAuthenticationMethodImpl;
import org.ikasan.spec.security.model.AuthenticationMethod;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;

public class SolrAuthenticationMethodDaoImplTest extends SolrTestCaseJ4 {

    private SolrAuthenticationMethodDaoImpl dao;
    private NodeConfig config;
    private Path tmppath;

    @Before
    public void setup() {
        tmppath = createTempDir();
        config = new NodeConfig.NodeConfigBuilder("testnode", tmppath)
            .setConfigSetBaseDirectory(Paths.get(TEST_HOME()).resolve("configsets").toString()).build();
    }

    @After
    public void teardown() throws IOException {
        FileSystemUtils.deleteRecursively(tmppath);
    }

    private void init(EmbeddedSolrServer server) throws IOException, SolrServerException {
        CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
        createRequest.setCoreName("ikasan");
        createRequest.setConfigSet("minimal");
        server.request(createRequest);

        dao = new SolrAuthenticationMethodDaoImpl();
        dao.setSolrClient(server);
    }

    @Test
    public void test_createAuthenticationMethod() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            AuthenticationMethod authMethod = dao.createAuthenticationMethod();

            Assert.assertNotNull(authMethod);
            Assert.assertTrue(authMethod instanceof SolrAuthenticationMethodImpl);
        }
    }

    @Test
    public void test_saveOrUpdateAuthenticationMethod_and_getAuthenticationMethod() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrAuthenticationMethodImpl authMethod = new SolrAuthenticationMethodImpl();
            authMethod.setName("LDAP");
            authMethod.setOrder(1L);
            authMethod.setMethod("ldap");
            authMethod.setLastSynchronised(new Date(1000000L));

            dao.saveOrUpdateAuthenticationMethod(authMethod);

            AuthenticationMethod found = dao.getAuthenticationMethod(authMethod.getId());

            Assert.assertNotNull(found);
            Assert.assertEquals("LDAP", found.getName());
            Assert.assertEquals(Long.valueOf(1L), found.getOrder());
            Assert.assertEquals("ldap", found.getMethod());
        }
    }

    @Test
    public void test_saveOrUpdateAuthenticationMethod_update_existing() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrAuthenticationMethodImpl authMethod = new SolrAuthenticationMethodImpl();
            authMethod.setName("LOCAL");
            authMethod.setOrder(1L);
            authMethod.setMethod("local");

            dao.saveOrUpdateAuthenticationMethod(authMethod);

            // Update the authentication method
            authMethod.setOrder(2L);
            authMethod.setMethod("local-updated");

            dao.saveOrUpdateAuthenticationMethod(authMethod);

            AuthenticationMethod found = dao.getAuthenticationMethod(authMethod.getId());

            Assert.assertNotNull(found);
            Assert.assertEquals("LOCAL", found.getName());
            Assert.assertEquals(Long.valueOf(2L), found.getOrder());
            Assert.assertEquals("local-updated", found.getMethod());
        }
    }

    @Test
    public void test_getAuthenticationMethod_not_found() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            AuthenticationMethod found = dao.getAuthenticationMethod("NonExistent");

            Assert.assertNull(found);
        }
    }

    @Test
    public void test_getAuthenticationMethods() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrAuthenticationMethodImpl authMethod1 = new SolrAuthenticationMethodImpl();
            authMethod1.setName("LDAP");
            authMethod1.setOrder(2L);
            authMethod1.setMethod("ldap");

            SolrAuthenticationMethodImpl authMethod2 = new SolrAuthenticationMethodImpl();
            authMethod2.setName("LOCAL");
            authMethod2.setOrder(1L);
            authMethod2.setMethod("local");

            SolrAuthenticationMethodImpl authMethod3 = new SolrAuthenticationMethodImpl();
            authMethod3.setName("SAML");
            authMethod3.setOrder(3L);
            authMethod3.setMethod("saml");

            dao.saveOrUpdateAuthenticationMethod(authMethod1);
            dao.saveOrUpdateAuthenticationMethod(authMethod2);
            dao.saveOrUpdateAuthenticationMethod(authMethod3);

            List<AuthenticationMethod> allMethods = dao.getAuthenticationMethods();

            Assert.assertEquals(3, allMethods.size());
            // Verify they are ordered by order field
            Assert.assertEquals("LOCAL", allMethods.get(0).getName());
            Assert.assertEquals("LDAP", allMethods.get(1).getName());
            Assert.assertEquals("SAML", allMethods.get(2).getName());
        }
    }

    @Test
    public void test_getAuthenticationMethods_empty() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            List<AuthenticationMethod> allMethods = dao.getAuthenticationMethods();

            Assert.assertNotNull(allMethods);
            Assert.assertTrue(allMethods.isEmpty());
        }
    }

    @Test
    public void test_getNumberOfAuthenticationMethods() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrAuthenticationMethodImpl authMethod1 = new SolrAuthenticationMethodImpl();
            authMethod1.setName("LDAP");
            authMethod1.setOrder(1L);

            SolrAuthenticationMethodImpl authMethod2 = new SolrAuthenticationMethodImpl();
            authMethod2.setName("LOCAL");
            authMethod2.setOrder(2L);

            dao.saveOrUpdateAuthenticationMethod(authMethod1);
            dao.saveOrUpdateAuthenticationMethod(authMethod2);

            long count = dao.getNumberOfAuthenticationMethods();

            Assert.assertEquals(2, count);
        }
    }

    @Test
    public void test_getNumberOfAuthenticationMethods_empty() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            long count = dao.getNumberOfAuthenticationMethods();

            Assert.assertEquals(0, count);
        }
    }

    @Test
    public void test_getAuthenticationMethodByOrder() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrAuthenticationMethodImpl authMethod1 = new SolrAuthenticationMethodImpl();
            authMethod1.setName("LDAP");
            authMethod1.setOrder(5L);
            authMethod1.setMethod("ldap");

            SolrAuthenticationMethodImpl authMethod2 = new SolrAuthenticationMethodImpl();
            authMethod2.setName("LOCAL");
            authMethod2.setOrder(10L);
            authMethod2.setMethod("local");

            dao.saveOrUpdateAuthenticationMethod(authMethod1);
            dao.saveOrUpdateAuthenticationMethod(authMethod2);

            AuthenticationMethod found = dao.getAuthenticationMethodByOrder(5L);

            Assert.assertNotNull(found);
            Assert.assertEquals("LDAP", found.getName());
            Assert.assertEquals(Long.valueOf(5L), found.getOrder());
            Assert.assertEquals("ldap", found.getMethod());
        }
    }

    @Test
    public void test_getAuthenticationMethodByOrder_not_found() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrAuthenticationMethodImpl authMethod = new SolrAuthenticationMethodImpl();
            authMethod.setName("LDAP");
            authMethod.setOrder(Long.valueOf(5L));

            dao.saveOrUpdateAuthenticationMethod(authMethod);

            AuthenticationMethod found = dao.getAuthenticationMethodByOrder(99L);

            Assert.assertNull(found);
        }
    }

    @Test
    public void test_deleteAuthenticationMethod() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrAuthenticationMethodImpl authMethod = new SolrAuthenticationMethodImpl();
            authMethod.setName("LDAP");
            authMethod.setOrder(1L);

            dao.saveOrUpdateAuthenticationMethod(authMethod);

            AuthenticationMethod found = dao.getAuthenticationMethod(authMethod.getId());
            Assert.assertNotNull(found);

            dao.deleteAuthenticationMethod(authMethod);

            AuthenticationMethod notFound = dao.getAuthenticationMethod(authMethod.getId());
            Assert.assertNull(notFound);
        }
    }

    @Test
    public void test_deleteAuthenticationMethod_and_verify_count() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrAuthenticationMethodImpl authMethod1 = new SolrAuthenticationMethodImpl();
            authMethod1.setName("LDAP");
            authMethod1.setOrder(1L);

            SolrAuthenticationMethodImpl authMethod2 = new SolrAuthenticationMethodImpl();
            authMethod2.setName("LOCAL");
            authMethod2.setOrder(2L);

            dao.saveOrUpdateAuthenticationMethod(authMethod1);
            dao.saveOrUpdateAuthenticationMethod(authMethod2);

            Assert.assertEquals(2, dao.getNumberOfAuthenticationMethods());

            dao.deleteAuthenticationMethod(authMethod1);

            Assert.assertEquals(1, dao.getNumberOfAuthenticationMethods());
            Assert.assertNull(dao.getAuthenticationMethod(authMethod1.getId()));
            Assert.assertNotNull(dao.getAuthenticationMethod(authMethod2.getId()));
        }
    }

    @Test
    public void test_saveOrUpdateAuthenticationMethod_with_null_lastSynchronised() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrAuthenticationMethodImpl authMethod = new SolrAuthenticationMethodImpl();
            authMethod.setName("LDAP");
            authMethod.setOrder(1L);
            authMethod.setMethod("ldap");
            authMethod.setLastSynchronised(null);

            dao.saveOrUpdateAuthenticationMethod(authMethod);

            AuthenticationMethod found = dao.getAuthenticationMethod(authMethod.getId());

            Assert.assertNotNull(found);
            Assert.assertEquals("LDAP", found.getName());
        }
    }

    @Test
    public void test_getAuthenticationMethods_ordering_with_gaps() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrAuthenticationMethodImpl authMethod1 = new SolrAuthenticationMethodImpl();
            authMethod1.setName("Method1");
            authMethod1.setOrder(100L);

            SolrAuthenticationMethodImpl authMethod2 = new SolrAuthenticationMethodImpl();
            authMethod2.setName("Method2");
            authMethod2.setOrder(10L);

            SolrAuthenticationMethodImpl authMethod3 = new SolrAuthenticationMethodImpl();
            authMethod3.setName("Method3");
            authMethod3.setOrder(50L);

            dao.saveOrUpdateAuthenticationMethod(authMethod1);
            dao.saveOrUpdateAuthenticationMethod(authMethod2);
            dao.saveOrUpdateAuthenticationMethod(authMethod3);

            List<AuthenticationMethod> allMethods = dao.getAuthenticationMethods();

            Assert.assertEquals(3, allMethods.size());
            Assert.assertEquals("Method2", allMethods.get(0).getName());
            Assert.assertEquals(Long.valueOf(10L), allMethods.get(0).getOrder());
            Assert.assertEquals("Method3", allMethods.get(1).getName());
            Assert.assertEquals(Long.valueOf(50L), allMethods.get(1).getOrder());
            Assert.assertEquals("Method1", allMethods.get(2).getName());
            Assert.assertEquals(Long.valueOf(100L), allMethods.get(2).getOrder());
        }
    }

    @Test
    public void test_multiple_authentication_methods_with_same_order() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrAuthenticationMethodImpl authMethod1 = new SolrAuthenticationMethodImpl();
            authMethod1.setName("LDAP");
            authMethod1.setOrder(1L);

            SolrAuthenticationMethodImpl authMethod2 = new SolrAuthenticationMethodImpl();
            authMethod2.setName("LOCAL");
            authMethod2.setOrder(1L);

            dao.saveOrUpdateAuthenticationMethod(authMethod1);
            dao.saveOrUpdateAuthenticationMethod(authMethod2);

            // Both should be retrievable
            AuthenticationMethod found1 = dao.getAuthenticationMethod(authMethod1.getId());
            AuthenticationMethod found2 = dao.getAuthenticationMethod(authMethod2.getId());

            Assert.assertNotNull(found1);
            Assert.assertNotNull(found2);
            Assert.assertEquals(2, dao.getNumberOfAuthenticationMethods());
        }
    }

    @Test
    public void test_saveOrUpdateAuthenticationMethod_with_complex_data() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrAuthenticationMethodImpl authMethod = new SolrAuthenticationMethodImpl();
            authMethod.setName("ComplexAuth");
            authMethod.setOrder(1L);
            authMethod.setMethod("complex-method-type");
            authMethod.setLastSynchronised(new Date());

            dao.saveOrUpdateAuthenticationMethod(authMethod);

            AuthenticationMethod found = dao.getAuthenticationMethod(authMethod.getId());

            Assert.assertNotNull(found);
            Assert.assertEquals("ComplexAuth", found.getName());
            Assert.assertEquals("complex-method-type", found.getMethod());
            Assert.assertEquals(Long.valueOf(1L), found.getOrder());
        }
    }

    @Test
    public void test_getAuthenticationMethod_with_special_characters_in_name() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrAuthenticationMethodImpl authMethod = new SolrAuthenticationMethodImpl();
            authMethod.setName("Auth-Method_1");
            authMethod.setOrder(1L);

            dao.saveOrUpdateAuthenticationMethod(authMethod);

            AuthenticationMethod found = dao.getAuthenticationMethod(authMethod.getId());

            Assert.assertNotNull(found);
            Assert.assertEquals("Auth-Method_1", found.getName());
        }
    }

    @Test
    public void test_deleteAuthenticationMethod_not_existing() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrAuthenticationMethodImpl authMethod = new SolrAuthenticationMethodImpl();
            authMethod.setName("NonExistent");
            authMethod.setOrder(1L);

            // Should not throw exception
            dao.deleteAuthenticationMethod(authMethod);

            Assert.assertEquals(0, dao.getNumberOfAuthenticationMethods());
        }
    }

    @Test
    public void test_saveOrUpdateAuthenticationMethod_order_zero() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrAuthenticationMethodImpl authMethod = new SolrAuthenticationMethodImpl();
            authMethod.setName("ZeroOrder");
            authMethod.setOrder(0L);

            dao.saveOrUpdateAuthenticationMethod(authMethod);

            AuthenticationMethod found = dao.getAuthenticationMethodByOrder(0L);

            Assert.assertNotNull(found);
            Assert.assertEquals("ZeroOrder", found.getName());
            Assert.assertEquals(Long.valueOf(0L), found.getOrder());
        }
    }

    @Test
    public void test_saveOrUpdateAuthenticationMethod_negative_order() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrAuthenticationMethodImpl authMethod = new SolrAuthenticationMethodImpl();
            authMethod.setName("NegativeOrder");
            authMethod.setOrder(-1L);

            dao.saveOrUpdateAuthenticationMethod(authMethod);

            AuthenticationMethod found = dao.getAuthenticationMethodByOrder(-1L);

            Assert.assertNotNull(found);
            Assert.assertEquals("NegativeOrder", found.getName());
            Assert.assertEquals(Long.valueOf(-1L), found.getOrder());
        }
    }

    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
    }
}
