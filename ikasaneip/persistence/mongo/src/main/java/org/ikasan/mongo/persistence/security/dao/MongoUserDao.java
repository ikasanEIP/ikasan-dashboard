package org.ikasan.mongo.persistence.security.dao;

import org.ikasan.mongo.persistence.security.model.MongoUserImpl;
import org.ikasan.mongo.persistence.security.repository.MongoUserRepository;
import org.ikasan.spec.security.dao.UserDao;
import org.ikasan.spec.security.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
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
public class MongoUserDao implements UserDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoUserDao.class);
    private static final long DO_NOT_EXPIRE = -1L;

    private final MongoUserRepository repository;
    private final MongoTemplate mongoTemplate;
    private final MongoIkasanPrincipalDao ikasanPrincipalDao;

    /**
     * Constructor for MongoUserDao.
     *
     * @param repository the MongoDB repository for user persistence
     * @param mongoTemplate the MongoTemplate for custom queries
     * @param ikasanPrincipalDao the IkasanPrincipal DAO for querying principals
     */
    public MongoUserDao(MongoUserRepository repository, MongoTemplate mongoTemplate,
                        MongoIkasanPrincipalDao ikasanPrincipalDao) {
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

        List<MongoUserImpl> users = repository.findByPrincipalIdsContaining(principalId);
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
        query.addCriteria(Criteria.where("username").in(principalNames));

        return (int) mongoTemplate.count(query, MongoUserImpl.class);
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
        query.addCriteria(Criteria.where("username").in(principalNames));

        return (int) mongoTemplate.count(query, MongoUserImpl.class);
    }

    @Override
    public int getUserCount(UserFilter userFilter) {
        Query query = new Query();
        addUserFilterCriteria(query, userFilter);
        return (int) mongoTemplate.count(query, MongoUserImpl.class);
    }

    @Override
    public List<User> getUsers() {
        return repository.findAll().stream()
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

        return mongoTemplate.find(query, MongoUserImpl.class).stream()
            .map(this::loadPrincipals)
            .collect(Collectors.toList());
    }

    @Override
    public List<UserLite> getUserLites() {
        return repository.findAll().stream()
            .map(user -> (UserLite) user)
            .collect(Collectors.toList());
    }

    @Override
    public List<UserLite> getUserLites(int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        return repository.findAll(pageable).stream()
            .map(user -> (UserLite) user)
            .collect(Collectors.toList());
    }

    @Override
    public User getUser(String username) {
        return repository.findByUsername(username)
            .map(this::loadPrincipals)
            .orElse(null);
    }

    @Override
    public List<User> getUserByUsernameLike(String username) {
        return repository.findByUsernameContainingIgnoreCase(username).stream()
            .map(this::loadPrincipals)
            .collect(Collectors.toList());
    }

    @Override
    public List<User> getUserByFirstnameLike(String firstname) {
        return repository.findByFirstNameContainingIgnoreCase(firstname).stream()
            .map(this::loadPrincipals)
            .collect(Collectors.toList());
    }

    @Override
    public List<User> getUserBySurnameLike(String surname) {
        return repository.findBySurnameContainingIgnoreCase(surname).stream()
            .map(this::loadPrincipals)
            .collect(Collectors.toList());
    }

    @Override
    public void save(User user) {
        MongoUserImpl mongoUser;

        if (user instanceof MongoUserImpl) {
            mongoUser = (MongoUserImpl) user;
        } else {
            // Convert from another User implementation
            mongoUser = convertToMongoUser(user);
        }

        // Set id to username if not set
        if (mongoUser.getId() == null) {
            mongoUser.setId(user.getUsername());
        }

        long currentTime = System.currentTimeMillis();
        mongoUser.setModifiedTimestamp(currentTime);

        // Set created timestamp only if this is a new user
        if (mongoUser.getCreatedTimestamp() == 0) {
            repository.findByUsername(user.getUsername()).ifPresentOrElse(
                existing -> mongoUser.setCreatedTimestamp(existing.getCreatedTimestamp()),
                () -> mongoUser.setCreatedTimestamp(currentTime)
            );
        }

        mongoUser.setExpiry(DO_NOT_EXPIRE);

        // Update principal IDs from principals
        if (user.getPrincipals() != null) {
            List<String> principalIds = user.getPrincipals().stream()
                .map(p -> p.getId().toString())
                .collect(Collectors.toList());
            mongoUser.setPrincipalIds(principalIds);
        }

        repository.save(mongoUser);
        logger.debug("Saved user with username: {}", user.getUsername());
    }

    @Override
    public void delete(User user) {
        repository.deleteByUsername(user.getUsername());
        logger.debug("Deleted user with username: {}", user.getUsername());
    }

    /**
     * Delete users with expiry less than the current timestamp.
     */
    public void deleteExpired() {
        long currentTime = System.currentTimeMillis();
        repository.deleteByExpiryLessThan(currentTime);
        logger.debug("Deleted expired users before timestamp: {}", currentTime);
    }

    /**
     * Loads principals for a user from the principal DAO.
     *
     * @param user the user to load principals for
     * @return the user with principals loaded
     */
    private User loadPrincipals(MongoUserImpl user) {
        if (user.getPrincipalIds() != null && !user.getPrincipalIds().isEmpty()) {
            user.getPrincipalIds().forEach(principalId -> {
                IkasanPrincipal principal = this.ikasanPrincipalDao.findById(principalId);
                if (principal != null) {
                    user.addPrincipal(principal);
                }
            });
        }
        return user;
    }

    /**
     * Converts a User to a MongoUserImpl.
     *
     * @param user the user to convert
     * @return the MongoUserImpl
     */
    private MongoUserImpl convertToMongoUser(User user) {
        MongoUserImpl mongoUser = new MongoUserImpl();
        mongoUser.setId((String)user.getId());
        mongoUser.setUsername(user.getUsername());
        mongoUser.setPassword(user.getPassword());
        mongoUser.setEmail(user.getEmail());
        mongoUser.setFirstName(user.getFirstName());
        mongoUser.setSurname(user.getSurname());
        mongoUser.setDepartment(user.getDepartment());
        mongoUser.setEnabled(user.isEnabled());
        mongoUser.setAccountNonExpired(user.isAccountNonExpired());
        mongoUser.setAccountNonLocked(user.isAccountNonLocked());
        mongoUser.setCredentialsNonExpired(user.isCredentialsNonExpired());
        mongoUser.setRequiresPasswordChange(user.isRequiresPasswordChange());
        mongoUser.setPreviousAccessTimestamp(user.getPreviousAccessTimestamp());
        mongoUser.setPrincipals(user.getPrincipals());
        return mongoUser;
    }

    /**
     * Retrieves users by their principal names.
     *
     * @param principalNames list of principal names
     * @return list of UserLite objects
     */
    private List<UserLite> getUsersByPrincipalNames(List<String> principalNames) {
        Query query = new Query();
        query.addCriteria(Criteria.where("username").in(principalNames));

        return mongoTemplate.find(query, MongoUserImpl.class).stream()
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
        query.addCriteria(Criteria.where("username").in(principalNames));
        query.fields().include("username");

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
        // Simple filter implementation
        return new IkasanPrincipalFilter() {
            private String typeFilter = type;
            private String nameFilter;
            private String descriptionFilter;
            private String sortOrder;
            private String sortColumn;

            @Override
            public void setTypeFilter(String typeFilter) {
                this.typeFilter = typeFilter;
            }

            @Override
            public String getTypeFilter() {
                return typeFilter;
            }

            @Override
            public String getNameFilter() {
                return nameFilter;
            }

            @Override
            public void setNameFilter(String nameFilter) {
                this.nameFilter = nameFilter;
            }

            @Override
            public String getDescriptionFilter() {
                return descriptionFilter;
            }

            @Override
            public void setDescriptionFilter(String descriptionFilter) {
                this.descriptionFilter = descriptionFilter;
            }

            @Override
            public String getSortOrder() {
                return sortOrder;
            }

            @Override
            public void setSortOrder(String sortOrder) {
                this.sortOrder = sortOrder;
            }

            @Override
            public String getSortColumn() {
                return sortColumn;
            }

            @Override
            public void setSortColumn(String sortColumn) {
                this.sortColumn = sortColumn;
            }
        };
    }

    /**
     * Adds user filter criteria to a MongoDB query.
     *
     * @param query the query to modify
     * @param filter the user filter
     */
    private void addUserFilterCriteria(Query query, UserFilter filter) {
        if (filter == null) {
            return;
        }

        if (filter.getUsernameFilter() != null && !filter.getUsernameFilter().isEmpty()) {
            query.addCriteria(Criteria.where("username").regex(filter.getUsernameFilter(), "i"));
        }

        if (filter.getEmailFilter() != null && !filter.getEmailFilter().isEmpty()) {
            query.addCriteria(Criteria.where("email").regex(filter.getEmailFilter(), "i"));
        }

        if (filter.getNameFilter() != null && !filter.getNameFilter().isEmpty()) {
            query.addCriteria(Criteria.where("firstName").regex(filter.getNameFilter(), "i"));
        }

        if (filter.getLastNameFilter() != null && !filter.getLastNameFilter().isEmpty()) {
            query.addCriteria(Criteria.where("surname").regex(filter.getLastNameFilter(), "i"));
        }

        if (filter.getDepartmentFilter() != null && !filter.getDepartmentFilter().isEmpty()) {
            query.addCriteria(Criteria.where("department").regex(filter.getDepartmentFilter(), "i"));
        }
    }
}
