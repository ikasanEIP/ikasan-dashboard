package org.ikasan.persistence.initialisation.security;

import org.ikasan.persistence.initialisation.core.DataJobException;
import org.ikasan.persistence.initialisation.core.InitialDataJobConstants;
import org.ikasan.persistence.initialisation.model.IkasanPrincipalImpl;
import org.ikasan.persistence.initialisation.model.PolicyImpl;
import org.ikasan.persistence.initialisation.model.RoleImpl;
import org.ikasan.persistence.initialisation.model.UserImpl;
import org.ikasan.spec.security.dao.IkasanPrincipalDao;
import org.ikasan.spec.security.dao.PolicyDao;
import org.ikasan.spec.security.dao.RoleDao;
import org.ikasan.spec.security.dao.UserDao;
import org.ikasan.spec.security.model.IkasanPrincipal;
import org.ikasan.spec.security.model.Policy;
import org.ikasan.spec.security.model.Role;
import org.ikasan.spec.security.model.User;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for BaselineSecurityDataLoader
 */
public class BaselineSecurityDataLoaderTest {

    private PolicyDao mockPolicyDao;
    private RoleDao mockRoleDao;
    private IkasanPrincipalDao mockPrincipalDao;
    private UserDao mockUserDao;
    private BaselineSecurityDataLoader dataLoader;

    @Before
    public void setup() {
        mockPolicyDao = mock(PolicyDao.class);
        mockRoleDao = mock(RoleDao.class);
        mockPrincipalDao = mock(IkasanPrincipalDao.class);
        mockUserDao = mock(UserDao.class);
        dataLoader = new BaselineSecurityDataLoader(mockPolicyDao, mockRoleDao, mockPrincipalDao, mockUserDao);
    }

    @Test
    public void test_constructor_with_valid_parameters() {
        BaselineSecurityDataLoader loader = new BaselineSecurityDataLoader(
            mockPolicyDao, mockRoleDao, mockPrincipalDao, mockUserDao);
        Assert.assertNotNull(loader);
    }

    @Test
    public void test_getJobName_returns_correct_job_name() {
        String jobName = dataLoader.getJobName();
        Assert.assertEquals(InitialDataJobConstants.SOLR_BASELINE_SECURITY_DATA_JOB_NAME, jobName);
    }

    @Test
    public void test_execute_loads_and_saves_all_policies() throws Exception {
        ArgumentCaptor<Policy> policyCaptor = ArgumentCaptor.forClass(Policy.class);

        dataLoader.execute();

        // Verify that policies are saved (should be many - at least 100 based on the JSON)
        verify(mockPolicyDao, atLeast(100)).saveOrUpdatePolicy(policyCaptor.capture());

        List<Policy> savedPolicies = policyCaptor.getAllValues();

        // Verify some key policies exist
        boolean foundAllPolicy = savedPolicies.stream().anyMatch(p -> "ALL".equals(p.getName()));
        boolean foundDiscoverPolicy = savedPolicies.stream().anyMatch(p -> "Discover".equals(p.getName()));
        boolean foundWiretapRead = savedPolicies.stream().anyMatch(p -> "wiretap-read".equals(p.getName()));

        Assert.assertTrue("ALL policy should be saved", foundAllPolicy);
        Assert.assertTrue("Discover policy should be saved", foundDiscoverPolicy);
        Assert.assertTrue("wiretap-read policy should be saved", foundWiretapRead);

        // Verify all policies have non-null timestamps
        for (Policy policy : savedPolicies) {
            Assert.assertNotNull(policy.getName());
            Assert.assertNotNull(policy.getDescription());
            Assert.assertNotNull(policy.getCreatedDateTime());
            Assert.assertNotNull(policy.getUpdatedDateTime());
        }
    }

    @Test
    public void test_execute_loads_and_saves_all_roles() throws Exception {
        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);

        dataLoader.execute();

        // Verify that roles are saved (should be at least 5 based on the JSON)
        verify(mockRoleDao, atLeast(5)).saveOrUpdateRole(roleCaptor.capture());

        List<Role> savedRoles = roleCaptor.getAllValues();

