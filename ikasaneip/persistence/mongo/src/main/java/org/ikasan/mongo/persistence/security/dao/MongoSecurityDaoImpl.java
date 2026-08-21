package org.ikasan.mongo.persistence.security.dao;

import org.ikasan.mongo.persistence.security.model.MongoRoleJobPlanImpl;
import org.ikasan.mongo.persistence.security.model.MongoRoleModuleImpl;
import org.ikasan.spec.security.dao.SecurityDao;
import org.ikasan.spec.security.dao.UserDao;
import org.ikasan.spec.security.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * MongoDB implementation of SecurityDao facade.
 *
 * This class acts as a facade that delegates all security-related operations
 * to the specialized MongoDB DAO implementations. It implements the SecurityDao
 * interface and provides a unified entry point for all security entity operations.
 *
 * The facade delegates to:
 * <ul>
 *   <li>MongoIkasanPrincipalDaoImpl - for principal operations</li>
 *   <li>MongoRoleDaoImpl - for role operations</li>
 *   <li>MongoPolicyDaoImpl - for policy operations</li>
 *   <li>MongoAuthenticationMethodDaoImpl - for authentication method operations</li>
 *   <li>MongoUserDaoImpl - for user operations</li>
 * </ul>
 *
 * @author Ikasan Development Team
 */
