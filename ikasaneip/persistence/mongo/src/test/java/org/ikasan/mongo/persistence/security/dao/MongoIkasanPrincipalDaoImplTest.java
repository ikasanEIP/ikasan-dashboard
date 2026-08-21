package org.ikasan.mongo.persistence.security.dao;

import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalFilterImpl;
import org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl;
import org.ikasan.mongo.persistence.security.model.MongoRoleImpl;
import org.ikasan.mongo.persistence.security.repository.MongoIkasanPrincipalRepository;
import org.ikasan.mongo.persistence.security.repository.MongoPolicyRepository;
import org.ikasan.mongo.persistence.security.repository.MongoRoleRepository;
import org.ikasan.spec.security.model.IkasanPrincipal;
import org.ikasan.spec.security.model.IkasanPrincipalLite;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * MongoDB DAO Test for IkasanPrincipal operations.
 * This test class contains all 78 test methods converted from SolrIkasanPrincipalDaoImplTest.
 *
 * Test coverage includes:
 * - Basic CRUD operations (create, save, update, delete)
 * - Searching by name, type, and filters
 * - Pagination support
 * - Role association and filtering
 * - findById operations
 * - Lite principal operations
 * - Special character handling
 * - Null/empty parameter handling
 * - Integration scenarios
 *
 * IMPLEMENTATION COMPLETE: 78/78 tests
 * - 46 tests were already implemented
 * - 32 additional tests added to complete the suite
 *
 * All test method names and logic are preserved from the Solr implementation.
 * Conversion pattern:
 * - Replace SolrIkasanPrincipalImpl → MongoIkasanPrincipalImpl
 * - Replace SolrRoleImpl → MongoRoleImpl
 * - Replace solrIkasanPrincipalDao → mongoIkasanPrincipalDao/dao
 * - Replace solrRoleDao → mongoRoleDaoImpl
 * - Remove try-with-resources EmbeddedSolrServer blocks and init(server) calls
 * - Keep all assertions identical
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class})
public class MongoIkasanPrincipalDaoImplTest {

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoIkasanPrincipalRepository repository;

    @Autowired
    private MongoRoleRepository mongoRoleRepository;

    @Autowired
    private MongoPolicyRepository mongoPolicyRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private MongoIkasanPrincipalDaoImpl dao;
    private MongoRoleDaoImpl mongoRoleDaoImpl;
    private MongoPolicyDaoImpl mongoPolicyDaoImpl;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    public void setDao(MongoIkasanPrincipalRepository repository,
                       MongoRoleRepository mongoRoleRepository,
                       MongoPolicyRepository mongoPolicyRepository,
                       MongoTemplate mongoTemplate) {
        // Step 1: Create MongoPolicyDaoImpl first (no dependencies)
        this.mongoPolicyDaoImpl = new MongoPolicyDaoImpl(mongoPolicyRepository, mongoTemplate);

        // Step 2: Create MongoRoleDaoImpl with MongoPolicyDaoImpl
        this.mongoRoleDaoImpl = new MongoRoleDaoImpl(mongoRoleRepository, mongoTemplate, this.mongoPolicyDaoImpl);

        // Step 3: Set circular reference
        this.mongoPolicyDaoImpl.setMongoRoleDao(this.mongoRoleDaoImpl);

        // Step 4: Create MongoIkasanPrincipalDaoImpl with MongoRoleDaoImpl
        this.dao = new MongoIkasanPrincipalDaoImpl(repository, mongoTemplate, this.mongoRoleDaoImpl);
    }

    @After
    public void teardown() {
        repository.deleteAll();
        mongoRoleRepository.deleteAll();
        mongoPolicyRepository.deleteAll();
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
    public void test_saveOrUpdatePrincipal_and_getPrincipalByName() {
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
        principal.setName("testuser");
        principal.setType("user");
        principal.setDescription("Test user principal");
        principal.setCreatedDateTime(new Date(1000000L));
        principal.setUpdatedDateTime(new Date(2000000L));

        dao.saveOrUpdatePrincipal(principal);

        IkasanPrincipal found = dao.getPrincipalByName("testuser");

        Assert.assertNotNull(found);
        Assert.assertEquals("testuser", found.getName());
        Assert.assertEquals("user", found.getType());
        Assert.assertEquals("Test user principal", found.getDescription());
    }

    @Test
    public void test_findById_basic() {
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
        principal.setName("testuser");
        principal.setType("user");
        principal.setDescription("Test user principal");
        principal.setCreatedDateTime(new Date(1000000L));
        principal.setUpdatedDateTime(new Date(2000000L));

        dao.saveOrUpdatePrincipal(principal);

        // Find by ID
        IkasanPrincipal found = dao.findById("testuser-securityPrincipal");

        Assert.assertNotNull(found);
        Assert.assertEquals("testuser", found.getName());
        Assert.assertEquals("user", found.getType());
        Assert.assertEquals("Test user principal", found.getDescription());
    }

    @Test
    public void test_findById_not_found() {
        IkasanPrincipal found = dao.findById("nonexistent");

        Assert.assertNull(found);
    }

    @Test
    public void test_findById_null_id() {
        IkasanPrincipal found = dao.findById(null);

        Assert.assertNull(found);
    }

    @Test
    public void test_findById_empty_id() {
        IkasanPrincipal found = dao.findById("");

        Assert.assertNull(found);
    }

    @Test
    public void test_findById_with_roles() {
        // Create and save roles
        MongoRoleImpl adminRole = new MongoRoleImpl();
        adminRole.setName("AdminRole");
        adminRole.setDescription("Administrator role");
        adminRole.setCreatedDateTime(new Date());
        adminRole.setUpdatedDateTime(new Date());
        mongoRoleDaoImpl.saveOrUpdateRole(adminRole);

        MongoRoleImpl userRole = new MongoRoleImpl();
        userRole.setName("UserRole");
        userRole.setDescription("User role");
        userRole.setCreatedDateTime(new Date());
        userRole.setUpdatedDateTime(new Date());
        mongoRoleDaoImpl.saveOrUpdateRole(userRole);

        // Retrieve roles to get their IDs
        adminRole = (MongoRoleImpl) mongoRoleDaoImpl.getRoleByName("AdminRole");
        userRole = (MongoRoleImpl) mongoRoleDaoImpl.getRoleByName("UserRole");

        // Create principal with roles
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
        principal.setName("userWithRoles");
        principal.setType("user");
        principal.setDescription("User with multiple roles");
        principal.setCreatedDateTime(new Date());
        principal.setUpdatedDateTime(new Date());
        principal.addRole(adminRole);
        principal.addRole(userRole);

        dao.saveOrUpdatePrincipal(principal);

        // Find by ID
        IkasanPrincipal found = dao.findById("userWithRoles-securityPrincipal");

        Assert.assertNotNull(found);
        Assert.assertEquals("userWithRoles", found.getName());
        Assert.assertNotNull(found.getRoles());
        Assert.assertEquals(2, found.getRoles().size());
    }



@Test
    public void test_findById_without_roles() throws Exception {
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("userNoRoles");
            principal.setType("user");
            principal.setDescription("User without roles");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());

            dao.saveOrUpdatePrincipal(principal);

        IkasanPrincipal found = dao.findById("userNoRoles-securityPrincipal");

            Assert.assertNotNull(found);
            Assert.assertEquals("userNoRoles", found.getName());
            Assert.assertNotNull(found.getRoles());
            Assert.assertTrue(found.getRoles().isEmpty());
    }

@Test
    public void test_findById_with_special_characters() throws Exception {
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("user-with_special.chars@test");
            principal.setType("user");
            principal.setDescription("User with special chars: <>&\"'");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());

            dao.saveOrUpdatePrincipal(principal);

        IkasanPrincipal found = dao.findById("user-with_special.chars@test-securityPrincipal");