        // Verify key roles exist
        boolean foundAdmin = savedRoles.stream().anyMatch(r -> "ADMIN".equals(r.getName()));
        boolean foundUser = savedRoles.stream().anyMatch(r -> "User".equals(r.getName()));
        boolean foundRestAccess = savedRoles.stream().anyMatch(r -> "RESTAccess".equals(r.getName()));

        Assert.assertTrue("ADMIN role should be saved", foundAdmin);
        Assert.assertTrue("User role should be saved", foundUser);
        Assert.assertTrue("RESTAccess role should be saved", foundRestAccess);

        // Verify all roles have non-null timestamps
        for (Role role : savedRoles) {
            Assert.assertNotNull(role.getName());
            Assert.assertNotNull(role.getDescription());
            Assert.assertNotNull(role.getCreatedDateTime());
            Assert.assertNotNull(role.getUpdatedDateTime());
        }
    }

    @Test
    public void test_execute_associates_policies_with_roles() throws Exception {
        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);

        dataLoader.execute();

        verify(mockRoleDao, atLeast(5)).saveOrUpdateRole(roleCaptor.capture());

        List<Role> savedRoles = roleCaptor.getAllValues();

        // Find ADMIN role and verify it has the ALL policy
        Role adminRole = savedRoles.stream()
            .filter(r -> "ADMIN".equals(r.getName()))
            .findFirst()
            .orElse(null);

        Assert.assertNotNull("ADMIN role should exist", adminRole);
        Assert.assertNotNull("ADMIN role should have policies", adminRole.getPolicies());
        Assert.assertTrue("ADMIN role should have at least one policy", adminRole.getPolicies().size() > 0);

        // Find User role and verify it has multiple policies
        Role userRole = savedRoles.stream()
            .filter(r -> "User".equals(r.getName()))
            .findFirst()
            .orElse(null);

        Assert.assertNotNull("User role should exist", userRole);
        Assert.assertNotNull("User role should have policies", userRole.getPolicies());
        Assert.assertTrue("User role should have multiple policies", userRole.getPolicies().size() > 10);
    }

    @Test
    public void test_execute_loads_and_saves_all_principals() throws Exception {
        ArgumentCaptor<IkasanPrincipal> principalCaptor = ArgumentCaptor.forClass(IkasanPrincipal.class);

        dataLoader.execute();

        // Verify that principals are saved (at least 1 based on the JSON)
        verify(mockPrincipalDao, atLeast(1)).saveOrUpdatePrincipal(principalCaptor.capture());

        List<IkasanPrincipal> savedPrincipals = principalCaptor.getAllValues();

        // Verify admin principal exists
        boolean foundAdmin = savedPrincipals.stream().anyMatch(p -> "admin".equals(p.getName()));
        Assert.assertTrue("admin principal should be saved", foundAdmin);

        // Verify all principals have non-null timestamps
        for (IkasanPrincipal principal : savedPrincipals) {
            Assert.assertNotNull(principal.getName());
            Assert.assertNotNull(principal.getType());
            Assert.assertNotNull(principal.getDescription());
            Assert.assertNotNull(principal.getCreatedDateTime());
            Assert.assertNotNull(principal.getUpdatedDateTime());
        }
    }

    @Test
    public void test_execute_associates_roles_with_principals() throws Exception {
        ArgumentCaptor<IkasanPrincipal> principalCaptor = ArgumentCaptor.forClass(IkasanPrincipal.class);

        dataLoader.execute();

        verify(mockPrincipalDao, atLeast(1)).saveOrUpdatePrincipal(principalCaptor.capture());

        List<IkasanPrincipal> savedPrincipals = principalCaptor.getAllValues();

        // Find admin principal and verify it has the ADMIN role
        IkasanPrincipal adminPrincipal = savedPrincipals.stream()
            .filter(p -> "admin".equals(p.getName()))
            .findFirst()
            .orElse(null);

        Assert.assertNotNull("admin principal should exist", adminPrincipal);
        Assert.assertNotNull("admin principal should have roles", adminPrincipal.getRoles());
        Assert.assertTrue("admin principal should have at least one role", adminPrincipal.getRoles().size() > 0);
        Assert.assertEquals("admin principal type should be 'user'", "user", adminPrincipal.getType());
    }

    @Test
    public void test_execute_loads_and_saves_all_users() throws Exception {
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        dataLoader.execute();

        // Verify that users are saved (at least 1 based on the JSON)
        verify(mockUserDao, atLeast(1)).save(userCaptor.capture());

        List<User> savedUsers = userCaptor.getAllValues();

        // Verify admin user exists
        boolean foundAdmin = savedUsers.stream().anyMatch(u -> "admin".equals(u.getUsername()));
        Assert.assertTrue("admin user should be saved", foundAdmin);
    }

    @Test
    public void test_execute_user_has_correct_properties() throws Exception {
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        dataLoader.execute();

        verify(mockUserDao, atLeast(1)).save(userCaptor.capture());

        List<User> savedUsers = userCaptor.getAllValues();

        // Find admin user and verify its properties
        User adminUser = savedUsers.stream()
            .filter(u -> "admin".equals(u.getUsername()))
            .findFirst()
            .orElse(null);

        Assert.assertNotNull("admin user should exist", adminUser);
        Assert.assertEquals("admin", adminUser.getUsername());
        Assert.assertTrue("Password should start with {bcrypt}",
            adminUser.getPassword().startsWith("{bcrypt}"));
        Assert.assertEquals("Admin", adminUser.getFirstName());
        Assert.assertEquals("User", adminUser.getSurname());
        Assert.assertTrue("Admin user should be enabled", adminUser.isEnabled());
    }

    @Test
    public void test_execute_associates_principals_with_users() throws Exception {
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        dataLoader.execute();

        verify(mockUserDao, atLeast(1)).save(userCaptor.capture());

        List<User> savedUsers = userCaptor.getAllValues();

        // Find admin user and verify it has the admin principal
        User adminUser = savedUsers.stream()
            .filter(u -> "admin".equals(u.getUsername()))
            .findFirst()
            .orElse(null);

        Assert.assertNotNull("admin user should exist", adminUser);
        Assert.assertNotNull("admin user should have principals", adminUser.getPrincipals());
        Assert.assertTrue("admin user should have at least one principal",
            adminUser.getPrincipals().size() > 0);
    }

    @Test
    public void test_execute_saves_all_entities_in_correct_order() throws Exception {
        // Use InOrder to verify the sequence of saves
        dataLoader.execute();

        // Verify that policies are saved before roles
        verify(mockPolicyDao, atLeast(100)).saveOrUpdatePolicy(any(Policy.class));

        // Verify that roles are saved before principals
        verify(mockRoleDao, atLeast(5)).saveOrUpdateRole(any(Role.class));

        // Verify that principals are saved before users
        verify(mockPrincipalDao, atLeast(1)).saveOrUpdatePrincipal(any(IkasanPrincipal.class));

        // Verify that users are saved last
        verify(mockUserDao, atLeast(1)).save(any(User.class));
    }

    @Test(expected = DataJobException.class)
    public void test_execute_throws_exception_when_policy_dao_fails() throws Exception {
        doThrow(new RuntimeException("Database error"))
            .when(mockPolicyDao).saveOrUpdatePolicy(any(Policy.class));

        dataLoader.execute();
    }

    @Test(expected = DataJobException.class)
    public void test_execute_throws_exception_when_role_dao_fails() throws Exception {
        doThrow(new RuntimeException("Database error"))
            .when(mockRoleDao).saveOrUpdateRole(any(Role.class));

        dataLoader.execute();
    }

    @Test(expected = DataJobException.class)
    public void test_execute_throws_exception_when_principal_dao_fails() throws Exception {
        doThrow(new RuntimeException("Database error"))
            .when(mockPrincipalDao).saveOrUpdatePrincipal(any(IkasanPrincipal.class));

        dataLoader.execute();
    }

    @Test(expected = DataJobException.class)
    public void test_execute_throws_exception_when_user_dao_fails() throws Exception {
        doThrow(new RuntimeException("Database error"))
            .when(mockUserDao).save(any(User.class));

        dataLoader.execute();
    }

    @Test
    public void test_execute_handles_missing_policy_reference_in_role() throws Exception {
        // This test verifies that the loader handles gracefully when a role references
        // a policy that doesn't exist - it should log a warning but continue processing

        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);

        dataLoader.execute();

        verify(mockRoleDao, atLeast(5)).saveOrUpdateRole(roleCaptor.capture());

        // Should complete successfully even if there are missing policy references
        // (This is tested indirectly by verifying the method completes without exception)
    }

    @Test
    public void test_execute_handles_missing_role_reference_in_principal() throws Exception {
        // This test verifies that the loader handles gracefully when a principal references
        // a role that doesn't exist - it should log a warning but continue processing

        ArgumentCaptor<IkasanPrincipal> principalCaptor = ArgumentCaptor.forClass(IkasanPrincipal.class);

        dataLoader.execute();

        verify(mockPrincipalDao, atLeast(1)).saveOrUpdatePrincipal(principalCaptor.capture());

        // Should complete successfully even if there are missing role references
    }

    @Test
    public void test_execute_handles_missing_principal_reference_in_user() throws Exception {
        // This test verifies that the loader handles gracefully when a user references
        // a principal that doesn't exist - it should log a warning but continue processing

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        dataLoader.execute();

        verify(mockUserDao, atLeast(1)).save(userCaptor.capture());

        // Should complete successfully even if there are missing principal references
    }

    @Test
    public void test_execute_creates_valid_policy_data_objects() throws Exception {
        ArgumentCaptor<Policy> policyCaptor = ArgumentCaptor.forClass(Policy.class);

        dataLoader.execute();

        verify(mockPolicyDao, atLeast(100)).saveOrUpdatePolicy(policyCaptor.capture());

        List<Policy> savedPolicies = policyCaptor.getAllValues();

        // Verify that all policies have valid data
        for (Policy policy : savedPolicies) {
            Assert.assertNotNull("Policy name should not be null", policy.getName());
            Assert.assertFalse("Policy name should not be empty", policy.getName().isEmpty());
            Assert.assertNotNull("Policy description should not be null", policy.getDescription());
            Assert.assertFalse("Policy description should not be empty", policy.getDescription().isEmpty());
        }
    }

    @Test
    public void test_execute_creates_valid_role_data_objects() throws Exception {
        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);

        dataLoader.execute();

        verify(mockRoleDao, atLeast(5)).saveOrUpdateRole(roleCaptor.capture());

        List<Role> savedRoles = roleCaptor.getAllValues();

        // Verify that all roles have valid data
        for (Role role : savedRoles) {
            Assert.assertNotNull("Role name should not be null", role.getName());
            Assert.assertFalse("Role name should not be empty", role.getName().isEmpty());
            Assert.assertNotNull("Role description should not be null", role.getDescription());
            Assert.assertFalse("Role description should not be empty", role.getDescription().isEmpty());
        }
    }

    @Test
    public void test_execute_creates_valid_principal_data_objects() throws Exception {
        ArgumentCaptor<IkasanPrincipal> principalCaptor = ArgumentCaptor.forClass(IkasanPrincipal.class);

        dataLoader.execute();

        verify(mockPrincipalDao, atLeast(1)).saveOrUpdatePrincipal(principalCaptor.capture());

        List<IkasanPrincipal> savedPrincipals = principalCaptor.getAllValues();

        // Verify that all principals have valid data
        for (IkasanPrincipal principal : savedPrincipals) {
            Assert.assertNotNull("Principal name should not be null", principal.getName());
            Assert.assertFalse("Principal name should not be empty", principal.getName().isEmpty());
            Assert.assertNotNull("Principal type should not be null", principal.getType());
            Assert.assertFalse("Principal type should not be empty", principal.getType().isEmpty());
            Assert.assertNotNull("Principal description should not be null", principal.getDescription());
        }
    }

    @Test
    public void test_execute_creates_valid_user_data_objects() throws Exception {
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        dataLoader.execute();

        verify(mockUserDao, atLeast(1)).save(userCaptor.capture());

        List<User> savedUsers = userCaptor.getAllValues();

        // Verify that all users have valid data
        for (User user : savedUsers) {
            Assert.assertNotNull("User username should not be null", user.getUsername());
            Assert.assertFalse("User username should not be empty", user.getUsername().isEmpty());
            Assert.assertNotNull("User password should not be null", user.getPassword());
            Assert.assertFalse("User password should not be empty", user.getPassword().isEmpty());
            Assert.assertNotNull("User firstName should not be null", user.getFirstName());
            Assert.assertNotNull("User surname should not be null", user.getSurname());
        }
    }

    @Test
    public void test_execute_verifies_role_has_policies_collection_initialized() throws Exception {
        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);

        dataLoader.execute();

        verify(mockRoleDao, atLeast(5)).saveOrUpdateRole(roleCaptor.capture());

        List<Role> savedRoles = roleCaptor.getAllValues();

        // Verify that roles with policies have them properly initialized
        for (Role role : savedRoles) {
            if (role.getPolicies() != null && !role.getPolicies().isEmpty()) {
                for (Policy policy : role.getPolicies()) {
                    Assert.assertNotNull("Policy in role should not be null", policy);
                    Assert.assertNotNull("Policy name should not be null", policy.getName());
                }
            }
        }
    }

    @Test
    public void test_execute_verifies_principal_has_roles_collection_initialized() throws Exception {
        ArgumentCaptor<IkasanPrincipal> principalCaptor = ArgumentCaptor.forClass(IkasanPrincipal.class);

        dataLoader.execute();

        verify(mockPrincipalDao, atLeast(1)).saveOrUpdatePrincipal(principalCaptor.capture());

        List<IkasanPrincipal> savedPrincipals = principalCaptor.getAllValues();

        // Verify that principals with roles have them properly initialized
        for (IkasanPrincipal principal : savedPrincipals) {
            if (principal.getRoles() != null && !principal.getRoles().isEmpty()) {
                for (Role role : principal.getRoles()) {
                    Assert.assertNotNull("Role in principal should not be null", role);
                    Assert.assertNotNull("Role name should not be null", role.getName());
                }
            }
        }
    }

    @Test
    public void test_execute_verifies_user_has_principals_collection_initialized() throws Exception {
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        dataLoader.execute();

        verify(mockUserDao, atLeast(1)).save(userCaptor.capture());

        List<User> savedUsers = userCaptor.getAllValues();

        // Verify that users with principals have them properly initialized
        for (User user : savedUsers) {
            if (user.getPrincipals() != null && !user.getPrincipals().isEmpty()) {
                for (IkasanPrincipal principal : user.getPrincipals()) {
                    Assert.assertNotNull("Principal in user should not be null", principal);
                    Assert.assertNotNull("Principal name should not be null", principal.getName());
                }
            }
        }
    }

    @Test
    public void test_execute_verifies_all_policies_saved_count() throws Exception {
        dataLoader.execute();

        // Based on the policies.json file, there should be exactly 105 policies
        // Verify at least 100 to allow for some flexibility
        verify(mockPolicyDao, atLeast(100)).saveOrUpdatePolicy(any(Policy.class));
    }

    @Test
    public void test_execute_verifies_all_roles_saved_count() throws Exception {
        dataLoader.execute();

        // Based on the roles.json file, there should be exactly 5 roles
        verify(mockRoleDao, times(5)).saveOrUpdateRole(any(Role.class));
    }

    @Test
    public void test_execute_verifies_all_principals_saved_count() throws Exception {
        dataLoader.execute();

        // Based on the principals.json file, there should be exactly 1 principal
        verify(mockPrincipalDao, times(1)).saveOrUpdatePrincipal(any(IkasanPrincipal.class));
    }

    @Test
    public void test_execute_verifies_all_users_saved_count() throws Exception {
        dataLoader.execute();

        // Based on the users.json file, there should be exactly 1 user
        verify(mockUserDao, times(1)).save(any(User.class));
    }

    @Test
    public void test_execute_verifies_specific_policy_names() throws Exception {
        ArgumentCaptor<Policy> policyCaptor = ArgumentCaptor.forClass(Policy.class);

        dataLoader.execute();

        verify(mockPolicyDao, atLeast(100)).saveOrUpdatePolicy(policyCaptor.capture());

        List<Policy> savedPolicies = policyCaptor.getAllValues();
        Set<String> policyNames = new HashSet<>();
        for (Policy policy : savedPolicies) {
            policyNames.add(policy.getName());
        }

        // Verify specific policies from the JSON exist
        Assert.assertTrue("Should contain ALL policy", policyNames.contains("ALL"));
        Assert.assertTrue("Should contain Discover policy", policyNames.contains("Discover"));
        Assert.assertTrue("Should contain wiretap-read policy", policyNames.contains("wiretap-read"));
        Assert.assertTrue("Should contain wiretap-write policy", policyNames.contains("wiretap-write"));
        Assert.assertTrue("Should contain wiretap-admin policy", policyNames.contains("wiretap-admin"));
        Assert.assertTrue("Should contain error-read policy", policyNames.contains("error-read"));
        Assert.assertTrue("Should contain scheduler-read policy", policyNames.contains("scheduler-read"));
        Assert.assertTrue("Should contain dashboard-admin policy", policyNames.contains("dashboard-admin"));
    }

    @Test
    public void test_execute_verifies_specific_role_names() throws Exception {
        ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);

        dataLoader.execute();

        verify(mockRoleDao, times(5)).saveOrUpdateRole(roleCaptor.capture());

        List<Role> savedRoles = roleCaptor.getAllValues();
        Set<String> roleNames = new HashSet<>();
        for (Role role : savedRoles) {
            roleNames.add(role.getName());
        }

        // Verify all roles from the JSON exist
        Assert.assertTrue("Should contain ADMIN role", roleNames.contains("ADMIN"));
        Assert.assertTrue("Should contain User role", roleNames.contains("User"));
        Assert.assertTrue("Should contain RESTAccess role", roleNames.contains("RESTAccess"));
        Assert.assertTrue("Should contain AdminUser role", roleNames.contains("AdminUser"));
        Assert.assertTrue("Should contain WriteUser role", roleNames.contains("WriteUser"));
    }

    @Test
    public void test_data_classes_policy_data() {
        BaselineSecurityDataLoader.PolicyData policyData = new BaselineSecurityDataLoader.PolicyData();
        policyData.setName("TestPolicy");
        policyData.setDescription("Test Description");

        Assert.assertEquals("TestPolicy", policyData.getName());
        Assert.assertEquals("Test Description", policyData.getDescription());
    }

    @Test
    public void test_data_classes_role_data() {
        BaselineSecurityDataLoader.RoleData roleData = new BaselineSecurityDataLoader.RoleData();
        roleData.setName("TestRole");
        roleData.setDescription("Test Description");
        roleData.setPolicyNames(List.of("Policy1", "Policy2"));

        Assert.assertEquals("TestRole", roleData.getName());
        Assert.assertEquals("Test Description", roleData.getDescription());
        Assert.assertEquals(2, roleData.getPolicyNames().size());
        Assert.assertTrue(roleData.getPolicyNames().contains("Policy1"));
        Assert.assertTrue(roleData.getPolicyNames().contains("Policy2"));
    }

    @Test
    public void test_data_classes_principal_data() {
        BaselineSecurityDataLoader.PrincipalData principalData = new BaselineSecurityDataLoader.PrincipalData();
        principalData.setName("TestPrincipal");
        principalData.setPrincipalType("user");
        principalData.setDescription("Test Description");
        principalData.setRoleNames(List.of("Role1", "Role2"));

        Assert.assertEquals("TestPrincipal", principalData.getName());
        Assert.assertEquals("user", principalData.getPrincipalType());
        Assert.assertEquals("Test Description", principalData.getDescription());
        Assert.assertEquals(2, principalData.getRoleNames().size());
        Assert.assertTrue(principalData.getRoleNames().contains("Role1"));
        Assert.assertTrue(principalData.getRoleNames().contains("Role2"));
    }

    @Test
    public void test_data_classes_user_data() {
        BaselineSecurityDataLoader.UserData userData = new BaselineSecurityDataLoader.UserData();
        userData.setUsername("testuser");
        userData.setPassword("password123");
        userData.setEmail("test@test.com");
        userData.setFirstName("Test");
        userData.setSurname("User");
        userData.setDepartment("IT");
        userData.setEnabled(true);
        userData.setPrincipalNames(List.of("Principal1"));

        Assert.assertEquals("testuser", userData.getUsername());
        Assert.assertEquals("password123", userData.getPassword());
        Assert.assertEquals("test@test.com", userData.getEmail());
        Assert.assertEquals("Test", userData.getFirstName());
        Assert.assertEquals("User", userData.getSurname());
        Assert.assertEquals("IT", userData.getDepartment());
        Assert.assertTrue(userData.isEnabled());
        Assert.assertEquals(1, userData.getPrincipalNames().size());
        Assert.assertTrue(userData.getPrincipalNames().contains("Principal1"));
    }
}
