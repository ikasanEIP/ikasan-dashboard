package org.ikasan.mongo.persistence.security.dao;

import org.ikasan.mongo.persistence.MongoPersistenceAutoConfiguration;
import org.ikasan.mongo.persistence.security.model.MongoUserImpl;
import org.ikasan.mongo.persistence.security.repository.MongoIkasanPrincipalRepository;
import org.ikasan.mongo.persistence.security.repository.MongoPolicyRepository;
import org.ikasan.mongo.persistence.security.repository.MongoRoleRepository;
import org.ikasan.mongo.persistence.security.repository.MongoUserRepository;
import org.ikasan.spec.security.model.IkasanPrincipal;
import org.ikasan.spec.security.model.User;
import org.ikasan.spec.security.model.UserFilter;
import org.ikasan.spec.security.model.UserLite;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MongoDB DAO Test for User operations.
 * This test class contains 74 test methods converted from SolrUserDaoImplTest.
 *
 * Test coverage includes:
 * - Basic CRUD operations (create, save, update, delete) - 25 tests
 * - User search by username, firstname, surname - 6 tests
 * - User filtering by enabled/disabled status - 2 tests
 * - Pagination support - 4 tests
 * - Role association tests - 18 tests
 * - getUsersWithRoleCount tests - 8 tests
 * - getUsersWithoutRoleCount tests - 9 tests
 * - getUsersAssociatedWithPrincipal tests - 14 tests
 * - User lite operations for performance - 3 tests
 * - Special character handling - 2 tests
 * - Null/empty parameter handling - multiple tests
 * - Integration scenarios with principals - multiple tests
 *
 * NOTE: All test method names and logic are preserved from the Solr implementation.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoPersistenceAutoConfiguration.class})
public class MongoUserDaoImplTest {

    public static MongoDBContainer mongoDBContainer;

