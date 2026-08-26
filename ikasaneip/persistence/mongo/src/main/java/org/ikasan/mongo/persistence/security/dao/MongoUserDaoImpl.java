package org.ikasan.mongo.persistence.security.dao;

import org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalFilterImpl;
import org.ikasan.mongo.persistence.security.model.MongoUserImpl;
import org.ikasan.mongo.persistence.security.model.MongoUserRecord;
import org.ikasan.mongo.persistence.security.repository.MongoUserRepository;
import org.ikasan.mongo.persistence.security.util.MongoSecurityObjectMapperFactory;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.security.dao.UserDao;
import org.ikasan.spec.security.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of UserDao.
 *
 * This DAO provides methods to perform CRUD operations on users stored in MongoDB.
 *
 * @author Ikasan Development Team
 */
public class MongoUserDaoImpl implements UserDao {

    private static final JsonMapper OBJECT_MAPPER = MongoSecurityObjectMapperFactory.newInstance();

    private static final Logger logger = LoggerFactory.getLogger(MongoUserDaoImpl.class);
    private static final long DO_NOT_EXPIRE = -1L;

    private final MongoUserRepository repository;
    private final MongoTemplate mongoTemplate;
    private final MongoIkasanPrincipalDaoImpl ikasanPrincipalDao;

