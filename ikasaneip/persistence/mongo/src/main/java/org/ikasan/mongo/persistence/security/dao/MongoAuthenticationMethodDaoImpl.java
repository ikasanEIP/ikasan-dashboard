package org.ikasan.mongo.persistence.security.dao;

import org.ikasan.mongo.persistence.security.model.MongoAuthenticationMethodImpl;
import org.ikasan.mongo.persistence.security.model.MongoAuthenticationMethodRecord;
import org.ikasan.mongo.persistence.security.repository.MongoAuthenticationMethodRepository;
import org.ikasan.mongo.persistence.security.util.MongoSecurityObjectMapperFactory;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.security.dao.AuthenticationMethodDao;
import org.ikasan.spec.security.model.AuthenticationMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

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
public class MongoAuthenticationMethodDaoImpl implements AuthenticationMethodDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoAuthenticationMethodDaoImpl.class);

    /** Jackson JsonMapper for JSON serialization/deserialization */
    private static final JsonMapper OBJECT_MAPPER = MongoSecurityObjectMapperFactory.newInstance();
    private static final long DO_NOT_EXPIRE = -1L;

    private final MongoAuthenticationMethodRepository repository;
    private final MongoTemplate mongoTemplate;

    /**
     * Constructor for MongoAuthenticationMethodDaoImpl.
     *
     * @param repository the MongoDB repository for authentication method persistence
     * @param mongoTemplate the MongoTemplate for custom queries
     */
    public MongoAuthenticationMethodDaoImpl(MongoAuthenticationMethodRepository repository,
                                            MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Creates a new empty AuthenticationMethod instance.
     *
     * @return a new MongoAuthenticationMethodImpl instance
     */
    @Override
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
    @Override
    public void saveOrUpdateAuthenticationMethod(AuthenticationMethod authenticationMethod) {
        MongoAuthenticationMethodRecord mongoAuthMethod = convertToMongoAuthenticationMethod(authenticationMethod);

        repository.save(mongoAuthMethod);
        logger.debug("Saved authentication method with name: {}", authenticationMethod.getName());
    }

    /**
     * Retrieves an authentication method by its ID.
     *
     * @param id the unique identifier of the authentication method
     * @return the authentication method, or null if not found
     */
    @Override
    public AuthenticationMethod getAuthenticationMethod(Object id) {
        if (id == null) {
            return null;
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id.toString()));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(AUTHENTICATION_METHOD_TYPE));

        MongoAuthenticationMethodRecord result = mongoTemplate.findOne(query, MongoAuthenticationMethodRecord.class);
        if (result == null) {
            return null;
        }
        return OBJECT_MAPPER.readValue(result.getAuthenticationMethod(), MongoAuthenticationMethodImpl.class);
    }

    /**
     * Retrieves all authentication methods from MongoDB.
     *
     * @return a list of all authentication methods, ordered by their order field
     */
    @Override
    public List<AuthenticationMethod> getAuthenticationMethods() {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(AUTHENTICATION_METHOD_TYPE));
        query.with(Sort.by(Sort.Direction.ASC, "order"));

        List<MongoAuthenticationMethodRecord> results = mongoTemplate.find(query, MongoAuthenticationMethodRecord.class);
        return results.stream()
            .map(authMethod -> OBJECT_MAPPER
                .readValue(authMethod.getAuthenticationMethod(), MongoAuthenticationMethodImpl.class))
            .collect(Collectors.toList());
    }

    /**
     * Gets the total count of authentication methods in MongoDB.
     *
     * @return the number of authentication methods
     */
    @Override
    public long getNumberOfAuthenticationMethods() {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(AUTHENTICATION_METHOD_TYPE));

        return mongoTemplate.count(query, MongoAuthenticationMethodRecord.class);
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
    @Override
    public AuthenticationMethod getAuthenticationMethodByOrder(long order) {
        Query query = new Query();
        query.addCriteria(Criteria.where("order").is(order));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(AUTHENTICATION_METHOD_TYPE));
        query.with(Sort.by(Sort.Direction.ASC, "order"));

        MongoAuthenticationMethodRecord result = mongoTemplate.findOne(query, MongoAuthenticationMethodRecord.class);
        if(result == null) return null;
        return OBJECT_MAPPER.readValue(result.getAuthenticationMethod(), MongoAuthenticationMethodImpl.class);
    }

    /**
     * Deletes an authentication method from MongoDB.
     *
     * The authentication method is identified by its name. This operation
     * removes the authentication method document from MongoDB permanently.
     *
     * @param authenticationMethod the authentication method to delete
     */
    @Override
    public void deleteAuthenticationMethod(AuthenticationMethod authenticationMethod) {
        Query query = new Query();
        query.addCriteria(Criteria.where("name").is(authenticationMethod.getName()));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(AUTHENTICATION_METHOD_TYPE));

        mongoTemplate.remove(query, MongoAuthenticationMethodRecord.class);
        logger.debug("Deleted authentication method with name: {}", authenticationMethod.getName());
    }

    /**
     * Converts an AuthenticationMethod to a MongoAuthenticationMethodImpl.
     *
     * @param authenticationMethod the authentication method to convert
     * @return the MongoAuthenticationMethodImpl
     */
    private MongoAuthenticationMethodRecord convertToMongoAuthenticationMethod(AuthenticationMethod authenticationMethod) {
        MongoAuthenticationMethodRecord record = new MongoAuthenticationMethodRecord();
        record.setId(authenticationMethod.getName() + "-" + AUTHENTICATION_METHOD_TYPE);
        record.setType(AUTHENTICATION_METHOD_TYPE);
        record.setName(authenticationMethod.getName());
        record.setOrder(authenticationMethod.getOrder());
        record.setTimestamp(authenticationMethod.getLastSynchronised() != null
            ? authenticationMethod.getLastSynchronised().getTime()
            : System.currentTimeMillis());
        record.setModifiedTimestamp(System.currentTimeMillis());
        record.setExpiry(DO_NOT_EXPIRE);
        try {
            record.setAuthenticationMethod(OBJECT_MAPPER.writeValueAsString(authenticationMethod));
        } catch (JacksonException e) {
            throw new RuntimeException("Cannot convert AuthenticationMethod to string! ["
                + authenticationMethod.getName() + "]", e);
        }

        return record;
    }
}
