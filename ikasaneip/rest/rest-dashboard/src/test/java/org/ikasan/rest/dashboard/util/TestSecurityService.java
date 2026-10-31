package org.ikasan.rest.dashboard.util;

import org.ikasan.security.model.*;
import org.ikasan.security.service.SecurityService;

import java.util.*;

/**
 * Test stub implementation of SecurityService for unit testing.
 */
public class TestSecurityService implements SecurityService {

    private Map<String, Role> roles = new HashMap<>();
    private List<RoleModule> roleModules = new ArrayList<>();
    private Map<String, List<String>> jobPlanRoles = new HashMap<>();

    public TestSecurityService() {
        // Pre-populate with some test roles
        Role adminRole = new Role();
        adminRole.setName("Admin");
        adminRole.setDescription("Administrator role");
        roles.put("Admin", adminRole);

        Role userRole = new Role();
        userRole.setName("User");
        userRole.setDescription("User role");
        roles.put("User", userRole);

        Role devRole = new Role();
        devRole.setName("Developer");
        devRole.setDescription("Developer role");
        roles.put("Developer", devRole);
    }

    public List<RoleModule> getSavedRoleModules() {
        return new ArrayList<>(roleModules);
    }

    public Map<String, List<String>> getJobPlanRoles() {
        return new HashMap<>(jobPlanRoles);
    }

    public void reset() {
        roleModules.clear();
        jobPlanRoles.clear();
    }

    @Override
    public IkasanPrincipal findPrincipalByName(String name) {
        return null;
    }

    @Override
    public Role findRoleByName(String name) {
        return roles.get(name);
    }

    @Override
    public Policy findPolicyByName(String name) {
        return null;
    }

    @Override
    public IkasanPrincipal createNewPrincipal(String name, String type) {
        IkasanPrincipal principal = new IkasanPrincipal();
        principal.setName(name);
        principal.setType(type);
        return principal;
    }

    @Override
    public void savePrincipal(IkasanPrincipal principal) {
    }

    @Override
    public List<IkasanPrincipal> getAllPrincipals() {
        return new ArrayList<>();
    }

    @Override
    public List<IkasanPrincipalLite> getAllPrincipalLites() {
        return new ArrayList<>();
    }

    @Override
    public int getPrincipalCount(IkasanPrincipalFilter filter) {
        return 0;
    }

    @Override
    public int getPrincipalsWithRoleCount(String roleName, IkasanPrincipalFilter filter) {
        return 0;
    }

    @Override
    public List<IkasanPrincipalLite> getAllPrincipalsWithoutRole(String roleName, IkasanPrincipalFilter filter, int limit, int offset) {
        return new ArrayList<>();
    }

    @Override
    public int getPrincipalsWithoutRoleCount(String roleName, IkasanPrincipalFilter filter) {
        return 0;
    }

    @Override
    public List<IkasanPrincipal> getPrincipals(IkasanPrincipalFilter filter, int limit, int offset) {
        return new ArrayList<>();
    }

    @Override
    public List<IkasanPrincipalLite> getPrincipalLites(IkasanPrincipalFilter filter, int limit, int offset) {
        return new ArrayList<>();
    }

    @Override
    public List<IkasanPrincipal> getAllPrincipalsWithRole(String roleName) {
        return new ArrayList<>();
    }

    @Override
    public List<IkasanPrincipalLite> getAllPrincipalsWithRole(String roleName, IkasanPrincipalFilter filter, int limit, int offset) {
        return new ArrayList<>();
    }

    @Override
    public List<IkasanPrincipal> getPrincipalsByName(List<String> names) {
        return new ArrayList<>();
    }

    @Override
    public void deletePrincipal(IkasanPrincipal principal) {
    }

    @Override
    public Role createNewRole(String name, String description) {
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        roles.put(name, role);
        return role;
    }

    @Override
    public void saveRole(Role role) {
    }

    @Override
    public void deleteRole(Role role) {
    }

    @Override
    public List<Role> getAllRoles() {
        return new ArrayList<>(roles.values());
    }

    @Override
    public Policy createNewPolicy(String name, String description) {
        Policy policy = new Policy();
        policy.setName(name);
        policy.setDescription(description);
        return policy;
    }

    @Override
    public void savePolicy(Policy policy) {
    }

    @Override
    public void deletePolicy(Policy policy) {
    }

    @Override
    public void deleteRoleModule(RoleModule roleModule) {
    }

    @Override
    public void deleteRoleJobPlan(RoleJobPlan roleJobPlan) {
    }

    @Override
    public List<Policy> getAllPolicies() {
        return new ArrayList<>();
    }

    @Override
    public void saveOrUpdateAuthenticationMethod(AuthenticationMethod authenticationMethod) {
    }

    @Override
    public List<AuthenticationMethod> getAuthenticationMethods() {
        return new ArrayList<>();
    }

    @Override
    public AuthenticationMethod getAuthenticationMethod(Long id) {
        return null;
    }

    @Override
    public void deleteAuthenticationMethod(AuthenticationMethod authenticationMethod) {
    }

    @Override
    public List<IkasanPrincipal> getPrincipalByNameLike(String name) {
        return new ArrayList<>();
    }

    @Override
    public List<PolicyLinkType> getAllPolicyLinkTypes() {
        return List.of();
    }

    @Override
    public List<Policy> getPolicyByNameLike(String name) {
        return new ArrayList<>();
    }

    @Override
    public void savePolicyLink(PolicyLink policyLink) {

    }

    @Override
    public void saveRoleModule(RoleModule roleModule) {
        roleModules.add(roleModule);
    }

    @Override
    public void saveRoleJobPlan(RoleJobPlan roleJobPlan) {
    }

    @Override
    public void deletePolicyLink(PolicyLink policyLink) {

    }

    @Override
    public List<Policy> getAllPoliciesWithRole(String roleName) {
        return new ArrayList<>();
    }

    @Override
    public List<Role> getRoleByNameLike(String name) {
        return new ArrayList<>();
    }

    @Override
    public long getNumberOfAuthenticationMethods() {
        return 0;
    }

    @Override
    public AuthenticationMethod getAuthenticationMethodByOrder(long order) {
        return null;
    }

    @Override
    public List<User> getUsersAssociatedWithPrincipal(long principalId) {
        return List.of();
    }

    @Override
    public Role getRoleById(Long id) {
        return null;
    }

    @Override
    public Policy getPolicyById(Long id) {
        return null;
    }


    @Override
    public void setJobPlanRoles(String jobPlanName, List<String> roleNames) {
        jobPlanRoles.put(jobPlanName, new ArrayList<>(roleNames));
    }
}