    /**
     * Constructor for MongoUserDaoImpl.
     *
     * @param repository the MongoDB repository for user persistence
     * @param mongoTemplate the MongoTemplate for custom queries
     * @param ikasanPrincipalDao the IkasanPrincipal DAO for querying principals
     */
    public MongoUserDaoImpl(MongoUserRepository repository, MongoTemplate mongoTemplate,
                            MongoIkasanPrincipalDaoImpl ikasanPrincipalDao) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.ikasanPrincipalDao = ikasanPrincipalDao;
    }

    @Override
    public User createUser(String username, String password, String email, boolean enabled) {
        return new MongoUserImpl(username, password, email, enabled);
    }

    @Override
    public List<UserLite> getUsersWithRole(String roleName, UserFilter userFilter, int limit, int offset) {
        IkasanPrincipalFilter ikasanPrincipalFilter = createPrincipalFilter("user");
        List<String> principalNames = this.ikasanPrincipalDao
            .getAllPrincipalNamesWithRole(roleName, ikasanPrincipalFilter, limit, offset);

        if (principalNames.isEmpty()) {
            return Collections.emptyList();
        }

        return getUsersByPrincipalNames(principalNames);
    }

    public List<String> getUserNamesWithRole(String roleName, UserFilter userFilter, int limit, int offset) {
        IkasanPrincipalFilter ikasanPrincipalFilter = createPrincipalFilter("user");
        List<String> principalNames = this.ikasanPrincipalDao
            .getAllPrincipalNamesWithRole(roleName, ikasanPrincipalFilter, limit, offset);

        if (principalNames.isEmpty()) {
            return Collections.emptyList();
        }

        return getUserNamesByPrincipalNames(principalNames);
    }

    public List<User> getUsersAssociatedWithPrincipal(String principalId) {
        IkasanPrincipal ikasanPrincipal = this.ikasanPrincipalDao.findById(principalId);
        if (ikasanPrincipal == null) {
            return Collections.emptyList();
        }

        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.PRINCIPAL_RELATED_ENTITY_COLLECTION).in(principalId));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(USER_TYPE));

        List<MongoUserRecord> users = mongoTemplate.find(query, MongoUserRecord.class);
        return users.stream()
            .map(this::loadPrincipals)
            .collect(Collectors.toList());
    }

    @Override
    public int getUsersWithRoleCount(String roleName, UserFilter userFilter) {
        IkasanPrincipalFilter ikasanPrincipalFilter = createPrincipalFilter("user");
        List<IkasanPrincipalLite> principals = this.ikasanPrincipalDao
            .getAllPrincipalsWithRole(roleName, ikasanPrincipalFilter, -1, -1);

        if (principals.isEmpty()) {
            return 0;
        }

        List<String> principalNames = principals.stream()
            .map(IkasanPrincipalLite::getName)
            .collect(Collectors.toList());

        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.NAME).in(principalNames));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(USER_TYPE));

        return (int) mongoTemplate.count(query, MongoUserRecord.class);
    }

    @Override
    public List<UserLite> getUsersWithoutRole(String roleName, UserFilter userFilter, int limit, int offset) {
        IkasanPrincipalFilter ikasanPrincipalFilter = createPrincipalFilter("user");
        List<String> principalNames = this.ikasanPrincipalDao
            .getAllPrincipalNamesWithoutRole(roleName, ikasanPrincipalFilter, limit, offset);

        if (principalNames.isEmpty()) {
            return Collections.emptyList();
        }

        return getUsersByPrincipalNames(principalNames);
    }

    @Override
    public int getUsersWithoutRoleCount(String roleName, UserFilter userFilter) {
        IkasanPrincipalFilter ikasanPrincipalFilter = createPrincipalFilter("user");
        List<IkasanPrincipalLite> principals = this.ikasanPrincipalDao
            .getAllPrincipalsWithoutRole(roleName, ikasanPrincipalFilter, -1, -1);

        if (principals.isEmpty()) {
            return 0;
        }

        List<String> principalNames = principals.stream()
            .map(IkasanPrincipalLite::getName)
            .collect(Collectors.toList());

        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.NAME).in(principalNames));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(USER_TYPE));

        return (int) mongoTemplate.count(query, MongoUserRecord.class);
    }

    @Override
    public int getUserCount(UserFilter userFilter) {
        Query query = new Query();
        addUserFilterCriteria(query, userFilter);
        return (int) mongoTemplate.count(query, MongoUserRecord.class);
    }

    @Override
    public List<User> getUsers() {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(USER_TYPE));

        return mongoTemplate.find(query, MongoUserRecord.class).stream()
            .map(this::loadPrincipals)
            .collect(Collectors.toList());
    }

    @Override
    public List<User> getUsers(UserFilter userFilter, int limit, int offset) {
        Query query = new Query();
        addUserFilterCriteria(query, userFilter);

        if (limit > 0) {
            query.limit(limit);
        }
        if (offset > 0) {
            query.skip(offset);
        }

        return mongoTemplate.find(query, MongoUserRecord.class).stream()
            .map(this::loadPrincipals)
            .collect(Collectors.toList());
    }

    @Override
    public List<UserLite> getUserLites() {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(USER_TYPE));

        return mongoTemplate.find(query, MongoUserRecord.class).stream()
            .map(userRecord -> OBJECT_MAPPER.readValue(userRecord.getUser(), MongoUserImpl.class))
            .map(user -> (UserLite) user)
            .collect(Collectors.toList());
    }

    @Override
    public List<UserLite> getUserLites(int limit, int offset) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(USER_TYPE));

        Pageable pageable = PageRequest.of(offset / limit, limit);
        query.with(pageable);

        return mongoTemplate.find(query, MongoUserRecord.class).stream()
            .map(userRecord -> OBJECT_MAPPER.readValue(userRecord.getUser(), MongoUserImpl.class))
            .map(user -> (UserLite) user)
            .collect(Collectors.toList());
    }

    @Override
    public User getUser(String username) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.NAME).is(username));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(USER_TYPE));

        MongoUserRecord result = mongoTemplate.findOne(query, MongoUserRecord.class);
        if (result == null) {
            return null;
        }
        return loadPrincipals(result);
    }

    @Override
    public List<User> getUserByUsernameLike(String username) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.NAME).regex(".*" + username + ".*", "i"));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(USER_TYPE));

        return mongoTemplate.find(query, MongoUserRecord.class).stream()
            .map(this::loadPrincipals)
            .collect(Collectors.toList());
    }

    @Override
    public List<User> getUserByFirstnameLike(String firstname) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.FIRST_NAME).regex(".*" + firstname + ".*", "i"));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(USER_TYPE));

        return mongoTemplate.find(query, MongoUserRecord.class).stream()
            .map(this::loadPrincipals)
            .collect(Collectors.toList());
    }

    @Override
    public List<User> getUserBySurnameLike(String surname) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.SURNAME).regex(".*" + surname + ".*", "i"));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(USER_TYPE));

        return mongoTemplate.find(query, MongoUserRecord.class).stream()
            .map(this::loadPrincipals)
            .collect(Collectors.toList());
    }

    @Override
    public void save(User user) {
        MongoUserRecord mongoUser = convertToMongoUser(user);

        long currentTime = System.currentTimeMillis();
        mongoUser.setModifiedTimestamp(currentTime);

        // Set created timestamp only if this is a new user
        if (mongoUser.getTimestamp() == 0) {
            repository.findByUsername(user.getUsername()).ifPresentOrElse(
                existing -> mongoUser.setTimestamp(existing.getTimestamp()),
                () -> mongoUser.setTimestamp(currentTime)
            );
        }

        mongoUser.setExpiry(DO_NOT_EXPIRE);

        // Update principal IDs from principals
        if (user.getPrincipals() != null) {
            List<String> principalIds = user.getPrincipals().stream()
                .map(p -> p.getId().toString())
                .collect(Collectors.toList());
            mongoUser.setRelatedPrincipalIdentifiers(principalIds);
        }

        repository.save(mongoUser);
        logger.debug("Saved user with username: {}", user.getUsername());
    }

    @Override
    public void delete(User user) {
        Query query = new Query();
        query.addCriteria(Criteria.where("username").is(user.getUsername()));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(USER_TYPE));

        mongoTemplate.remove(query, MongoUserRecord.class);
        logger.debug("Deleted user with username: {}", user.getUsername());
    }

    /**
     * Delete users with expiry less than the current timestamp.
     */
    public void deleteExpired() {
        long currentTime = System.currentTimeMillis();
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.EXPIRY).lt(currentTime));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(USER_TYPE));

        mongoTemplate.remove(query, MongoUserRecord.class);
        logger.debug("Deleted expired users before timestamp: {}", currentTime);
    }


    /**
     * Loads and associates principals to a user based on the provided MongoUserRecord.
     *
     * @param userRecord the MongoUserRecord containing the user data and principal identifiers
     * @return a User object with the associated principals
     */
    private User loadPrincipals(MongoUserRecord userRecord) {
        MongoUserImpl user = OBJECT_MAPPER.readValue(userRecord.getUser(), MongoUserImpl.class);

        if(userRecord.getRelatedPrincipalIdentifiers() != null) {
            userRecord.getRelatedPrincipalIdentifiers().forEach(principalId
                -> user.addPrincipal(this.ikasanPrincipalDao.findById(principalId)));
        }

        return user;
    }

    /**
     * Converts a User to a MongoUserImpl.
     *
     * @param user the user to convert
     * @return the MongoUserImpl
     */
    private MongoUserRecord convertToMongoUser(User user) {
        MongoUserRecord record = new MongoUserRecord();
        record.setId(user.getUsername() + "-" + USER_TYPE);
        record.setType(USER_TYPE);
        record.setUsername(user.getUsername());
        record.setEmail(user.getEmail());
        record.setFirstName(user.getFirstName());
        record.setSurname(user.getSurname());
        record.setDepartment(user.getDepartment());
        record.setTimestamp(System.currentTimeMillis());
        record.setModifiedTimestamp(System.currentTimeMillis());
        record.setExpiry(DO_NOT_EXPIRE);
        record.setRelatedPrincipalIdentifiers(user.getPrincipals().stream()
            .map(IkasanPrincipal::getId)
            .map(String::valueOf)
            .collect(Collectors.toList()));

        try {
            record.setUser(OBJECT_MAPPER.writeValueAsString(user));
        } catch (JacksonException e) {
            throw new RuntimeException("Cannot convert User to string! [" + user.getUsername() + "]", e);
        }

        return record;
    }

    /**
     * Retrieves users by their principal names.
     *
     * @param principalNames list of principal names
     * @return list of UserLite objects
     */
    private List<UserLite> getUsersByPrincipalNames(List<String> principalNames) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.NAME).in(principalNames));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(USER_TYPE));

        return mongoTemplate.find(query, MongoUserRecord.class).stream()
            .map(userRecord -> OBJECT_MAPPER.readValue(userRecord.getUser(), MongoUserImpl.class))
            .map(user -> (UserLite) user)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves user names by their principal names.
     *
     * @param principalNames list of principal names
     * @return list of usernames
     */
    private List<String> getUserNamesByPrincipalNames(List<String> principalNames) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.NAME).in(principalNames));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(USER_TYPE));
        query.fields().include(EntityFields.NAME);

        return mongoTemplate.find(query, MongoUserImpl.class).stream()
            .map(MongoUserImpl::getUsername)
            .collect(Collectors.toList());
    }

    /**
     * Creates a principal filter with the specified type.
     *
     * @param type the principal type
     * @return an IkasanPrincipalFilter
     */
    private IkasanPrincipalFilter createPrincipalFilter(String type) {
        MongoIkasanPrincipalFilterImpl filter = new MongoIkasanPrincipalFilterImpl();
        filter.setTypeFilter(type);

        return filter;
    }

    /**
     * Adds user filter criteria to a MongoDB query.
     *
     * @param query the query to modify
     * @param filter the user filter
     */
    private void addUserFilterCriteria(Query query, UserFilter filter) {
        // Always add type filter to ensure we only query user documents
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(USER_TYPE));

        if (filter == null) {
            return;
        }

        if (filter.getUsernameFilter() != null && !filter.getUsernameFilter().isEmpty()) {
            query.addCriteria(Criteria.where(EntityFields.NAME).regex(filter.getUsernameFilter(), "i"));
        }

        if (filter.getEmailFilter() != null && !filter.getEmailFilter().isEmpty()) {
            query.addCriteria(Criteria.where(EntityFields.EMAIL).regex(filter.getEmailFilter(), "i"));
        }

        if (filter.getNameFilter() != null && !filter.getNameFilter().isEmpty()) {
            query.addCriteria(Criteria.where(EntityFields.FIRST_NAME).regex(filter.getNameFilter(), "i"));
        }

        if (filter.getLastNameFilter() != null && !filter.getLastNameFilter().isEmpty()) {
            query.addCriteria(Criteria.where(EntityFields.SURNAME).regex(filter.getLastNameFilter(), "i"));
        }

        if (filter.getDepartmentFilter() != null && !filter.getDepartmentFilter().isEmpty()) {
            query.addCriteria(Criteria.where(EntityFields.DEPARTMENT).regex(filter.getDepartmentFilter(), "i"));
        }
    }
}
