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
 *   <li>MongoIkasanPrincipalDao - for principal operations</li>
 *   <li>MongoRoleDao - for role operations</li>
 *   <li>MongoPolicyDao - for policy operations</li>
 *   <li>MongoAuthenticationMethodDao - for authentication method operations</li>
 *   <li>MongoUserDao - for user operations</li>
 * </ul>
 *
 * @author Ikasan Development Team
 */
public class MongoSecurityDao implements SecurityDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoSecurityDao.class);

    private final MongoIkasanPrincipalDao mongoIkasanPrincipalDao;
    private final MongoRoleDao mongoRoleDao;
    private final MongoPolicyDao mongoPolicyDao;
    private final MongoAuthenticationMethodDao mongoAuthenticationMethodDao;
    private final UserDao mongoUserDao;

    /**
     * Constructor for MongoSecurityDao that initializes its dependencies.
     *
     * @param mongoIkasanPrincipalDao the DAO implementation for managing IkasanPrincipal entities
     * @param mongoPolicyDao the DAO implementation for managing Policy entities
     * @param mongoRoleDao the DAO implementation for managing Role entities
     * @param mongoAuthenticationMethodDao the DAO implementation for managing AuthenticationMethod entities
     * @param mongoUserDao the DAO implementation for managing User entities
     */
    public MongoSecurityDao(MongoIkasanPrincipalDao mongoIkasanPrincipalDao,
                            MongoPolicyDao mongoPolicyDao,
                            MongoRoleDao mongoRoleDao,
                            MongoAuthenticationMethodDao mongoAuthenticationMethodDao,
                            UserDao mongoUserDao) {
        this.mongoIkasanPrincipalDao = mongoIkasanPrincipalDao;
        this.mongoPolicyDao = mongoPolicyDao;
        this.mongoRoleDao = mongoRoleDao;
        this.mongoAuthenticationMethodDao = mongoAuthenticationMethodDao;
        this.mongoUserDao = mongoUserDao;
    }

    @Override
    public IkasanPrincipal createPrincipal() {
        return this.mongoIkasanPrincipalDao.createPrincipal();
    }

    @Override
    public Role createRole() {
        return this.mongoRoleDao.createRole();
    }

    @Override
    public Policy createPolicy() {
        return this.mongoPolicyDao.createPolicy();
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
        return this.mongoAuthenticationMethodDao.createAuthenticationMethod();
    }

    @Override
    public void saveOrUpdateRole(Role role) {
        this.mongoRoleDao.saveOrUpdateRole(role);
    }

    @Override
    public void deleteRole(Role role) {
        this.mongoRoleDao.deleteRole(role);
    }

    @Override
    public void saveOrUpdatePolicy(Policy policy) {
        this.mongoPolicyDao.saveOrUpdatePolicy(policy);
    }

    @Override
    public void deletePolicy(Policy policy) {
        this.mongoPolicyDao.deletePolicy(policy);
    }

    @Override
    public void deleteRoleModule(RoleModule roleModule) {
        this.mongoRoleDao.deleteRoleModule(roleModule);
    }

    @Override
    public void saveRoleModule(RoleModule roleModule) {
        this.mongoRoleDao.saveRoleModule(roleModule);
    }

    @Override
    public void deleteRoleJobPlan(RoleJobPlan roleJobPlan) {
        this.mongoRoleDao.deleteRoleJobPlan(roleJobPlan);
    }

    @Override
    public void saveRoleJobPlan(RoleJobPlan roleJobPlan) {
        this.mongoRoleDao.saveRoleJobPlan(roleJobPlan);
    }

    @Override
    public void saveOrUpdatePrincipal(IkasanPrincipal principal) {
        this.mongoIkasanPrincipalDao.saveOrUpdatePrincipal(principal);
    }

    @Override
    public void deletePrincipal(IkasanPrincipal principal) {
        this.mongoIkasanPrincipalDao.deletePrincipal(principal);
    }

    @Override
    public IkasanPrincipal getPrincipalByName(String name) {
        return this.mongoIkasanPrincipalDao.getPrincipalByName(name);
    }

    @Override
    public List<IkasanPrincipal> getPrincipalsByRoleNames(List<String> names) {
        return this.mongoIkasanPrincipalDao.getPrincipalsByRoleNames(names);
    }

    @Override
    public int getPrincipalCount(IkasanPrincipalFilter filter) {
        return this.mongoIkasanPrincipalDao.getPrincipalCount(filter);
    }

    @Override
    public int getPrincipalsWithRoleCount(String roleName, IkasanPrincipalFilter filter) {
        return this.mongoIkasanPrincipalDao.getPrincipalsWithRoleCount(roleName, filter);
    }

    @Override
    public int getPrincipalsWithoutRoleCount(String roleName, IkasanPrincipalFilter filter) {
        return this.mongoIkasanPrincipalDao.getPrincipalsWithoutRoleCount(roleName, filter);
    }

    @Override
    public List<Policy> getAllPolicies() {
        return this.mongoPolicyDao.getAllPolicies();
    }

    @Override
    public List<Role> getAllRoles() {
        return this.mongoRoleDao.getAllRoles();
    }

    @Override
    public List<IkasanPrincipal> getAllPrincipals() {
        return this.mongoIkasanPrincipalDao.getAllPrincipals();
    }

    @Override
    public List<IkasanPrincipal> getPrincipals(IkasanPrincipalFilter filter, int limit, int offset) {
        return this.mongoIkasanPrincipalDao.getPrincipals(filter, limit, offset);
    }

    @Override
    public List<IkasanPrincipalLite> getAllPrincipalLites() {
        return this.mongoIkasanPrincipalDao.getAllPrincipalLites();
    }

    @Override
    public List<IkasanPrincipalLite> getPrincipalLites(IkasanPrincipalFilter filter, int limit, int offset) {
        return this.mongoIkasanPrincipalDao.getPrincipalLites(filter, limit, offset);
    }

    @Override
    public List<IkasanPrincipal> getAllPrincipalsWithRole(String roleName) {
        return this.mongoIkasanPrincipalDao.getAllPrincipalsWithRole(roleName);
    }

    @Override
    public List<IkasanPrincipalLite> getAllPrincipalsWithRole(String roleName, IkasanPrincipalFilter filter,
                                                              int limit, int offset) {
        return this.mongoIkasanPrincipalDao.getAllPrincipalsWithRole(roleName, filter, limit, offset);
    }

    @Override
    public List<IkasanPrincipalLite> getAllPrincipalsWithoutRole(String roleName, IkasanPrincipalFilter filter,
                                                                 int limit, int offset) {
        return this.mongoIkasanPrincipalDao.getAllPrincipalsWithoutRole(roleName, filter, limit, offset);
    }

    @Override
    public List<Policy> getAllPoliciesWithRole(String roleName) {
        return this.mongoPolicyDao.getAllPoliciesWithRole(roleName);
    }

    @Override
    public Policy getPolicyByName(String name) {
        return this.mongoPolicyDao.getPolicyByName(name);
    }

    @Override
    public Role getRoleByName(String name) {
        return this.mongoRoleDao.getRoleByName(name);
    }

    @Override
    public Role getRoleById(Object id) {
        return this.mongoRoleDao.getRoleById(String.valueOf(id));
    }

    @Override
    public void saveOrUpdateAuthenticationMethod(AuthenticationMethod authenticationMethod) {
        this.mongoAuthenticationMethodDao.saveOrUpdateAuthenticationMethod(authenticationMethod);
    }

    @Override
    public AuthenticationMethod getAuthenticationMethod(Object id) {
        return this.mongoAuthenticationMethodDao.getAuthenticationMethod(id);
    }

    @Override
    public List<AuthenticationMethod> getAuthenticationMethods() {
        return this.mongoAuthenticationMethodDao.getAuthenticationMethods();
    }

    @Override
    public List<IkasanPrincipal> getPrincipalByNameLike(String name) {
        return this.mongoIkasanPrincipalDao.getPrincipalByNameLike(name);
    }

    @Override
    public void deleteAuthenticationMethod(AuthenticationMethod authenticationMethod) {
        this.mongoAuthenticationMethodDao.deleteAuthenticationMethod(authenticationMethod);
    }

    @Override
    public List<Policy> getPolicyByNameLike(String name) {
        return this.mongoPolicyDao.getPolicyByNameLike(name);
    }

    @Override
    public List<Role> getRoleByNameLike(String name) {
        return this.mongoRoleDao.getRoleByNameLike(name);
    }

    @Override
    public long getNumberOfAuthenticationMethods() {
        return this.mongoAuthenticationMethodDao.getNumberOfAuthenticationMethods();
    }

    @Override
    public AuthenticationMethod getAuthenticationMethodByOrder(long order) {
        return this.mongoAuthenticationMethodDao.getAuthenticationMethodByOrder(order);
    }

    @Override
    public List<User> getUsersAssociatedWithPrincipal(Object principalId) {
        if (this.mongoUserDao instanceof MongoUserDao) {
            return ((MongoUserDao) this.mongoUserDao).getUsersAssociatedWithPrincipal(String.valueOf(principalId));
        }
        throw new UnsupportedOperationException("getUsersAssociatedWithPrincipal is only supported for MongoUserDao");
    }

    @Override
    public Policy getPolicyById(Object id) {
        return this.mongoPolicyDao.getPolicyById(String.valueOf(id));
    }

    @Override
    public List<RoleJobPlan> getRoleJobPlansByJobPlanName(String jobPlanName) {
        return this.mongoRoleDao.getRoleJobPlansByJobPlanName(jobPlanName);
    }

    /**
     * Retrieves a list of roles associated with the specified policy identifier.
     *
     * This method delegates to the MongoRoleDao to find all roles that have
     * the specified policy.
     *
     * @param policyId the unique identifier of the policy
     * @return a list of roles associated with the specified policy
     */
    public List<Role> getRolesAssociatedWithPolicy(Object policyId) {
        return this.mongoRoleDao.getRolesAssociatedWithPolicy(policyId);
    }
}
