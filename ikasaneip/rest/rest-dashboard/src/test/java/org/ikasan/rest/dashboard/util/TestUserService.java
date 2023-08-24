package org.ikasan.rest.dashboard.util;

import org.ikasan.security.model.*;
import org.ikasan.security.service.UserService;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Set;

public class TestUserService implements UserService {
    @Override
    public List<User> getUsers() {
        return List.of(this.createUser("username1"),
            this.createUser("username2"),
            this.createUser("username3"));
    }

    @Override
    public List<UserLite> getUserLites() {
        return null;
    }

    @Override
    public List<Policy> getAuthorities() {
        return null;
    }

    @Override
    public User loadUserByUsername(String s) throws UsernameNotFoundException, DataAccessException {
        return this.createUser("username");
    }

    @Override
    public void grantAuthority(String s, String s1) {

    }

    @Override
    public void revokeAuthority(String s, String s1) {

    }

    @Override
    public void changeUsersPassword(String s, String s1, String s2) throws IllegalArgumentException {

    }

    @Override
    public void changeUsersEmail(String s, String s1) throws IllegalArgumentException {

    }

    @Override
    public void disableUser(String s) {

    }

    @Override
    public void enableUser(String s) {

    }

    @Override
    public List<User> getUserByUsernameLike(String s) {
        return null;
    }

    @Override
    public List<User> getUserByFirstnameLike(String s) {
        return null;
    }

    @Override
    public List<User> getUserBySurnameLike(String s) {
        return null;
    }

    @Override
    public void createUser(UserDetails userDetails) {

    }

    @Override
    public void updateUser(UserDetails userDetails) {

    }

    @Override
    public void deleteUser(String s) {

    }

    @Override
    public void changePassword(String s, String s1) {

    }

    @Override
    public boolean userExists(String s) {
        return false;
    }

    private User createUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPrincipals(Set.of(createPrincipal("principal1"),
            createPrincipal("principal2")));

        return user;
    }

    private IkasanPrincipal createPrincipal(String name) {
        IkasanPrincipal ikasanPrincipal = new IkasanPrincipal();
        ikasanPrincipal.setName(name);
        ikasanPrincipal.setRoles(Set.of(createRole("role1"),
            createRole("role2"), createRole("role3")));

        return ikasanPrincipal;
    }

    private Role createRole(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        role.setDescription("Role Description");
        role.setPolicies(Set.of(createPolicy("policy1")
            , createPolicy("policy2")
            , createPolicy("policy3")));
        role.setRoleModules(Set.of(createRoleModule(role, "module1"),
            createRoleModule(role, "module2"),
            createRoleModule(role, "module3")));
        role.setRoleJobPlans(Set.of(createRoleJobPlan(role, "plan1"),
            createRoleJobPlan(role, "plan2"),
            createRoleJobPlan(role, "plan3")));
        return role;
    }

    private Policy createPolicy(String policyName) {
        Policy policy = new Policy();
        policy.setName(policyName);

        return policy;
    }

    private RoleModule createRoleModule(Role role, String moduleName) {
        RoleModule roleModule = new RoleModule();
        roleModule.setRole(role);
        roleModule.setModuleName(moduleName);
        return roleModule;
    }

    private RoleJobPlan createRoleJobPlan(Role role, String planName) {
        RoleJobPlan roleModule = new RoleJobPlan();
        roleModule.setRole(role);
        roleModule.setJobPlanName(planName);
        return roleModule;
    }
}
