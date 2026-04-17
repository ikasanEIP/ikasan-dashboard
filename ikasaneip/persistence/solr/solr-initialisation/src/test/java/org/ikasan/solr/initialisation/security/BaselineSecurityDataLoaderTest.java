package org.ikasan.solr.initialisation.security;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.security.dao.SolrIkasanPrincipalDaoImpl;
import org.ikasan.security.dao.SolrPolicyDaoImpl;
import org.ikasan.security.dao.SolrRoleDaoImpl;
import org.ikasan.security.dao.SolrUserDaoImpl;
import org.ikasan.spec.security.model.IkasanPrincipal;
import org.ikasan.spec.security.model.Policy;
import org.ikasan.spec.security.model.Role;
import org.ikasan.spec.security.model.User;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class BaselineSecurityDataLoaderTest extends SolrTestCaseJ4 {

    private BaselineSecurityDataLoader dataLoader;
    private SolrPolicyDaoImpl policyDao;
    private SolrRoleDaoImpl roleDao;
    private SolrIkasanPrincipalDaoImpl principalDao;
    private SolrUserDaoImpl userDao;
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

        policyDao = new SolrPolicyDaoImpl();
        policyDao.setSolrClient(server);

        roleDao = new SolrRoleDaoImpl(policyDao);
        roleDao.setSolrClient(server);

        principalDao = new SolrIkasanPrincipalDaoImpl(roleDao);
        principalDao.setSolrClient(server);

        userDao = new SolrUserDaoImpl(principalDao);
        userDao.setSolrClient(server);

        dataLoader = new BaselineSecurityDataLoader(policyDao, roleDao, principalDao, userDao);
    }

    @Test
    public void test_loadBaselineData() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);
            // Load baseline data
            dataLoader.execute();

            // Verify policies were loaded
            List<Policy> policies = policyDao.getAllPolicies();
            Assert.assertNotNull(policies);
            Assert.assertTrue("Should have loaded 104 policies", policies.size() >= 100);

            // Verify specific policies exist
            Policy allPolicy = policyDao.getPolicyById("ALL-securityPolicy");
            Assert.assertNotNull("ALL policy should exist", allPolicy);
            Assert.assertEquals("ALL", allPolicy.getName());
            Assert.assertEquals("Policy to do everything", allPolicy.getDescription());

            Policy wiretapReadPolicy = policyDao.getPolicyById("wiretap-read-securityPolicy");
            Assert.assertNotNull("wiretap-read policy should exist", wiretapReadPolicy);
            Assert.assertEquals("wiretap-read", wiretapReadPolicy.getName());

            // Verify roles were loaded
            List<Role> roles = roleDao.getAllRoles();
            Assert.assertNotNull(roles);
            Assert.assertEquals("Should have loaded 5 roles", 5, roles.size());

            // Verify ADMIN role
            Role adminRole = roleDao.getRoleById("ADMIN-securityRole");
            Assert.assertNotNull("ADMIN role should exist", adminRole);
            Assert.assertEquals("ADMIN", adminRole.getName());
            Assert.assertEquals("Users who may perform administration functions on the system", adminRole.getDescription());
            Assert.assertNotNull("ADMIN role should have policies", adminRole.getPolicies());
            Assert.assertFalse("ADMIN role should have at least 1 policy", adminRole.getPolicies().isEmpty());

            // Verify User role
            Role userRole = roleDao.getRoleById("User-securityRole");
            Assert.assertNotNull("User role should exist", userRole);
            Assert.assertEquals("User", userRole.getName());
            Assert.assertNotNull("User role should have policies", userRole.getPolicies());
            Assert.assertTrue("User role should have multiple policies", userRole.getPolicies().size() > 10);

            // Verify principals were loaded
            List<IkasanPrincipal> principals = principalDao.getAllPrincipals();
            Assert.assertNotNull(principals);
            Assert.assertEquals("Should have loaded 1 principal", 1, principals.size());

            // Verify admin principal
            IkasanPrincipal adminPrincipal = principalDao.findById("admin-securityPrincipal");
            Assert.assertNotNull("admin principal should exist", adminPrincipal);
            Assert.assertEquals("admin", adminPrincipal.getName());
            Assert.assertEquals("user", adminPrincipal.getType());
            Assert.assertEquals("The administrator user", adminPrincipal.getDescription());
            Assert.assertNotNull("admin principal should have roles", adminPrincipal.getRoles());
            Assert.assertEquals("admin principal should have 1 role", 1, adminPrincipal.getRoles().size());

            // Verify users were loaded
            User adminUser = userDao.getUser("admin");
            Assert.assertNotNull("admin user should exist", adminUser);
            Assert.assertEquals("admin", adminUser.getUsername());
            Assert.assertEquals("{bcrypt}$2a$10$OuV2SIg.0Nj3zsO7LUpTEOuh2N6YrAheAeE3rPCKzgtqVR.mMZqSW", adminUser.getPassword());
            Assert.assertEquals("Admin", adminUser.getFirstName());
            Assert.assertEquals("User", adminUser.getSurname());
            Assert.assertTrue("admin user should be enabled", adminUser.isEnabled());
            Assert.assertNotNull("admin user should have principals", adminUser.getPrincipals());
            Assert.assertEquals("admin user should have 1 principal", 1, adminUser.getPrincipals().size());
        }
    }

    @Test
    public void test_loadBaselineData_policies_include_expected_names() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);
            dataLoader.execute();

            // Verify specific policy names
            Assert.assertNotNull(policyDao.getPolicyById("ALL-securityPolicy"));
            Assert.assertNotNull(policyDao.getPolicyById("wiretap-read-securityPolicy"));
            Assert.assertNotNull(policyDao.getPolicyById("error-read-securityPolicy"));
            Assert.assertNotNull(policyDao.getPolicyById("WebServiceAdmin-securityPolicy"));
            Assert.assertNotNull(policyDao.getPolicyById("scheduler-read-securityPolicy"));
            Assert.assertNotNull(policyDao.getPolicyById("dashboard-admin-securityPolicy"));
        }
    }

    @Test
    public void test_loadBaselineData_roles_have_correct_policy_associations() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);
            dataLoader.execute();

            // Verify RESTAccess role has WebServiceAdmin policy
            Role restAccessRole = roleDao.getRoleById("RESTAccess-securityRole");
            Assert.assertNotNull(restAccessRole);
            Assert.assertEquals("RESTAccess", restAccessRole.getName());
            Assert.assertNotNull(restAccessRole.getPolicies());
            Assert.assertEquals("RESTAccess should have 1 policy", 1, restAccessRole.getPolicies().size());

            boolean hasWebServiceAdmin = restAccessRole.getPolicies().stream()
                .anyMatch(p -> "WebServiceAdmin".equals(p.getName()));
            Assert.assertTrue("RESTAccess role should have WebServiceAdmin policy", hasWebServiceAdmin);
        }
    }

    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
    }
}
