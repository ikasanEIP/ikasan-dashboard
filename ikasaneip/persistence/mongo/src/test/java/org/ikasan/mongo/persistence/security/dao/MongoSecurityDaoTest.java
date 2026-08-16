package org.ikasan.mongo.persistence.security.dao;

import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.security.model.*;
import org.ikasan.mongo.persistence.security.repository.*;
import org.ikasan.spec.security.model.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class})
public class MongoSecurityDaoTest {

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoIkasanPrincipalRepository principalRepository;

    @Autowired
    private MongoPolicyRepository policyRepository;

    @Autowired
    private MongoRoleRepository roleRepository;

    @Autowired
    private MongoAuthenticationMethodRepository authMethodRepository;

    @Autowired
    private MongoUserRepository userRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private MongoSecurityDao dao;
    private MongoIkasanPrincipalDao mongoIkasanPrincipalDao;
    private MongoRoleDao mongoRoleDao;
    private MongoPolicyDao mongoPolicyDao;
    private MongoAuthenticationMethodDao mongoAuthenticationMethodDao;
    private MongoUserDao mongoUserDao;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    public void setDao(MongoIkasanPrincipalRepository principalRepository,
                       MongoPolicyRepository policyRepository,
                       MongoRoleRepository roleRepository,
                       MongoAuthenticationMethodRepository authMethodRepository,
                       MongoUserRepository userRepository,
                       MongoTemplate mongoTemplate) {
        // Step 1: Create MongoPolicyDao first (no dependencies)
        this.mongoPolicyDao = new MongoPolicyDao(policyRepository, mongoTemplate);

        // Step 2: Create MongoRoleDao with MongoPolicyDao
        this.mongoRoleDao = new MongoRoleDao(roleRepository, mongoTemplate, this.mongoPolicyDao);

        // Step 3: Set circular reference
        this.mongoPolicyDao.setMongoRoleDao(this.mongoRoleDao);

        // Step 4: Create MongoIkasanPrincipalDao with MongoRoleDao
        this.mongoIkasanPrincipalDao = new MongoIkasanPrincipalDao(principalRepository, mongoTemplate, this.mongoRoleDao);

        // Step 5: Create MongoUserDao with MongoIkasanPrincipalDao
        this.mongoUserDao = new MongoUserDao(userRepository, mongoTemplate, this.mongoIkasanPrincipalDao);

        this.mongoAuthenticationMethodDao = new MongoAuthenticationMethodDao(authMethodRepository, mongoTemplate);

        this.dao = new MongoSecurityDao(mongoIkasanPrincipalDao, mongoPolicyDao, mongoRoleDao,
            mongoAuthenticationMethodDao, mongoUserDao);
    }

