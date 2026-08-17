package org.ikasan.mongo.persistence.security.dao;

import org.ikasan.mongo.persistence.security.model.MongoPolicyImpl;
import org.ikasan.mongo.persistence.security.repository.MongoPolicyRepository;
import org.ikasan.spec.security.dao.PolicyDao;
import org.ikasan.spec.security.model.Policy;
import org.ikasan.spec.security.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of Policy DAO.
 *
 * This DAO provides methods to perform CRUD operations on security policies stored in MongoDB.
 * Policies are stored as MongoPolicyImpl documents with direct object storage
 * (no JSON serialization needed).
 *
 * Key features:
 * <ul>
 *   <li>Policy storage and retrieval using MongoDB</li>
 *   <li>Direct object persistence (no JSON serialization)</li>
 *   <li>Wildcard search capabilities by policy name</li>
 *   <li>Simple and efficient CRUD operations</li>
 * </ul>
 *
 * @author Ikasan Development Team
 */
public class MongoPolicyDao implements PolicyDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoPolicyDao.class);

    private final MongoPolicyRepository repository;
    private final MongoTemplate mongoTemplate;
    private MongoRoleDao mongoRoleDao; // Circular dependency - will be set via setter

    /**
     * Constructor for MongoPolicyDao.
     *
     * @param repository the MongoDB repository for policy persistence
     * @param mongoTemplate the MongoTemplate for custom queries
     */
    public MongoPolicyDao(MongoPolicyRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Sets the MongoRoleDao for querying role relationships.
     * This is needed to avoid circular dependency issues.
     *
     * @param mongoRoleDao the role DAO
     */
    public void setMongoRoleDao(MongoRoleDao mongoRoleDao) {
        this.mongoRoleDao = mongoRoleDao;
    }

    /**
     * Creates a new empty Policy instance.
     *
     * @return a new MongoPolicyImpl instance
     */
    public Policy createPolicy() {
        return new MongoPolicyImpl();
    }

    /**
     * Saves or updates a policy in MongoDB.
     *
     * This method persists the Policy object directly to MongoDB.
     * If the policy already exists (same ID or name), it will be updated.
     * The policy is identified by its name, which must be unique.
     *
     * Date handling:
     * <ul>
     *   <li>If createdDateTime is null, uses current system time</li>
     *   <li>Always sets updatedDateTime to current system time</li>
     * </ul>
     *
     * @param policy the policy to save or update
     */
    public void saveOrUpdatePolicy(Policy policy) {
        MongoPolicyImpl mongoPolicy;

        if (policy instanceof MongoPolicyImpl) {
            mongoPolicy = (MongoPolicyImpl) policy;
        } else {
            mongoPolicy = convertToMongoPolicy(policy);
        }

        // Generate ID from name if not set
        if (mongoPolicy.getId() == null && mongoPolicy.getName() != null) {
            mongoPolicy.setId(mongoPolicy.getName());
        }

        // Set timestamps
        Date now = new Date();
        if (mongoPolicy.getCreatedDateTime() == null) {
            mongoPolicy.setCreatedDateTime(now);
        }
        mongoPolicy.setUpdatedDateTime(now);

        repository.save(mongoPolicy);
        logger.debug("Saved policy with name: {}", policy.getName());
    }

    /**
     * Deletes a policy from MongoDB.
     *
     * The policy is identified by its name. This operation removes
     * the policy document from MongoDB permanently.
     *
     * @param policy the policy to delete
     */
    public void deletePolicy(Policy policy) {
        repository.deleteByName(policy.getName());
        logger.debug("Deleted policy with name: {}", policy.getName());
    }

    /**
     * Retrieves all policies from MongoDB.
     *
     * This method queries MongoDB for all policy documents.
     *
     * @return a list of all policies, or an empty list if no policies exist
     */
    public List<Policy> getAllPolicies() {
        return repository.findAll().stream()
            .map(policy -> (Policy) policy)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves all policies associated with a specific role.
     *
     * This method queries roles that have the specified role, then returns
     * all associated policies. Since the relationship is maintained on the
     * Role side, we need to query through the role DAO.
     *
     * @param roleName the name of the role to filter policies by
     * @return a list of policies associated with the given role, or an empty list
     */
    public List<Policy> getAllPoliciesWithRole(String roleName) {
        if (roleName == null || roleName.isEmpty()) {
            return Collections.emptyList();
        }

        if (mongoRoleDao == null) {
            logger.warn("MongoRoleDao not set, cannot query policies by role");
            return Collections.emptyList();
        }

        // Get the role to find its policy IDs
        Role role = mongoRoleDao.getRoleByName(roleName);
        if (role == null || role.getPolicies() == null) {
            return Collections.emptyList();
        }

        // Return the policies from the role
        return new ArrayList<>(role.getPolicies());
    }

    /**
     * Retrieves a policy by its exact name.
     *
     * This method performs an exact match query on the policy name field.
     *
     * @param name the exact name of the policy to retrieve
     * @return the Policy object if found, or null if no policy exists with the given name
     */
    public Policy getPolicyByName(String name) {
        return repository.findByName(name)
            .map(policy -> (Policy) policy)
            .orElse(null);
    }

    /**
     * Retrieves policies whose names contain the specified search term.
     *
     * This method performs a case-insensitive wildcard search on policy names.
     * The search matches any policy name that contains the given substring.
     *
     * @param name the search term to match against policy names
     * @return a list of policies whose names contain the search term, or an empty list if no matches found
     */
    public List<Policy> getPolicyByNameLike(String name) {
        return repository.findByNameContainingIgnoreCase(name).stream()
            .map(policy -> (Policy) policy)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves a Policy by its unique identifier.
     *
     * The identifier is the policy ID, which queries MongoDB directly.
     *
     * @param id the unique identifier of the policy to retrieve
     * @return the Policy object if a matching record is found, or null if no record exists for the provided identifier
     */
    public Policy getPolicyById(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }

        return repository.findById(id)
            .map(policy -> (Policy) policy)
            .orElse(null);
    }

    /**
     * Converts a Policy to a MongoPolicyImpl.
     *
     * @param policy the policy to convert
     * @return the MongoPolicyImpl
     */
    private MongoPolicyImpl convertToMongoPolicy(Policy policy) {
        MongoPolicyImpl mongoPolicy = new MongoPolicyImpl();
        mongoPolicy.setId(policy.getId());
        mongoPolicy.setName(policy.getName());
        mongoPolicy.setDescription(policy.getDescription());
        mongoPolicy.setCreatedDateTime(policy.getCreatedDateTime());
        mongoPolicy.setUpdatedDateTime(policy.getUpdatedDateTime());
        return mongoPolicy;
    }
}
