package org.ikasan.mongo.persistence.security.dao;

import org.ikasan.mongo.persistence.security.model.MongoRoleImpl;
import org.ikasan.mongo.persistence.security.model.MongoRoleJobPlanImpl;
import org.ikasan.mongo.persistence.security.model.MongoRoleModuleImpl;
import org.ikasan.mongo.persistence.security.repository.MongoRoleRepository;
import org.ikasan.spec.security.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of Role DAO.
 *
 * This DAO provides methods to perform CRUD operations on security roles stored in MongoDB.
 * Roles are stored as MongoRoleImpl documents with direct object storage
 * (no JSON serialization needed).
 *
 * Key features:
 * <ul>
 *   <li>Role storage and retrieval using MongoDB</li>
 *   <li>Direct object persistence (no JSON serialization)</li>
 *   <li>Wildcard search capabilities by role name</li>
 *   <li>RoleModule and RoleJobPlan management embedded within roles</li>
 *   <li>Support for role-based queries including ID and name lookups</li>
 *   <li>Policy relationship management</li>
 * </ul>
 *
 * @author Ikasan Development Team
 */
public class MongoRoleDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoRoleDao.class);

    private final MongoRoleRepository repository;
    private final MongoTemplate mongoTemplate;
    private final MongoPolicyDao mongoPolicyDao;

    /**
     * Constructor for MongoRoleDao.
     *
     * @param repository the MongoDB repository for role persistence
     * @param mongoTemplate the MongoTemplate for custom queries
     * @param mongoPolicyDao the policy DAO for loading policy relationships
     */
    public MongoRoleDao(MongoRoleRepository repository, MongoTemplate mongoTemplate,
                        MongoPolicyDao mongoPolicyDao) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.mongoPolicyDao = mongoPolicyDao;
    }

    /**
     * Creates a new empty Role instance.
     *
     * @return a new MongoRoleImpl instance
     */
    public Role createRole() {
        return new MongoRoleImpl();
    }

    /**
     * Saves or updates a role in MongoDB.
     *
     * This method persists the Role object directly to MongoDB.
     * If the role already exists (same ID or name), it will be updated.
     * The role is identified by its name, which must be unique.
     *
     * Date handling:
     * <ul>
     *   <li>If createdDateTime is null, uses current system time</li>
     *   <li>Always sets updatedDateTime to current system time</li>
     * </ul>
     *
     * This method also updates bidirectional relationships:
     * <ul>
     *   <li>Updates roleIds in associated principals</li>
     * </ul>
     *
     * @param role the role to save or update
     */
    public void saveOrUpdateRole(Role role) {
        MongoRoleImpl mongoRole;

        if (role instanceof MongoRoleImpl) {
            mongoRole = (MongoRoleImpl) role;
        } else {
            mongoRole = convertToMongoRole(role);
        }

        // Generate ID from name if not set
        if (mongoRole.getId() == null && mongoRole.getName() != null) {
            mongoRole.setId(mongoRole.getName());
        }

        // Set timestamps
        Date now = new Date();
        if (mongoRole.getCreatedDateTime() == null) {
            mongoRole.setCreatedDateTime(now);
        }
        mongoRole.setUpdatedDateTime(now);

        // Update policy IDs from policies
        if (role.getPolicies() != null) {
            List<String> policyIds = role.getPolicies().stream()
                .map(policy -> policy.getId().toString())
                .collect(Collectors.toList());
            mongoRole.setPolicyIds(policyIds);
        }

        // Update role module IDs from role modules
        if (role.getRoleModules() != null) {
            List<String> roleModuleIds = role.getRoleModules().stream()
                .map(RoleModule::getModuleName)
                .collect(Collectors.toList());
            mongoRole.setRoleModuleIds(roleModuleIds);
        }

        // Update role job plan IDs from role job plans
        if (role.getRoleJobPlans() != null) {
            List<String> roleJobPlanIds = role.getRoleJobPlans().stream()
                .map(RoleJobPlan::getJobPlanName)
                .collect(Collectors.toList());
            mongoRole.setRoleJobPlanIds(roleJobPlanIds);
        }

        repository.save(mongoRole);
        logger.debug("Saved role with name: {}", role.getName());
    }

    /**
     * Deletes a role from MongoDB.
     *
     * The role is identified by its name. This operation removes
     * the role document from MongoDB permanently.
     *
     * @param role the role to delete
     */
    public void deleteRole(Role role) {
        repository.deleteByName(role.getName());
        logger.debug("Deleted role with name: {}", role.getName());
    }

    /**
     * Associates a RoleModule instance with its corresponding Role and persists the updated Role.
     *
     * This method retrieves the Role associated with the provided RoleModule by the Role's ID.
     * The RoleModule is then added to the Role, and the updated Role is saved.
     *
     * @param roleModule the RoleModule to save and associate with its corresponding Role
     * @throws IllegalArgumentException if the RoleModule does not have an associated Role
     */
    public void saveRoleModule(RoleModule roleModule) {
        if (roleModule.getRole() == null) {
            throw new IllegalArgumentException("RoleModule must have a non-null Role");
        }
        Role role = this.getRoleById(roleModule.getRole().getId().toString());
        if (role == null) {
            throw new IllegalArgumentException("Role not found with ID: " + roleModule.getRole().getId());
        }
        role.addRoleModule(roleModule);
        this.saveOrUpdateRole(role);
        logger.debug("Saved RoleModule for module: {} and role: {}", roleModule.getModuleName(), role.getName());
    }

    /**
     * Deletes the specified RoleModule from its associated Role and persists the changes.
     *
     * This method retrieves the Role associated with the given RoleModule by its ID,
     * removes the RoleModule from the Role's list of RoleModules, and updates the Role.
     *
     * @param roleModule the RoleModule instance to be removed from its associated Role
     * @throws IllegalArgumentException if the RoleModule does not have an associated Role
     */
    public void deleteRoleModule(RoleModule roleModule) {
        if (roleModule.getRole() == null) {
            throw new IllegalArgumentException("RoleModule must have a non-null Role");
        }
        Role role = this.getRoleById(roleModule.getRole().getId().toString());
        if (role == null) {
            throw new IllegalArgumentException("Role not found with ID: " + roleModule.getRole().getId());
        }
        role.getRoleModules().remove(roleModule);
        this.saveOrUpdateRole(role);
        logger.debug("Deleted RoleModule for module: {} and role: {}", roleModule.getModuleName(), role.getName());
    }

    /**
     * Associates a RoleJobPlan instance with its corresponding Role and persists the updated Role.
     *
     * This method retrieves the Role associated with the provided RoleJobPlan by the Role's ID.
     * The RoleJobPlan is then added to the Role, and the updated Role is saved.
     *
     * @param roleJobPlan the RoleJobPlan to save and associate with its corresponding Role
     * @throws IllegalArgumentException if the RoleJobPlan does not have an associated Role
     */
    public void saveRoleJobPlan(RoleJobPlan roleJobPlan) {
        if (roleJobPlan.getRole() == null) {
            throw new IllegalArgumentException("RoleJobPlan must have a non-null Role");
        }
        Role role = this.getRoleById(roleJobPlan.getRole().getId().toString());
        if (role == null) {
            throw new IllegalArgumentException("Role not found with ID: " + roleJobPlan.getRole().getId());
        }
        role.addRoleJobPlan(roleJobPlan);
        this.saveOrUpdateRole(role);
        logger.debug("Saved RoleJobPlan for job plan: {} and role: {}", roleJobPlan.getJobPlanName(), role.getName());
    }

    /**
     * Deletes the specified RoleJobPlan from its associated Role and persists the changes.
     *
     * This method retrieves the Role associated with the given RoleJobPlan by its ID,
     * removes the RoleJobPlan from the Role's list of RoleJobPlans, and updates the Role.
     *
     * @param roleJobPlan the RoleJobPlan instance to be removed from its associated Role
     * @throws IllegalArgumentException if the RoleJobPlan does not have an associated Role
     */
    public void deleteRoleJobPlan(RoleJobPlan roleJobPlan) {
        if (roleJobPlan.getRole() == null) {
            throw new IllegalArgumentException("RoleJobPlan must have a non-null Role");
        }
        Role role = this.getRoleById(roleJobPlan.getRole().getId().toString());
        if (role == null) {
            throw new IllegalArgumentException("Role not found with ID: " + roleJobPlan.getRole().getId());
        }
        role.getRoleJobPlans().remove(roleJobPlan);
        this.saveOrUpdateRole(role);
        logger.debug("Deleted RoleJobPlan for job plan: {} and role: {}", roleJobPlan.getJobPlanName(), role.getName());
    }

    /**
     * Retrieves all roles from MongoDB.
     *
     * This method queries MongoDB for all role documents and loads their relationships.
     *
     * @return a list of all roles, or an empty list if no roles exist
     */
    public List<Role> getAllRoles() {
        return repository.findAll().stream()
            .map(this::loadRelationships)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves a role by its exact name.
     *
     * This method performs an exact match query on the role name field.
     *
     * @param name the exact name of the role to retrieve
     * @return the Role object if found, or null if no role exists with the given name
     */
    public Role getRoleByName(String name) {
        return repository.findByName(name)
            .map(this::loadRelationships)
            .orElse(null);
    }

    /**
     * Retrieves a Role by its unique identifier.
     *
     * The identifier is the role ID, which queries MongoDB directly.
     *
     * @param id the unique identifier of the role to retrieve
     * @return the Role object if a matching record is found, or null if no record exists for the provided identifier
     */
    public Role getRoleById(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }

        return repository.findById(id)
            .map(this::loadRelationships)
            .orElse(null);
    }

    /**
     * Retrieves roles whose names contain the specified search term.
     *
     * This method performs a case-insensitive wildcard search on role names.
     * The search matches any role name that contains the given substring.
     *
     * @param name the search term to match against role names
     * @return a list of roles whose names contain the search term, or an empty list if no matches found
     */
    public List<Role> getRoleByNameLike(String name) {
        return repository.findByNameContainingIgnoreCase(name).stream()
            .map(this::loadRelationships)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves all role job plans associated with a specific job plan name.
     *
     * This method queries roles that have the specified job plan in their roleJobPlanIds collection.
     * It returns all RoleJobPlan objects from matching roles that have the given job plan name.
     *
     * @param jobPlanName the name of the job plan
     * @return a list of RoleJobPlan objects that match the job plan name, or an empty list if none found
     */
    public List<RoleJobPlan> getRoleJobPlansByJobPlanName(String jobPlanName) {
        if (jobPlanName == null || jobPlanName.isEmpty()) {
            return Collections.emptyList();
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("roleJobPlanIds").in(jobPlanName));

        List<MongoRoleImpl> roles = mongoTemplate.find(query, MongoRoleImpl.class);

        // Extract all RoleJobPlans with matching job plan name from all matching roles
        return roles.stream()
            .map(this::loadRelationships)
            .flatMap(role -> role.getRoleJobPlans().stream())
            .filter(roleJobPlan -> jobPlanName.equals(roleJobPlan.getJobPlanName()))
            .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of roles associated with the specified policy identifier.
     *
     * This method queries MongoDB to find all roles that have the given policy ID
     * in their policyIds collection.
     *
     * @param policyId the unique identifier of the policy for which associated roles need to be retrieved
     * @return a list of roles associated with the specified policy, or an empty list if no roles are found
     */
    public List<Role> getRolesAssociatedWithPolicy(Object policyId) {
        if (policyId == null) {
            return Collections.emptyList();
        }

        String policyIdStr = policyId.toString();
        return repository.findByPolicyIdsContaining(policyIdStr).stream()
            .map(this::loadRelationships)
            .collect(Collectors.toList());
    }

    /**
     * Loads relationships for a role (policies, modules, job plans).
     *
     * @param role the role to load relationships for
     * @return the role with relationships loaded
     */
    private Role loadRelationships(MongoRoleImpl role) {
        // Load policies
        if (role.getPolicyIds() != null && !role.getPolicyIds().isEmpty()) {
            Set<Policy> policies = new HashSet<>();
            for (String policyId : role.getPolicyIds()) {
                Policy policy = mongoPolicyDao.getPolicyById(policyId);
                if (policy != null) {
                    policies.add(policy);
                }
            }
            role.setPolicies(policies);
        }

        // Load role modules
        if (role.getRoleModuleIds() != null && !role.getRoleModuleIds().isEmpty()) {
            Set<RoleModule> roleModules = new HashSet<>();
            for (String moduleName : role.getRoleModuleIds()) {
                MongoRoleModuleImpl roleModule = new MongoRoleModuleImpl();
                roleModule.setId(role.getId() + "-" + moduleName);
                roleModule.setModuleName(moduleName);
                roleModule.setRole(role);
                roleModule.setCreatedDateTime(role.getCreatedDateTime());
                roleModule.setUpdatedDateTime(role.getUpdatedDateTime());
                roleModules.add(roleModule);
            }
            role.setRoleModules(roleModules);
        }

        // Load role job plans
        if (role.getRoleJobPlanIds() != null && !role.getRoleJobPlanIds().isEmpty()) {
            Set<RoleJobPlan> roleJobPlans = new HashSet<>();
            for (String jobPlanName : role.getRoleJobPlanIds()) {
                MongoRoleJobPlanImpl roleJobPlan = new MongoRoleJobPlanImpl();
                roleJobPlan.setId(role.getId() + "-" + jobPlanName);
                roleJobPlan.setJobPlanName(jobPlanName);
                roleJobPlan.setRole(role);
                roleJobPlan.setCreatedDateTime(role.getCreatedDateTime());
                roleJobPlan.setUpdatedDateTime(role.getUpdatedDateTime());
                roleJobPlans.add(roleJobPlan);
            }
            role.setRoleJobPlans(roleJobPlans);
        }

        return role;
    }

    /**
     * Converts a Role to a MongoRoleImpl.
     *
     * @param role the role to convert
     * @return the MongoRoleImpl
     */
    private MongoRoleImpl convertToMongoRole(Role role) {
        MongoRoleImpl mongoRole = new MongoRoleImpl();
        mongoRole.setId(role.getId());
        mongoRole.setName(role.getName());
        mongoRole.setDescription(role.getDescription());
        mongoRole.setCreatedDateTime(role.getCreatedDateTime());
        mongoRole.setUpdatedDateTime(role.getUpdatedDateTime());
        mongoRole.setPolicies(role.getPolicies());
        mongoRole.setRoleModules(role.getRoleModules());
        mongoRole.setRoleJobPlans(role.getRoleJobPlans());
        return mongoRole;
    }
}
