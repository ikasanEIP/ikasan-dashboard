package org.ikasan.security.dao;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.security.model.SolrIkasanPrincipalFilterImpl;
import org.ikasan.security.model.SolrUserImpl;
import org.ikasan.security.model.SolrUserLiteImpl;
import org.ikasan.security.model.SolrUserRecord;
import org.ikasan.security.util.SolrSecurityObjectMapperFactory;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.security.dao.UserDao;
import org.ikasan.spec.security.model.*;
import org.ikasan.spec.solr.SolrDaoBase;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Solr-based Data Access Object implementation for managing users.
 *
 * This DAO provides methods to perform CRUD operations on users stored
 * in a Solr index. Users are serialized to JSON and stored as SolrUserRecord
 * documents with the type "securityUser".
 *
 * Key features:
 * <ul>
 *   <li>User storage and retrieval using Solr</li>
 *   <li>JSON serialization/deserialization of User objects</li>
 *   <li>Wildcard search capabilities by username, firstname, and surname</li>
 *   <li>Support for user filtering and pagination</li>
 *   <li>Lightweight UserLite objects for list operations</li>
 *   <li>Related principal identifiers for efficient querying</li>
 * </ul>
 *
 * @author Ikasan Development Team
 */
public class SolrUserDaoImpl extends SolrDaoBase<SolrUserRecord> implements UserDao {

    /** The Solr document type identifier for users */
    public static final String USER_TYPE = "securityUser";

    /** Jackson JsonMapper for JSON serialization/deserialization */
    private static final JsonMapper OBJECT_MAPPER = SolrSecurityObjectMapperFactory.newInstance();

    /**
     * Maximum number of predicates allowed in a single Solr query.
     * Solr has a limit of 1024 boolean clauses per query.
     */
    private static final int MAX_PREDICATES_PER_QUERY = 512;

    /**
     * An instance of SolrIkasanPrincipalDaoImpl used for managing Ikasan principal data.
     * This DAO (Data Access Object) provides methods to perform CRUD operations
     * and query execution related to Ikasan principal entities within a Solr datastore.
     */
    private final SolrIkasanPrincipalDaoImpl ikasanPrincipalDao;

    /**
     * Constructor for SolrUserDaoImpl.
     *
     * @param ikasanPrincipalDao the IkasanPrincipal DAO implementation for querying principals with roles
     */
    public SolrUserDaoImpl(SolrIkasanPrincipalDaoImpl ikasanPrincipalDao) {
        this.ikasanPrincipalDao = ikasanPrincipalDao;
    }