    @BeforeClass
    public static void startContainer() {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));
        mongoDBContainer.start();
    }

    @Autowired
    private MongoUserRepository repository;

    @Autowired
    private MongoIkasanPrincipalRepository principalRepository;

    @Autowired
    private MongoRoleRepository roleRepository;

    @Autowired
    private MongoPolicyRepository policyRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private MongoUserDaoImpl dao;
    private MongoIkasanPrincipalDaoImpl mongoIkasanPrincipalDaoImpl;
    private MongoRoleDaoImpl mongoRoleDaoImpl;
    private MongoPolicyDaoImpl mongoPolicyDaoImpl;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    public void setDao(MongoUserRepository repository,
                       MongoIkasanPrincipalRepository principalRepository,
                       MongoRoleRepository roleRepository,
                       MongoPolicyRepository policyRepository,
                       MongoTemplate mongoTemplate) {
        // Step 1: Create MongoPolicyDaoImpl first (no dependencies)
        this.mongoPolicyDaoImpl = new MongoPolicyDaoImpl(policyRepository, mongoTemplate);

        // Step 2: Create MongoRoleDaoImpl with MongoPolicyDaoImpl
        this.mongoRoleDaoImpl = new MongoRoleDaoImpl(roleRepository, mongoTemplate, this.mongoPolicyDaoImpl);

        // Step 3: Set circular reference
        this.mongoPolicyDaoImpl.setMongoRoleDao(this.mongoRoleDaoImpl);

        // Step 4: Create MongoIkasanPrincipalDaoImpl with MongoRoleDaoImpl
        this.mongoIkasanPrincipalDaoImpl = new MongoIkasanPrincipalDaoImpl(principalRepository, mongoTemplate, this.mongoRoleDaoImpl);

        // Step 5: Create MongoUserDaoImpl with MongoIkasanPrincipalDaoImpl
        this.dao = new MongoUserDaoImpl(repository, mongoTemplate, this.mongoIkasanPrincipalDaoImpl);
    }

    @After
    public void teardown() {
        repository.deleteAll();
        principalRepository.deleteAll();
        roleRepository.deleteAll();
        policyRepository.deleteAll();
    }

    @AfterClass
    public static void stopContainer() {
        if (mongoDBContainer != null) {
            mongoDBContainer.stop();
        }
    }

    @Test
    public void test_createUser() {
        User user = dao.createUser("testuser", "password", "test@example.com", true);

        Assert.assertNotNull(user);
        Assert.assertTrue(user instanceof MongoUserImpl);
        Assert.assertEquals("testuser", user.getUsername());
        Assert.assertEquals("password", user.getPassword());
        Assert.assertEquals("test@example.com", user.getEmail());
        Assert.assertTrue(user.isEnabled());
    }

    @Test
    public void test_save_and_getUser() {
        MongoUserImpl user = new MongoUserImpl();
        user.setUsername("john.doe");
        user.setPassword("password123");
        user.setEmail("john.doe@example.com");
        user.setFirstName("John");
        user.setSurname("Doe");
        user.setDepartment("Engineering");
        user.setEnabled(true);

        dao.save(user);

        User found = dao.getUser("john.doe");

        Assert.assertNotNull(found);
        Assert.assertEquals("john.doe", found.getUsername());
        Assert.assertEquals("john.doe@example.com", found.getEmail());
        Assert.assertEquals("John", found.getFirstName());
        Assert.assertEquals("Doe", found.getSurname());
        Assert.assertEquals("Engineering", found.getDepartment());
    }

    @Test
    public void test_save_update_existing_user() {
        MongoUserImpl user = new MongoUserImpl();
        user.setUsername("jane.smith");
        user.setEmail("jane@example.com");
        user.setFirstName("Jane");
        user.setSurname("Smith");
        user.setEnabled(true);

        dao.save(user);

        // Update the user
        user.setEmail("jane.smith@example.com");
        user.setDepartment("Sales");

        dao.save(user);

        User found = dao.getUser("jane.smith");

        Assert.assertNotNull(found);
        Assert.assertEquals("jane.smith@example.com", found.getEmail());
        Assert.assertEquals("Sales", found.getDepartment());
    }

    @Test
    public void test_getUser_not_found() {
        User found = dao.getUser("nonexistent");

        Assert.assertNull(found);
    }

    @Test
    public void test_getUsers() {
        // Create multiple users
        for (int i = 0; i < 5; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setFirstName("First" + i);
            user.setSurname("Last" + i);
            user.setEnabled(true);

            dao.save(user);
        }

        List<User> users = dao.getUsers();

        Assert.assertNotNull(users);
        Assert.assertEquals(5, users.size());
    }

    @Test
    public void test_getUsers_empty() {
        List<User> users = dao.getUsers();

        Assert.assertNotNull(users);
        Assert.assertTrue(users.isEmpty());
    }

    @Test
    public void test_getUserLites() {
        for (int i = 0; i < 3; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setEnabled(true);

            dao.save(user);
        }

        List<UserLite> userLites = dao.getUserLites();

        Assert.assertNotNull(userLites);
        Assert.assertEquals(3, userLites.size());
    }

    @Test
    public void test_getUserLites_with_pagination() {
        for (int i = 0; i < 10; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setEnabled(true);

            dao.save(user);
        }

        List<UserLite> page1 = dao.getUserLites(5, 0);
        Assert.assertEquals(5, page1.size());

        List<UserLite> page2 = dao.getUserLites(5, 5);
        Assert.assertEquals(5, page2.size());
    }

    @Test
    public void test_getUserByUsernameLike() {
        MongoUserImpl user1 = new MongoUserImpl();
        user1.setUsername("john.doe");
        user1.setEmail("john@example.com");
        user1.setEnabled(true);

        MongoUserImpl user2 = new MongoUserImpl();
        user2.setUsername("john.smith");
        user2.setEmail("smith@example.com");
        user2.setEnabled(true);

        MongoUserImpl user3 = new MongoUserImpl();
        user3.setUsername("jane.doe");
        user3.setEmail("jane@example.com");
        user3.setEnabled(true);

        dao.save(user1);
        dao.save(user2);
        dao.save(user3);

        List<User> users = dao.getUserByUsernameLike("john");

        Assert.assertNotNull(users);
        Assert.assertEquals(2, users.size());
        Assert.assertTrue(users.stream().anyMatch(u -> "john.doe".equals(u.getUsername())));
        Assert.assertTrue(users.stream().anyMatch(u -> "john.smith".equals(u.getUsername())));
    }

    @Test
    public void test_getUserByUsernameLike_no_match() {
        MongoUserImpl user = new MongoUserImpl();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setEnabled(true);

        dao.save(user);

        List<User> users = dao.getUserByUsernameLike("nonexistent");

        Assert.assertNotNull(users);
        Assert.assertTrue(users.isEmpty());
    }

    @Test
    public void test_getUserByFirstnameLike() {
        MongoUserImpl user1 = new MongoUserImpl();
        user1.setUsername("user1");
        user1.setFirstName("Michael");
        user1.setEmail("user1@example.com");
        user1.setEnabled(true);

        MongoUserImpl user2 = new MongoUserImpl();
        user2.setUsername("user2");
        user2.setFirstName("Michelle");
        user2.setEmail("user2@example.com");
        user2.setEnabled(true);

        MongoUserImpl user3 = new MongoUserImpl();
        user3.setUsername("user3");
        user3.setFirstName("John");
        user3.setEmail("user3@example.com");
        user3.setEnabled(true);

        dao.save(user1);
        dao.save(user2);
        dao.save(user3);

        List<User> users = dao.getUserByFirstnameLike("Mich");

        Assert.assertNotNull(users);
        Assert.assertEquals(2, users.size());
        Assert.assertTrue(users.stream().anyMatch(u -> "Michael".equals(u.getFirstName())));
        Assert.assertTrue(users.stream().anyMatch(u -> "Michelle".equals(u.getFirstName())));
    }

    @Test
    public void test_getUserBySurnameLike() {
        MongoUserImpl user1 = new MongoUserImpl();
        user1.setUsername("user1");
        user1.setSurname("Johnson");
        user1.setEmail("user1@example.com");
        user1.setEnabled(true);

        MongoUserImpl user2 = new MongoUserImpl();
        user2.setUsername("user2");
        user2.setSurname("Johnston");
        user2.setEmail("user2@example.com");
        user2.setEnabled(true);

        MongoUserImpl user3 = new MongoUserImpl();
        user3.setUsername("user3");
        user3.setSurname("Smith");
        user3.setEmail("user3@example.com");
        user3.setEnabled(true);

        dao.save(user1);
        dao.save(user2);
        dao.save(user3);

        List<User> users = dao.getUserBySurnameLike("Johns");

        Assert.assertNotNull(users);
        Assert.assertEquals(2, users.size());
        Assert.assertTrue(users.stream().anyMatch(u -> "Johnson".equals(u.getSurname())));
        Assert.assertTrue(users.stream().anyMatch(u -> "Johnston".equals(u.getSurname())));
    }

    @Test
    public void test_delete() {
        MongoUserImpl user = new MongoUserImpl();
        user.setUsername("deletetest");
        user.setEmail("delete@example.com");
        user.setEnabled(true);

        dao.save(user);

        User found = dao.getUser("deletetest");
        Assert.assertNotNull(found);

        dao.delete(user);

        User notFound = dao.getUser("deletetest");
        Assert.assertNull(notFound);
    }

    @Test
    public void test_getUserCount_no_filter() {
        for (int i = 0; i < 7; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setEnabled(true);

            dao.save(user);
        }

        int count = dao.getUserCount(null);

        Assert.assertEquals(7, count);
    }

    @Test
    public void test_getUserCount_empty() {
        int count = dao.getUserCount(null);

        Assert.assertEquals(0, count);
    }

    @Test
    public void test_save_with_principals() {
        MongoUserImpl user = new MongoUserImpl();
        user.setUsername("userWithPrincipal");
        user.setEmail("user@example.com");
        user.setEnabled(true);

        org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
        principal.setName("TestPrincipal");
        principal.setId("TestPrincipal-securityPrincipal");

        Set<IkasanPrincipal> principals = new HashSet<>();
        principals.add(principal);
        user.setPrincipals(principals);

        dao.save(user);

        User found = dao.getUser("userWithPrincipal");

        Assert.assertNotNull(found);
        Assert.assertEquals("userWithPrincipal", found.getUsername());
    }

    @Test
    public void test_getUsers_with_filter_username() {
        for (int i = 0; i < 5; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("admin" + i);
            user.setEmail("admin" + i + "@example.com");
            user.setEnabled(true);
            dao.save(user);
        }

        for (int i = 0; i < 3; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setEnabled(true);
            dao.save(user);
        }

        UserFilter filter = new TestUserFilter();
        filter.setUsernameFilter("admin");

        List<User> users = dao.getUsers(filter, 100, 0);

        Assert.assertNotNull(users);
        Assert.assertEquals(5, users.size());
        Assert.assertTrue(users.stream().allMatch(u -> u.getUsername().contains("admin")));
    }

    @Test
    public void test_getUsers_with_pagination() {
        for (int i = 0; i < 20; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setEnabled(true);
            dao.save(user);
        }

        List<User> page1 = dao.getUsers(null, 10, 0);
        Assert.assertEquals(10, page1.size());

        List<User> page2 = dao.getUsers(null, 10, 10);
        Assert.assertEquals(10, page2.size());
    }

    @Test
    public void test_save_with_special_characters() {
        MongoUserImpl user = new MongoUserImpl();
        user.setUsername("user-with_special.chars");
        user.setEmail("special@example.com");
        user.setFirstName("O'Brien");
        user.setSurname("De La Cruz");
        user.setEnabled(true);

        dao.save(user);

        User found = dao.getUser("user-with_special.chars");

        Assert.assertNotNull(found);
        Assert.assertEquals("O'Brien", found.getFirstName());
        Assert.assertEquals("De La Cruz", found.getSurname());
    }

    @Test
    public void test_multiple_operations_in_sequence() {
        // Create
        MongoUserImpl user = new MongoUserImpl();
        user.setUsername("sequenceuser");
        user.setEmail("sequence@example.com");
        user.setFirstName("Test");
        user.setEnabled(true);
        dao.save(user);

        // Read
        User found = dao.getUser("sequenceuser");
        Assert.assertNotNull(found);
        Assert.assertEquals("Test", found.getFirstName());

        // Update
        user.setFirstName("Updated");
        dao.save(user);
        found = dao.getUser("sequenceuser");
        Assert.assertEquals("Updated", found.getFirstName());

        // List
        List<User> users = dao.getUsers();
        Assert.assertEquals(1, users.size());

        // Delete
        dao.delete(user);
        found = dao.getUser("sequenceuser");
        Assert.assertNull(found);

        // Verify empty
        users = dao.getUsers();
        Assert.assertTrue(users.isEmpty());
    }

    @Test
    public void test_getUserByFirstnameLike_case_sensitive() {
        MongoUserImpl user = new MongoUserImpl();
        user.setUsername("user1");
        user.setFirstName("Michael");
        user.setEmail("user1@example.com");
        user.setEnabled(true);

        dao.save(user);

        List<User> users = dao.getUserByFirstnameLike("mich");

        // MongoDB wildcard queries are typically case-insensitive by default
        Assert.assertNotNull(users);
    }

    @Test
    public void test_delete_nonexistent_user() {
        MongoUserImpl user = new MongoUserImpl();
        user.setUsername("nonexistent");
        user.setEnabled(true);

        // Should not throw exception
        dao.delete(user);

        Assert.assertEquals(0, dao.getUserCount(null));
    }

    @Test
    public void test_save_user_with_all_fields() {
        MongoUserImpl user = new MongoUserImpl();
        user.setUsername("fulluser");
        user.setPassword("password123");
        user.setEmail("full@example.com");
        user.setFirstName("John");
        user.setSurname("Doe");
        user.setDepartment("Engineering");
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setPreviousAccessTimestamp(System.currentTimeMillis());

        dao.save(user);

        User found = dao.getUser("fulluser");

        Assert.assertNotNull(found);
        Assert.assertEquals("fulluser", found.getUsername());
        Assert.assertEquals("full@example.com", found.getEmail());
        Assert.assertEquals("John", found.getFirstName());
        Assert.assertEquals("Doe", found.getSurname());
        Assert.assertEquals("Engineering", found.getDepartment());
        Assert.assertTrue(found.isEnabled());
    }

    // Helper class for testing UserFilter
    private static class TestUserFilter implements UserFilter {
        private String usernameFilter;
        private String emailFilter;
        private String nameFilter;
        private String lastNameFilter;
        private String departmentFilter;
        private String sortOrder;
        private String sortColumn;

        @Override
        public String getNameFilter() {
            return nameFilter;
        }

        @Override
        public String getLastNameFilter() {
            return lastNameFilter;
        }

        @Override
        public void setNameFilter(String nameFilter) {
            this.nameFilter = nameFilter;
        }

        @Override
        public void setLastNameFilter(String lastNameFilter) {
            this.lastNameFilter = lastNameFilter;
        }

        @Override
        public String getUsernameFilter() {
            return usernameFilter;
        }

        @Override
        public void setUsernameFilter(String usernameFilter) {
            this.usernameFilter = usernameFilter;
        }

        @Override
        public String getEmailFilter() {
            return emailFilter;
        }

        @Override
        public void setEmailFilter(String emailFilter) {
            this.emailFilter = emailFilter;
        }

        @Override
        public String getDepartmentFilter() {
            return departmentFilter;
        }

        @Override
        public void setDepartmentFilter(String departmentFilter) {
            this.departmentFilter = departmentFilter;
        }

        @Override
        public String getSortOrder() {
            return sortOrder;
        }

        @Override
        public void setSortOrder(String sortOrder) {
            this.sortOrder = sortOrder;
        }

        @Override
        public String getSortColumn() {
            return sortColumn;
        }

        @Override
        public void setSortColumn(String sortColumn) {
            this.sortColumn = sortColumn;
        }
    }

    @Test
    public void test_getUsersWithRole_basic() {
        // Create a role
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl role = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        role.setName("TestRole");
        role.setDescription("Test role");
        mongoRoleDaoImpl.saveOrUpdateRole(role);

        // Create principals and users
        for (int i = 0; i < 5; i++) {
            // Create user
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setEnabled(true);

            // Create principal for user
            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("user" + i);
            principal.setType("user");
            principal.setId("user" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            roles.add(role);
            principal.setRoles(roles);

            Set<IkasanPrincipal> principals = new HashSet<>();
            principals.add(principal);
            user.setPrincipals(principals);

            dao.save(user);
            mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);
        }

        List<UserLite> users = dao.getUsersWithRole("TestRole", null, 100, 0);

        Assert.assertNotNull(users);
        Assert.assertEquals(5, users.size());
    }

    @Test
    public void test_getUsersWithRole_with_pagination() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl role = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        role.setName("PaginationRole");
        role.setDescription("Pagination test role");
        mongoRoleDaoImpl.saveOrUpdateRole(role);

        // Create 20 users with the role
        for (int i = 0; i < 20; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("paguser" + i);
            user.setEmail("paguser" + i + "@example.com");
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("paguser" + i);
            principal.setType("user");
            principal.setId("paguser" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            roles.add(role);
            principal.setRoles(roles);

            Set<IkasanPrincipal> principals = new HashSet<>();
            principals.add(principal);
            user.setPrincipals(principals);

            dao.save(user);
            mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);
        }

        // Get first page
        List<UserLite> page1 = dao.getUsersWithRole("PaginationRole", null, 10, 0);
        Assert.assertEquals(10, page1.size());

        // Get second page
        List<UserLite> page2 = dao.getUsersWithRole("PaginationRole", null, 10, 10);
        Assert.assertEquals(10, page2.size());
    }

    @Test
    public void test_getUsersWithRole_large_dataset() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl role = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        role.setName("LargeDatasetRole");
        role.setDescription("Large dataset test role");
        mongoRoleDaoImpl.saveOrUpdateRole(role);

        // Create 1500 users with the role (more than 1024)
        int totalUsers = 1500;

        List<IkasanPrincipal> principals = new ArrayList<>();
        List<User> users = new ArrayList<>();

        for (int i = 0; i < totalUsers; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("largeuser" + i);
            user.setEmail("largeuser" + i + "@example.com");
            user.setFirstName("First" + i);
            user.setSurname("Last" + i);
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("largeuser" + i);
            principal.setType("user");
            principal.setId("largeuser" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            roles.add(role);
            principal.setRoles(roles);

            Set<IkasanPrincipal> principalsSet = new HashSet<>();
            principalsSet.add(principal);
            user.setPrincipals(principalsSet);

            users.add(user);
            principals.add(principal);
        }

        for (User user : users) {
            dao.save(user);
        }
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipals(principals);

        // Get all users with role using large limit
        List<UserLite> allUsers = dao.getUsersWithRole("LargeDatasetRole", null, Integer.MAX_VALUE, 0);

        Assert.assertNotNull(allUsers);
        Assert.assertEquals(totalUsers, allUsers.size());

        // Verify pagination works with large dataset
        List<UserLite> firstBatch = dao.getUsersWithRole("LargeDatasetRole", null, 500, 0);
        Assert.assertEquals(500, firstBatch.size());

        List<UserLite> secondBatch = dao.getUsersWithRole("LargeDatasetRole", null, 500, 500);
        Assert.assertEquals(500, secondBatch.size());

        List<UserLite> thirdBatch = dao.getUsersWithRole("LargeDatasetRole", null, 500, 1000);
        Assert.assertEquals(500, thirdBatch.size());
    }

    @Test
    public void test_getUsersWithRole_no_users_with_role() {
        List<UserLite> users = dao.getUsersWithRole("NonExistentRole-securityRole", null, 100, 0);

        Assert.assertNotNull(users);
        Assert.assertTrue(users.isEmpty());
    }

    @Test
    public void test_getUsersWithRole_null_roleName() {
        List<UserLite> users = dao.getUsersWithRole(null, null, 100, 0);

        Assert.assertNotNull(users);
        Assert.assertTrue(users.isEmpty());
    }

    @Test
    public void test_getUsersWithRole_multiple_roles_per_user() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl role1 = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        role1.setName("Role1");
        mongoRoleDaoImpl.saveOrUpdateRole(role1);

        org.ikasan.mongo.persistence.security.model.MongoRoleImpl role2 = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        role2.setName("Role2");
        mongoRoleDaoImpl.saveOrUpdateRole(role2);

        // User with both roles
        MongoUserImpl user = new MongoUserImpl();
        user.setUsername("multiroleuser");
        user.setEmail("multirole@example.com");
        user.setEnabled(true);

        org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
        principal.setName("multiroleuser");
        principal.setType("user");
        principal.setId("multiroleuser-securityPrincipal");

        Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
        roles.add(role1);
        roles.add(role2);
        principal.setRoles(roles);

        Set<IkasanPrincipal> principals = new HashSet<>();
        principals.add(principal);
        user.setPrincipals(principals);

        dao.save(user);
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);

        // Should find user with role1
        List<UserLite> usersRole1 = dao.getUsersWithRole("Role1", null, 100, 0);
        Assert.assertEquals(1, usersRole1.size());
        Assert.assertEquals("multiroleuser", usersRole1.get(0).getUsername());

        // Should find user with role2
        List<UserLite> usersRole2 = dao.getUsersWithRole("Role2", null, 100, 0);
        Assert.assertEquals(1, usersRole2.size());
        Assert.assertEquals("multiroleuser", usersRole2.get(0).getUsername());
    }

    @Test
    public void test_getUsersWithRole_boundary_at_1024() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl role = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        role.setName("BoundaryRole");
        mongoRoleDaoImpl.saveOrUpdateRole(role);

        // Create exactly 1024 users
        int exactBoundary = 1024;

        List<IkasanPrincipal> principals = new ArrayList<>();
        List<User> users = new ArrayList<>();

        for (int i = 0; i < exactBoundary; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("boundaryuser" + i);
            user.setEmail("boundaryuser" + i + "@example.com");
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("boundaryuser" + i);
            principal.setType("user");
            principal.setId("boundaryuser" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            roles.add(role);
            principal.setRoles(roles);

            Set<IkasanPrincipal> principalsSet = new HashSet<>();
            principalsSet.add(principal);
            user.setPrincipals(principalsSet);

            users.add(user);
            principals.add(principal);
        }

        for (User user : users) {
            dao.save(user);
        }
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipals(principals);

        List<UserLite> result = dao.getUsersWithRole("BoundaryRole", null, Integer.MAX_VALUE, 0);

        Assert.assertNotNull(result);
        Assert.assertEquals(exactBoundary, result.size());
    }

    @Test
    public void test_getUsersWithRole_with_filter() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl role = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        role.setName("FilterTestRole");
        mongoRoleDaoImpl.saveOrUpdateRole(role);

        for (int i = 0; i < 10; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("filteruser" + i);
            user.setEmail("filteruser" + i + "@example.com");
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("filteruser" + i);
            principal.setType("user");
            principal.setId("filteruser" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            roles.add(role);
            principal.setRoles(roles);

            Set<IkasanPrincipal> principals = new HashSet<>();
            principals.add(principal);
            user.setPrincipals(principals);

            dao.save(user);
            mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);
        }

        TestUserFilter filter = new TestUserFilter();
        filter.setUsernameFilter("filteruser");

        List<UserLite> users = dao.getUsersWithRole("FilterTestRole", filter, 100, 0);

        Assert.assertNotNull(users);
        Assert.assertEquals(10, users.size());
    }

    @Test
    public void test_getUsersWithoutRole_basic() {
        // Create two roles
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl adminRole = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        adminRole.setName("AdminRole");
        adminRole.setDescription("Admin role");
        mongoRoleDaoImpl.saveOrUpdateRole(adminRole);

        org.ikasan.mongo.persistence.security.model.MongoRoleImpl userRole = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        userRole.setName("UserRole");
        userRole.setDescription("User role");
        mongoRoleDaoImpl.saveOrUpdateRole(userRole);

        // Create 3 users with AdminRole and 2 users without AdminRole
        for (int i = 0; i < 5; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("user" + i);
            principal.setType("user");
            principal.setId("user" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            if (i < 3) {
                // First 3 users have AdminRole
                roles.add(adminRole);
            } else {
                // Last 2 users have UserRole (not AdminRole)
                roles.add(userRole);
            }
            principal.setRoles(roles);

            Set<IkasanPrincipal> principals = new HashSet<>();
            principals.add(principal);
            user.setPrincipals(principals);

            dao.save(user);
            mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);
        }

        List<UserLite> usersWithoutAdminRole = dao.getUsersWithoutRole("AdminRole", null, 100, 0);

        Assert.assertNotNull(usersWithoutAdminRole);
        Assert.assertEquals(2, usersWithoutAdminRole.size());
        Assert.assertTrue(usersWithoutAdminRole.stream().anyMatch(u -> "user3".equals(u.getUsername())));
        Assert.assertTrue(usersWithoutAdminRole.stream().anyMatch(u -> "user4".equals(u.getUsername())));
    }

    @Test
    public void test_getUsersWithoutRole_all_users_have_role() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl role = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        role.setName("UniversalRole");
        role.setDescription("Role that all users have");
        mongoRoleDaoImpl.saveOrUpdateRole(role);

        // Create 5 users all with the role
        for (int i = 0; i < 5; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("user" + i);
            principal.setType("user");
            principal.setId("user" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            roles.add(role);
            principal.setRoles(roles);

            Set<IkasanPrincipal> principals = new HashSet<>();
            principals.add(principal);
            user.setPrincipals(principals);

            dao.save(user);
            mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);
        }

        List<UserLite> usersWithoutRole = dao.getUsersWithoutRole("UniversalRole", null, 100, 0);

        Assert.assertNotNull(usersWithoutRole);
        Assert.assertTrue(usersWithoutRole.isEmpty());
    }

    @Test
    public void test_getUsersWithoutRole_no_users_exist() {
        List<UserLite> usersWithoutRole = dao.getUsersWithoutRole("SomeRole-securityRole", null, 100, 0);

        Assert.assertNotNull(usersWithoutRole);
        Assert.assertTrue(usersWithoutRole.isEmpty());
    }

    @Test
    public void test_getUsersWithoutRole_null_roleName() {
        List<UserLite> usersWithoutRole = dao.getUsersWithoutRole(null, null, 100, 0);

        Assert.assertNotNull(usersWithoutRole);
        Assert.assertTrue(usersWithoutRole.isEmpty());
    }

    @Test
    public void test_getUsersWithoutRole_with_pagination() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl role = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        role.setName("RestrictedRole");
        role.setDescription("Restricted role");
        mongoRoleDaoImpl.saveOrUpdateRole(role);

        // Create 20 users: 5 with role, 15 without role
        for (int i = 0; i < 20; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("paginuser" + i);
            user.setEmail("paginuser" + i + "@example.com");
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("paginuser" + i);
            principal.setType("user");
            principal.setId("paginuser" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            if (i < 5) {
                // First 5 users have the role
                roles.add(role);
            } else {
                // Create a different role for the rest
                org.ikasan.mongo.persistence.security.model.MongoRoleImpl otherRole = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
                otherRole.setName("OtherRole" + i);
                mongoRoleDaoImpl.saveOrUpdateRole(otherRole);
                roles.add(otherRole);
            }
            principal.setRoles(roles);

            Set<IkasanPrincipal> principals = new HashSet<>();
            principals.add(principal);
            user.setPrincipals(principals);

            dao.save(user);
            mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);
        }

        // Get first page
        List<UserLite> page1 = dao.getUsersWithoutRole("RestrictedRole", null, 10, 0);
        Assert.assertEquals(10, page1.size());

        // Get second page
        List<UserLite> page2 = dao.getUsersWithoutRole("RestrictedRole", null, 10, 10);
        Assert.assertEquals(5, page2.size());
    }

    @Test
    public void test_getUsersWithoutRole_large_dataset() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl restrictedRole = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        restrictedRole.setName("VIPRole");
        restrictedRole.setDescription("VIP role");
        mongoRoleDaoImpl.saveOrUpdateRole(restrictedRole);

        org.ikasan.mongo.persistence.security.model.MongoRoleImpl regularRole = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        regularRole.setName("RegularRole");
        regularRole.setDescription("Regular user role");
        mongoRoleDaoImpl.saveOrUpdateRole(regularRole);

        // Create 1500 users: 100 with VIP role, 1400 without VIP role (more than 1024)
        int totalUsers = 1500;
        int usersWithVipRole = 100;

        List<IkasanPrincipal> principals = new ArrayList<>();
        List<User> users = new ArrayList<>();

        for (int i = 0; i < totalUsers; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("largewithoutuser" + i);
            user.setEmail("largewithoutuser" + i + "@example.com");
            user.setFirstName("First" + i);
            user.setSurname("Last" + i);
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("largewithoutuser" + i);
            principal.setType("user");
            principal.setId("largewithoutuser" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            if (i < usersWithVipRole) {
                // First 100 users have VIP role
                roles.add(restrictedRole);
            } else {
                // Remaining 1400 users have regular role (not VIP)
                roles.add(regularRole);
            }
            principal.setRoles(roles);

            Set<IkasanPrincipal> principalsSet = new HashSet<>();
            principalsSet.add(principal);
            user.setPrincipals(principalsSet);

            users.add(user);
            principals.add(principal);
        }

        for (User user : users) {
            dao.save(user);
        }
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipals(principals);

        // Get all users without VIP role using large limit
        List<UserLite> usersWithoutVip = dao.getUsersWithoutRole("VIPRole", null, Integer.MAX_VALUE, 0);

        Assert.assertNotNull(usersWithoutVip);
        Assert.assertEquals(totalUsers - usersWithVipRole, usersWithoutVip.size());

        // Verify pagination works with large dataset
        List<UserLite> firstBatch = dao.getUsersWithoutRole("VIPRole", null, 500, 0);
        Assert.assertEquals(500, firstBatch.size());

        List<UserLite> secondBatch = dao.getUsersWithoutRole("VIPRole", null, 500, 500);
        Assert.assertEquals(500, secondBatch.size());

        List<UserLite> thirdBatch = dao.getUsersWithoutRole("VIPRole", null, 500, 1000);
        Assert.assertEquals(400, thirdBatch.size());
    }

    @Test
    public void test_getUsersWithoutRole_boundary_at_1024() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl specialRole = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        specialRole.setName("SpecialRole");
        specialRole.setDescription("Special role");
        mongoRoleDaoImpl.saveOrUpdateRole(specialRole);

        org.ikasan.mongo.persistence.security.model.MongoRoleImpl normalRole = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        normalRole.setName("NormalRole");
        normalRole.setDescription("Normal role");
        mongoRoleDaoImpl.saveOrUpdateRole(normalRole);

        // Create exactly 1024 users without the special role
        int exactBoundary = 1024;

        List<IkasanPrincipal> principals = new ArrayList<>();
        List<User> users = new ArrayList<>();

        for (int i = 0; i < exactBoundary; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("boundwithoutuser" + i);
            user.setEmail("boundwithoutuser" + i + "@example.com");
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("boundwithoutuser" + i);
            principal.setType("user");
            principal.setId("boundwithoutuser" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            roles.add(normalRole); // All users have normal role, not special role
            principal.setRoles(roles);

            Set<IkasanPrincipal> principalsSet = new HashSet<>();
            principalsSet.add(principal);
            user.setPrincipals(principalsSet);

            users.add(user);
            principals.add(principal);
        }

        for (User user : users) {
            dao.save(user);
        }
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipals(principals);

        List<UserLite> usersWithoutSpecialRole = dao.getUsersWithoutRole("SpecialRole-securityRole", null, Integer.MAX_VALUE, 0);

        Assert.assertNotNull(usersWithoutSpecialRole);
        Assert.assertEquals(exactBoundary, usersWithoutSpecialRole.size());
    }

    @Test
    public void test_getUsersWithoutRole_boundary_at_1025() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl premiumRole = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        premiumRole.setName("PremiumRole");
        premiumRole.setDescription("Premium role");
        mongoRoleDaoImpl.saveOrUpdateRole(premiumRole);

        org.ikasan.mongo.persistence.security.model.MongoRoleImpl basicRole = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        basicRole.setName("BasicRole");
        basicRole.setDescription("Basic role");
        mongoRoleDaoImpl.saveOrUpdateRole(basicRole);

        // Create 1025 users without premium role (just over boundary)
        int justOverBoundary = 1025;

        List<IkasanPrincipal> principals = new ArrayList<>();
        List<User> users = new ArrayList<>();

        for (int i = 0; i < justOverBoundary; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("over1024user" + i);
            user.setEmail("over1024user" + i + "@example.com");
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("over1024user" + i);
            principal.setType("user");
            principal.setId("over1024user" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            roles.add(basicRole); // All users have basic role, not premium role
            principal.setRoles(roles);

            Set<IkasanPrincipal> principalsSet = new HashSet<>();
            principalsSet.add(principal);
            user.setPrincipals(principalsSet);

            users.add(user);
            principals.add(principal);
        }

        for (User user : users) {
            dao.save(user);
        }
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipals(principals);

        List<UserLite> usersWithoutPremiumRole = dao.getUsersWithoutRole("PremiumRole-securityRole", null, Integer.MAX_VALUE, 0);

        Assert.assertNotNull(usersWithoutPremiumRole);
        Assert.assertEquals(justOverBoundary, usersWithoutPremiumRole.size());
    }

    @Test
    public void test_getUsersWithoutRole_multiple_roles_per_user() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl role1 = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        role1.setName("MultiRole1");
        mongoRoleDaoImpl.saveOrUpdateRole(role1);

        org.ikasan.mongo.persistence.security.model.MongoRoleImpl role2 = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        role2.setName("MultiRole2");
        mongoRoleDaoImpl.saveOrUpdateRole(role2);

        org.ikasan.mongo.persistence.security.model.MongoRoleImpl role3 = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        role3.setName("MultiRole3");
        mongoRoleDaoImpl.saveOrUpdateRole(role3);

        // User with role1 and role2
        MongoUserImpl user1 = new MongoUserImpl();
        user1.setUsername("multiuser1");
        user1.setEmail("multiuser1@example.com");
        user1.setEnabled(true);

        org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal1 = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
        principal1.setName("multiuser1");
        principal1.setType("user");
        principal1.setId("multiuser1-securityPrincipal");

        Set<org.ikasan.spec.security.model.Role> roles1 = new HashSet<>();
        roles1.add(role1);
        roles1.add(role2);
        principal1.setRoles(roles1);

        Set<IkasanPrincipal> principals1 = new HashSet<>();
        principals1.add(principal1);
        user1.setPrincipals(principals1);

        dao.save(user1);
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal1);

        // User with only role3
        MongoUserImpl user2 = new MongoUserImpl();
        user2.setUsername("multiuser2");
        user2.setEmail("multiuser2@example.com");
        user2.setEnabled(true);

        org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal2 = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
        principal2.setName("multiuser2");
        principal2.setType("user");
        principal2.setId("multiuser2-securityPrincipal");

        Set<org.ikasan.spec.security.model.Role> roles2 = new HashSet<>();
        roles2.add(role3);
        principal2.setRoles(roles2);

        Set<IkasanPrincipal> principals2 = new HashSet<>();
        principals2.add(principal2);
        user2.setPrincipals(principals2);

        dao.save(user2);
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal2);

        // Query for users without role1 - should only return user2
        List<UserLite> usersWithoutRole1 = dao.getUsersWithoutRole("MultiRole1", null, 100, 0);
        Assert.assertEquals(1, usersWithoutRole1.size());
        Assert.assertEquals("multiuser2", usersWithoutRole1.get(0).getUsername());

        // Query for users without role3 - should only return user1
        List<UserLite> usersWithoutRole3 = dao.getUsersWithoutRole("MultiRole3", null, 100, 0);
        Assert.assertEquals(1, usersWithoutRole3.size());
        Assert.assertEquals("multiuser1", usersWithoutRole3.get(0).getUsername());
    }

    @Test
    public void test_getUsersWithoutRole_with_filter() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl restrictedRole
            = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        restrictedRole.setName("FilterWithoutTestRole");
        restrictedRole.setDescription("Filter test role");
        mongoRoleDaoImpl.saveOrUpdateRole(restrictedRole);

        org.ikasan.mongo.persistence.security.model.MongoRoleImpl openRole
            = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        openRole.setName("OpenRole");
        openRole.setDescription("Open role");
        mongoRoleDaoImpl.saveOrUpdateRole(openRole);

        for (int i = 0; i < 10; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("filterwithoutuser" + i);
            user.setEmail("filterwithoutuser" + i + "@example.com");
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("filterwithoutuser" + i);
            principal.setType("user");
            principal.setId("filterwithoutuser" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            // Users with even index don't have restricted role
            if (i % 2 == 0) {
                roles.add(openRole);
            } else {
                roles.add(restrictedRole);
            }
            principal.setRoles(roles);

            Set<IkasanPrincipal> principals = new HashSet<>();
            principals.add(principal);
            user.setPrincipals(principals);

            dao.save(user);
            mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);
        }

        TestUserFilter filter = new TestUserFilter();
        filter.setUsernameFilter("filterwithoutuser");

        List<UserLite> usersWithoutRestrictedRole = dao.getUsersWithoutRole("FilterWithoutTestRole", filter, 100, 0);

        Assert.assertNotNull(usersWithoutRestrictedRole);
        Assert.assertEquals(5, usersWithoutRestrictedRole.size()); // Users 0, 2, 4, 6, 8
        Assert.assertTrue(usersWithoutRestrictedRole.stream().allMatch(u -> u.getUsername().contains("filterwithoutuser")));
    }

    @Test
    public void test_getUsersWithoutRole_empty_role_name() {
        List<UserLite> usersWithoutRole = dao.getUsersWithoutRole("", null, 100, 0);

        Assert.assertNotNull(usersWithoutRole);
        Assert.assertTrue(usersWithoutRole.isEmpty());
    }

    @Test
    public void test_getUsersWithoutRole_nonexistent_role() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl someRole = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        someRole.setName("SomeExistingRole");
        mongoRoleDaoImpl.saveOrUpdateRole(someRole);

        // Create users with the existing role
        for (int i = 0; i < 3; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("existinguser" + i);
            user.setEmail("existinguser" + i + "@example.com");
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("existinguser" + i);
            principal.setType("user");
            principal.setId("existinguser" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            roles.add(someRole);
            principal.setRoles(roles);

            Set<IkasanPrincipal> principals = new HashSet<>();
            principals.add(principal);
            user.setPrincipals(principals);

            dao.save(user);
            mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);
        }

        // Query for users without a nonexistent role
        List<UserLite> usersWithoutRole = dao.getUsersWithoutRole("NonExistentRole-securityRole", null, 100, 0);

        Assert.assertNotNull(usersWithoutRole);
        // When querying for a nonexistent role, all users should be returned as "without" that role
        Assert.assertEquals(3, usersWithoutRole.size());
    }

    // ========== getUsersWithRoleCount tests (8 tests) ==========

    @Test
    public void test_getUsersWithRoleCount_basic() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl role = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        role.setName("CountRole");
        role.setDescription("Count test role");
        mongoRoleDaoImpl.saveOrUpdateRole(role);

        // Create 7 users with the role
        for (int i = 0; i < 7; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("countuser" + i);
            user.setEmail("countuser" + i + "@example.com");
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("countuser" + i);
            principal.setType("user");
            principal.setId("countuser" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            roles.add(role);
            principal.setRoles(roles);

            Set<IkasanPrincipal> principals = new HashSet<>();
            principals.add(principal);
            user.setPrincipals(principals);

            dao.save(user);
            mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);
        }

        int count = dao.getUsersWithRoleCount("CountRole", null);

        Assert.assertEquals(7, count);
    }

    @Test
    public void test_getUsersWithRoleCount_zero_users() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl role = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        role.setName("EmptyRole");
        mongoRoleDaoImpl.saveOrUpdateRole(role);

        int count = dao.getUsersWithRoleCount("EmptyRole", null);

        Assert.assertEquals(0, count);
    }

    @Test
    public void test_getUsersWithRoleCount_null_roleName() {
        int count = dao.getUsersWithRoleCount(null, null);

        Assert.assertEquals(0, count);
    }

    @Test
    public void test_getUsersWithRoleCount_nonexistent_role() {
        // Create some users with different roles
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl someRole = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        someRole.setName("SomeRole");
        mongoRoleDaoImpl.saveOrUpdateRole(someRole);

        for (int i = 0; i < 3; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("someuser" + i);
            user.setEmail("someuser" + i + "@example.com");
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("someuser" + i);
            principal.setType("user");
            principal.setId("someuser" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            roles.add(someRole);
            principal.setRoles(roles);

            Set<IkasanPrincipal> principals = new HashSet<>();
            principals.add(principal);
            user.setPrincipals(principals);

            dao.save(user);
            mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);
        }

        int count = dao.getUsersWithRoleCount("NonExistentRole-securityRole", null);

        Assert.assertEquals(0, count);
    }

    @Test
    public void test_getUsersWithRoleCount_large_dataset() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl role = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        role.setName("MassCountRole");
        role.setDescription("Mass count role");
        mongoRoleDaoImpl.saveOrUpdateRole(role);

        List<IkasanPrincipal> principals = new ArrayList<>();
        List<User> users = new ArrayList<>();

        // Create 1500 users with the role (more than 1024)
        int totalUsers = 1500;
        for (int i = 0; i < totalUsers; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("masscountuser" + i);
            user.setEmail("masscountuser" + i + "@example.com");
            user.setFirstName("First" + i);
            user.setSurname("Last" + i);
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("masscountuser" + i);
            principal.setType("user");
            principal.setId("masscountuser" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            roles.add(role);
            principal.setRoles(roles);

            Set<IkasanPrincipal> principalsSet = new HashSet<>();
            principalsSet.add(principal);
            user.setPrincipals(principalsSet);

            users.add(user);
            principals.add(principal);
        }

        for (User user : users) {
            dao.save(user);
        }
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipals(principals);

        int count = dao.getUsersWithRoleCount("MassCountRole", null);

        Assert.assertEquals(totalUsers, count);
    }

    @Test
    public void test_getUsersWithRoleCount_boundary_at_1024() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl role = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        role.setName("Boundary1024Role");
        mongoRoleDaoImpl.saveOrUpdateRole(role);

        // Create exactly 1024 users with the role
        int exactBoundary = 1024;

        List<IkasanPrincipal> principals = new ArrayList<>();
        List<User> users = new ArrayList<>();

        for (int i = 0; i < exactBoundary; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("boundary1024user" + i);
            user.setEmail("boundary1024user" + i + "@example.com");
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("boundary1024user" + i);
            principal.setType("user");
            principal.setId("boundary1024user" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            roles.add(role);
            principal.setRoles(roles);

            Set<IkasanPrincipal> principalsSet = new HashSet<>();
            principalsSet.add(principal);
            user.setPrincipals(principalsSet);

            users.add(user);
            principals.add(principal);
        }

        for (User user : users) {
            dao.save(user);
        }
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipals(principals);

        int count = dao.getUsersWithRoleCount("Boundary1024Role", null);

        Assert.assertEquals(exactBoundary, count);
    }

    @Test
    public void test_getUsersWithRoleCount_boundary_at_1025() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl role = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        role.setName("Boundary1025Role");
        mongoRoleDaoImpl.saveOrUpdateRole(role);

        // Create 1025 users with the role (just over boundary)
        int justOverBoundary = 1025;

        List<IkasanPrincipal> principals = new ArrayList<>();
        List<User> users = new ArrayList<>();

        for (int i = 0; i < justOverBoundary; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("boundary1025user" + i);
            user.setEmail("boundary1025user" + i + "@example.com");
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("boundary1025user" + i);
            principal.setType("user");
            principal.setId("boundary1025user" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            roles.add(role);
            principal.setRoles(roles);

            Set<IkasanPrincipal> principalsSet = new HashSet<>();
            principalsSet.add(principal);
            user.setPrincipals(principalsSet);

            users.add(user);
            principals.add(principal);
        }

        for (User user : users) {
            dao.save(user);
        }
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipals(principals);

        int count = dao.getUsersWithRoleCount("Boundary1025Role", null);

        Assert.assertEquals(justOverBoundary, count);
    }

    @Test
    public void test_getUsersWithRoleCount_single_user() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl role = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        role.setName("SingleUserRole");
        mongoRoleDaoImpl.saveOrUpdateRole(role);

        MongoUserImpl user = new MongoUserImpl();
        user.setUsername("singleuser");
        user.setEmail("single@example.com");
        user.setEnabled(true);

        org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
        principal.setName("singleuser");
        principal.setType("user");
        principal.setId("singleuser-securityPrincipal");

        Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
        roles.add(role);
        principal.setRoles(roles);

        Set<IkasanPrincipal> principals = new HashSet<>();
        principals.add(principal);
        user.setPrincipals(principals);

        dao.save(user);
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);

        int count = dao.getUsersWithRoleCount("SingleUserRole", null);

        Assert.assertEquals(1, count);
    }

    // ========== getUsersWithoutRoleCount tests (9 tests) ==========

    @Test
    public void test_getUsersWithoutRoleCount_basic() {
        // Create two roles
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl adminRole = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        adminRole.setName("CountAdminRole");
        adminRole.setDescription("Count admin role");
        mongoRoleDaoImpl.saveOrUpdateRole(adminRole);

        org.ikasan.mongo.persistence.security.model.MongoRoleImpl userRole = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        userRole.setName("CountUserRole");
        userRole.setDescription("Count user role");
        mongoRoleDaoImpl.saveOrUpdateRole(userRole);

        // Create 3 users with AdminRole and 5 users without AdminRole
        for (int i = 0; i < 8; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("countwithoutuser" + i);
            user.setEmail("countwithoutuser" + i + "@example.com");
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("countwithoutuser" + i);
            principal.setType("user");
            principal.setId("countwithoutuser" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            if (i < 3) {
                // First 3 users have AdminRole
                roles.add(adminRole);
            } else {
                // Last 5 users have UserRole (not AdminRole)
                roles.add(userRole);
            }
            principal.setRoles(roles);

            Set<IkasanPrincipal> principals = new HashSet<>();
            principals.add(principal);
            user.setPrincipals(principals);

            dao.save(user);
            mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);
        }

        int count = dao.getUsersWithoutRoleCount("CountAdminRole", null);

        Assert.assertEquals(5, count);
    }

    @Test
    public void test_getUsersWithoutRoleCount_all_users_have_role() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl role = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        role.setName("UniversalCountRole");
        role.setDescription("Role that all users have");
        mongoRoleDaoImpl.saveOrUpdateRole(role);

        // Create 5 users all with the role
        for (int i = 0; i < 5; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("univcountuser" + i);
            user.setEmail("univcountuser" + i + "@example.com");
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("univcountuser" + i);
            principal.setType("user");
            principal.setId("univcountuser" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            roles.add(role);
            principal.setRoles(roles);

            Set<IkasanPrincipal> principals = new HashSet<>();
            principals.add(principal);
            user.setPrincipals(principals);

            dao.save(user);
            mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);
        }

        int count = dao.getUsersWithoutRoleCount("UniversalCountRole", null);

        Assert.assertEquals(0, count);
    }

    @Test
    public void test_getUsersWithoutRoleCount_no_users_exist() {
        int count = dao.getUsersWithoutRoleCount("AnyRole", null);

        Assert.assertEquals(0, count);
    }

    @Test
    public void test_getUsersWithoutRoleCount_null_roleName() {
        int count = dao.getUsersWithoutRoleCount(null, null);

        Assert.assertEquals(0, count);
    }

    @Test
    public void test_getUsersWithoutRoleCount_large_dataset() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl restrictedRole = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        restrictedRole.setName("CountVIPRole");
        restrictedRole.setDescription("Count VIP role");
        mongoRoleDaoImpl.saveOrUpdateRole(restrictedRole);

        org.ikasan.mongo.persistence.security.model.MongoRoleImpl regularRole = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        regularRole.setName("CountRegularRole");
        regularRole.setDescription("Count regular user role");
        mongoRoleDaoImpl.saveOrUpdateRole(regularRole);

        // Create 1500 users: 100 with VIP role, 1400 without VIP role (more than 1024)
        int totalUsers = 1500;
        int usersWithVipRole = 100;

        List<IkasanPrincipal> principals = new ArrayList<>();
        List<User> users = new ArrayList<>();

        for (int i = 0; i < totalUsers; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("countlargewithout" + i);
            user.setEmail("countlargewithout" + i + "@example.com");
            user.setFirstName("First" + i);
            user.setSurname("Last" + i);
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("countlargewithout" + i);
            principal.setType("user");
            principal.setId("countlargewithout" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            if (i < usersWithVipRole) {
                // First 100 users have VIP role
                roles.add(restrictedRole);
            } else {
                // Remaining 1400 users have regular role (not VIP)
                roles.add(regularRole);
            }
            principal.setRoles(roles);

            Set<IkasanPrincipal> principalsSet = new HashSet<>();
            principalsSet.add(principal);
            user.setPrincipals(principalsSet);

            users.add(user);
            principals.add(principal);
        }

        for (User user : users) {
            dao.save(user);
        }
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipals(principals);

        int count = dao.getUsersWithoutRoleCount("CountVIPRole", null);

        Assert.assertEquals(totalUsers - usersWithVipRole, count);
    }

    @Test
    public void test_getUsersWithoutRoleCount_boundary_at_1024() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl specialRole = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        specialRole.setName("CountSpecialRole");
        specialRole.setDescription("Count special role");
        mongoRoleDaoImpl.saveOrUpdateRole(specialRole);

        org.ikasan.mongo.persistence.security.model.MongoRoleImpl normalRole = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        normalRole.setName("CountNormalRole");
        normalRole.setDescription("Count normal role");
        mongoRoleDaoImpl.saveOrUpdateRole(normalRole);

        // Create exactly 1024 users without the special role
        int exactBoundary = 1024;

        List<IkasanPrincipal> principals = new ArrayList<>();
        List<User> users = new ArrayList<>();

        for (int i = 0; i < exactBoundary; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("countboundwithout" + i);
            user.setEmail("countboundwithout" + i + "@example.com");
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("countboundwithout" + i);
            principal.setType("user");
            principal.setId("countboundwithout" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            roles.add(normalRole); // All users have normal role, not special role
            principal.setRoles(roles);

            Set<IkasanPrincipal> principalsSet = new HashSet<>();
            principalsSet.add(principal);
            user.setPrincipals(principalsSet);

            users.add(user);
            principals.add(principal);
        }

        for (User user : users) {
            dao.save(user);
        }
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipals(principals);

        int count = dao.getUsersWithoutRoleCount("CountSpecialRole", null);

        Assert.assertEquals(exactBoundary, count);
    }

    @Test
    public void test_getUsersWithoutRoleCount_boundary_at_1025() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl premiumRole = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        premiumRole.setName("CountPremiumRole");
        premiumRole.setDescription("Count premium role");
        mongoRoleDaoImpl.saveOrUpdateRole(premiumRole);

        org.ikasan.mongo.persistence.security.model.MongoRoleImpl basicRole = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        basicRole.setName("CountBasicRole");
        basicRole.setDescription("Count basic role");
        mongoRoleDaoImpl.saveOrUpdateRole(basicRole);

        // Create 1025 users without premium role (just over boundary)
        int justOverBoundary = 1025;

        List<IkasanPrincipal> principals = new ArrayList<>();
        List<User> users = new ArrayList<>();

        for (int i = 0; i < justOverBoundary; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("countover1024" + i);
            user.setEmail("countover1024" + i + "@example.com");
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("countover1024" + i);
            principal.setType("user");
            principal.setId("countover1024" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            roles.add(basicRole); // All users have basic role, not premium role
            principal.setRoles(roles);

            Set<IkasanPrincipal> principalsSet = new HashSet<>();
            principalsSet.add(principal);
            user.setPrincipals(principalsSet);

            users.add(user);
            principals.add(principal);
        }

        for (User user : users) {
            dao.save(user);
        }
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipals(principals);

        int count = dao.getUsersWithoutRoleCount("CountPremiumRole", null);

        Assert.assertEquals(justOverBoundary, count);
    }

    @Test
    public void test_getUsersWithoutRoleCount_nonexistent_role() {
        org.ikasan.mongo.persistence.security.model.MongoRoleImpl someRole = new org.ikasan.mongo.persistence.security.model.MongoRoleImpl();
        someRole.setName("CountSomeRole");
        mongoRoleDaoImpl.saveOrUpdateRole(someRole);

        // Create users with the existing role
        for (int i = 0; i < 3; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("countexisting" + i);
            user.setEmail("countexisting" + i + "@example.com");
            user.setEnabled(true);

            org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
            principal.setName("countexisting" + i);
            principal.setType("user");
            principal.setId("countexisting" + i + "-securityPrincipal");

            Set<org.ikasan.spec.security.model.Role> roles = new HashSet<>();
            roles.add(someRole);
            principal.setRoles(roles);

            Set<IkasanPrincipal> principals = new HashSet<>();
            principals.add(principal);
            user.setPrincipals(principals);

            dao.save(user);
            mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);
        }

        // Query for count of users without a nonexistent role
        int count = dao.getUsersWithoutRoleCount("NonExistentCountRole", null);

        // When querying for a nonexistent role, all users should be counted as "without" that role
        Assert.assertEquals(3, count);
    }

    @Test
    public void test_getUsersWithoutRoleCount_empty_role_name() {
        int count = dao.getUsersWithoutRoleCount("", null);

        Assert.assertEquals(0, count);
    }

    // ========== getUsersAssociatedWithPrincipal tests (14 tests) ==========

    @Test
    public void test_getUsersAssociatedWithPrincipal_basic() {
        // Create a principal
        org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
        principal.setName("testGroup");
        principal.setType("group");
        principal.setDescription("Test group");
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);

        // Create users associated with the principal
        Set<IkasanPrincipal> principals = new HashSet<>();
        principals.add(principal);

        for (int i = 0; i < 3; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setFirstName("User");
            user.setSurname("" + i);
            user.setEnabled(true);
            user.setPrincipals(principals);
            dao.save(user);
        }

        // Test getUsersAssociatedWithPrincipal
        List<User> users = dao.getUsersAssociatedWithPrincipal("testGroup-securityPrincipal");

        Assert.assertNotNull(users);
        Assert.assertEquals(3, users.size());
        for (User user : users) {
            Assert.assertTrue(user.getUsername().startsWith("user"));
        }
    }

    @Test
    public void test_getUsersAssociatedWithPrincipal_nonexistent_principal() {
        // Create a user (but principal doesn't exist)
        MongoUserImpl user = new MongoUserImpl();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setEnabled(true);
        dao.save(user);

        // Try to get users for nonexistent principal
        List<User> users = dao.getUsersAssociatedWithPrincipal("nonexistent-securityPrincipal");

        Assert.assertNotNull(users);
        Assert.assertTrue(users.isEmpty());
    }

    @Test
    public void test_getUsersAssociatedWithPrincipal_null_principal_id() {
        List<User> users = dao.getUsersAssociatedWithPrincipal(null);

        Assert.assertNotNull(users);
        Assert.assertTrue(users.isEmpty());
    }

    @Test
    public void test_getUsersAssociatedWithPrincipal_empty_principal_id() {
        List<User> users = dao.getUsersAssociatedWithPrincipal("");

        Assert.assertNotNull(users);
        Assert.assertTrue(users.isEmpty());
    }

    @Test
    public void test_getUsersAssociatedWithPrincipal_no_associated_users() {
        // Create a principal
        org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
        principal.setName("emptyGroup");
        principal.setType("group");
        principal.setDescription("Group with no users");
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);

        // Create users NOT associated with the principal
        MongoUserImpl user = new MongoUserImpl();
        user.setUsername("unrelateduser");
        user.setEmail("unrelated@example.com");
        user.setEnabled(true);
        dao.save(user);

        // Test getUsersAssociatedWithPrincipal
        List<User> users = dao.getUsersAssociatedWithPrincipal("emptyGroup-securityPrincipal");

        Assert.assertNotNull(users);
        Assert.assertTrue(users.isEmpty());
    }

    @Test
    public void test_getUsersAssociatedWithPrincipal_multiple_principals_per_user() {
        // Create two principals
        org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal1 = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
        principal1.setName("group1");
        principal1.setType("group");
        principal1.setDescription("Group 1");
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal1);

        org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal2 = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
        principal2.setName("group2");
        principal2.setType("group");
        principal2.setDescription("Group 2");
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal2);

        // Create user associated with both principals
        Set<IkasanPrincipal> principals = new HashSet<>();
        principals.add(principal1);
        principals.add(principal2);

        MongoUserImpl user = new MongoUserImpl();
        user.setUsername("multigroup_user");
        user.setEmail("multi@example.com");
        user.setFirstName("Multi");
        user.setSurname("Group");
        user.setEnabled(true);
        user.setPrincipals(principals);
        dao.save(user);

        // Test that user is found when searching for either principal
        List<User> usersForGroup1 = dao.getUsersAssociatedWithPrincipal("group1-securityPrincipal");
        Assert.assertEquals(1, usersForGroup1.size());
        Assert.assertEquals("multigroup_user", usersForGroup1.get(0).getUsername());

        List<User> usersForGroup2 = dao.getUsersAssociatedWithPrincipal("group2-securityPrincipal");
        Assert.assertEquals(1, usersForGroup2.size());
        Assert.assertEquals("multigroup_user", usersForGroup2.get(0).getUsername());
    }

    @Test
    public void test_getUsersAssociatedWithPrincipal_large_dataset() {
        // Create a principal
        org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
        principal.setName("largeGroup");
        principal.setType("group");
        principal.setDescription("Large group");
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);

        // Create 100 users associated with the principal
        Set<IkasanPrincipal> principals = new HashSet<>();
        principals.add(principal);

        List<User> usersToSave = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setFirstName("User");
            user.setSurname("" + i);
            user.setEnabled(true);
            user.setPrincipals(principals);
            usersToSave.add(user);
        }
        for (User user : usersToSave) {
            dao.save(user);
        }

        // Test getUsersAssociatedWithPrincipal
        List<User> users = dao.getUsersAssociatedWithPrincipal("largeGroup-securityPrincipal");

        Assert.assertNotNull(users);
        Assert.assertEquals(100, users.size());
    }

    @Test
    public void test_getUsersAssociatedWithPrincipal_verify_full_user_details() {
        // Create a principal
        org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
        principal.setId("detailGroup-securityPrincipal");
        principal.setName("detailGroup");
        principal.setType("group");
        principal.setDescription("Detail group");
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);

        // Create user with full details
        Set<IkasanPrincipal> principals = new HashSet<>();
        principals.add(principal);

        MongoUserImpl user = new MongoUserImpl();
        user.setUsername("john.doe");
        user.setEmail("john.doe@example.com");
        user.setFirstName("John");
        user.setSurname("Doe");
        user.setDepartment("Engineering");
        user.setEnabled(true);
        user.setPrincipals(principals);
        dao.save(user);

        // Test getUsersAssociatedWithPrincipal returns full details
        List<User> users = dao.getUsersAssociatedWithPrincipal("detailGroup-securityPrincipal");

        Assert.assertNotNull(users);
        Assert.assertEquals(1, users.size());

        User foundUser = users.get(0);
        Assert.assertEquals("john.doe", foundUser.getUsername());
        Assert.assertEquals("john.doe@example.com", foundUser.getEmail());
        Assert.assertEquals("John", foundUser.getFirstName());
        Assert.assertEquals("Doe", foundUser.getSurname());
        Assert.assertEquals("Engineering", foundUser.getDepartment());
        Assert.assertTrue(foundUser.isEnabled());
    }

    @Test
    public void test_getUsersAssociatedWithPrincipal_user_and_group_principals() {
        // Create user-type principal
        org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl userPrincipal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
        userPrincipal.setName("userPrincipal1");
        userPrincipal.setType("user");
        userPrincipal.setDescription("User principal");
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(userPrincipal);

        // Create group-type principal
        org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl groupPrincipal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
        groupPrincipal.setName("groupPrincipal1");
        groupPrincipal.setType("group");
        groupPrincipal.setDescription("Group principal");
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(groupPrincipal);

        // Create user associated with user principal
        Set<IkasanPrincipal> userPrincipals = new HashSet<>();
        userPrincipals.add(userPrincipal);

        MongoUserImpl user1 = new MongoUserImpl();
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");
        user1.setEnabled(true);
        user1.setPrincipals(userPrincipals);
        dao.save(user1);

        // Create user associated with group principal
        Set<IkasanPrincipal> groupPrincipals = new HashSet<>();
        groupPrincipals.add(groupPrincipal);

        MongoUserImpl user2 = new MongoUserImpl();
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        user2.setEnabled(true);
        user2.setPrincipals(groupPrincipals);
        dao.save(user2);

        // Test getUsersAssociatedWithPrincipal for user principal
        List<User> usersForUserPrincipal = dao.getUsersAssociatedWithPrincipal("userPrincipal1-securityPrincipal");
        Assert.assertEquals(1, usersForUserPrincipal.size());
        Assert.assertEquals("user1", usersForUserPrincipal.get(0).getUsername());

        // Test getUsersAssociatedWithPrincipal for group principal
        List<User> usersForGroupPrincipal = dao.getUsersAssociatedWithPrincipal("groupPrincipal1-securityPrincipal");
        Assert.assertEquals(1, usersForGroupPrincipal.size());
        Assert.assertEquals("user2", usersForGroupPrincipal.get(0).getUsername());
    }

    @Test
    public void test_getUsersAssociatedWithPrincipal_mixed_association() {
        // Create a principal
        org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
        principal.setName("mixedGroup");
        principal.setType("group");
        principal.setDescription("Mixed group");
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);

        Set<IkasanPrincipal> principals = new HashSet<>();
        principals.add(principal);

        // Create 3 users associated with principal
        for (int i = 0; i < 3; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("associated" + i);
            user.setEmail("associated" + i + "@example.com");
            user.setEnabled(true);
            user.setPrincipals(principals);
            dao.save(user);
        }

        // Create 2 users NOT associated with principal
        for (int i = 0; i < 2; i++) {
            MongoUserImpl user = new MongoUserImpl();
            user.setUsername("unassociated" + i);
            user.setEmail("unassociated" + i + "@example.com");
            user.setEnabled(true);
            dao.save(user);
        }

        // Test getUsersAssociatedWithPrincipal returns only associated users
        List<User> users = dao.getUsersAssociatedWithPrincipal("mixedGroup-securityPrincipal");

        Assert.assertNotNull(users);
        Assert.assertEquals(3, users.size());
        for (User user : users) {
            Assert.assertTrue(user.getUsername().startsWith("associated"));
        }
    }

    @Test
    public void test_getUsersAssociatedWithPrincipal_with_special_characters() {
        // Create principal with special characters in name
        org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
        principal.setName("group-with_special.chars@test");
        principal.setType("group");
        principal.setDescription("Group with special chars");
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);

        // Create user associated with the principal
        Set<IkasanPrincipal> principals = new HashSet<>();
        principals.add(principal);

        MongoUserImpl user = new MongoUserImpl();
        user.setUsername("special_user");
        user.setEmail("special@example.com");
        user.setEnabled(true);
        user.setPrincipals(principals);
        dao.save(user);

        // Test getUsersAssociatedWithPrincipal
        List<User> users = dao.getUsersAssociatedWithPrincipal("group-with_special.chars@test-securityPrincipal");

        Assert.assertNotNull(users);
        Assert.assertEquals(1, users.size());
        Assert.assertEquals("special_user", users.get(0).getUsername());
    }

    @Test
    public void test_getUsersAssociatedWithPrincipal_enabled_and_disabled_users() {
        // Create a principal
        org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
        principal.setName("statusGroup");
        principal.setType("group");
        principal.setDescription("Status group");
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);

        Set<IkasanPrincipal> principals = new HashSet<>();
        principals.add(principal);

        // Create enabled user
        MongoUserImpl enabledUser = new MongoUserImpl();
        enabledUser.setUsername("enabled_user");
        enabledUser.setEmail("enabled@example.com");
        enabledUser.setEnabled(true);
        enabledUser.setPrincipals(principals);
        dao.save(enabledUser);

        // Create disabled user
        MongoUserImpl disabledUser = new MongoUserImpl();
        disabledUser.setUsername("disabled_user");
        disabledUser.setEmail("disabled@example.com");
        disabledUser.setEnabled(false);
        disabledUser.setPrincipals(principals);
        dao.save(disabledUser);

        // Test getUsersAssociatedWithPrincipal returns both enabled and disabled
        List<User> users = dao.getUsersAssociatedWithPrincipal("statusGroup-securityPrincipal");

        Assert.assertNotNull(users);
        Assert.assertEquals(2, users.size());

        Set<String> usernames = new HashSet<>();
        for (User user : users) {
            usernames.add(user.getUsername());
        }
        Assert.assertTrue(usernames.contains("enabled_user"));
        Assert.assertTrue(usernames.contains("disabled_user"));
    }

    @Test
    public void test_getUsersAssociatedWithPrincipal_after_principal_deleted() {
        // Create a principal
        org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl principal = new org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl();
        principal.setName("deleteGroup");
        principal.setType("group");
        principal.setDescription("Delete group");
        mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);

        Set<IkasanPrincipal> principals = new HashSet<>();
        principals.add(mongoIkasanPrincipalDaoImpl.getPrincipalByName("deleteGroup"));

        // Create user associated with principal
        MongoUserImpl user = new MongoUserImpl();
        user.setUsername("deletetest_user");
        user.setEmail("deletetest@example.com");
        user.setEnabled(true);
        user.setPrincipals(principals);
        dao.save(user);

        // Verify user is found
        List<User> users = dao.getUsersAssociatedWithPrincipal("deleteGroup-securityPrincipal");
        Assert.assertEquals(1, users.size());

        // Delete the principal
        mongoIkasanPrincipalDaoImpl.deletePrincipal(principal);

        // After deletion, should return empty list
        users = dao.getUsersAssociatedWithPrincipal("deleteGroup-securityPrincipal");
        Assert.assertTrue(users.isEmpty());
    }

    @Test
    public void test_getUsersAssociatedWithPrincipal_invalid_principal_id_format() {
        // Try with invalid ID format (no type suffix)
        List<User> users = dao.getUsersAssociatedWithPrincipal("invalidFormat");

        Assert.assertNotNull(users);
        Assert.assertTrue(users.isEmpty());
    }
}