public class MongoSecurityDaoImpl implements SecurityDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoSecurityDaoImpl.class);

    private final MongoIkasanPrincipalDaoImpl mongoIkasanPrincipalDaoImpl;
    private final MongoRoleDaoImpl mongoRoleDaoImpl;
    private final MongoPolicyDaoImpl mongoPolicyDaoImpl;
    private final MongoAuthenticationMethodDaoImpl mongoAuthenticationMethodDaoImpl;
    private final UserDao mongoUserDao;

    /**
     * Constructor for MongoSecurityDaoImpl that initializes its dependencies.
     *
     * @param mongoIkasanPrincipalDaoImpl the DAO implementation for managing IkasanPrincipal entities
     * @param mongoPolicyDaoImpl the DAO implementation for managing Policy entities
     * @param mongoRoleDaoImpl the DAO implementation for managing Role entities
     * @param mongoAuthenticationMethodDaoImpl the DAO implementation for managing AuthenticationMethod entities
     * @param mongoUserDao the DAO implementation for managing User entities
     */
    public MongoSecurityDaoImpl(MongoIkasanPrincipalDaoImpl mongoIkasanPrincipalDaoImpl,
                                MongoPolicyDaoImpl mongoPolicyDaoImpl,
                                MongoRoleDaoImpl mongoRoleDaoImpl,
                                MongoAuthenticationMethodDaoImpl mongoAuthenticationMethodDaoImpl,
                                UserDao mongoUserDao) {
        this.mongoIkasanPrincipalDaoImpl = mongoIkasanPrincipalDaoImpl;
        this.mongoPolicyDaoImpl = mongoPolicyDaoImpl;
        this.mongoRoleDaoImpl = mongoRoleDaoImpl;
        this.mongoAuthenticationMethodDaoImpl = mongoAuthenticationMethodDaoImpl;
        this.mongoUserDao = mongoUserDao;
    }

    @Override
    public IkasanPrincipal createPrincipal() {
        return this.mongoIkasanPrincipalDaoImpl.createPrincipal();
    }

    @Override
    public Role createRole() {
        return this.mongoRoleDaoImpl.createRole();
    }

    @Override
    public Policy createPolicy() {
        return this.mongoPolicyDaoImpl.createPolicy();
    }

    @Override
    public RoleModule createRoleModule() {
        return new MongoRoleModuleImpl();
    }

    @Override
    public RoleJobPlan createRoleJobPlan() {
        return new MongoRoleJobPlanImpl();
    }

    @Override
    public AuthenticationMethod createAuthenticationMethod() {
        return this.mongoAuthenticationMethodDaoImpl.createAuthenticationMethod();
    }

    @Override
    public void saveOrUpdateRole(Role role) {
        this.mongoRoleDaoImpl.saveOrUpdateRole(role);
    }

    @Override
    public void deleteRole(Role role) {
        this.mongoRoleDaoImpl.deleteRole(role);
    }

    @Override
    public void saveOrUpdatePolicy(Policy policy) {
        this.mongoPolicyDaoImpl.saveOrUpdatePolicy(policy);
    }

    @Override
    public void deletePolicy(Policy policy) {
        this.mongoPolicyDaoImpl.deletePolicy(policy);
    }

    @Override
    public void deleteRoleModule(RoleModule roleModule) {
        this.mongoRoleDaoImpl.deleteRoleModule(roleModule);
    }

    @Override
    public void saveRoleModule(RoleModule roleModule) {
        this.mongoRoleDaoImpl.saveRoleModule(roleModule);
    }

    @Override
    public void deleteRoleJobPlan(RoleJobPlan roleJobPlan) {
        this.mongoRoleDaoImpl.deleteRoleJobPlan(roleJobPlan);
    }

    @Override
    public void saveRoleJobPlan(RoleJobPlan roleJobPlan) {
        this.mongoRoleDaoImpl.saveRoleJobPlan(roleJobPlan);
    }

    @Override
    public void saveOrUpdatePrincipal(IkasanPrincipal principal) {
        this.mongoIkasanPrincipalDaoImpl.saveOrUpdatePrincipal(principal);
    }

    @Override
    public void deletePrincipal(IkasanPrincipal principal) {
        this.mongoIkasanPrincipalDaoImpl.deletePrincipal(principal);
    }

    @Override
    public IkasanPrincipal getPrincipalByName(String name) {
        return this.mongoIkasanPrincipalDaoImpl.getPrincipalByName(name);
    }

    @Override
    public List<IkasanPrincipal> getPrincipalsByRoleNames(List<String> names) {
        return this.mongoIkasanPrincipalDaoImpl.getPrincipalsByRoleNames(names);
    }

    @Override
    public int getPrincipalCount(IkasanPrincipalFilter filter) {
        return this.mongoIkasanPrincipalDaoImpl.getPrincipalCount(filter);
    }

    @Override
    public int getPrincipalsWithRoleCount(String roleName, IkasanPrincipalFilter filter) {
        return this.mongoIkasanPrincipalDaoImpl.getPrincipalsWithRoleCount(roleName, filter);
    }

    @Override
    public int getPrincipalsWithoutRoleCount(String roleName, IkasanPrincipalFilter filter) {
        return this.mongoIkasanPrincipalDaoImpl.getPrincipalsWithoutRoleCount(roleName, filter);
    }

    @Override
    public List<Policy> getAllPolicies() {
        return this.mongoPolicyDaoImpl.getAllPolicies();
    }

    @Override
    public List<Role> getAllRoles() {
        return this.mongoRoleDaoImpl.getAllRoles();
    }

    @Override
    public List<IkasanPrincipal> getAllPrincipals() {
        return this.mongoIkasanPrincipalDaoImpl.getAllPrincipals();
    }

    @Override
    public List<IkasanPrincipal> getPrincipals(IkasanPrincipalFilter filter, int limit, int offset) {
        return this.mongoIkasanPrincipalDaoImpl.getPrincipals(filter, limit, offset);
    }

    @Override
    public List<IkasanPrincipalLite> getAllPrincipalLites() {
        return this.mongoIkasanPrincipalDaoImpl.getAllPrincipalLites();
    }

    @Override
    public List<IkasanPrincipalLite> getPrincipalLites(IkasanPrincipalFilter filter, int limit, int offset) {
        return this.mongoIkasanPrincipalDaoImpl.getPrincipalLites(filter, limit, offset);
    }

    @Override
    public List<IkasanPrincipal> getAllPrincipalsWithRole(String roleName) {
        return this.mongoIkasanPrincipalDaoImpl.getAllPrincipalsWithRole(roleName);
    }

    @Override
    public List<IkasanPrincipalLite> getAllPrincipalsWithRole(String roleName, IkasanPrincipalFilter filter,
                                                              int limit, int offset) {
        return this.mongoIkasanPrincipalDaoImpl.getAllPrincipalsWithRole(roleName, filter, limit, offset);
    }

    @Override
    public List<IkasanPrincipalLite> getAllPrincipalsWithoutRole(String roleName, IkasanPrincipalFilter filter,
                                                                 int limit, int offset) {
        return this.mongoIkasanPrincipalDaoImpl.getAllPrincipalsWithoutRole(roleName, filter, limit, offset);
    }

    @Override
    public List<Policy> getAllPoliciesWithRole(String roleName) {
        return this.mongoPolicyDaoImpl.getAllPoliciesWithRole(roleName);
    }

    @Override
    public Policy getPolicyByName(String name) {
        return this.mongoPolicyDaoImpl.getPolicyByName(name);
    }

    @Override
    public Role getRoleByName(String name) {
        return this.mongoRoleDaoImpl.getRoleByName(name);
    }

    @Override
    public Role getRoleById(Object id) {
        return this.mongoRoleDaoImpl.getRoleById(String.valueOf(id));
    }

    @Override
    public void saveOrUpdateAuthenticationMethod(AuthenticationMethod authenticationMethod) {
        this.mongoAuthenticationMethodDaoImpl.saveOrUpdateAuthenticationMethod(authenticationMethod);
    }

    @Override
    public AuthenticationMethod getAuthenticationMethod(Object id) {
        return this.mongoAuthenticationMethodDaoImpl.getAuthenticationMethod(id);
    }

    @Override
    public List<AuthenticationMethod> getAuthenticationMethods() {
        return this.mongoAuthenticationMethodDaoImpl.getAuthenticationMethods();
    }

    @Override
    public List<IkasanPrincipal> getPrincipalByNameLike(String name) {
        return this.mongoIkasanPrincipalDaoImpl.getPrincipalByNameLike(name);
    }

    @Override
    public void deleteAuthenticationMethod(AuthenticationMethod authenticationMethod) {
        this.mongoAuthenticationMethodDaoImpl.deleteAuthenticationMethod(authenticationMethod);
    }

    @Override
    public List<Policy> getPolicyByNameLike(String name) {
        return this.mongoPolicyDaoImpl.getPolicyByNameLike(name);
    }

    @Override
    public List<Role> getRoleByNameLike(String name) {
        return this.mongoRoleDaoImpl.getRoleByNameLike(name);
    }

    @Override
    public long getNumberOfAuthenticationMethods() {
        return this.mongoAuthenticationMethodDaoImpl.getNumberOfAuthenticationMethods();
    }

    @Override
    public AuthenticationMethod getAuthenticationMethodByOrder(long order) {
        return this.mongoAuthenticationMethodDaoImpl.getAuthenticationMethodByOrder(order);
    }

    @Override
    public List<User> getUsersAssociatedWithPrincipal(Object principalId) {
        if (this.mongoUserDao instanceof MongoUserDaoImpl) {
            return ((MongoUserDaoImpl) this.mongoUserDao).getUsersAssociatedWithPrincipal(String.valueOf(principalId));
        }
        throw new UnsupportedOperationException("getUsersAssociatedWithPrincipal is only supported for MongoUserDaoImpl");
    }

    @Override
    public Policy getPolicyById(Object id) {
        return this.mongoPolicyDaoImpl.getPolicyById(String.valueOf(id));
    }

    @Override
    public List<RoleJobPlan> getRoleJobPlansByJobPlanName(String jobPlanName) {
        return this.mongoRoleDaoImpl.getRoleJobPlansByJobPlanName(jobPlanName);
    }

    /**
     * Retrieves a list of roles associated with the specified policy identifier.
     *
     * This method delegates to the MongoRoleDaoImpl to find all roles that have
     * the specified policy.
     *
     * @param policyId the unique identifier of the policy
     * @return a list of roles associated with the specified policy
     */
    public List<Role> getRolesAssociatedWithPolicy(Object policyId) {
        return this.mongoRoleDaoImpl.getRolesAssociatedWithPolicy(policyId);
    }
}