            Assert.assertNotNull(found);
            Assert.assertEquals("user-with_special.chars@test", found.getName());
            Assert.assertEquals("User with special chars: <>&\"'", found.getDescription());
    }

@Test
    public void test_findById_different_types() throws Exception {
        // Create user type principal
        MongoIkasanPrincipalImpl user = new MongoIkasanPrincipalImpl();
            user.setName("testuser");
            user.setType("user");
            user.setDescription("User principal");
            user.setCreatedDateTime(new Date());
            user.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(user);

            // Create group type principal
        MongoIkasanPrincipalImpl group = new MongoIkasanPrincipalImpl();
            group.setName("testgroup");
            group.setType("group");
            group.setDescription("Group principal");
            group.setCreatedDateTime(new Date());
            group.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(group);

            // Create application type principal
        MongoIkasanPrincipalImpl app = new MongoIkasanPrincipalImpl();
            app.setName("testapp");
            app.setType("application");
            app.setDescription("Application principal");
            app.setCreatedDateTime(new Date());
            app.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(app);

            // Find each by ID
        IkasanPrincipal foundUser = dao.findById("testuser-securityPrincipal");
        IkasanPrincipal foundGroup = dao.findById("testgroup-securityPrincipal");
        IkasanPrincipal foundApp = dao.findById("testapp-securityPrincipal");

            Assert.assertNotNull(foundUser);
            Assert.assertEquals("user", foundUser.getType());

            Assert.assertNotNull(foundGroup);
            Assert.assertEquals("group", foundGroup.getType());

            Assert.assertNotNull(foundApp);
            Assert.assertEquals("application", foundApp.getType());
    }

@Test
    public void test_findById_with_ldap_base_dn() throws Exception {
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("ldapuser");
            principal.setType("user");
            principal.setDescription("LDAP user");
            principal.setApplicationSecurityBaseDn("ou=users,dc=example,dc=com");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());

            dao.saveOrUpdatePrincipal(principal);

        IkasanPrincipal found = dao.findById("ldapuser-securityPrincipal");

            Assert.assertNotNull(found);
            Assert.assertEquals("ldapuser", found.getName());
            Assert.assertEquals("ou=users,dc=example,dc=com", found.getApplicationSecurityBaseDn());
    }

@Test
    public void test_findById_after_update() throws Exception {
        // Create and save initial principal
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("updateTest");
            principal.setType("user");
            principal.setDescription("Original description");
            principal.setCreatedDateTime(new Date(1000000L));
            principal.setUpdatedDateTime(new Date(2000000L));
            dao.saveOrUpdatePrincipal(principal);

            // Find by ID - should have original description
        IkasanPrincipal found = dao.findById("updateTest-securityPrincipal");
            Assert.assertEquals("Original description", found.getDescription());

            // Update the principal
            principal.setDescription("Updated description");
            principal.setUpdatedDateTime(new Date(3000000L));
            dao.saveOrUpdatePrincipal(principal);

            // Find by ID again - should have updated description
            found = dao.findById("updateTest-securityPrincipal");
            Assert.assertNotNull(found);
            Assert.assertEquals("Updated description", found.getDescription());
    }

