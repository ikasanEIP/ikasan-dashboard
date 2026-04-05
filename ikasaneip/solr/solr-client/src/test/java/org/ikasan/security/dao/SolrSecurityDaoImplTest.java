package org.ikasan.security.dao;

import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.NodeConfig;
import org.ikasan.security.model.*;
import org.ikasan.spec.security.model.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SolrSecurityDaoImplTest extends SolrTestCaseJ4 {

    private SolrSecurityDaoImpl dao;
    private SolrIkasanPrincipalDaoImpl solrIkasanPrincipalDao;
    private SolrRoleDaoImpl solrRoleDao;
    private SolrPolicyDaoImpl solrPolicyDao;
    private SolrAuthenticationMethodDaoImpl solrAuthenticationMethodDao;
    private SolrUserDaoImpl solrUserDao;
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

        this.solrPolicyDao = new SolrPolicyDaoImpl();
        this.solrPolicyDao.setSolrClient(server);

        this.solrRoleDao = new SolrRoleDaoImpl(solrPolicyDao);
        this.solrRoleDao.setSolrClient(server);

        this.solrIkasanPrincipalDao = new SolrIkasanPrincipalDaoImpl(solrRoleDao);
        this.solrIkasanPrincipalDao.setSolrClient(server);

        this.solrAuthenticationMethodDao = new SolrAuthenticationMethodDaoImpl();
        this.solrAuthenticationMethodDao.setSolrClient(server);

        this.solrUserDao = new SolrUserDaoImpl(solrIkasanPrincipalDao);
        this.solrUserDao.setSolrClient(server);

        this.dao = new SolrSecurityDaoImpl(solrIkasanPrincipalDao, solrPolicyDao, solrRoleDao,
            solrAuthenticationMethodDao, solrUserDao);
    }

    @Test
    public void test_createPrincipal() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            IkasanPrincipal principal = dao.createPrincipal();

            Assert.assertNotNull(principal);
            Assert.assertTrue(principal instanceof SolrIkasanPrincipalImpl);
        }
    }

    @Test
    public void test_createRole() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            Role role = dao.createRole();

            Assert.assertNotNull(role);
            Assert.assertTrue(role instanceof SolrRoleImpl);
        }
    }

    @Test
    public void test_createPolicy() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            Policy policy = dao.createPolicy();

            Assert.assertNotNull(policy);
            Assert.assertTrue(policy instanceof SolrPolicyImpl);
        }
    }

    @Test
    public void test_createRoleModule() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            RoleModule roleModule = dao.createRoleModule();

            Assert.assertNotNull(roleModule);
            Assert.assertTrue(roleModule instanceof SolrRoleModuleImpl);
        }
    }

    @Test
    public void test_createRoleJobPlan() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            RoleJobPlan roleJobPlan = dao.createRoleJobPlan();

            Assert.assertNotNull(roleJobPlan);
            Assert.assertTrue(roleJobPlan instanceof SolrRoleJobPlanImpl);
        }
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
    public void test_saveOrUpdateRole_and_getRoleByName() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("TestRole");
            role.setDescription("Test role description");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            dao.saveOrUpdateRole(role);

            Role found = dao.getRoleByName("TestRole");

            Assert.assertNotNull(found);
            Assert.assertEquals("TestRole", found.getName());
            Assert.assertEquals("Test role description", found.getDescription());
        }
    }

    @Test
    public void test_deleteRole() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleToDelete");
            role.setDescription("Will be deleted");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            dao.saveOrUpdateRole(role);
            Role found = dao.getRoleByName("RoleToDelete");
            Assert.assertNotNull(found);

            dao.deleteRole(role);
            found = dao.getRoleByName("RoleToDelete");
            Assert.assertNull(found);
        }
    }

    @Test
    public void test_saveOrUpdatePolicy_and_getPolicyByName() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrPolicyImpl policy = new SolrPolicyImpl();
            policy.setName("TestPolicy");
            policy.setDescription("Test policy description");
            policy.setCreatedDateTime(new Date());
            policy.setUpdatedDateTime(new Date());

            dao.saveOrUpdatePolicy(policy);

            Policy found = dao.getPolicyByName("TestPolicy");

            Assert.assertNotNull(found);
            Assert.assertEquals("TestPolicy", found.getName());
            Assert.assertEquals("Test policy description", found.getDescription());
        }
    }

    @Test
    public void test_deletePolicy() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrPolicyImpl policy = new SolrPolicyImpl();
            policy.setName("PolicyToDelete");
            policy.setDescription("Will be deleted");
            policy.setCreatedDateTime(new Date());
            policy.setUpdatedDateTime(new Date());

            dao.saveOrUpdatePolicy(policy);
            Policy found = dao.getPolicyByName("PolicyToDelete");
            Assert.assertNotNull(found);

            dao.deletePolicy(policy);
            found = dao.getPolicyByName("PolicyToDelete");
            Assert.assertNull(found);
        }
    }

    @Test
    public void test_saveOrUpdatePrincipal_and_getPrincipalByName() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrIkasanPrincipalImpl principal = new SolrIkasanPrincipalImpl();
            principal.setName("TestPrincipal");
            principal.setType("user");
            principal.setDescription("Test principal");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());

            dao.saveOrUpdatePrincipal(principal);

            IkasanPrincipal found = dao.getPrincipalByName("TestPrincipal");

            Assert.assertNotNull(found);
            Assert.assertEquals("TestPrincipal", found.getName());
            Assert.assertEquals("user", found.getType());
        }
    }

    @Test
    public void test_deletePrincipal() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrIkasanPrincipalImpl principal = new SolrIkasanPrincipalImpl();
            principal.setName("PrincipalToDelete");
            principal.setType("user");
            principal.setDescription("Will be deleted");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());

            dao.saveOrUpdatePrincipal(principal);
            IkasanPrincipal found = dao.getPrincipalByName("PrincipalToDelete");
            Assert.assertNotNull(found);

            dao.deletePrincipal(principal);
            found = dao.getPrincipalByName("PrincipalToDelete");
            Assert.assertNull(found);
        }
    }

    @Test
    public void test_getAllPolicies() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            for (int i = 0; i < 5; i++) {
                SolrPolicyImpl policy = new SolrPolicyImpl();
                policy.setName("Policy" + i);
                policy.setDescription("Description " + i);
                policy.setCreatedDateTime(new Date());
                policy.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePolicy(policy);
            }

            List<Policy> policies = dao.getAllPolicies();

            Assert.assertNotNull(policies);
            Assert.assertEquals(5, policies.size());
        }
    }

    @Test
    public void test_getAllRoles() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            for (int i = 0; i < 3; i++) {
                SolrRoleImpl role = new SolrRoleImpl();
                role.setName("Role" + i);
                role.setDescription("Description " + i);
                role.setCreatedDateTime(new Date());
                role.setUpdatedDateTime(new Date());
                dao.saveOrUpdateRole(role);
            }

            List<Role> roles = dao.getAllRoles();

            Assert.assertNotNull(roles);
            Assert.assertEquals(3, roles.size());
        }
    }

    @Test
    public void test_getAllPrincipals() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            for (int i = 0; i < 4; i++) {
                SolrIkasanPrincipalImpl principal = new SolrIkasanPrincipalImpl();
                principal.setName("Principal" + i);
                principal.setType("user");
                principal.setDescription("Description " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            List<IkasanPrincipal> principals = dao.getAllPrincipals();

            Assert.assertNotNull(principals);
            Assert.assertEquals(4, principals.size());
        }
    }

    @Test
    public void test_getPrincipals_with_pagination() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            for (int i = 0; i < 10; i++) {
                SolrIkasanPrincipalImpl principal = new SolrIkasanPrincipalImpl();
                principal.setName("Principal" + i);
                principal.setType("user");
                principal.setDescription("Description " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            List<IkasanPrincipal> page1 = dao.getPrincipals(null, 5, 0);
            Assert.assertEquals(5, page1.size());

            List<IkasanPrincipal> page2 = dao.getPrincipals(null, 5, 5);
            Assert.assertEquals(5, page2.size());
        }
    }

    @Test
    public void test_getAllPrincipalLites() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            for (int i = 0; i < 3; i++) {
                SolrIkasanPrincipalImpl principal = new SolrIkasanPrincipalImpl();
                principal.setName("Principal" + i);
                principal.setType("user");
                principal.setDescription("Description " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            List<IkasanPrincipalLite> principals = dao.getAllPrincipalLites();

            Assert.assertNotNull(principals);
            Assert.assertEquals(3, principals.size());
        }
    }

    @Test
    public void test_getPrincipalLites_with_pagination() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            for (int i = 0; i < 8; i++) {
                SolrIkasanPrincipalImpl principal = new SolrIkasanPrincipalImpl();
                principal.setName("Principal" + i);
                principal.setType("user");
                principal.setDescription("Description " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            List<IkasanPrincipalLite> page1 = dao.getPrincipalLites(null, 3, 0);
            Assert.assertEquals(3, page1.size());

            List<IkasanPrincipalLite> page2 = dao.getPrincipalLites(null, 3, 3);
            Assert.assertEquals(3, page2.size());
        }
    }

    @Test
    public void test_getPrincipalCount() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            int initialCount = dao.getPrincipalCount(null);
            Assert.assertEquals(0, initialCount);

            for (int i = 0; i < 7; i++) {
                SolrIkasanPrincipalImpl principal = new SolrIkasanPrincipalImpl();
                principal.setName("Principal" + i);
                principal.setType("user");
                principal.setDescription("Description " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            int count = dao.getPrincipalCount(null);
            Assert.assertEquals(7, count);
        }
    }

    @Test
    public void test_getAllPrincipalsWithRole() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create role
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("TestRole");
            role.setDescription("Test role");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(role);

            // Create principals with role
            for (int i = 0; i < 3; i++) {
                SolrIkasanPrincipalImpl principal = new SolrIkasanPrincipalImpl();
                principal.setName("Principal" + i);
                principal.setType("user");
                principal.setDescription("Description " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                principal.addRole(role);
                dao.saveOrUpdatePrincipal(principal);
            }

            List<IkasanPrincipal> principals = dao.getAllPrincipalsWithRole("TestRole");

            Assert.assertNotNull(principals);
            Assert.assertEquals(3, principals.size());
        }
    }

    @Test
    public void test_getAllPrincipalsWithRole_with_pagination() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create role
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("PaginatedRole");
            role.setDescription("Paginated role");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(role);

            // Create 10 principals with role
            for (int i = 0; i < 10; i++) {
                SolrIkasanPrincipalImpl principal = new SolrIkasanPrincipalImpl();
                principal.setName("Principal" + i);
                principal.setType("user");
                principal.setDescription("Description " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                principal.addRole(role);
                dao.saveOrUpdatePrincipal(principal);
            }

            List<IkasanPrincipalLite> page1 = dao.getAllPrincipalsWithRole("PaginatedRole", null, 5, 0);
            Assert.assertEquals(5, page1.size());

            List<IkasanPrincipalLite> page2 = dao.getAllPrincipalsWithRole("PaginatedRole", null, 5, 5);
            Assert.assertEquals(5, page2.size());
        }
    }

    @Test
    public void test_getAllPrincipalsWithoutRole() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create role
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("ExclusionRole");
            role.setDescription("Exclusion role");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(role);

            // Create principals with role
            for (int i = 0; i < 2; i++) {
                SolrIkasanPrincipalImpl principal = new SolrIkasanPrincipalImpl();
                principal.setName("WithRole" + i);
                principal.setType("user");
                principal.setDescription("Has role");
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                principal.addRole(role);
                dao.saveOrUpdatePrincipal(principal);
            }

            // Create principals without role
            for (int i = 0; i < 5; i++) {
                SolrIkasanPrincipalImpl principal = new SolrIkasanPrincipalImpl();
                principal.setName("WithoutRole" + i);
                principal.setType("user");
                principal.setDescription("No role");
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            List<IkasanPrincipalLite> principals = dao.getAllPrincipalsWithoutRole("ExclusionRole", null, 100, 0);

            Assert.assertNotNull(principals);
            Assert.assertEquals(5, principals.size());
            for (IkasanPrincipalLite principal : principals) {
                Assert.assertTrue(principal.getName().startsWith("WithoutRole"));
            }
        }
    }

    @Test
    public void test_getPrincipalsWithRoleCount() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create role
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("CountRole");
            role.setDescription("Count role");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(role);

            // Create principals with role
            for (int i = 0; i < 7; i++) {
                SolrIkasanPrincipalImpl principal = new SolrIkasanPrincipalImpl();
                principal.setName("Principal" + i);
                principal.setType("user");
                principal.setDescription("Description " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                principal.addRole(role);
                dao.saveOrUpdatePrincipal(principal);
            }

            int count = dao.getPrincipalsWithRoleCount("CountRole", null);
            Assert.assertEquals(7, count);
        }
    }

    @Test
    public void test_getPrincipalsWithoutRoleCount() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create role
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("CountExclusionRole");
            role.setDescription("Count exclusion role");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(role);

            // Create principals with role
            for (int i = 0; i < 3; i++) {
                SolrIkasanPrincipalImpl principal = new SolrIkasanPrincipalImpl();
                principal.setName("WithRole" + i);
                principal.setType("user");
                principal.setDescription("Has role");
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                principal.addRole(role);
                dao.saveOrUpdatePrincipal(principal);
            }

            // Create principals without role
            for (int i = 0; i < 6; i++) {
                SolrIkasanPrincipalImpl principal = new SolrIkasanPrincipalImpl();
                principal.setName("WithoutRole" + i);
                principal.setType("user");
                principal.setDescription("No role");
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            int count = dao.getPrincipalsWithoutRoleCount("CountExclusionRole", null);
            Assert.assertEquals(6, count);
        }
    }

    @Test
    public void test_getPrincipalsByRoleNames() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create two roles
            SolrRoleImpl role1 = new SolrRoleImpl();
            role1.setName("Role1");
            role1.setDescription("First role");
            role1.setCreatedDateTime(new Date());
            role1.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(role1);

            SolrRoleImpl role2 = new SolrRoleImpl();
            role2.setName("Role2");
            role2.setDescription("Second role");
            role2.setCreatedDateTime(new Date());
            role2.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(role2);

            // Create principals with role1
            for (int i = 0; i < 2; i++) {
                SolrIkasanPrincipalImpl principal = new SolrIkasanPrincipalImpl();
                principal.setName("Role1Principal" + i);
                principal.setType("user");
                principal.setDescription("Role1 principal");
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                principal.addRole(role1);
                dao.saveOrUpdatePrincipal(principal);
            }

            // Create principals with role2
            for (int i = 0; i < 3; i++) {
                SolrIkasanPrincipalImpl principal = new SolrIkasanPrincipalImpl();
                principal.setName("Role2Principal" + i);
                principal.setType("user");
                principal.setDescription("Role2 principal");
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                principal.addRole(role2);
                dao.saveOrUpdatePrincipal(principal);
            }

            List<IkasanPrincipal> principals = dao.getPrincipalsByRoleNames(Arrays.asList("Role1", "Role2"));

            Assert.assertNotNull(principals);
            Assert.assertEquals(5, principals.size());
        }
    }

    @Test
    public void test_getAllPoliciesWithRole() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create role
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("PolicyRole");
            role.setDescription("Policy role");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());

            // Create policies with role
            for (int i = 0; i < 3; i++) {
                SolrPolicyImpl policy = new SolrPolicyImpl();
                policy.setName("Policy" + i);
                policy.setDescription("Description " + i);
                policy.setCreatedDateTime(new Date());
                policy.setUpdatedDateTime(new Date());
                role.addPolicy(policy);
                dao.saveOrUpdatePolicy(policy);
            }

            dao.saveOrUpdateRole(role);

            List<Policy> policies = dao.getAllPoliciesWithRole("PolicyRole");

            Assert.assertNotNull(policies);
            Assert.assertEquals(3, policies.size());
        }
    }

    @Test
    public void test_getRoleById() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("RoleById");
            role.setDescription("Role by ID");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(role);

            Role savedRole = dao.getRoleByName("RoleById");
            Object roleId = savedRole.getId();

            Role found = dao.getRoleById(roleId);

            Assert.assertNotNull(found);
            Assert.assertEquals("RoleById", found.getName());
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


            dao.saveOrUpdateAuthenticationMethod(authMethod);

            AuthenticationMethod saved = dao.getAuthenticationMethod(authMethod.getId());
            Assert.assertNotNull(saved);

            AuthenticationMethod found = dao.getAuthenticationMethod(saved.getId());

            Assert.assertNotNull(found);
            Assert.assertEquals("LDAP", found.getName());
            Assert.assertEquals(1L, found.getOrder().longValue());
        }
    }

    @Test
    public void test_getAuthenticationMethods() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            for (int i = 0; i < 3; i++) {
                SolrAuthenticationMethodImpl authMethod = new SolrAuthenticationMethodImpl();
                authMethod.setName("AuthMethod" + i);
                authMethod.setOrder((long) i);
                authMethod.setMethod("method" + i);
                dao.saveOrUpdateAuthenticationMethod(authMethod);
            }

            List<AuthenticationMethod> methods = dao.getAuthenticationMethods();

            Assert.assertNotNull(methods);
            Assert.assertEquals(3, methods.size());
        }
    }

    @Test
    public void test_deleteAuthenticationMethod() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrAuthenticationMethodImpl authMethod = new SolrAuthenticationMethodImpl();
            authMethod.setName("ToDelete");
            authMethod.setOrder(1L);
            authMethod.setMethod("delete");
            dao.saveOrUpdateAuthenticationMethod(authMethod);
            AuthenticationMethod saved = dao.getAuthenticationMethod(authMethod.getId());
            Assert.assertNotNull(saved);

            dao.deleteAuthenticationMethod(authMethod);

            AuthenticationMethod found = dao.getAuthenticationMethod(authMethod.getId());
            Assert.assertNull(found);
        }
    }

    @Test
    public void test_getPrincipalByNameLike() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create principals with different names
            SolrIkasanPrincipalImpl p1 = new SolrIkasanPrincipalImpl();
            p1.setName("admin_user");
            p1.setType("user");
            p1.setDescription("Admin user");
            p1.setCreatedDateTime(new Date());
            p1.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(p1);

            SolrIkasanPrincipalImpl p2 = new SolrIkasanPrincipalImpl();
            p2.setName("admin_group");
            p2.setType("group");
            p2.setDescription("Admin group");
            p2.setCreatedDateTime(new Date());
            p2.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(p2);

            SolrIkasanPrincipalImpl p3 = new SolrIkasanPrincipalImpl();
            p3.setName("regular_user");
            p3.setType("user");
            p3.setDescription("Regular user");
            p3.setCreatedDateTime(new Date());
            p3.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(p3);

            List<IkasanPrincipal> principals = dao.getPrincipalByNameLike("admin");

            Assert.assertNotNull(principals);
            Assert.assertEquals(2, principals.size());
        }
    }

    @Test
    public void test_getPolicyByNameLike() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create policies
            SolrPolicyImpl p1 = new SolrPolicyImpl();
            p1.setName("read_policy");
            p1.setDescription("Read policy");
            p1.setCreatedDateTime(new Date());
            p1.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePolicy(p1);

            SolrPolicyImpl p2 = new SolrPolicyImpl();
            p2.setName("write_policy");
            p2.setDescription("Write policy");
            p2.setCreatedDateTime(new Date());
            p2.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePolicy(p2);

            SolrPolicyImpl p3 = new SolrPolicyImpl();
            p3.setName("admin_policy");
            p3.setDescription("Admin policy");
            p3.setCreatedDateTime(new Date());
            p3.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePolicy(p3);

            List<Policy> policies = dao.getPolicyByNameLike("policy");

            Assert.assertNotNull(policies);
            Assert.assertEquals(3, policies.size());
        }
    }

    @Test
    public void test_getRoleByNameLike() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create roles
            SolrRoleImpl r1 = new SolrRoleImpl();
            r1.setName("admin_role");
            r1.setDescription("Admin role");
            r1.setCreatedDateTime(new Date());
            r1.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(r1);

            SolrRoleImpl r2 = new SolrRoleImpl();
            r2.setName("user_role");
            r2.setDescription("User role");
            r2.setCreatedDateTime(new Date());
            r2.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(r2);

            List<Role> roles = dao.getRoleByNameLike("role");

            Assert.assertNotNull(roles);
            Assert.assertEquals(2, roles.size());
        }
    }

    @Test
    public void test_getNumberOfAuthenticationMethods() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            long initialCount = dao.getNumberOfAuthenticationMethods();
            Assert.assertEquals(0, initialCount);

            for (int i = 0; i < 5; i++) {
                SolrAuthenticationMethodImpl authMethod = new SolrAuthenticationMethodImpl();
                authMethod.setName("Method" + i);
                authMethod.setOrder((long) i);
                authMethod.setMethod("method" + i);
                dao.saveOrUpdateAuthenticationMethod(authMethod);
            }

            long count = dao.getNumberOfAuthenticationMethods();
            Assert.assertEquals(5, count);
        }
    }

    @Test
    public void test_getAuthenticationMethodByOrder() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create authentication methods with different orders
            for (int i = 1; i <= 3; i++) {
                SolrAuthenticationMethodImpl authMethod = new SolrAuthenticationMethodImpl();
                authMethod.setName("Method" + i);
                authMethod.setOrder((long) i);
                authMethod.setMethod("method" + i);
                dao.saveOrUpdateAuthenticationMethod(authMethod);
            }

            AuthenticationMethod found = dao.getAuthenticationMethodByOrder(2L);

            Assert.assertNotNull(found);
            Assert.assertEquals("Method2", found.getName());
            Assert.assertEquals(2L, found.getOrder().longValue());
        }
    }

    @Test
    public void test_getUsersAssociatedWithPrincipal() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create a principal
            SolrIkasanPrincipalImpl principal = new SolrIkasanPrincipalImpl();
            principal.setName("testGroup");
            principal.setType("group");
            principal.setDescription("Test group");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(principal);

            // Create users associated with the principal
            Set<IkasanPrincipal> principals = new HashSet<>();
            principals.add(principal);

            for (int i = 0; i < 3; i++) {
                SolrUserImpl user = new SolrUserImpl();
                user.setUsername("user" + i);
                user.setEmail("user" + i + "@example.com");
                user.setEnabled(true);
                user.setPrincipals(principals);
                solrUserDao.save(user);
            }

            List<User> users = dao.getUsersAssociatedWithPrincipal("testGroup-securityPrincipal");

            Assert.assertNotNull(users);
            Assert.assertEquals(3, users.size());
        }
    }

    @Test
    public void test_getPolicyById() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            SolrPolicyImpl policy = new SolrPolicyImpl();
            policy.setName("PolicyById");
            policy.setDescription("Policy by ID");
            policy.setCreatedDateTime(new Date());
            policy.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePolicy(policy);

            Policy savedPolicy = dao.getPolicyByName("PolicyById");
            Object policyId = savedPolicy.getId();

            Policy found = dao.getPolicyById(policyId);

            Assert.assertNotNull(found);
            Assert.assertEquals("PolicyById", found.getName());
        }
    }

    @Test
    public void test_saveRoleModule_and_deleteRoleModule() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create role
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("ModuleRole");
            role.setDescription("Module role");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(role);

            // Get saved role to get its ID
            role = (SolrRoleImpl) dao.getRoleByName("ModuleRole");

            // Create role module
            SolrRoleModuleImpl roleModule = new SolrRoleModuleImpl();
            roleModule.setRole(role);
            roleModule.setModuleName("TestModule");

            dao.saveRoleModule(roleModule);

            // Retrieve role to verify module was saved
            Role foundRole = dao.getRoleByName("ModuleRole");
            Assert.assertNotNull(foundRole);
            Assert.assertNotNull(foundRole.getRoleModules());
            Assert.assertEquals(1, foundRole.getRoleModules().size());

            // Delete role module
            dao.deleteRoleModule(roleModule);

            // Verify module was deleted
            foundRole = dao.getRoleByName("ModuleRole");
            Assert.assertNotNull(foundRole);
            Assert.assertTrue(foundRole.getRoleModules() == null || foundRole.getRoleModules().isEmpty());
        }
    }

    @Test
    public void test_saveRoleJobPlan_and_deleteRoleJobPlan() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create role
            SolrRoleImpl role = new SolrRoleImpl();
            role.setName("JobPlanRole");
            role.setDescription("Job plan role");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(role);

            // Get saved role to get its ID
            role = (SolrRoleImpl) dao.getRoleByName("JobPlanRole");

            // Create role job plan
            SolrRoleJobPlanImpl roleJobPlan = new SolrRoleJobPlanImpl();
            roleJobPlan.setRole(role);
            roleJobPlan.setJobPlanName("TestJobPlan");

            dao.saveRoleJobPlan(roleJobPlan);

            // Retrieve role to verify job plan was saved
            Role foundRole = dao.getRoleByName("JobPlanRole");
            Assert.assertNotNull(foundRole);
            Assert.assertNotNull(foundRole.getRoleJobPlans());
            Assert.assertEquals(1, foundRole.getRoleJobPlans().size());

            // Delete role job plan
            dao.deleteRoleJobPlan(roleJobPlan);

            // Verify job plan was deleted
            foundRole = dao.getRoleByName("JobPlanRole");
            Assert.assertNotNull(foundRole);
            Assert.assertTrue(foundRole.getRoleJobPlans() == null || foundRole.getRoleJobPlans().isEmpty());
        }
    }

    @Test
    public void test_getRoleJobPlansByJobPlanName() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create two roles
            SolrRoleImpl role1 = new SolrRoleImpl();
            role1.setName("JobPlanRole1");
            role1.setDescription("Job plan role 1");
            role1.setCreatedDateTime(new Date());
            role1.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(role1);

            SolrRoleImpl role2 = new SolrRoleImpl();
            role2.setName("JobPlanRole2");
            role2.setDescription("Job plan role 2");
            role2.setCreatedDateTime(new Date());
            role2.setUpdatedDateTime(new Date());
            dao.saveOrUpdateRole(role2);

            // Get saved roles
            role1 = (SolrRoleImpl) dao.getRoleByName("JobPlanRole1");
            role2 = (SolrRoleImpl) dao.getRoleByName("JobPlanRole2");

            // Create role job plans for same job plan name
            SolrRoleJobPlanImpl roleJobPlan1 = new SolrRoleJobPlanImpl();
            roleJobPlan1.setRole(role1);
            roleJobPlan1.setJobPlanName("SharedJobPlan");
            dao.saveRoleJobPlan(roleJobPlan1);

            SolrRoleJobPlanImpl roleJobPlan2 = new SolrRoleJobPlanImpl();
            roleJobPlan2.setRole(role2);
            roleJobPlan2.setJobPlanName("SharedJobPlan");
            dao.saveRoleJobPlan(roleJobPlan2);

            // Query by job plan name
            List<RoleJobPlan> roleJobPlans = dao.getRoleJobPlansByJobPlanName("SharedJobPlan");

            Assert.assertNotNull(roleJobPlans);
            Assert.assertEquals(2, roleJobPlans.size());
        }
    }

    @Test
    public void test_integration_complete_workflow() throws Exception {
        try (EmbeddedSolrServer server = new EmbeddedSolrServer(config, "ikasan")) {
            init(server);

            // Create policy
            Policy policy = dao.createPolicy();
            policy.setName("IntegrationPolicy");
            policy.setDescription("Integration test policy");
            dao.saveOrUpdatePolicy(policy);

            // Create role
            Role role = dao.createRole();
            role.setName("IntegrationRole");
            role.setDescription("Integration test role");
            role.addPolicy(policy);
            dao.saveOrUpdateRole(role);

            // Create principal with role
            IkasanPrincipal principal = dao.createPrincipal();
            principal.setName("IntegrationPrincipal");
            principal.setType("user");
            principal.setDescription("Integration test principal");
            principal.addRole(role);
            dao.saveOrUpdatePrincipal(principal);

            // Verify the complete chain
            IkasanPrincipal foundPrincipal = dao.getPrincipalByName("IntegrationPrincipal");
            Assert.assertNotNull(foundPrincipal);
            Assert.assertEquals(1, foundPrincipal.getRoles().size());

            Role foundRole = foundPrincipal.getRoles().iterator().next();
            Assert.assertEquals("IntegrationRole", foundRole.getName());
            Assert.assertEquals(1, foundRole.getPolicies().size());

            Policy foundPolicy = foundRole.getPolicies().iterator().next();
            Assert.assertEquals("IntegrationPolicy", foundPolicy.getName());
        }
    }

    public static String TEST_HOME() {
        return getFile("solr/ikasan").getParent();
    }

    public static Path TEST_PATH() {
        return getFile("solr/ikasan").getParentFile().toPath();
    }
}