    /**
     * Converts a SolrUserRecord entity into a Solr input document for indexing.
     *
     * This method maps the user record fields to Solr document fields, including:
     * <ul>
     *   <li>ID: Composite key of username and type</li>
     *   <li>TYPE: Document type identifier (securityUser)</li>
     *   <li>NAME: Username for searching</li>
     *   <li>EMAIL: User email address</li>
     *   <li>FIRST_NAME: User first name</li>
     *   <li>SURNAME: User surname</li>
     *   <li>DEPARTMENT: User department</li>
     *   <li>PRINCIPAL_RELATED_ENTITY_COLLECTION: Related principal identifiers</li>
     *   <li>CREATED_DATE_TIME: Original creation timestamp</li>
     *   <li>UPDATED_DATE_TIME: Current update timestamp</li>
     *   <li>EXPIRY: Document expiration time</li>
     *   <li>PAYLOAD_CONTENT: JSON serialized user object</li>
     * </ul>
     *
     * @param expiry the expiration timestamp for the document
     * @param event the SolrUserRecord to convert
     * @return a SolrInputDocument ready for indexing
     */
    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, SolrUserRecord event) {
        SolrInputDocument document = new SolrInputDocument();

        document.addField(ID, event.getUsername() + "-" + USER_TYPE);
        document.addField(TYPE, USER_TYPE);
        document.addField(NAME, event.getUsername());
        document.addField(EMAIL, event.getEmail());
        document.addField(FIRST_NAME, event.getFirstName());
        document.addField(SURNAME, event.getSurname());
        document.addField(DEPARTMENT, event.getDepartment());
        document.addField(PAYLOAD_CONTENT, event.getUser());
        document.addField(PRINCIPAL_RELATED_ENTITY_COLLECTION, event.getRelatedPrincipalIdentifiers());
        document.addField(CREATED_DATE_TIME, event.getTimestamp());
        document.addField(UPDATED_DATE_TIME, System.currentTimeMillis());
        document.setField(EXPIRY, DO_NOT_EXPIRE);

        return document;
    }

    /**
     * Creates a new User instance with the specified parameters.
     *
     * @param username the username of the user
     * @param password the password of the user
     * @param email the email address of the user
     * @param enabled whether the user is enabled
     * @return a new SolrUserImpl instance
     */
    @Override
    public User createUser(String username, String password, String email, boolean enabled) {
        return new SolrUserImpl(username, password, email, enabled);
    }

    /**
     * Retrieves users associated with a specific role.
     *
     * This method queries for users whose related principals have the specified role.
     * Results can be filtered and paginated.
     *
     * If there are more than 1024 principals, the query is broken into multiple batches
     * to avoid exceeding Solr's maximum boolean clause limit.
     *
     * @param roleName the name of the role to filter by
     * @param userFilter optional filter for additional user criteria
     * @param limit maximum number of results to return
     * @param offset offset for pagination
     * @return a list of lightweight UserLite objects
     */
    @Override
    public List<UserLite> getUsersWithRole(String roleName, UserFilter userFilter, int limit, int offset) {
        IkasanPrincipalFilter ikasanPrincipalFilter = new SolrIkasanPrincipalFilterImpl();
        ikasanPrincipalFilter.setTypeFilter("user");
        List principalNames = this.ikasanPrincipalDao
            .getAllPrincipalNamesWithRole(roleName, ikasanPrincipalFilter, limit, offset);

        if (principalNames.isEmpty()) {
            return Collections.emptyList();
        }

        // Build list of user IDs from principals
        List userIds = new ArrayList<>();
        principalNames.forEach(principalName
            -> userIds.add("\"" + principalName + "-" + USER_TYPE + "\""));

        // If userIds exceed the max predicates limit, break into batches
        if (userIds.size() > MAX_PREDICATES_PER_QUERY) {
            return getUsersInBatches(userIds);
        }

        // Single query for <= 1024 predicates
        return executeUserQuery(userIds);
    }

    /**
     * Efficiently retrieves only the usernames of users with a specific role.
     *
     * This method delegates to Solr to retrieve only the 'username' field, making it much more
     * efficient than retrieving full user objects when only usernames are needed.
     * It uses Solr's field list (fl) parameter to limit the fields returned.
     *
     * If there are more than 1024 principals, the query is broken into multiple batches
     * to avoid exceeding Solr's maximum boolean clause limit.
     *
     * @param roleName the name of the role to filter by
     * @param userFilter optional filter for additional user criteria
     * @param limit maximum number of results to return
     * @param offset offset for pagination
     * @return a list of usernames (strings) for users with the specified role
     */
    public List getUserNamesWithRole(String roleName, UserFilter userFilter, int limit, int offset) {
        IkasanPrincipalFilter ikasanPrincipalFilter = new SolrIkasanPrincipalFilterImpl();
        ikasanPrincipalFilter.setTypeFilter("user");

        // Use the efficient method that only returns principal names
        List principalNames = this.ikasanPrincipalDao
            .getAllPrincipalNamesWithRole(roleName, ikasanPrincipalFilter, limit, offset);

        if (principalNames.isEmpty()) {
            return Collections.emptyList();
        }

        // Build list of user IDs from principal names
        List userIds = new ArrayList<>();
        principalNames.forEach(name -> userIds.add("\"" + name + "-" + USER_TYPE + "\""));

        // If userIds exceed the max predicates limit, break into batches
        if (userIds.size() > MAX_PREDICATES_PER_QUERY) {
            return getUserNamesInBatches(userIds);
        }

        // Single query for <= 1024 predicates
        return executeUserNameQuery(userIds);
    }

    /**
     * Executes multiple batched queries for user name retrieval when the number of user IDs
     * exceeds the maximum predicates per query limit.
     *
     * @param userIds the list of user IDs to query for
     * @return a combined list of usernames from all batches
     */
    private List getUserNamesInBatches(List userIds) {
        List allUserNames = new ArrayList<>();
        int totalIds = userIds.size();

        // Process in batches of MAX_PREDICATES_PER_QUERY
        for (int i = 0; i < totalIds; i += MAX_PREDICATES_PER_QUERY) {
            int endIndex = Math.min(i + MAX_PREDICATES_PER_QUERY, totalIds);
            List batchIds = userIds.subList(i, endIndex);
            allUserNames.addAll(executeUserNameQuery(batchIds));
        }

        return allUserNames;
    }

    /**
     * Executes a single query to retrieve only usernames by their IDs.
     * Uses Solr's field list to retrieve only the username field for efficiency.
     *
     * @param userIds the list of user IDs to query for
     * @return a list of usernames matching the IDs
     */
    private List executeUserNameQuery(List userIds) {
        StringBuilder queryBuffer = new StringBuilder(TYPE + COLON + "\"" + USER_TYPE + "\"");

        queryBuffer.append(AND).append(ID + COLON).append(OPEN_BRACKET);
        queryBuffer.append(userIds.stream().collect(Collectors.joining(",")));
        queryBuffer.append(CLOSE_BRACKET);

        SolrQuery query = new SolrQuery();
        query.setQuery(queryBuffer.toString());
        query.setRows(Integer.MAX_VALUE);

        // Only retrieve the username field for efficiency
        query.setFields(NAME);

        SearchResults<SolrUserRecord> results = this.findByQuery(query, SolrUserRecord.class);

        return results.getResultList().stream()
            .map(SolrUserRecord::getUsername)
            .collect(Collectors.toList());
    }

    /**
     * Executes multiple batched queries for user retrieval when the number of user IDs
     * exceeds the maximum predicates per query limit.
     *
     * @param userIds the list of user IDs to query for
     * @return a combined list of UserLite objects from all batches
     */
    private List<UserLite> getUsersInBatches(List userIds) {
        List<UserLite> allUsers = new ArrayList<>();
        int totalIds = userIds.size();

        // Process in batches of MAX_PREDICATES_PER_QUERY
        for (int i = 0; i < totalIds; i += MAX_PREDICATES_PER_QUERY) {
            int endIndex = Math.min(i + MAX_PREDICATES_PER_QUERY, totalIds);
            List batchIds = userIds.subList(i, endIndex);
            allUsers.addAll(executeUserQuery(batchIds));
        }

        return allUsers;
    }

    /**
     * Calculates the total number of users by processing the given list of user IDs in batches.
     * Each batch is processed separately using the `executeUserCountQuery` method.
     *
     * @param userIds the list of user IDs to process
     * @return the total count of users corresponding to the provided user IDs
     */
    private int getUsersCountInBatches(List userIds) {
        List<UserLite> allUsers = new ArrayList<>();
        int totalIds = userIds.size();

        int count = 0;
        // Process in batches of MAX_PREDICATES_PER_QUERY
        for (int i = 0; i < totalIds; i += MAX_PREDICATES_PER_QUERY) {
            int endIndex = Math.min(i + MAX_PREDICATES_PER_QUERY, totalIds);
            List batchIds = userIds.subList(i, endIndex);
            count += executeUserCountQuery(batchIds);
        }

        return count;
    }

    /**
     * Executes a single query to retrieve users by their IDs.
     *
     * @param userIds the list of user IDs to query for
     * @return a list of UserLite objects matching the IDs
     */
    private List<UserLite> executeUserQuery(List userIds) {
        StringBuilder queryBuffer = new StringBuilder(TYPE + COLON + "\"" + USER_TYPE + "\"");

        queryBuffer.append(AND).append(ID + COLON).append(OPEN_BRACKET);
        queryBuffer.append(userIds.stream().collect(Collectors.joining(",")));
        queryBuffer.append(CLOSE_BRACKET);

        SolrQuery query = new SolrQuery();
        query.setQuery(queryBuffer.toString());
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrUserRecord> results = this.findByQuery(query, SolrUserRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToUserLite)
            .collect(Collectors.toList());
    }

    /**
     * Executes a Solr query to count the number of users matching the specified user IDs.
     *
     * @param userIds a list of user IDs used as a filter for the query
     * @return the total count of users matching the query
     */
    private int executeUserCountQuery(List userIds) {
        StringBuilder queryBuffer = new StringBuilder(TYPE + COLON + "\"" + USER_TYPE + "\"");

        queryBuffer.append(AND).append(ID + COLON).append(OPEN_BRACKET);
        queryBuffer.append(userIds.stream().collect(Collectors.joining(",")));
        queryBuffer.append(CLOSE_BRACKET);

        SolrQuery query = new SolrQuery();
        query.setQuery(queryBuffer.toString());
        query.setRows(0);

        SearchResults<SolrUserRecord> results = this.findByQuery(query, SolrUserRecord.class);

        return Math.toIntExact(results.getTotalNumberOfResults());
    }

    /**
     * Retrieves a list of users associated with the specified principal.
     *
     * @param principalId the unique identifier of the principal whose associated users are to be retrieved
     * @return a list of users associated with the given principal; an empty list is returned
     * if the principal does not exist or has no associated users
     */
    public List<User> getUsersAssociatedWithPrincipal(String principalId) {
        IkasanPrincipal ikasanPrincipal = this.ikasanPrincipalDao.findById(principalId);
        if(ikasanPrincipal == null) {
            return Collections.emptyList();
        }

        StringBuilder queryBuffer = new StringBuilder(TYPE + COLON + "\"" + USER_TYPE + "\"");

        queryBuffer.append(AND).append(PRINCIPAL_RELATED_ENTITY_COLLECTION).append(COLON).append(ikasanPrincipal.getId());

        SolrQuery query = new SolrQuery();
        query.setQuery(queryBuffer.toString());
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrUserRecord> results = this.findByQuery(query, SolrUserRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToUser)
            .collect(Collectors.toList());
    }

    /**
     * Gets the count of users associated with a specific role.
     *
     * @param roleName the name of the role to filter by
     * @param userFilter optional filter for additional user criteria
     * @return the number of users with the specified role
     */
    @Override
    public int getUsersWithRoleCount(String roleName, UserFilter userFilter) {
        IkasanPrincipalFilter ikasanPrincipalFilter = new SolrIkasanPrincipalFilterImpl();
        ikasanPrincipalFilter.setTypeFilter("user");
        List<IkasanPrincipalLite> principals = this.ikasanPrincipalDao
            .getAllPrincipalsWithRole(roleName, ikasanPrincipalFilter, -1, -1);

        if (principals.isEmpty()) {
            return 0;
        }

        // Build list of user IDs from principals
        List userIds = new ArrayList<>();
        principals.forEach(principalLite
            -> userIds.add("\"" + principalLite.getName() + "-" + USER_TYPE + "\""));

        // If userIds exceed the max predicates limit, break into batches
        if (userIds.size() > MAX_PREDICATES_PER_QUERY) {
            return getUsersCountInBatches(userIds);
        }

        // Single query for <= 1024 predicates
        return executeUserCountQuery(userIds);
    }

    /**
     * Retrieves users not associated with a specific role.
     *
     * @param roleName the name of the role to exclude
     * @param userFilter optional filter for additional user criteria
     * @param limit maximum number of results to return
     * @param offset offset for pagination
     * @return a list of lightweight UserLite objects
     */
    @Override
    public List<UserLite> getUsersWithoutRole(String roleName, UserFilter userFilter, int limit, int offset) {
        IkasanPrincipalFilter ikasanPrincipalFilter = new SolrIkasanPrincipalFilterImpl();
        ikasanPrincipalFilter.setTypeFilter("user");
        List principalNames = this.ikasanPrincipalDao
            .getAllPrincipalNamesWithoutRole(roleName, ikasanPrincipalFilter, limit, offset);

        if (principalNames.isEmpty()) {
            return Collections.emptyList();
        }

        // Build list of user IDs from principals
        List userIds = new ArrayList<>();
        principalNames.forEach(principalName
            -> userIds.add("\"" + principalName + "-" + USER_TYPE + "\""));

        // If userIds exceed the max predicates limit, break into batches
        if (userIds.size() > MAX_PREDICATES_PER_QUERY) {
            return getUsersInBatches(userIds);
        }

        // Single query for <= 1024 predicates
        return executeUserQuery(userIds);
    }

    /**
     * Gets the count of users not associated with a specific role.
     *
     * This method queries for users whose related principals do not have the specified role.
     *
     * If there are more than 1024 principals, the query is broken into multiple batches
     * to avoid exceeding Solr's maximum boolean clause limit.
     *
     * @param roleName the name of the role to exclude
     * @param userFilter optional filter for additional user criteria
     * @return the number of users without the specified role
     */
    @Override
    public int getUsersWithoutRoleCount(String roleName, UserFilter userFilter) {
        IkasanPrincipalFilter ikasanPrincipalFilter = new SolrIkasanPrincipalFilterImpl();
        ikasanPrincipalFilter.setTypeFilter("user");
        List<IkasanPrincipalLite> principals = this.ikasanPrincipalDao
            .getAllPrincipalsWithoutRole(roleName, ikasanPrincipalFilter, -1, -1);

        if (principals.isEmpty()) {
            return 0;
        }

        // Build list of user IDs from principals
        List userIds = new ArrayList<>();
        principals.forEach(principalLite
            -> userIds.add("\"" + principalLite.getName() + "-" + USER_TYPE + "\""));

        // If userIds exceed the max predicates limit, break into batches
        if (userIds.size() > MAX_PREDICATES_PER_QUERY) {
            return getUsersCountInBatches(userIds);
        }

        // Single query for <= 1024 predicates
        return executeUserCountQuery(userIds);
    }

    /**
     * Gets the total count of users matching the specified filter.
     *
     * @param userFilter optional filter for user criteria
     * @return the number of users matching the filter
     */
    @Override
    public int getUserCount(UserFilter userFilter) {
        StringBuilder queryBuffer = new StringBuilder(TYPE + COLON + "\"" + USER_TYPE + "\"");

        if (userFilter != null) {
            addOptionalUserFilteringLogic(userFilter, queryBuffer);
        }

        SolrQuery query = new SolrQuery();
        query.setQuery(queryBuffer.toString());
        query.setRows(0);

        SearchResults<SolrUserRecord> results = this.findByQuery(query, SolrUserRecord.class);

        return (int) results.getTotalNumberOfResults();
    }

    /**
     * Retrieves all users from the Solr index.
     *
     * @return a list of all users
     */
    @Override
    public List<User> getUsers() {
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + USER_TYPE + "\"");
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrUserRecord> results = this.findByQuery(query, SolrUserRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToUser)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves users with optional filtering and pagination.
     *
     * @param userFilter optional filter for user criteria
     * @param limit maximum number of results to return
     * @param offset offset for pagination
     * @return a list of users matching the criteria
     */
    @Override
    public List<User> getUsers(UserFilter userFilter, int limit, int offset) {
        StringBuilder queryBuffer = new StringBuilder(TYPE + COLON + "\"" + USER_TYPE + "\"");

        if (userFilter != null) {
            addOptionalUserFilteringLogic(userFilter, queryBuffer);
        }

        SolrQuery query = new SolrQuery();
        query.setQuery(queryBuffer.toString());
        query.setRows(limit);
        query.setStart(offset);

        SearchResults<SolrUserRecord> results = this.findByQuery(query, SolrUserRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToUser)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves all users as lightweight UserLite objects.
     *
     * @return a list of all UserLite objects
     */
    @Override
    public List<UserLite> getUserLites() {
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + USER_TYPE + "\"");
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrUserRecord> results = this.findByQuery(query, SolrUserRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToUserLite)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves users as lightweight UserLite objects with pagination.
     *
     * @param limit maximum number of results to return
     * @param offset offset for pagination
     * @return a list of UserLite objects
     */
    @Override
    public List<UserLite> getUserLites(int limit, int offset) {
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + USER_TYPE + "\"");
        query.setRows(limit);
        query.setStart(offset);

        SearchResults<SolrUserRecord> results = this.findByQuery(query, SolrUserRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToUserLite)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves a user by their exact username.
     *
     * @param username the username to search for
     * @return the User object if found, or null if not found
     */
    @Override
    public User getUser(String username) {
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + USER_TYPE + "\" AND " + NAME + COLON + "\"" + username + "\"");

        SearchResults<SolrUserRecord> results = this.findByQuery(query, SolrUserRecord.class);

        if (results.getResultList().isEmpty()) {
            return null;
        }

        return convertRecordToUser(results.getResultList().get(0));
    }

    /**
     * Retrieves users whose usernames contain the specified search term.
     *
     * This method performs a wildcard search on usernames using the pattern "*searchTerm*".
     *
     * @param username the search term to match against usernames
     * @return a list of users whose usernames contain the search term
     */
    @Override
    public List<User> getUserByUsernameLike(String username) {
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + USER_TYPE + "\" AND " + NAME + COLON + "*" + username + "*");
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrUserRecord> results = this.findByQuery(query, SolrUserRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToUser)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves users whose first names contain the specified search term.
     *
     * @param firstname the search term to match against first names
     * @return a list of users whose first names contain the search term
     */
    @Override
    public List<User> getUserByFirstnameLike(String firstname) {
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + USER_TYPE + "\" AND " + FIRST_NAME + COLON + "*" + firstname + "*");
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrUserRecord> results = this.findByQuery(query, SolrUserRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToUser)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves users whose surnames contain the specified search term.
     *
     * @param surname the search term to match against surnames
     * @return a list of users whose surnames contain the search term
     */
    @Override
    public List<User> getUserBySurnameLike(String surname) {
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + USER_TYPE + "\" AND " + SURNAME + COLON + "*" + surname + "*");
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrUserRecord> results = this.findByQuery(query, SolrUserRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToUser)
            .collect(Collectors.toList());
    }

    /**
     * Saves or updates a user in the Solr index.
     *
     * This method converts the User object to JSON and stores it as a SolrUserRecord.
     * If the user already exists (same username), it will be updated. The user is identified
     * by their username, which must be unique.
     *
     * @param user the user to save or update
     * @throws RuntimeException if the user cannot be serialized to JSON
     */
    @Override
    public void save(User user) {
        SolrUserRecord record = new SolrUserRecord();
        record.setUsername(user.getUsername());
        record.setEmail(user.getEmail());
        record.setFirstName(user.getFirstName());
        record.setSurname(user.getSurname());
        record.setDepartment(user.getDepartment());
        record.setTimestamp(System.currentTimeMillis());
        record.setModifiedTimestamp(System.currentTimeMillis());
        record.setRelatedPrincipalIdentifiers(user.getPrincipals().stream()
            .map(IkasanPrincipal::getId)
            .map(String::valueOf)
            .collect(Collectors.toList()));

        try {
            record.setUser(OBJECT_MAPPER.writeValueAsString(user));
        } catch (JacksonException e) {
            throw new RuntimeException("Cannot convert User to string! [" + user.getUsername() + "]", e);
        }

        super.save(record);
    }

    /**
     * Saves a list of users by transforming them into SolrUserRecord objects
     * and persisting them.
     *
     * @param users the list of User objects to be saved. Each user is converted
     *              into a SolrUserRecord with necessary attributes populated.
     */
    public void saveUsers(List<User> users) {
        List<SolrUserRecord> records = new ArrayList<>();

        users.forEach(user -> {
            SolrUserRecord record = new SolrUserRecord();
            record.setUsername(user.getUsername());
            record.setEmail(user.getEmail());
            record.setFirstName(user.getFirstName());
            record.setSurname(user.getSurname());
            record.setDepartment(user.getDepartment());
            record.setTimestamp(System.currentTimeMillis());
            record.setModifiedTimestamp(System.currentTimeMillis());
            record.setRelatedPrincipalIdentifiers(user.getPrincipals().stream()
                .map(IkasanPrincipal::getId)
                .map(String::valueOf)
                .collect(Collectors.toList()));

            try {
                record.setUser(OBJECT_MAPPER.writeValueAsString(user));
            } catch (JacksonException e) {
                throw new RuntimeException("Cannot convert User to string! [" + user.getUsername() + "]", e);
            }

            records.add(record);
        });

        super.save(records);
    }

    /**
     * Deletes a user from the Solr index.
     *
     * The user is identified by their username and document type. This operation removes
     * the user document from the Solr index permanently.
     *
     * @param user the user to delete
     */
    @Override
    public void delete(User user) {
        super.removeById(USER_TYPE, user.getUsername() + "-" + USER_TYPE);
    }

    /**
     * Converts a SolrUserRecord to a User object.
     *
     * @param record the SolrUserRecord to convert
     * @return the deserialized User object
     * @throws RuntimeException if the JSON cannot be deserialized
     */
    private User convertRecordToUser(SolrUserRecord record) {
        try {
            User user = OBJECT_MAPPER.readValue(record.getUser(), SolrUserImpl.class);

            if(record.getRelatedPrincipalIdentifiers() != null) {
                record.getRelatedPrincipalIdentifiers().forEach(principalId
                    -> user.addPrincipal(this.ikasanPrincipalDao.findById(principalId)));
            }

            return user;
        } catch (JacksonException e) {
            throw new RuntimeException("Cannot convert SolrUserRecord to User! [" + record.getUsername() + "]", e);
        }
    }

    /**
     * Converts a SolrUserRecord to a lightweight UserLite object.
     *
     * @param record the SolrUserRecord to convert
     * @return the deserialized UserLite object
     * @throws RuntimeException if the JSON cannot be deserialized
     */
    private UserLite convertRecordToUserLite(SolrUserRecord record) {
        try {
            return OBJECT_MAPPER.readValue(record.getUser(), SolrUserLiteImpl.class);
        } catch (JacksonException e) {
            throw new RuntimeException("Cannot convert SolrUserRecord to UserLite! [" + record.getUsername() + "]", e);
        }
    }

    /**
     * Adds optional filtering logic to the query based on the UserFilter.
     *
     * @param filter the UserFilter containing filter criteria
     * @param queryBuffer the StringBuilder to append filter logic to
     */
    private void addOptionalUserFilteringLogic(UserFilter filter, StringBuilder queryBuffer) {
        if(filter == null) return;

        // Add username filter if present
        if (filter.getUsernameFilter() != null && !filter.getUsernameFilter().isEmpty()) {
            queryBuffer.append(AND).append(NAME).append(COLON).append("*").append(filter.getUsernameFilter()).append("*");
        }

        // Add email filter if present
        if (filter.getEmailFilter() != null && !filter.getEmailFilter().isEmpty()) {
            queryBuffer.append(AND).append(EMAIL).append(COLON).append("*").append(filter.getEmailFilter()).append("*");
        }

        // Add first name filter if present
        if (filter.getNameFilter() != null && !filter.getNameFilter().isEmpty()) {
            queryBuffer.append(AND).append(FIRST_NAME).append(COLON).append("*").append(filter.getNameFilter()).append("*");
        }

        // Add surname filter if present
        if (filter.getLastNameFilter() != null && !filter.getLastNameFilter().isEmpty()) {
            queryBuffer.append(AND).append(SURNAME).append(COLON).append("*").append(filter.getLastNameFilter()).append("*");
        }

        // Add department filter if present
        if (filter.getDepartmentFilter() != null && !filter.getDepartmentFilter().isEmpty()) {
            queryBuffer.append(AND).append(DEPARTMENT).append(COLON).append("*").append(filter.getDepartmentFilter()).append("*");
        }
    }
}
