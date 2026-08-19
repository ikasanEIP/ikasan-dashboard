package org.ikasan.security.dao;

import org.ikasan.security.model.SolrRoleJobPlanImpl;
import org.ikasan.security.model.SolrRoleModuleImpl;
import org.ikasan.spec.security.dao.SecurityDao;
import org.ikasan.spec.security.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.ikasan.spec.entity.EntityFields.*;

public class SolrSecurityDaoImpl implements SecurityDao {

    private static final Logger logger = LoggerFactory.getLogger(SolrSecurityDaoImpl.class);

    private final SolrIkasanPrincipalDaoImpl solrIkasanPrincipalDaoImpl;
    private final SolrRoleDaoImpl solrRoleDaoImpl;
    private final SolrPolicyDaoImpl solrPolicyDaoImpl;
    private final SolrAuthenticationMethodDaoImpl solrAuthenticationMethodDaoImpl;
    private final SolrUserDaoImpl solrUserDao;

    /**
     * Constructor for SolrSecurityDaoImpl that initializes its dependencies.
     *
     * @param solrIkasanPrincipalDaoImpl the DAO implementation for managing IkasanPrincipal entities.
     * @param solrPolicyDaoImpl the DAO implementation for managing Policy entities.
     * @param solrRoleDaoImpl the DAO implementation for managing Role entities.
     * @param solrAuthenticationMethodDaoImpl the DAO implementation for managing AuthenticationMethod entities.
     * @param solrUserDao the DAO implementation for managing User entities.
     */
    public SolrSecurityDaoImpl(SolrIkasanPrincipalDaoImpl solrIkasanPrincipalDaoImpl, SolrPolicyDaoImpl solrPolicyDaoImpl
        , SolrRoleDaoImpl solrRoleDaoImpl, SolrAuthenticationMethodDaoImpl solrAuthenticationMethodDaoImpl, SolrUserDaoImpl solrUserDao) {
        this.solrIkasanPrincipalDaoImpl = solrIkasanPrincipalDaoImpl;
        this.solrPolicyDaoImpl = solrPolicyDaoImpl;
        this.solrRoleDaoImpl = solrRoleDaoImpl;
        this.solrAuthenticationMethodDaoImpl = solrAuthenticationMethodDaoImpl;
        this.solrUserDao = solrUserDao;
    }

    @Override
    public IkasanPrincipal createPrincipal() {
        return this.solrIkasanPrincipalDaoImpl.createPrincipal();
    }

    @Override
    public Role createRole() {
        return this.solrRoleDaoImpl.createRole();
    }

    @Override
    public Policy createPolicy() {
        return this.solrPolicyDaoImpl.createPolicy();
    }

    @Override
    public RoleModule createRoleModule() {
        return new SolrRoleModuleImpl();
    }

    @Override
    public RoleJobPlan createRoleJobPlan() {
        return new SolrRoleJobPlanImpl();
    }

    @Override
    public AuthenticationMethod createAuthenticationMethod() {
        return this.solrAuthenticationMethodDaoImpl.createAuthenticationMethod();
    }

    @Override
    public void saveOrUpdateRole(Role role) {
        this.solrRoleDaoImpl.saveOrUpdateRole(role);
    }

    @Override
    public void deleteRole(Role role) {
        this.solrRoleDaoImpl.deleteRole(role);
    }

    @Override
    public void saveOrUpdatePolicy(Policy policy) {
        this.solrPolicyDaoImpl.saveOrUpdatePolicy(policy);
    }

    @Override
    public void deletePolicy(Policy policy) {
        this.solrPolicyDaoImpl.deletePolicy(policy);
    }

    @Override
    public void deleteRoleModule(RoleModule roleModule) {
        this.solrRoleDaoImpl.deleteRoleModule(roleModule);
    }

    @Override
    public void saveRoleModule(RoleModule roleModule) {
        this.solrRoleDaoImpl.saveRoleModule(roleModule);
    }

    @Override
    public void deleteRoleJobPlan(RoleJobPlan roleJobPlan) {
        this.solrRoleDaoImpl.deleteRoleJobPlan(roleJobPlan);
    }

    @Override
    public void saveRoleJobPlan(RoleJobPlan roleJobPlan) {
        this.solrRoleDaoImpl.saveRoleJobPlan(roleJobPlan);
    }

