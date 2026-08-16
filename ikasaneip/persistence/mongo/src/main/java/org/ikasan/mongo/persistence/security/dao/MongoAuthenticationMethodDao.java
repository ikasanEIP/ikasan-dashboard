package org.ikasan.mongo.persistence.security.dao;

import org.ikasan.mongo.persistence.security.model.MongoAuthenticationMethodImpl;
import org.ikasan.mongo.persistence.security.repository.MongoAuthenticationMethodRepository;
import org.ikasan.spec.security.model.AuthenticationMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of AuthenticationMethod DAO.
 *
 * This DAO provides methods to perform CRUD operations on authentication methods stored in MongoDB.
 * Authentication methods are stored as MongoAuthenticationMethodImpl documents with direct object storage
 * (no JSON serialization needed).
 *
 * Key features:
 * <ul>
 *   <li>Authentication method storage and retrieval using MongoDB</li>
 *   <li>Direct object persistence (no JSON serialization)</li>
 *   <li>Support for ordering authentication methods</li>
 *   <li>Query by order for authentication method priority</li>
 * </ul>
 *
 * @author Ikasan Development Team
 */
public class MongoAuthenticationMethodDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoAuthenticationMethodDao.class);

    private final MongoAuthenticationMethodRepository repository;
    private final MongoTemplate mongoTemplate;

    /**
     * Constructor for MongoAuthenticationMethodDao.
     *
     * @param repository the MongoDB repository for authentication method persistence
     * @param mongoTemplate the MongoTemplate for custom queries
     */
    public MongoAuthenticationMethodDao(MongoAuthenticationMethodRepository repository,
                                        MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Creates a new empty AuthenticationMethod instance.
     *
     * @return a new MongoAuthenticationMethodImpl instance
     */
    public AuthenticationMethod createAuthenticationMethod() {
        return new MongoAuthenticationMethodImpl();
    }

    /**
     * Saves or updates an authentication method in MongoDB.
     *
     * This method persists the AuthenticationMethod object directly to MongoDB.
     * If the authentication method already exists (same ID or name), it will be updated.
     * The authentication method is identified by its name, which must be unique.
     *
     * @param authenticationMethod the authentication method to save or update
     */
    public void saveOrUpdateAuthenticationMethod(AuthenticationMethod authenticationMethod) {
        MongoAuthenticationMethodImpl mongoAuthMethod;

        if (authenticationMethod instanceof MongoAuthenticationMethodImpl) {
            mongoAuthMethod = (MongoAuthenticationMethodImpl) authenticationMethod;
        } else {
            mongoAuthMethod = convertToMongoAuthenticationMethod(authenticationMethod);
        }

        // Generate ID from name if not set
        if (mongoAuthMethod.getId() == null && mongoAuthMethod.getName() != null) {
            mongoAuthMethod.setId(mongoAuthMethod.getName());
        }

        // Set lastSynchronised if not set
        if (mongoAuthMethod.getLastSynchronised() == null) {
            mongoAuthMethod.setLastSynchronised(new Date());
        }

        repository.save(mongoAuthMethod);
        logger.debug("Saved authentication method with name: {}", authenticationMethod.getName());
    }

    /**
     * Retrieves an authentication method by its ID.
     *
     * @param id the unique identifier of the authentication method
     * @return the authentication method, or null if not found
     */
    public AuthenticationMethod getAuthenticationMethod(Object id) {
        if (id == null) {
            return null;
        }

        String idStr = id.toString();
        return repository.findById(idStr)
            .map(authMethod -> (AuthenticationMethod) authMethod)
            .orElse(null);
    }

    /**
     * Retrieves all authentication methods from MongoDB.
     *
     * @return a list of all authentication methods, ordered by their order field
     */
    public List<AuthenticationMethod> getAuthenticationMethods() {
        return repository.findAllByOrderByOrderAsc().stream()
            .map(authMethod -> (AuthenticationMethod) authMethod)
            .collect(Collectors.toList());
    }

    /**
     * Gets the total count of authentication methods in MongoDB.
     *
     * @return the number of authentication methods
     */
    public long getNumberOfAuthenticationMethods() {
        return repository.count();
    }

    /**
     * Retrieves an authentication method by its order/priority.
     *
     * Authentication methods can be ordered to define their priority. This method
     * retrieves the authentication method with the specified order value.
     *
     * @param order the order/priority of the authentication method
     * @return the authentication method with the specified order, or null if not found
     */
    public AuthenticationMethod getAuthenticationMethodByOrder(long order) {
        Query query = new Query();
        query.addCriteria(Criteria.where("order").is(order));
        query.with(Sort.by(Sort.Direction.ASC, "order"));

        MongoAuthenticationMethodImpl result = mongoTemplate.findOne(query, MongoAuthenticationMethodImpl.class);
        return result;
    }

    /**
     * Deletes an authentication method from MongoDB.
     *
     * The authentication method is identified by its name. This operation
     * removes the authentication method document from MongoDB permanently.
     *
     * @param authenticationMethod the authentication method to delete
     */
    public void deleteAuthenticationMethod(AuthenticationMethod authenticationMethod) {
        repository.deleteByName(authenticationMethod.getName());
        logger.debug("Deleted authentication method with name: {}", authenticationMethod.getName());
    }

    /**
     * Converts an AuthenticationMethod to a MongoAuthenticationMethodImpl.
     *
     * @param authenticationMethod the authentication method to convert
     * @return the MongoAuthenticationMethodImpl
     */
    private MongoAuthenticationMethodImpl convertToMongoAuthenticationMethod(AuthenticationMethod authenticationMethod) {
        MongoAuthenticationMethodImpl mongoAuthMethod = new MongoAuthenticationMethodImpl();
        mongoAuthMethod.setId(authenticationMethod.getId());
        mongoAuthMethod.setName(authenticationMethod.getName());
        mongoAuthMethod.setMethod(authenticationMethod.getMethod());
        mongoAuthMethod.setOrder(authenticationMethod.getOrder());
        mongoAuthMethod.setEnabled(authenticationMethod.isEnabled());
        mongoAuthMethod.setScheduled(authenticationMethod.isScheduled());
        mongoAuthMethod.setLastSynchronised(authenticationMethod.getLastSynchronised());
        mongoAuthMethod.setLdapServerUrl(authenticationMethod.getLdapServerUrl());
        mongoAuthMethod.setLdapBindUserDn(authenticationMethod.getLdapBindUserDn());
        mongoAuthMethod.setLdapBindUserPassword(authenticationMethod.getLdapBindUserPassword());
        mongoAuthMethod.setLdapUserSearchBaseDn(authenticationMethod.getLdapUserSearchBaseDn());
        mongoAuthMethod.setLdapUserSearchFilter(authenticationMethod.getLdapUserSearchFilter());
        mongoAuthMethod.setApplicationSecurityBaseDn(authenticationMethod.getApplicationSecurityBaseDn());
        mongoAuthMethod.setAccountTypeAttributeName(authenticationMethod.getAccountTypeAttributeName());
        mongoAuthMethod.setUserAccountMappingAttributeName(authenticationMethod.getUserAccountMappingAttributeName());
        mongoAuthMethod.setUserAccountNameAttributeName(authenticationMethod.getUserAccountNameAttributeName());
        mongoAuthMethod.setEmailAttributeName(authenticationMethod.getEmailAttributeName());
        mongoAuthMethod.setApplicationSecurityGroupAttributeName(authenticationMethod.getApplicationSecurityGroupAttributeName());
        mongoAuthMethod.setFirstNameAttributeName(authenticationMethod.getFirstNameAttributeName());
        mongoAuthMethod.setSurnameAttributeName(authenticationMethod.getSurnameAttributeName());
        mongoAuthMethod.setDepartmentAttributeName(authenticationMethod.getDepartmentAttributeName());
        mongoAuthMethod.setLdapUserDescriptionAttributeName(authenticationMethod.getLdapUserDescriptionAttributeName());
        mongoAuthMethod.setApplicationSecurityDescriptionAttributeName(authenticationMethod.getApplicationSecurityDescriptionAttributeName());
        mongoAuthMethod.setMemberofAttributeName(authenticationMethod.getMemberofAttributeName());
        mongoAuthMethod.setUserSynchronisationFilter(authenticationMethod.getUserSynchronisationFilter());
        mongoAuthMethod.setGroupSynchronisationFilter(authenticationMethod.getGroupSynchronisationFilter());
        mongoAuthMethod.setSynchronisationCronExpression(authenticationMethod.getSynchronisationCronExpression());
        return mongoAuthMethod;
    }
}
