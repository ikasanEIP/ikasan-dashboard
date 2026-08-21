package org.ikasan.mongo.persistence.security.dao;

import org.ikasan.mongo.persistence.security.model.MongoPolicyImpl;
import org.ikasan.mongo.persistence.security.model.MongoPolicyRecord;
import org.ikasan.mongo.persistence.security.repository.MongoPolicyRepository;
import org.ikasan.mongo.persistence.security.util.MongoSecurityObjectMapperFactory;
import org.ikasan.spec.security.dao.PolicyDao;
import org.ikasan.spec.security.model.Policy;
import org.ikasan.spec.security.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.ikasan.spec.entity.EntityFields.*;

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
public class MongoPolicyDaoImpl implements PolicyDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoPolicyDaoImpl.class);

    private static final JsonMapper OBJECT_MAPPER = MongoSecurityObjectMapperFactory.newInstance();
    private static final long DO_NOT_EXPIRE = -1L;

    private final MongoPolicyRepository repository;
    private final MongoTemplate mongoTemplate;
    private MongoRoleDaoImpl mongoRoleDaoImpl; // Circular dependency - will be set via setter

    /**
     * Constructor for MongoPolicyDaoImpl.
     *
     * @param repository the MongoDB repository for policy persistence
     * @param mongoTemplate the MongoTemplate for custom queries
     */
    public MongoPolicyDaoImpl(MongoPolicyRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Sets the MongoRoleDaoImpl for querying role relationships.
     * This is needed to avoid circular dependency issues.
     *
     * @param mongoRoleDaoImpl the role DAO
     */
    public void setMongoRoleDao(MongoRoleDaoImpl mongoRoleDaoImpl) {
        this.mongoRoleDaoImpl = mongoRoleDaoImpl;
    }

    /**
     * Creates a new empty Policy instance.
     *
     * @return a new MongoPolicyImpl instance
     */
    @Override
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
    @Override
    public void saveOrUpdatePolicy(Policy policy) {
        MongoPolicyRecord mongoPolicy = convertToMongoPolicy(policy);

        repository.save(mongoPolicy);
        logger.debug("Saved policy with name: {}", policy.getName());
    }

    /**
     * Saves or updates a policy in the MongoDB database.
     *
     * This method persists the provided MongoPolicyRecord object to the database.
     * If the policy already exists (based on its unique identifier or name), it
     * updates the existing record. Otherwise, it creates a new policy record.
     *
     * @param policy the MongoPolicyRecord instance to be saved or updated
     */
    public void saveOrUpdatePolicy(MongoPolicyRecord policy) {
        repository.save(policy);
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
    @Override
    public void deletePolicy(Policy policy) {
        Query query = new Query();
        query.addCriteria(Criteria.where(NAME).is(policy.getName()));
        query.addCriteria(Criteria.where(TYPE).is(POLICY_TYPE));

        mongoTemplate.remove(query, MongoPolicyRecord.class);
        logger.debug("Deleted policy with name: {}", policy.getName());
    }

    /**
     * Retrieves all policies from MongoDB.
     *
     * This method queries MongoDB for all policy documents.
     *
     * @return a list of all policies, or an empty list if no policies exist
     */
    @Override
    public List<Policy> getAllPolicies() {
        Query query = new Query();
        query.addCriteria(Criteria.where(TYPE).is(POLICY_TYPE));

        return mongoTemplate.find(query, MongoPolicyRecord.class).stream()
            .map(mongoPolicyRecord
                -> OBJECT_MAPPER.readValue(mongoPolicyRecord.getPolicy(), MongoPolicyImpl.class))
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
    @Override
    public List<Policy> getAllPoliciesWithRole(String roleName) {
        if (roleName == null || roleName.isEmpty()) {
            return Collections.emptyList();
        }

        if (mongoRoleDaoImpl == null) {
            logger.warn("MongoRoleDaoImpl not set, cannot query policies by role");
            return Collections.emptyList();
        }

        // Get the role to find its policy IDs
        Role role = mongoRoleDaoImpl.getRoleByName(roleName);
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
    @Override
    public Policy getPolicyByName(String name) {
        Query query = new Query();
        query.addCriteria(Criteria.where(NAME).is(name));
        query.addCriteria(Criteria.where(TYPE).is(POLICY_TYPE));

        MongoPolicyRecord result = mongoTemplate.findOne(query, MongoPolicyRecord.class);
        if (result != null) {
            return OBJECT_MAPPER.readValue(result.getPolicy(), MongoPolicyImpl.class);
        }
        return null;
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
    @Override
    public List<Policy> getPolicyByNameLike(String name) {
        Query query = new Query();
        query.addCriteria(Criteria.where(NAME).regex(name, "i"));
        query.addCriteria(Criteria.where(TYPE).is(POLICY_TYPE));

        return mongoTemplate.find(query, MongoPolicyRecord.class).stream()
            .map(mongoPolicyRecord
                -> OBJECT_MAPPER.readValue(mongoPolicyRecord.getPolicy(), MongoPolicyImpl.class))
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
    @Override
    public Policy getPolicyById(String id) {
        MongoPolicyRecord result = this.getPolicyRecordById(id);
        if (result != null) {
            return OBJECT_MAPPER.readValue(result.getPolicy(), MongoPolicyImpl.class);
        }
        return null;
    }

    /**
     * Retrieves a MongoPolicyRecord from the database using its unique identifier.
     * The method queries the database for a record with the specified ID and
     * verifies that the record is of type POLICY_TYPE before returning it.
     *
     * @param id the unique identifier of the MongoPolicyRecord to retrieve. It must not be null or empty.
     * @return the MongoPolicyRecord object if a matching record is found, or null if no record exists with the given ID or if the ID is invalid.
     */
    public MongoPolicyRecord getPolicyRecordById(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }

        Query query = new Query();
        query.addCriteria(Criteria.where(ID).is(id));
        query.addCriteria(Criteria.where(TYPE).is(POLICY_TYPE));

        return mongoTemplate.findOne(query, MongoPolicyRecord.class);
    }

    /**
     * Converts a Policy to a MongoPolicyImpl.
     *
     * @param policy the policy to convert
     * @return the MongoPolicyImpl
     */
    private MongoPolicyRecord convertToMongoPolicy(Policy policy) {
        MongoPolicyRecord record = new MongoPolicyRecord();
        record.setId(policy.getName() + "-" + POLICY_TYPE);
        policy.setId(policy.getName() + "-" + POLICY_TYPE);
        record.setType(POLICY_TYPE);
        record.setName(policy.getName());
        record.setTimestamp(policy.getCreatedDateTime() != null ? policy.getCreatedDateTime().getTime()
            : System.currentTimeMillis());
        record.setModifiedTimestamp(policy.getUpdatedDateTime() != null ? policy.getUpdatedDateTime().getTime()
            : System.currentTimeMillis());
        record.setExpiry(DO_NOT_EXPIRE);

        try {
            record.setPolicy(OBJECT_MAPPER.writeValueAsString(policy));
        } catch (JacksonException e) {
            throw new RuntimeException("Cannot convert Policy to string! [" + policy.getName() + "]", e);
        }
        return record;
    }
}