    @Override
    public void saveOrUpdatePrincipal(IkasanPrincipal principal) {
        this.solrIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);
    }

    @Override
    public void deletePrincipal(IkasanPrincipal principal) {
        this.solrIkasanPrincipalDaoImpl.deletePrincipal(principal);
    }

    @Override
    public IkasanPrincipal getPrincipalByName(String name) {
        return this.solrIkasanPrincipalDaoImpl.getPrincipalByName(name);
    }

    @Override
    public List<IkasanPrincipal> getPrincipalsByRoleNames(List names) {
        return this.solrIkasanPrincipalDaoImpl.getPrincipalsByRoleNames(names);
    }

    @Override
    public int getPrincipalCount(IkasanPrincipalFilter filter) {
        return this.solrIkasanPrincipalDaoImpl.getPrincipalCount(filter);
    }

    @Override
    public int getPrincipalsWithRoleCount(String roleName, IkasanPrincipalFilter filter) {
        return this.solrIkasanPrincipalDaoImpl.getPrincipalsWithRoleCount(roleName, filter);
    }

    @Override
    public int getPrincipalsWithoutRoleCount(String roleName, IkasanPrincipalFilter filter) {
        return this.solrIkasanPrincipalDaoImpl.getPrincipalsWithoutRoleCount(roleName, filter);
    }

    @Override
    public List<Policy> getAllPolicies() {
        return this.solrPolicyDaoImpl.getAllPolicies();
    }

    @Override
    public List<Role> getAllRoles() {
        return this.solrRoleDaoImpl.getAllRoles();
    }

    @Override
    public List<IkasanPrincipal> getAllPrincipals() {
        return this.solrIkasanPrincipalDaoImpl.getAllPrincipals();
    }

    @Override
    public List<IkasanPrincipal> getPrincipals(IkasanPrincipalFilter filter, int limit, int offset) {
        return this.solrIkasanPrincipalDaoImpl.getPrincipals(filter, limit, offset);
    }

    @Override
    public List<IkasanPrincipalLite> getAllPrincipalLites() {
        return this.solrIkasanPrincipalDaoImpl.getAllPrincipalLites();
    }

    @Override
    public List<IkasanPrincipalLite> getPrincipalLites(IkasanPrincipalFilter filter, int limit, int offset) {
        return this.solrIkasanPrincipalDaoImpl.getPrincipalLites(filter, limit, offset);
    }

    @Override
    public List<IkasanPrincipal> getAllPrincipalsWithRole(String roleName) {
        return this.solrIkasanPrincipalDaoImpl.getAllPrincipalsWithRole(roleName);
    }

    @Override
    public List<IkasanPrincipalLite> getAllPrincipalsWithRole(String roleName, IkasanPrincipalFilter filter, int limit, int offset) {
        return this.solrIkasanPrincipalDaoImpl.getAllPrincipalsWithRole(roleName, filter, limit, offset);
    }

    @Override
    public List<IkasanPrincipalLite> getAllPrincipalsWithoutRole(String roleName, IkasanPrincipalFilter filter, int limit, int offset) {
        return this.solrIkasanPrincipalDaoImpl.getAllPrincipalsWithoutRole(roleName, filter, limit, offset);
    }

    @Override
    public List<Policy> getAllPoliciesWithRole(String roleName) {
        return this.solrPolicyDaoImpl.getAllPoliciesWithRole(roleName);
    }

    @Override
    public Policy getPolicyByName(String name) {
        return this.solrPolicyDaoImpl.getPolicyByName(name);
    }

    @Override
    public Role getRoleByName(String name) {
        return this.solrRoleDaoImpl.getRoleByName(name);
    }

    @Override
    public Role getRoleById(Object id) {
        return this.solrRoleDaoImpl.getRoleById(String.valueOf(id));
    }

    @Override
    public void saveOrUpdateAuthenticationMethod(AuthenticationMethod authenticationMethod) {
        this.solrAuthenticationMethodDaoImpl.saveOrUpdateAuthenticationMethod(authenticationMethod);
    }

    @Override
    public AuthenticationMethod getAuthenticationMethod(Object id) {
        return this.solrAuthenticationMethodDaoImpl.getAuthenticationMethod(String.valueOf(id));
    }

    @Override
    public List<AuthenticationMethod> getAuthenticationMethods() {
        return this.solrAuthenticationMethodDaoImpl.getAuthenticationMethods();
    }

    @Override
    public List<IkasanPrincipal> getPrincipalByNameLike(String name) {
        return this.solrIkasanPrincipalDaoImpl.getPrincipalByNameLike(name);
    }

    @Override
    public void deleteAuthenticationMethod(AuthenticationMethod authenticationMethod) {
        this.solrAuthenticationMethodDaoImpl.deleteAuthenticationMethod(authenticationMethod);
    }

    @Override
    public List<Policy> getPolicyByNameLike(String name) {
        return this.solrPolicyDaoImpl.getPolicyByNameLike(name);
    }

    @Override
    public List<Role> getRoleByNameLike(String name) {
        return this.solrRoleDaoImpl.getRoleByNameLike(name);
    }

    @Override
    public long getNumberOfAuthenticationMethods() {
        return this.solrAuthenticationMethodDaoImpl.getNumberOfAuthenticationMethods();
    }

    @Override
    public AuthenticationMethod getAuthenticationMethodByOrder(long order) {
        return this.solrAuthenticationMethodDaoImpl.getAuthenticationMethodByOrder(order);
    }

    @Override
    public List<User> getUsersAssociatedWithPrincipal(Object principalId) {
        return this.solrUserDao.getUsersAssociatedWithPrincipal(String.valueOf(principalId));
    }

    @Override
    public Policy getPolicyById(Object id) {
        return this.solrPolicyDaoImpl.getPolicyById(String.valueOf(id));
    }

    @Override
    public List<RoleJobPlan> getRoleJobPlansByJobPlanName(String jobPlanName) {
        return this.solrRoleDaoImpl.getRoleJobPlansByJobPlanName(jobPlanName);
    }

    public List<Role> getRolesAssociatedWithPolicy(Object policyId) {
        return this.solrRoleDaoImpl.getRolesAssociatedWithPolicy(String.valueOf(policyId));
    }

}