    @After
    public void teardown() {
        principalRepository.deleteAll();
        policyRepository.deleteAll();
        roleRepository.deleteAll();
        authMethodRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void test_createPrincipal() {
        IkasanPrincipal principal = dao.createPrincipal();

        Assert.assertNotNull(principal);
        Assert.assertTrue(principal instanceof MongoIkasanPrincipalImpl);
    }

    @Test
    public void test_createRole() {
        Role role = dao.createRole();

        Assert.assertNotNull(role);
        Assert.assertTrue(role instanceof MongoRoleImpl);
    }

    @Test
    public void test_createPolicy() {
        Policy policy = dao.createPolicy();

        Assert.assertNotNull(policy);
        Assert.assertTrue(policy instanceof MongoPolicyImpl);
    }

    @Test
    public void test_createRoleModule() {
        RoleModule roleModule = dao.createRoleModule();

        Assert.assertNotNull(roleModule);
        Assert.assertTrue(roleModule instanceof MongoRoleModuleImpl);
    }

    @Test
    public void test_createRoleJobPlan() {
        RoleJobPlan roleJobPlan = dao.createRoleJobPlan();

        Assert.assertNotNull(roleJobPlan);
        Assert.assertTrue(roleJobPlan instanceof MongoRoleJobPlanImpl);
    }

    @Test
    public void test_createAuthenticationMethod() {
        AuthenticationMethod authMethod = dao.createAuthenticationMethod();

        Assert.assertNotNull(authMethod);
        Assert.assertTrue(authMethod instanceof MongoAuthenticationMethodImpl);
    }

    @Test
    public void test_saveOrUpdateRole_and_getRoleByName() {
        MongoRoleImpl role = new MongoRoleImpl();
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

    @Test
    public void test_deleteRole() {
        MongoRoleImpl role = new MongoRoleImpl();
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

    @Test
    public void test_saveOrUpdatePolicy_and_getPolicyByName() {
        MongoPolicyImpl policy = new MongoPolicyImpl();
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

    @Test
    public void test_deletePolicy() {
        MongoPolicyImpl policy = new MongoPolicyImpl();
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

    @Test
    public void test_saveOrUpdatePrincipal_and_getPrincipalByName() {
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
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

    @Test
    public void test_deletePrincipal() {
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
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

    @Test
    public void test_getAllPolicies() {
        for (int i = 0; i < 5; i++) {
            MongoPolicyImpl policy = new MongoPolicyImpl();
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

    @Test
    public void test_getAllRoles() {
        for (int i = 0; i < 3; i++) {
            MongoRoleImpl role = new MongoRoleImpl();
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

    @Test
    public void test_getAllPrincipals() {
        for (int i = 0; i < 4; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
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

    @Test
    public void test_getPrincipals_with_pagination() {
        for (int i = 0; i < 10; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
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

    @Test
    public void test_getAllPrincipalLites() {
        for (int i = 0; i < 3; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
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

    @Test
    public void test_getPrincipalLites_with_pagination() {
        for (int i = 0; i < 8; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
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

    @Test
    public void test_getPrincipalCount() {
        int initialCount = dao.getPrincipalCount(null);
        Assert.assertEquals(0, initialCount);

        for (int i = 0; i < 7; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
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

    @Test
    public void test_getAllPrincipalsWithRole() {
        // Create role
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("TestRole");
        role.setDescription("Test role");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());
        dao.saveOrUpdateRole(role);

        // Create principals with role
        for (int i = 0; i < 3; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
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

    @Test
    public void test_getAllPrincipalsWithRole_with_pagination() {
        // Create role
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("PaginatedRole");
        role.setDescription("Paginated role");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());
        dao.saveOrUpdateRole(role);

        // Create 10 principals with role
        for (int i = 0; i < 10; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
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

    @Test
    public void test_getAllPrincipalsWithoutRole() {
        // Create role
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("ExclusionRole");
        role.setDescription("Exclusion role");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());
        dao.saveOrUpdateRole(role);

        // Create principals with role
        for (int i = 0; i < 2; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
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
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
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

    @Test
    public void test_getPrincipalsWithRoleCount() {
        // Create role
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("CountRole");
        role.setDescription("Count role");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());
        dao.saveOrUpdateRole(role);

        // Create principals with role
        for (int i = 0; i < 7; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
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

    @Test
    public void test_getPrincipalsWithoutRoleCount() {
        // Create role
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("CountExclusionRole");
        role.setDescription("Count exclusion role");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());
        dao.saveOrUpdateRole(role);

        // Create principals with role
        for (int i = 0; i < 3; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
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
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
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

    @Test
    public void test_getPrincipalsByRoleNames() {
        // Create two roles
        MongoRoleImpl role1 = new MongoRoleImpl();
        role1.setName("Role1");
        role1.setDescription("First role");
        role1.setCreatedDateTime(new Date());
        role1.setUpdatedDateTime(new Date());
        dao.saveOrUpdateRole(role1);

        MongoRoleImpl role2 = new MongoRoleImpl();
        role2.setName("Role2");
        role2.setDescription("Second role");
        role2.setCreatedDateTime(new Date());
        role2.setUpdatedDateTime(new Date());
        dao.saveOrUpdateRole(role2);

        // Create principals with role1
        for (int i = 0; i < 2; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
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
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
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

    @Test
    public void test_getAllPoliciesWithRole() {
        // Create role
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("PolicyRole");
        role.setDescription("Policy role");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());

        // Create policies with role
        for (int i = 0; i < 3; i++) {
            MongoPolicyImpl policy = new MongoPolicyImpl();
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

    @Test
    public void test_getRoleById() {
        MongoRoleImpl role = new MongoRoleImpl();
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

    @Test
    public void test_saveOrUpdateAuthenticationMethod_and_getAuthenticationMethod() {
        MongoAuthenticationMethodImpl authMethod = new MongoAuthenticationMethodImpl();
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

    @Test
    public void test_getAuthenticationMethods() {
        for (int i = 0; i < 3; i++) {
            MongoAuthenticationMethodImpl authMethod = new MongoAuthenticationMethodImpl();
            authMethod.setName("AuthMethod" + i);
            authMethod.setOrder((long) i);
            authMethod.setMethod("method" + i);
            dao.saveOrUpdateAuthenticationMethod(authMethod);
        }

        List<AuthenticationMethod> methods = dao.getAuthenticationMethods();

        Assert.assertNotNull(methods);
        Assert.assertEquals(3, methods.size());
    }

    @Test
    public void test_deleteAuthenticationMethod() {
        MongoAuthenticationMethodImpl authMethod = new MongoAuthenticationMethodImpl();
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

    @Test
    public void test_getPrincipalByNameLike() {
        // Create principals with different names
        MongoIkasanPrincipalImpl p1 = new MongoIkasanPrincipalImpl();
        p1.setName("admin_user");
        p1.setType("user");
        p1.setDescription("Admin user");
        p1.setCreatedDateTime(new Date());
        p1.setUpdatedDateTime(new Date());
        dao.saveOrUpdatePrincipal(p1);

        MongoIkasanPrincipalImpl p2 = new MongoIkasanPrincipalImpl();
        p2.setName("admin_group");
        p2.setType("group");
        p2.setDescription("Admin group");
        p2.setCreatedDateTime(new Date());
        p2.setUpdatedDateTime(new Date());
        dao.saveOrUpdatePrincipal(p2);

        MongoIkasanPrincipalImpl p3 = new MongoIkasanPrincipalImpl();
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

    @Test
    public void test_getPolicyByNameLike() {
        // Create policies
        MongoPolicyImpl p1 = new MongoPolicyImpl();
        p1.setName("read_policy");
        p1.setDescription("Read policy");
        p1.setCreatedDateTime(new Date());
        p1.setUpdatedDateTime(new Date());
        dao.saveOrUpdatePolicy(p1);

        MongoPolicyImpl p2 = new MongoPolicyImpl();
        p2.setName("write_policy");
        p2.setDescription("Write policy");
        p2.setCreatedDateTime(new Date());
        p2.setUpdatedDateTime(new Date());
        dao.saveOrUpdatePolicy(p2);

        MongoPolicyImpl p3 = new MongoPolicyImpl();
        p3.setName("admin_policy");
        p3.setDescription("Admin policy");
        p3.setCreatedDateTime(new Date());
        p3.setUpdatedDateTime(new Date());
        dao.saveOrUpdatePolicy(p3);

        List<Policy> policies = dao.getPolicyByNameLike("policy");

        Assert.assertNotNull(policies);
        Assert.assertEquals(3, policies.size());
    }

    @Test
    public void test_getRoleByNameLike() {
        // Create roles
        MongoRoleImpl r1 = new MongoRoleImpl();
        r1.setName("admin_role");
        r1.setDescription("Admin role");
        r1.setCreatedDateTime(new Date());
        r1.setUpdatedDateTime(new Date());
        dao.saveOrUpdateRole(r1);

        MongoRoleImpl r2 = new MongoRoleImpl();
        r2.setName("user_role");
        r2.setDescription("User role");
        r2.setCreatedDateTime(new Date());
        r2.setUpdatedDateTime(new Date());
        dao.saveOrUpdateRole(r2);

        List<Role> roles = dao.getRoleByNameLike("role");

        Assert.assertNotNull(roles);
        Assert.assertEquals(2, roles.size());
    }

    @Test
    public void test_getNumberOfAuthenticationMethods() {
        long initialCount = dao.getNumberOfAuthenticationMethods();
        Assert.assertEquals(0, initialCount);

        for (int i = 0; i < 5; i++) {
            MongoAuthenticationMethodImpl authMethod = new MongoAuthenticationMethodImpl();
            authMethod.setName("Method" + i);
            authMethod.setOrder((long) i);
            authMethod.setMethod("method" + i);
            dao.saveOrUpdateAuthenticationMethod(authMethod);
        }

        long count = dao.getNumberOfAuthenticationMethods();
        Assert.assertEquals(5, count);
    }

    @Test
    public void test_getAuthenticationMethodByOrder() {
        // Create authentication methods with different orders
        for (int i = 1; i <= 3; i++) {
            MongoAuthenticationMethodImpl authMethod = new MongoAuthenticationMethodImpl();
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

    @Test
    public void test_getUsersAssociatedWithPrincipal() {
        // Create a principal
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
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
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setEnabled(true);
            user.setPrincipals(principals);
            mongoUserDao.save(user);
        }

        List<User> users = dao.getUsersAssociatedWithPrincipal("testGroup-securityPrincipal");

        Assert.assertNotNull(users);
        Assert.assertEquals(3, users.size());
    }

    @Test
    public void test_getPolicyById() {
        MongoPolicyImpl policy = new MongoPolicyImpl();
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

    @Test
    public void test_saveRoleModule_and_deleteRoleModule() {
        // Create role
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("ModuleRole");
        role.setDescription("Module role");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());
        dao.saveOrUpdateRole(role);

        // Get saved role to get its ID
        role = (MongoRoleImpl) dao.getRoleByName("ModuleRole");

        // Create role module
        MongoRoleModuleImpl roleModule = new MongoRoleModuleImpl();
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

    @Test
    public void test_saveRoleJobPlan_and_deleteRoleJobPlan() {
        // Create role
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("JobPlanRole");
        role.setDescription("Job plan role");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());
        dao.saveOrUpdateRole(role);

        // Get saved role to get its ID
        role = (MongoRoleImpl) dao.getRoleByName("JobPlanRole");

        // Create role job plan
        MongoRoleJobPlanImpl roleJobPlan = new MongoRoleJobPlanImpl();
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

    @Test
    public void test_getRoleJobPlansByJobPlanName() {
        // Create two roles
        MongoRoleImpl role1 = new MongoRoleImpl();
        role1.setName("JobPlanRole1");
        role1.setDescription("Job plan role 1");
        role1.setCreatedDateTime(new Date());
        role1.setUpdatedDateTime(new Date());
        dao.saveOrUpdateRole(role1);

        MongoRoleImpl role2 = new MongoRoleImpl();
        role2.setName("JobPlanRole2");
        role2.setDescription("Job plan role 2");
        role2.setCreatedDateTime(new Date());
        role2.setUpdatedDateTime(new Date());
        dao.saveOrUpdateRole(role2);

        // Get saved roles
        role1 = (MongoRoleImpl) dao.getRoleByName("JobPlanRole1");
        role2 = (MongoRoleImpl) dao.getRoleByName("JobPlanRole2");

        // Create role job plans for same job plan name
        MongoRoleJobPlanImpl roleJobPlan1 = new MongoRoleJobPlanImpl();
        roleJobPlan1.setRole(role1);
        roleJobPlan1.setJobPlanName("SharedJobPlan");
        dao.saveRoleJobPlan(roleJobPlan1);

        MongoRoleJobPlanImpl roleJobPlan2 = new MongoRoleJobPlanImpl();
        roleJobPlan2.setRole(role2);
        roleJobPlan2.setJobPlanName("SharedJobPlan");
        dao.saveRoleJobPlan(roleJobPlan2);

        // Query by job plan name
        List<RoleJobPlan> roleJobPlans = dao.getRoleJobPlansByJobPlanName("SharedJobPlan");

        Assert.assertNotNull(roleJobPlans);
        Assert.assertEquals(2, roleJobPlans.size());
    }

    @Test
    public void test_integration_complete_workflow() {
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