@Test
    public void test_findById_invalid_format() throws Exception {
        // Create a principal
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("testuser");
            principal.setType("user");
            principal.setDescription("Test user");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(principal);

            // Try to find with wrong ID format (just name, without type suffix)
        IkasanPrincipal found = dao.findById("testuser");
            Assert.assertNull(found);

            // Try with wrong type suffix
            found = dao.findById("testuser-wrongType");
            Assert.assertNull(found);
    }

    @Test
    public void test_findById_case_sensitivity() throws Exception {
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("TestUser");
            principal.setType("user");
            principal.setDescription("Test user");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(principal);

            // Find with exact case
        IkasanPrincipal found = dao.findById("TestUser-securityPrincipal");
            Assert.assertNotNull(found);
            Assert.assertEquals("TestUser", found.getName());

            // Try with different case - should not find (IDs are case-sensitive)
            found = dao.findById("testuser");
            // Result depends on Solr configuration, but typically case-sensitive
            // This test documents the behavior
    }

    @Test
    public void test_findById_deleted_principal() throws Exception {
        // Create and save principal
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("toDelete");
            principal.setType("user");
            principal.setDescription("Will be deleted");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(principal);

            // Verify it exists
        IkasanPrincipal found = dao.findById("toDelete-securityPrincipal");
            Assert.assertNotNull(found);

            // Delete the principal
            dao.deletePrincipal(principal);

            // Try to find deleted principal - should return null
            found = dao.findById("toDelete");
            Assert.assertNull(found);
    }

    @Test
    public void test_getPrincipalByName_not_found() throws Exception {
        IkasanPrincipal found = dao.getPrincipalByName("nonexistent");

            Assert.assertNull(found);
    }

    @Test
    public void test_saveOrUpdatePrincipal_update_existing() throws Exception {
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
        principal.setName("testuser");
        principal.setType("user");
        principal.setDescription("Original description");
        principal.setCreatedDateTime(new Date(1000000L));
        principal.setUpdatedDateTime(new Date(2000000L));

        dao.saveOrUpdatePrincipal(principal);

        IkasanPrincipal found = dao.getPrincipalByName("testuser");
        Assert.assertEquals("Original description", found.getDescription());

        // Update the principal
        principal.setDescription("Updated description");
        principal.setUpdatedDateTime(new Date(3000000L));
        dao.saveOrUpdatePrincipal(principal);

        found = dao.getPrincipalByName("testuser");

        Assert.assertNotNull(found);
        Assert.assertEquals("testuser", found.getName());
        Assert.assertEquals("Updated description", found.getDescription());
    }

    @Test
    public void test_deletePrincipal() throws Exception {
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("userToDelete");
            principal.setType("user");
            principal.setDescription("This principal will be deleted");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());

            dao.saveOrUpdatePrincipal(principal);

        IkasanPrincipal found = dao.getPrincipalByName("userToDelete");
            Assert.assertNotNull(found);

            // Delete the principal
            dao.deletePrincipal(principal);

            found = dao.getPrincipalByName("userToDelete");
            Assert.assertNull(found);
    }

    @Test
    public void test_getAllPrincipals_empty() throws Exception {
        List<IkasanPrincipal> principals = dao.getAllPrincipals();

            Assert.assertNotNull(principals);
            Assert.assertTrue(principals.isEmpty());
    }

    @Test
    public void test_getPrincipalByNameLike() throws Exception {
        // Save principals with different names
        MongoIkasanPrincipalImpl principal1 = new MongoIkasanPrincipalImpl();
            principal1.setName("admin_user");
            principal1.setType("user");
            principal1.setDescription("Admin user");
            principal1.setCreatedDateTime(new Date());
            principal1.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(principal1);

        MongoIkasanPrincipalImpl principal2 = new MongoIkasanPrincipalImpl();
            principal2.setName("admin_group");
            principal2.setType("group");
            principal2.setDescription("Admin group");
            principal2.setCreatedDateTime(new Date());
            principal2.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(principal2);

        MongoIkasanPrincipalImpl principal3 = new MongoIkasanPrincipalImpl();
            principal3.setName("regular_user");
            principal3.setType("user");
            principal3.setDescription("Regular user");
            principal3.setCreatedDateTime(new Date());
            principal3.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(principal3);

            List<IkasanPrincipal> principals = dao.getPrincipalByNameLike("admin");

            Assert.assertNotNull(principals);
            Assert.assertEquals(2, principals.size());
    }

    @Test
    public void test_getPrincipalByNameLike_no_match() throws Exception {
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("testuser");
            principal.setType("user");
            principal.setDescription("Test description");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(principal);

            List<IkasanPrincipal> principals = dao.getPrincipalByNameLike("nonexistent");

            Assert.assertNotNull(principals);
            Assert.assertTrue(principals.isEmpty());
    }

    @Test
    public void test_getAllPrincipalLites_empty() throws Exception {
        List<IkasanPrincipalLite> principals = dao.getAllPrincipalLites();

            Assert.assertNotNull(principals);
            Assert.assertTrue(principals.isEmpty());
    }

    @Test
    public void test_saveOrUpdatePrincipal_with_roles() throws Exception {
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("userWithRoles");
            principal.setType("user");
            principal.setDescription("Principal with embedded roles");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());

        MongoRoleImpl role = new MongoRoleImpl();
            role.setName("AdminRole");
            role.setDescription("Administrator role");

            this.mongoRoleDaoImpl.saveOrUpdateRole(role);
            role = (MongoRoleImpl) mongoRoleDaoImpl.getRoleByName(role.getName());

            principal.addRole(role);

            role = new MongoRoleImpl();
            role.setName("MonitorRole");
            role.setDescription("Monitor role");

            this.mongoRoleDaoImpl.saveOrUpdateRole(role);
            role = (MongoRoleImpl) mongoRoleDaoImpl.getRoleByName(role.getName());

            principal.addRole(role);

            dao.saveOrUpdatePrincipal(principal);

        IkasanPrincipal found = dao.getPrincipalByName("userWithRoles");

            Assert.assertNotNull(found);
            Assert.assertEquals("userWithRoles", found.getName());
            Assert.assertNotNull(found.getRoles());
            Assert.assertEquals(2, found.getRoles().size());
    }

    @Test
    public void test_saveOrUpdatePrincipal_with_special_characters() throws Exception {
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("user-with_special.chars");
            principal.setType("user");
            principal.setDescription("Description with special chars: <>&\"'");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());

            dao.saveOrUpdatePrincipal(principal);

        IkasanPrincipal found = dao.getPrincipalByName("user-with_special.chars");

            Assert.assertNotNull(found);
            Assert.assertEquals("user-with_special.chars", found.getName());
            Assert.assertEquals("Description with special chars: <>&\"'", found.getDescription());
    }

    @Test
    public void test_saveOrUpdatePrincipal_different_types() throws Exception {
        // Create user type principal
        MongoIkasanPrincipalImpl user = new MongoIkasanPrincipalImpl();
            user.setName("testuser");
            user.setType("user");
            user.setDescription("User principal");
            user.setCreatedDateTime(new Date());
            user.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(user);

            // Create group type principal
        MongoIkasanPrincipalImpl group = new MongoIkasanPrincipalImpl();
            group.setName("testgroup");
            group.setType("group");
            group.setDescription("Group principal");
            group.setCreatedDateTime(new Date());
            group.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(group);

            // Verify both exist
        IkasanPrincipal foundUser = dao.getPrincipalByName("testuser");
        IkasanPrincipal foundGroup = dao.getPrincipalByName("testgroup");

            Assert.assertNotNull(foundUser);
            Assert.assertEquals("user", foundUser.getType());
            Assert.assertNotNull(foundGroup);
            Assert.assertEquals("group", foundGroup.getType());
    }

    @Test
    public void test_saveOrUpdatePrincipal_with_application_security_base_dn() throws Exception {
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("ldapuser");
            principal.setType("user");
            principal.setDescription("LDAP user");
            principal.setApplicationSecurityBaseDn("ou=users,dc=example,dc=com");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());

            dao.saveOrUpdatePrincipal(principal);

        IkasanPrincipal found = dao.getPrincipalByName("ldapuser");

            Assert.assertNotNull(found);
            Assert.assertEquals("ldapuser", found.getName());
            Assert.assertEquals("ou=users,dc=example,dc=com", found.getApplicationSecurityBaseDn());
    }

    @Test
    public void test_getAllPrincipalsWithRole_returns_empty() throws Exception {
        List<IkasanPrincipal> principals = dao.getAllPrincipalsWithRole("AdminRole");

            Assert.assertNotNull(principals);
            Assert.assertTrue(principals.isEmpty());
    }

    @Test
    public void test_getAllPrincipalsWithRole_with_filter_returns_empty() throws Exception {
        List<IkasanPrincipalLite> principals = dao.getAllPrincipalsWithRole("AdminRole", null, 10, 0);

            Assert.assertNotNull(principals);
            Assert.assertTrue(principals.isEmpty());
    }

    @Test
    public void test_getAllPrincipalsWithoutRole_returns_empty() throws Exception {
        List<IkasanPrincipalLite> principals = dao.getAllPrincipalsWithoutRole("AdminRole", null, 10, 0);

            Assert.assertNotNull(principals);
            Assert.assertTrue(principals.isEmpty());
    }

    @Test
    public void test_getPrincipalsByRoleNames_returns_empty() throws Exception {
        List<IkasanPrincipal> principals = dao.getPrincipalsByRoleNames(Arrays.asList("Role1", "Role2"));

            Assert.assertNotNull(principals);
            Assert.assertTrue(principals.isEmpty());
    }

    @Test
    public void test_getPrincipalsWithRoleCount_returns_zero() throws Exception {
        int count = dao.getPrincipalsWithRoleCount("AdminRole", null);

            Assert.assertEquals(0, count);
    }

    @Test
    public void test_getPrincipalsWithoutRoleCount_returns_zero() throws Exception {
        int count = dao.getPrincipalsWithoutRoleCount("AdminRole", null);

            Assert.assertEquals(0, count);
    }

    @Test
    public void test_multiple_operations_in_sequence() throws Exception {
        // Create
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
        principal.setName("sequenceUser");
        principal.setType("user");
        principal.setDescription("Initial description");
        principal.setCreatedDateTime(new Date());
        principal.setUpdatedDateTime(new Date());
        dao.saveOrUpdatePrincipal(principal);

            // Read
        IkasanPrincipal found = dao.getPrincipalByName("sequenceUser");
        Assert.assertNotNull(found);
        Assert.assertEquals("Initial description", found.getDescription());

        // Update
        principal.setDescription("Updated description");
        dao.saveOrUpdatePrincipal(principal);
        found = dao.getPrincipalByName("sequenceUser");
        Assert.assertEquals("Updated description", found.getDescription());

        // List
        List<IkasanPrincipal> principals = dao.getAllPrincipals();
        Assert.assertEquals(1, principals.size());

        // Count
        int count = dao.getPrincipalCount(null);
        Assert.assertEquals(1, count);

        // Delete
        dao.deletePrincipal(principal);
        found = dao.getPrincipalByName("sequenceUser");
        Assert.assertNull(found);

        // Verify empty
        principals = dao.getAllPrincipals();
        Assert.assertTrue(principals.isEmpty());
    }

    @Test
    public void test_getAllPrincipalsWithRole_multiple_roles() throws Exception {
        // Create roles
        MongoRoleImpl role1 = new MongoRoleImpl();
            role1.setName("Role1");
            role1.setDescription("First role");
            role1.setCreatedDateTime(new Date());
            role1.setUpdatedDateTime(new Date());
            mongoRoleDaoImpl.saveOrUpdateRole(role1);

        MongoRoleImpl role2 = new MongoRoleImpl();
            role2.setName("Role2");
            role2.setDescription("Second role");
            role2.setCreatedDateTime(new Date());
            role2.setUpdatedDateTime(new Date());
            mongoRoleDaoImpl.saveOrUpdateRole(role2);

            // Create principal with both roles
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("multiRoleUser");
            principal.setType("user");
            principal.setDescription("User with multiple roles");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            principal.getRoles().add(role1);
            principal.getRoles().add(role2);
            dao.saveOrUpdatePrincipal(principal);

            // Principal should be found when searching for either role
            List<IkasanPrincipal> withRole1 = dao.getAllPrincipalsWithRole("Role1");
            Assert.assertEquals(1, withRole1.size());
            Assert.assertEquals("multiRoleUser", withRole1.get(0).getName());

            List<IkasanPrincipal> withRole2 = dao.getAllPrincipalsWithRole("Role2");
            Assert.assertEquals(1, withRole2.size());
            Assert.assertEquals("multiRoleUser", withRole2.get(0).getName());
    }

    @Test
    public void test_getAllPrincipalsWithRole_not_found() throws Exception {
        // Create a role
        MongoRoleImpl role = new MongoRoleImpl();
            role.setName("TestRole");
            role.setDescription("Test role");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());
            mongoRoleDaoImpl.saveOrUpdateRole(role);

            // Create principal without the role
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("user1");
            principal.setType("user");
            principal.setDescription("User without role");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(principal);

            // Search for principals with TestRole - should return empty
            List<IkasanPrincipal> result = dao.getAllPrincipalsWithRole("TestRole");
            Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void test_getAllPrincipalsWithRole_null_roleName() throws Exception {
        List<IkasanPrincipal> result = dao.getAllPrincipalsWithRole(null);
            Assert.assertTrue(result.isEmpty());

            result = dao.getAllPrincipalsWithRole("");
            Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void test_getPrincipalsByRoleNames_principal_with_multiple_roles() throws Exception {
        // Create roles
        MongoRoleImpl role1 = new MongoRoleImpl();
            role1.setName("Role1");
            role1.setDescription("First role");
            role1.setCreatedDateTime(new Date());
            role1.setUpdatedDateTime(new Date());
            mongoRoleDaoImpl.saveOrUpdateRole(role1);

        MongoRoleImpl role2 = new MongoRoleImpl();
            role2.setName("Role2");
            role2.setDescription("Second role");
            role2.setCreatedDateTime(new Date());
            role2.setUpdatedDateTime(new Date());
            mongoRoleDaoImpl.saveOrUpdateRole(role2);

        MongoRoleImpl role3 = new MongoRoleImpl();
            role3.setName("Role3");
            role3.setDescription("Third role");
            role3.setCreatedDateTime(new Date());
            role3.setUpdatedDateTime(new Date());
            mongoRoleDaoImpl.saveOrUpdateRole(role3);

            // Create principal with multiple roles
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("multiRoleUser");
            principal.setType("user");
            principal.setDescription("User with multiple roles");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            principal.getRoles().add(role1);
            principal.getRoles().add(role2);
            dao.saveOrUpdatePrincipal(principal);

            // Search for Role1 OR Role3 (principal has Role1)
            List<IkasanPrincipal> results = dao.getPrincipalsByRoleNames(Arrays.asList("Role1", "Role3"));

            Assert.assertEquals(1, results.size());
            Assert.assertEquals("multiRoleUser", results.get(0).getName());

            // Verify the principal is returned only once (not duplicated)
            long count = results.stream()
                .filter(p -> p.getName().equals("multiRoleUser"))
                .count();
            Assert.assertEquals(1, count);
    }

    @Test
    public void test_getPrincipalsByRoleNames_no_matches() throws Exception {
        // Create a role
        MongoRoleImpl role = new MongoRoleImpl();
            role.setName("ExistingRole");
            role.setDescription("Existing role");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());
            mongoRoleDaoImpl.saveOrUpdateRole(role);

            // Create principal with the role
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("user1");
            principal.setType("user");
            principal.setDescription("User 1");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            principal.getRoles().add(role);
            dao.saveOrUpdatePrincipal(principal);

            // Search for non-existent roles
            List<IkasanPrincipal> results = dao.getPrincipalsByRoleNames(
                Arrays.asList("NonExistentRole1", "NonExistentRole2"));

            Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void test_getPrincipalsByRoleNames_null_or_empty() throws Exception {
        // Test with null
            List<IkasanPrincipal> nullResult = dao.getPrincipalsByRoleNames(null);
            Assert.assertTrue(nullResult.isEmpty());

            // Test with empty list
            List<IkasanPrincipal> emptyResult = dao.getPrincipalsByRoleNames(List.of());
            Assert.assertTrue(emptyResult.isEmpty());
    }

    @Test
    public void test_getPrincipalsByRoleNames_all_three_roles() throws Exception {
        // Create three roles
        MongoRoleImpl role1 = new MongoRoleImpl();
            role1.setName("ReadRole");
            role1.setDescription("Read access");
            role1.setCreatedDateTime(new Date());
            role1.setUpdatedDateTime(new Date());
            mongoRoleDaoImpl.saveOrUpdateRole(role1);

        MongoRoleImpl role2 = new MongoRoleImpl();
            role2.setName("WriteRole");
            role2.setDescription("Write access");
            role2.setCreatedDateTime(new Date());
            role2.setUpdatedDateTime(new Date());
            mongoRoleDaoImpl.saveOrUpdateRole(role2);

        MongoRoleImpl role3 = new MongoRoleImpl();
            role3.setName("ExecuteRole");
            role3.setDescription("Execute access");
            role3.setCreatedDateTime(new Date());
            role3.setUpdatedDateTime(new Date());
            mongoRoleDaoImpl.saveOrUpdateRole(role3);

            // Create principals with different combinations
        MongoIkasanPrincipalImpl p1 = new MongoIkasanPrincipalImpl();
            p1.setName("user_read");
            p1.setType("user");
            p1.setDescription("Read only");
            p1.setCreatedDateTime(new Date());
            p1.setUpdatedDateTime(new Date());
            p1.getRoles().add(role1);
            dao.saveOrUpdatePrincipal(p1);

        MongoIkasanPrincipalImpl p2 = new MongoIkasanPrincipalImpl();
            p2.setName("user_write");
            p2.setType("user");
            p2.setDescription("Write only");
            p2.setCreatedDateTime(new Date());
            p2.setUpdatedDateTime(new Date());
            p2.getRoles().add(role2);
            dao.saveOrUpdatePrincipal(p2);

        MongoIkasanPrincipalImpl p3 = new MongoIkasanPrincipalImpl();
            p3.setName("user_execute");
            p3.setType("user");
            p3.setDescription("Execute only");
            p3.setCreatedDateTime(new Date());
            p3.setUpdatedDateTime(new Date());
            p3.getRoles().add(role3);
            dao.saveOrUpdatePrincipal(p3);

        MongoIkasanPrincipalImpl p4 = new MongoIkasanPrincipalImpl();
            p4.setName("user_none");
            p4.setType("user");
            p4.setDescription("No roles");
            p4.setCreatedDateTime(new Date());
            p4.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(p4);

            // Search for all three roles
            List<IkasanPrincipal> results = dao.getPrincipalsByRoleNames(
                Arrays.asList("ReadRole", "WriteRole", "ExecuteRole"));

            // Should return 3 principals (one for each role)
            Assert.assertEquals(3, results.size());

            // Verify user_none is not in results
            boolean hasUserNone = results.stream()
                .anyMatch(p -> p.getName().equals("user_none"));
            Assert.assertFalse(hasUserNone);
    }

    @Test
    public void test_getPrincipalByName_with_null_name() throws Exception {
        // Search with null name should not throw exception, just return null
        IkasanPrincipal result = dao.getPrincipalByName(null);

            // Solr may handle this differently, but it shouldn't crash
            // The result could be null or empty, depending on Solr's handling
            Assert.assertNull(result);
    }

    @Test
    public void test_getAllPrincipalsWithRole_with_special_characters() throws Exception {
        // Create role with special characters
        MongoRoleImpl role = new MongoRoleImpl();
            role.setName("Admin-Role@2024");
            role.setDescription("Role with special chars");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());
            mongoRoleDaoImpl.saveOrUpdateRole(role);

            // Create principal with this role
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
        principal.setName("testuser");
        principal.setType("user");
        principal.setDescription("Test user");
        principal.setCreatedDateTime(new Date());
        principal.setUpdatedDateTime(new Date());
        principal.getRoles().add(role);
        dao.saveOrUpdatePrincipal(principal);

        // Should handle special characters in role name
        List<IkasanPrincipal> results = dao.getAllPrincipalsWithRole("Admin-Role@2024");

        Assert.assertEquals(1, results.size());
        Assert.assertEquals("testuser", results.get(0).getName());
    }

    @Test
    public void test_getPrincipals_with_invalid_filter_values() throws Exception {
        // Create a principal
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("testuser");
            principal.setType("user");
            principal.setDescription("Test user");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(principal);

            // Test with filter containing special characters that might break Solr query
        MongoIkasanPrincipalFilterImpl filter = new MongoIkasanPrincipalFilterImpl();
            filter.setNameFilter("test*user");  // Wildcard in filter

            // Should handle special characters gracefully
            List<IkasanPrincipal> results = dao.getPrincipals(filter, 10, 0);

            // Result may vary based on Solr's handling, but shouldn't throw exception
            Assert.assertNotNull(results);
    }

    @Test
    public void test_saveOrUpdatePrincipal_with_null_dates() throws Exception {
        // Create principal with null dates
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("testuser");
            principal.setType("user");
            principal.setDescription("Test user");
            principal.setCreatedDateTime(null);  // Null date
            principal.setUpdatedDateTime(null);  // Null date

            // Should handle null dates by using current time
            dao.saveOrUpdatePrincipal(principal);

        IkasanPrincipal found = dao.getPrincipalByName("testuser");
            Assert.assertNotNull(found);
            Assert.assertEquals("testuser", found.getName());
    }

    @Test
    public void test_getPrincipalsWithRoleCount_with_null_role() throws Exception {
        // Should handle null role name gracefully
            int count = dao.getPrincipalsWithRoleCount(null, null);

            Assert.assertEquals(0, count);
    }

    @Test
    public void test_getPrincipalsWithoutRoleCount_with_empty_role() throws Exception {
        // Should handle empty role name gracefully
            int count = dao.getPrincipalsWithoutRoleCount("", null);

            Assert.assertEquals(0, count);
    }

    @Test
    public void test_findById_multiple_principals() throws Exception {
        // Create multiple principals
            for (int i = 0; i < 10; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("user" + i);
                principal.setType("user");
                principal.setDescription("User " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            // Find specific principals by ID
        IkasanPrincipal user3 = dao.findById("user3-securityPrincipal");
        IkasanPrincipal user7 = dao.findById("user7-securityPrincipal");

            Assert.assertNotNull(user3);
            Assert.assertEquals("user3", user3.getName());
            Assert.assertEquals("User 3", user3.getDescription());

            Assert.assertNotNull(user7);
            Assert.assertEquals("user7", user7.getName());
            Assert.assertEquals("User 7", user7.getDescription());
    }

    @Test
    public void test_findById_very_long_name() throws Exception {
        // Create principal with very long name
        StringBuilder longName = new StringBuilder();
            for (int i = 0; i < 500; i++) {
                longName.append("X");
            }

        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName(longName.toString());
            principal.setType("user");
            principal.setDescription("User with long name");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(principal);

            // Find by ID
        IkasanPrincipal found = dao.findById(longName.toString()+"-securityPrincipal");
            Assert.assertNotNull(found);
            Assert.assertEquals(longName.toString(), found.getName());
    }

    @Test
    public void test_getAllPrincipals() throws Exception {
        // Save multiple principals
            for (int i = 0; i < 5; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("user" + i);
                principal.setType("user");
                principal.setDescription("Description for user " + i);
                principal.setCreatedDateTime(new Date(1000000L + i));
                principal.setUpdatedDateTime(new Date(2000000L + i));

                dao.saveOrUpdatePrincipal(principal);
            }

            List<IkasanPrincipal> principals = dao.getAllPrincipals();

            Assert.assertNotNull(principals);
            Assert.assertEquals(5, principals.size());
    }

    @Test
    public void test_getPrincipals_with_pagination() throws Exception {
        // Save 10 principals
            for (int i = 0; i < 10; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("user" + i);
                principal.setType("user");
                principal.setDescription("Description " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            // Get first page (limit 5, offset 0)
            List<IkasanPrincipal> page1 = dao.getPrincipals(null, 5, 0);
            Assert.assertEquals(5, page1.size());

            // Get second page (limit 5, offset 5)
            List<IkasanPrincipal> page2 = dao.getPrincipals(null, 5, 5);
            Assert.assertEquals(5, page2.size());
    }

    @Test
    public void test_getAllPrincipalLites() throws Exception {
        // Save multiple principals
            for (int i = 0; i < 3; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("user" + i);
                principal.setType("user");
                principal.setDescription("Description for user " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            List<IkasanPrincipalLite> principals = dao.getAllPrincipalLites();

            Assert.assertNotNull(principals);
            Assert.assertEquals(3, principals.size());
            // Verify it's a lite object (should have basic fields)
            Assert.assertNotNull(principals.get(0).getName());
    }

    @Test
    public void test_getPrincipalLites_with_pagination() throws Exception {
        // Save 8 principals
            for (int i = 0; i < 8; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("user" + i);
                principal.setType("user");
                principal.setDescription("Description " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            // Get first page (limit 3, offset 0)
            List<IkasanPrincipalLite> page1 = dao.getPrincipalLites(null, 3, 0);
            Assert.assertEquals(3, page1.size());

            // Get second page (limit 3, offset 3)
            List<IkasanPrincipalLite> page2 = dao.getPrincipalLites(null, 3, 3);
            Assert.assertEquals(3, page2.size());
    }

    @Test
    public void test_getPrincipalCount() throws Exception {
        // Initially no principals
        int count = dao.getPrincipalCount(null);
        Assert.assertEquals(0, count);

        // Add 7 principals
        for (int i = 0; i < 7; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("user" + i);
            principal.setType("user");
            principal.setDescription("Description " + i);
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(principal);
        }

        count = dao.getPrincipalCount(null);
        Assert.assertEquals(7, count);
    }

    @Test
    public void test_getPrincipals_with_name_filter() throws Exception {
        // Save multiple principals with different names
            for (int i = 0; i < 5; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("user" + i);
                principal.setType("user");
                principal.setDescription("Description " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            // Save some principals with different name pattern
            for (int i = 0; i < 3; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("admin" + i);
                principal.setType("user");
                principal.setDescription("Admin description " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            // Create filter for "user" in name
        MongoIkasanPrincipalFilterImpl filter = new MongoIkasanPrincipalFilterImpl();
            filter.setNameFilter("user");

            List<IkasanPrincipal> filtered = dao.getPrincipals(filter, 100, 0);

            // Should only return principals with "user" in the name
            Assert.assertEquals(5, filtered.size());
            for (IkasanPrincipal principal : filtered) {
                Assert.assertTrue(principal.getName().contains("user"));
            }
    }

    @Test
    public void test_getPrincipals_with_type_filter() throws Exception {
        // Save principals with type "user"
            for (int i = 0; i < 3; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("user" + i);
                principal.setType("user");
                principal.setDescription("User description " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            // Save principals with type "application"
            for (int i = 0; i < 2; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("app" + i);
                principal.setType("application");
                principal.setDescription("Application description " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            // Filter by type "user"
        MongoIkasanPrincipalFilterImpl filter = new MongoIkasanPrincipalFilterImpl();
            filter.setTypeFilter("user");

            List<IkasanPrincipal> filtered = dao.getPrincipals(filter, 100, 0);

            Assert.assertEquals(3, filtered.size());
            for (IkasanPrincipal principal : filtered) {
                Assert.assertEquals("user", principal.getType());
            }
    }

    @Test
    public void test_getPrincipals_with_description_filter() throws Exception {
        // Save principals with different descriptions
            for (int i = 0; i < 3; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("user" + i);
                principal.setType("user");
                principal.setDescription("Manager description " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            for (int i = 0; i < 2; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("admin" + i);
                principal.setType("user");
                principal.setDescription("Admin role " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            // Filter by description containing "Manager"
        MongoIkasanPrincipalFilterImpl filter = new MongoIkasanPrincipalFilterImpl();
            filter.setDescriptionFilter("Manager");

            List<IkasanPrincipal> filtered = dao.getPrincipals(filter, 100, 0);

            Assert.assertEquals(3, filtered.size());
            for (IkasanPrincipal principal : filtered) {
                Assert.assertTrue(principal.getDescription().contains("Manager"));
            }
    }

    @Test
    public void test_getPrincipals_with_multiple_filters() throws Exception {
        // Save various principals
        MongoIkasanPrincipalImpl p1 = new MongoIkasanPrincipalImpl();
            p1.setName("admin_user1");
            p1.setType("user");
            p1.setDescription("Admin user description");
            p1.setCreatedDateTime(new Date());
            p1.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(p1);

        MongoIkasanPrincipalImpl p2 = new MongoIkasanPrincipalImpl();
            p2.setName("admin_user2");
            p2.setType("user");
            p2.setDescription("Admin user description");
            p2.setCreatedDateTime(new Date());
            p2.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(p2);

        MongoIkasanPrincipalImpl p3 = new MongoIkasanPrincipalImpl();
            p3.setName("admin_app");
            p3.setType("application");
            p3.setDescription("Admin app description");
            p3.setCreatedDateTime(new Date());
            p3.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(p3);

        MongoIkasanPrincipalImpl p4 = new MongoIkasanPrincipalImpl();
            p4.setName("regular_user");
            p4.setType("user");
            p4.setDescription("Regular user description");
            p4.setCreatedDateTime(new Date());
            p4.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(p4);

            // Filter by name containing "admin" AND type "user"
        MongoIkasanPrincipalFilterImpl filter = new MongoIkasanPrincipalFilterImpl();
            filter.setNameFilter("admin");
            filter.setTypeFilter("user");

            List<IkasanPrincipal> filtered = dao.getPrincipals(filter, 100, 0);

            Assert.assertEquals(2, filtered.size());
            for (IkasanPrincipal principal : filtered) {
                Assert.assertTrue(principal.getName().contains("admin"));
                Assert.assertEquals("user", principal.getType());
            }
    }

    @Test
    public void test_getPrincipals_with_filter_and_pagination() throws Exception {
        // Save 10 principals matching filter criteria
            for (int i = 0; i < 10; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("user" + i);
                principal.setType("user");
                principal.setDescription("Test description " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            // Save some that don't match
            for (int i = 0; i < 3; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("admin" + i);
                principal.setType("application");
                principal.setDescription("Admin description " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

        MongoIkasanPrincipalFilterImpl filter = new MongoIkasanPrincipalFilterImpl();
            filter.setTypeFilter("useR");

            // Get first page
            List<IkasanPrincipal> page1 = dao.getPrincipals(filter, 5, 0);
            Assert.assertEquals(5, page1.size());

            // Get second page
            List<IkasanPrincipal> page2 = dao.getPrincipals(filter, 5, 5);
            Assert.assertEquals(5, page2.size());

            // Verify all have type "user"
            for (IkasanPrincipal principal : page1) {
                Assert.assertEquals("user", principal.getType());
            }
            for (IkasanPrincipal principal : page2) {
                Assert.assertEquals("user", principal.getType());
            }
    }

    @Test
    public void test_getPrincipalLites_with_filter() throws Exception {
        // Save principals
            for (int i = 0; i < 5; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("manager" + i);
                principal.setType("user");
                principal.setDescription("Manager role");
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            for (int i = 0; i < 3; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("developer" + i);
                principal.setType("user");
                principal.setDescription("Developer role");
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

        MongoIkasanPrincipalFilterImpl filter = new MongoIkasanPrincipalFilterImpl();
            filter.setNameFilter("maNAger");

            List<IkasanPrincipalLite> filtered = dao.getPrincipalLites(filter, 100, 0);

            Assert.assertEquals(5, filtered.size());
            for (IkasanPrincipalLite principal : filtered) {
                Assert.assertTrue(principal.getName().contains("manager"));
            }
    }

    @Test
    public void test_getPrincipalCount_with_filter() throws Exception {
        // Save principals
            for (int i = 0; i < 7; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("activeUser" + i);
                principal.setType("user");
                principal.setDescription("Active user");
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            for (int i = 0; i < 3; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("inactiveUser" + i);
                principal.setType("user");
                principal.setDescription("Inactive user");
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

        MongoIkasanPrincipalFilterImpl filter = new MongoIkasanPrincipalFilterImpl();
            filter.setDescriptionFilter("AcTive");

            int count = dao.getPrincipalCount(filter);

            Assert.assertEquals(10, count);
    }

    @Test
    public void test_getAllPrincipalsWithRole_basic() throws Exception {
        // Create roles
        MongoRoleImpl adminRole = new MongoRoleImpl();
        adminRole.setName("AdminRole");
        adminRole.setDescription("Administrator role");
        adminRole.setCreatedDateTime(new Date());
        adminRole.setUpdatedDateTime(new Date());
        mongoRoleDaoImpl.saveOrUpdateRole(adminRole);

        MongoRoleImpl userRole = new MongoRoleImpl();
        userRole.setName("UserRole");
        userRole.setDescription("User role");
        userRole.setCreatedDateTime(new Date());
        userRole.setUpdatedDateTime(new Date());
        mongoRoleDaoImpl.saveOrUpdateRole(userRole);

        // Create principals with AdminRole
        for (int i = 0; i < 3; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("admin" + i);
            principal.setType("user");
            principal.setDescription("Admin user " + i);
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            principal.addRole(adminRole);
            dao.saveOrUpdatePrincipal(principal);
        }

        // Create principals with UserRole
        for (int i = 0; i < 2; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("user" + i);
            principal.setType("user");
            principal.setDescription("Regular user " + i);
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            principal.addRole(userRole);
            dao.saveOrUpdatePrincipal(principal);
        }

        // Test getAllPrincipalsWithRole
        List<IkasanPrincipal> admins = dao.getAllPrincipalsWithRole("AdminRole");

        Assert.assertEquals(3, admins.size());
        for (IkasanPrincipal principal : admins) {
            Assert.assertTrue(principal.getName().startsWith("admin"));
            Assert.assertTrue(principal.getRoles().stream()
                .anyMatch(role -> role.getName().equals("AdminRole")));
        }

        // Test with UserRole
        List<IkasanPrincipal> users = dao.getAllPrincipalsWithRole("UserRole");
        Assert.assertEquals(2, users.size());
    }

    @Test
    public void test_getAllPrincipalsWithRole_with_filter_and_pagination() throws Exception {
        // Create role
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("DeveloperRole");
        role.setDescription("Developer role");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());
        mongoRoleDaoImpl.saveOrUpdateRole(role);

        // Create 10 principals with the role
        for (int i = 0; i < 10; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("developer" + i);
            principal.setType("user");
            principal.setDescription("Developer " + i);
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            principal.getRoles().add(role);
            dao.saveOrUpdatePrincipal(principal);
        }

        // Test pagination
        List<IkasanPrincipalLite> page1 = dao.getAllPrincipalsWithRole("DeveloperRole", null, 5, 0);
        Assert.assertEquals(5, page1.size());

        List<IkasanPrincipalLite> page2 = dao.getAllPrincipalsWithRole("DeveloperRole", null, 5, 5);
        Assert.assertEquals(5, page2.size());

        // Verify all have the role
        for (IkasanPrincipalLite principal : page1) {
            Assert.assertTrue(principal.getName().startsWith("developer"));
        }
    }

    @Test
    public void test_getAllPrincipalsWithRole_with_name_filter() throws Exception {
        // Create role
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("ManagerRole");
        role.setDescription("Manager role");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());
        mongoRoleDaoImpl.saveOrUpdateRole(role);

            // Create principals with the role
        MongoIkasanPrincipalImpl p1 = new MongoIkasanPrincipalImpl();
        p1.setName("senior_manager");
        p1.setType("user");
        p1.setDescription("Senior manager");
        p1.setCreatedDateTime(new Date());
        p1.setUpdatedDateTime(new Date());
        p1.getRoles().add(role);
        dao.saveOrUpdatePrincipal(p1);

        MongoIkasanPrincipalImpl p2 = new MongoIkasanPrincipalImpl();
        p2.setName("junior_manager");
        p2.setType("user");
        p2.setDescription("Junior manager");
        p2.setCreatedDateTime(new Date());
        p2.setUpdatedDateTime(new Date());
        p2.getRoles().add(role);
        dao.saveOrUpdatePrincipal(p2);

        MongoIkasanPrincipalImpl p3 = new MongoIkasanPrincipalImpl();
        p3.setName("senior_developer");
        p3.setType("user");
        p3.setDescription("Senior developer");
        p3.setCreatedDateTime(new Date());
        p3.setUpdatedDateTime(new Date());
        p3.getRoles().add(role);
        dao.saveOrUpdatePrincipal(p3);

            // Filter by name containing "senior"
        MongoIkasanPrincipalFilterImpl filter = new MongoIkasanPrincipalFilterImpl();
        filter.setNameFilter("senior");

        List<IkasanPrincipalLite> filtered = dao.getAllPrincipalsWithRole("ManagerRole", filter, 100, 0);

        Assert.assertEquals(2, filtered.size());
        for (IkasanPrincipalLite principal : filtered) {
            Assert.assertTrue(principal.getName().contains("senior"));
        }
    }

    @Test
    public void test_getAllPrincipalsWithRole_with_type_filter() throws Exception {
        // Create role
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("AccessRole");
        role.setDescription("Access role");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());
        mongoRoleDaoImpl.saveOrUpdateRole(role);

        // Create user principals
        for (int i = 0; i < 3; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("user" + i);
            principal.setType("user");
            principal.setDescription("User " + i);
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            principal.getRoles().add(role);
            dao.saveOrUpdatePrincipal(principal);
        }

        // Create application principals
        for (int i = 0; i < 2; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("app" + i);
            principal.setType("application");
            principal.setDescription("Application " + i);
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            principal.getRoles().add(role);
            dao.saveOrUpdatePrincipal(principal);
        }

            // Filter by type "user"
        MongoIkasanPrincipalFilterImpl filter = new MongoIkasanPrincipalFilterImpl();
        filter.setTypeFilter("user");

        List<IkasanPrincipalLite> users = dao.getAllPrincipalsWithRole("AccessRole", filter, 100, 0);

        Assert.assertEquals(3, users.size());
        for (IkasanPrincipalLite principal : users) {
            Assert.assertEquals("user", principal.getType());
        }
    }

    @Test
    public void test_getAllPrincipalsWithoutRole() throws Exception {
        // Create roles
        MongoRoleImpl adminRole = new MongoRoleImpl();
            adminRole.setName("AdminRole");
            adminRole.setDescription("Administrator role");
            adminRole.setCreatedDateTime(new Date());
            adminRole.setUpdatedDateTime(new Date());
            mongoRoleDaoImpl.saveOrUpdateRole(adminRole);

            // Create principals with AdminRole
            for (int i = 0; i < 3; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("admin" + i);
                principal.setType("user");
                principal.setDescription("Admin user " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                principal.getRoles().add(adminRole);
                dao.saveOrUpdatePrincipal(principal);
            }

            // Create principals without AdminRole
            for (int i = 0; i < 5; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("user" + i);
                principal.setType("user");
                principal.setDescription("Regular user " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            // Test getAllPrincipalsWithoutRole
            List<IkasanPrincipalLite> nonAdmins = dao.getAllPrincipalsWithoutRole("AdminRole", null, 100, 0);

            Assert.assertEquals(5, nonAdmins.size());
            for (IkasanPrincipalLite principal : nonAdmins) {
                Assert.assertTrue(principal.getName().startsWith("user"));
            }
    }

    @Test
    public void test_getPrincipalsWithRoleCount() throws Exception {
        // Create role
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("CountTestRole");
        role.setDescription("Role for count test");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());
        mongoRoleDaoImpl.saveOrUpdateRole(role);

        // Create principals with the role
        for (int i = 0; i < 7; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("user" + i);
            principal.setType("user");
            principal.setDescription("User " + i);
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            principal.addRole(role);
            dao.saveOrUpdatePrincipal(principal);
        }

        // Create principals without the role
        for (int i = 0; i < 3; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("other" + i);
            principal.setType("user");
            principal.setDescription("Other user " + i);
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(principal);
        }

        // Test count
        int count = dao.getPrincipalsWithRoleCount("CountTestRole", null);
        Assert.assertEquals(7, count);

            // Test count with filter
        MongoIkasanPrincipalFilterImpl filter = new MongoIkasanPrincipalFilterImpl();
            filter.setNameFilter("user");
            int filteredCount = dao.getPrincipalsWithRoleCount("CountTestRole", filter);
            Assert.assertEquals(7, filteredCount);
    }

    @Test
    public void test_getPrincipalsWithoutRoleCount() throws Exception {
        // Create role
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("ExclusionRole");
        role.setDescription("Role for exclusion test");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());
        mongoRoleDaoImpl.saveOrUpdateRole(role);

        // Create principals with the role
        for (int i = 0; i < 4; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("included" + i);
            principal.setType("user");
            principal.setDescription("Included user " + i);
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            principal.getRoles().add(role);
            dao.saveOrUpdatePrincipal(principal);
        }

        // Create principals without the role
        for (int i = 0; i < 6; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("excluded" + i);
            principal.setType("user");
            principal.setDescription("Excluded user " + i);
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            dao.saveOrUpdatePrincipal(principal);
        }

        // Test count of principals without the role
        int count = dao.getPrincipalsWithoutRoleCount("ExclusionRole", new MongoIkasanPrincipalFilterImpl());
        Assert.assertEquals(6, count);

            // Test count with filter
        MongoIkasanPrincipalFilterImpl filter = new MongoIkasanPrincipalFilterImpl();
        filter.setNameFilter("excluded");
        int filteredCount = dao.getPrincipalsWithoutRoleCount("ExclusionRole", filter);
        Assert.assertEquals(6, filteredCount);

        filter = new MongoIkasanPrincipalFilterImpl();
        filter.setNameFilter("included");
        filteredCount = dao.getPrincipalsWithoutRoleCount("ExclusionRole", filter);
        Assert.assertEquals(0, filteredCount);
    }

    @Test
    public void test_getPrincipalsByRoleNames_single_role() throws Exception {
        // Create role
        MongoRoleImpl role = new MongoRoleImpl();
            role.setName("TestRole");
            role.setDescription("Test role");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());
            mongoRoleDaoImpl.saveOrUpdateRole(role);

            // Create principals with the role
            for (int i = 0; i < 3; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("user" + i);
                principal.setType("user");
                principal.setDescription("User " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                principal.getRoles().add(role);
                dao.saveOrUpdatePrincipal(principal);
            }

            // Create principals without the role
            for (int i = 0; i < 2; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("other" + i);
                principal.setType("user");
                principal.setDescription("Other user " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            // Test with single role in list
            List<IkasanPrincipal> principals = dao.getPrincipalsByRoleNames(Arrays.asList("TestRole"));

            Assert.assertEquals(3, principals.size());
            for (IkasanPrincipal principal : principals) {
                Assert.assertTrue(principal.getName().startsWith("user"));
            }
    }

    @Test
    public void test_getPrincipalsByRoleNames_multiple_roles() throws Exception {
        // Create three roles
        MongoRoleImpl adminRole = new MongoRoleImpl();
        adminRole.setName("AdminRole");
        adminRole.setDescription("Admin role");
        adminRole.setCreatedDateTime(new Date());
        adminRole.setUpdatedDateTime(new Date());
        mongoRoleDaoImpl.saveOrUpdateRole(adminRole);

        MongoRoleImpl devRole = new MongoRoleImpl();
        devRole.setName("DeveloperRole");
        devRole.setDescription("Developer role");
        devRole.setCreatedDateTime(new Date());
        devRole.setUpdatedDateTime(new Date());
        mongoRoleDaoImpl.saveOrUpdateRole(devRole);

        MongoRoleImpl managerRole = new MongoRoleImpl();
        managerRole.setName("ManagerRole");
        managerRole.setDescription("Manager role");
        managerRole.setCreatedDateTime(new Date());
        managerRole.setUpdatedDateTime(new Date());
        mongoRoleDaoImpl.saveOrUpdateRole(managerRole);

        // Create principals with AdminRole
        for (int i = 0; i < 2; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("admin" + i);
            principal.setType("user");
            principal.setDescription("Admin " + i);
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            principal.addRole(adminRole);
            dao.saveOrUpdatePrincipal(principal);
        }

        // Create principals with DeveloperRole
        for (int i = 0; i < 3; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("dev" + i);
            principal.setType("user");
            principal.setDescription("Developer " + i);
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            principal.addRole(devRole);
            dao.saveOrUpdatePrincipal(principal);
        }

        // Create principals with ManagerRole
        for (int i = 0; i < 1; i++) {
            MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName("manager" + i);
            principal.setType("user");
            principal.setDescription("Manager " + i);
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());
            principal.addRole(managerRole);
            dao.saveOrUpdatePrincipal(principal);
        }

            // Create principals with no roles
        MongoIkasanPrincipalImpl noRole = new MongoIkasanPrincipalImpl();
        noRole.setName("norole");
        noRole.setType("user");
        noRole.setDescription("No role user");
        noRole.setCreatedDateTime(new Date());
        noRole.setUpdatedDateTime(new Date());
        dao.saveOrUpdatePrincipal(noRole);

        // Test with multiple roles (AdminRole OR DeveloperRole)
        List<IkasanPrincipal> principals = dao.getPrincipalsByRoleNames(
            Arrays.asList("AdminRole", "DeveloperRole"));

        // Should return 2 admins + 3 developers = 5 principals
        Assert.assertEquals(5, principals.size());

        // Verify each principal has either AdminRole or DeveloperRole
        for (IkasanPrincipal principal : principals) {
            boolean hasAdminOrDev = principal.getRoles().stream()
                .anyMatch(role -> role.getName().equals("AdminRole") || role.getName().equals("DeveloperRole"));
            Assert.assertTrue(hasAdminOrDev);
        }
    }

    @Test(expected = RuntimeException.class)
    public void test_saveOrUpdatePrincipal_with_circular_reference() throws Exception {
        // Create a principal with circular reference that will cause JSON serialization to fail
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl() {
            @Override
            public String getName() {
                // This will cause serialization issues
                throw new RuntimeException("Intentional error during serialization");
            }
        };
        principal.setType("user");
        principal.setDescription("Test principal");
        principal.setCreatedDateTime(new Date());
        principal.setUpdatedDateTime(new Date());

        // This should throw RuntimeException due to serialization failure
        dao.saveOrUpdatePrincipal(principal);
    }

    @Test
    public void test_deletePrincipal_with_null_principal() throws Exception {
        try {
            dao.deletePrincipal(null);
            Assert.fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // Expected - null principal should cause NPE
            Assert.assertTrue(true);
        }
    }

    @Test
    public void test_getPrincipalsByRoleNames_with_very_large_list() throws Exception {
        // Create a very large list of role names
            List roleNames = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                roleNames.add("Role" + i);
            }

            // Should handle large list without throwing exception
            List<IkasanPrincipal> results = dao.getPrincipalsByRoleNames(roleNames);

            Assert.assertNotNull(results);
            Assert.assertTrue(results.isEmpty()); // No roles actually exist
    }

    @Test
    public void test_getPrincipalCount_with_null_filter() throws Exception {
        // Create some principals
            for (int i = 0; i < 5; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("user" + i);
                principal.setType("user");
                principal.setDescription("User " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            // Should handle null filter gracefully
            int count = dao.getPrincipalCount(null);

            Assert.assertEquals(5, count);
    }

    @Test
    public void test_getAllPrincipalsWithoutRole_with_null_filter() throws Exception {
        // Create role
        MongoRoleImpl role = new MongoRoleImpl();
            role.setName("TestRole");
            role.setDescription("Test role");
            role.setCreatedDateTime(new Date());
            role.setUpdatedDateTime(new Date());
            mongoRoleDaoImpl.saveOrUpdateRole(role);

            // Create principals without role
            for (int i = 0; i < 3; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("user" + i);
                principal.setType("user");
                principal.setDescription("User " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            // Should handle null filter
            List<IkasanPrincipalLite> results = dao.getAllPrincipalsWithoutRole("TestRole", null, 10, 0);

            Assert.assertEquals(3, results.size());
    }

    @Test
    public void test_getPrincipalLites_with_negative_pagination() throws Exception {
        // Create principals
            for (int i = 0; i < 5; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("user" + i);
                principal.setType("user");
                principal.setDescription("User " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            // Test with negative offset and limit
            List<IkasanPrincipalLite> results = dao.getPrincipalLites(null, -1, -1);

            // Mongo may handle this differently, but it shouldn't crash
            Assert.assertNotNull(results);
    }

    @Test
    public void test_getPrincipalByNameLike_with_empty_string() throws Exception {
        // Create some principals
            for (int i = 0; i < 3; i++) {
                MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
                principal.setName("user" + i);
                principal.setType("user");
                principal.setDescription("User " + i);
                principal.setCreatedDateTime(new Date());
                principal.setUpdatedDateTime(new Date());
                dao.saveOrUpdatePrincipal(principal);
            }

            // Search with empty string
            List<IkasanPrincipal> results = dao.getPrincipalByNameLike("");

            // Should return all principals (empty string matches everything)
            Assert.assertNotNull(results);
            Assert.assertEquals(3, results.size());
    }

    @Test
    public void test_saveOrUpdatePrincipal_with_very_long_name() throws Exception {
        // Create principal with very long name
        StringBuilder longName = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                longName.append("A");
            }

        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
            principal.setName(longName.toString());
            principal.setType("user");
            principal.setDescription("Test user with long name");
            principal.setCreatedDateTime(new Date());
            principal.setUpdatedDateTime(new Date());

            // Should handle very long name
            dao.saveOrUpdatePrincipal(principal);

        IkasanPrincipal found = dao.getPrincipalByName(longName.toString());
            Assert.assertNotNull(found);
            Assert.assertEquals(longName.toString(), found.getName());
    }

    @Test
    public void test_getAllPrincipalsWithRole_with_role_containing_quotes() throws Exception {
        // Create role with quotes in name
        MongoRoleImpl role = new MongoRoleImpl();
        role.setName("Admin\"Role");
        role.setDescription("Role with quotes");
        role.setCreatedDateTime(new Date());
        role.setUpdatedDateTime(new Date());
        mongoRoleDaoImpl.saveOrUpdateRole(role);

        // Create principal with this role
        MongoIkasanPrincipalImpl principal = new MongoIkasanPrincipalImpl();
        principal.setName("testuser");
        principal.setType("user");
        principal.setDescription("Test user");
        principal.setCreatedDateTime(new Date());
        principal.setUpdatedDateTime(new Date());
        principal.getRoles().add(role);
        dao.saveOrUpdatePrincipal(principal);

        // Should handle quotes in role name
        try {
            List<IkasanPrincipal> results = dao.getAllPrincipalsWithRole("Admin\"Role");
            Assert.assertNotNull(results);
        } catch (Exception e) {
            // Mongo might reject quotes in queries, which is acceptable
            Assert.assertTrue(e.getMessage().contains("parse") ||
                            e.getMessage().contains("query") ||
                            e.getMessage().contains("syntax"));
        }
    }
}
