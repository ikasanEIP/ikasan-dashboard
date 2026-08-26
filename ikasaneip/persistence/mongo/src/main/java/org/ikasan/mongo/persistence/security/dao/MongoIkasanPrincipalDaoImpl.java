package org.ikasan.mongo.persistence.security.dao;

import org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalImpl;
import org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalLiteImpl;
import org.ikasan.mongo.persistence.security.model.MongoIkasanPrincipalRecord;
import org.ikasan.mongo.persistence.security.repository.MongoIkasanPrincipalRepository;
import org.ikasan.mongo.persistence.security.util.MongoSecurityObjectMapperFactory;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.security.dao.IkasanPrincipalDao;
import org.ikasan.spec.security.model.IkasanPrincipal;
import org.ikasan.spec.security.model.IkasanPrincipalFilter;
import org.ikasan.spec.security.model.IkasanPrincipalLite;
import org.ikasan.spec.security.model.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MongoDB implementation of IkasanPrincipal DAO.
 *
 * This DAO provides methods to perform CRUD operations on Ikasan principals stored in MongoDB.
 * Principals are stored as MongoIkasanPrincipalImpl documents with direct object storage
 * (no JSON serialization needed).
 *
 * Key features:
 * <ul>
 *   <li>Principal storage and retrieval using MongoDB</li>
 *   <li>Direct object persistence (no JSON serialization)</li>
 *   <li>Wildcard search capabilities by principal name</li>
 *   <li>Support for principal filtering and pagination</li>
 *   <li>Lightweight IkasanPrincipalLite objects for list operations</li>
 *   <li>Role relationship management</li>
 * </ul>
 *
 * @author Ikasan Development Team
 */
public class MongoIkasanPrincipalDaoImpl implements IkasanPrincipalDao {

    private static final Logger logger = LoggerFactory.getLogger(MongoIkasanPrincipalDaoImpl.class);

    /** Jackson JsonMapper for JSON serialization/deserialization */
    private static final JsonMapper OBJECT_MAPPER = MongoSecurityObjectMapperFactory.newInstance();
    private static final long DO_NOT_EXPIRE = -1L;

    private final MongoIkasanPrincipalRepository repository;
    private final MongoTemplate mongoTemplate;
    private final MongoRoleDaoImpl mongoRoleDaoImpl;

    /**
     * Constructor for MongoIkasanPrincipalDaoImpl.
     *
     * @param repository the MongoDB repository for principal persistence
     * @param mongoTemplate the MongoTemplate for custom queries
     * @param mongoRoleDaoImpl the role DAO for loading role relationships
     */
    public MongoIkasanPrincipalDaoImpl(MongoIkasanPrincipalRepository repository, MongoTemplate mongoTemplate,
                                       MongoRoleDaoImpl mongoRoleDaoImpl) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.mongoRoleDaoImpl = mongoRoleDaoImpl;
    }

    /**
     * Creates a new empty IkasanPrincipal instance.
     *
     * @return a new MongoIkasanPrincipalImpl instance
     */
    @Override
    public IkasanPrincipal createPrincipal() {
        return new MongoIkasanPrincipalImpl();
    }

    /**
     * Saves or updates a principal in MongoDB.
     *
     * This method persists the IkasanPrincipal object directly to MongoDB.
     * If the principal already exists (same ID or name), it will be updated.
     * The principal is identified by its name, which must be unique.
     *
     * Date handling:
     * <ul>
     *   <li>If createdDateTime is null, uses current system time</li>
     *   <li>Always sets updatedDateTime to current system time</li>
     * </ul>
     *
     * @param principal the principal to save or update
     */
    @Override
    public void saveOrUpdatePrincipal(IkasanPrincipal principal) {
        repository.save(this.convertToMongoIkasanPrincipalRecord(principal));
        logger.debug("Saved principal with name: {}", principal.getName());
    }

    /**
     * Saves or updates multiple principals in a batch operation.
     *
     * @param principals the list of principals to save or update
     */
    @Override
    public void saveOrUpdatePrincipals(List<IkasanPrincipal> principals) {
        repository.saveAll(principals.stream()
            .map(this::convertToMongoIkasanPrincipalRecord)
            .toList());
        logger.debug("Saved {} principals", principals.size());
    }

    /**
     * Deletes a principal from MongoDB.
     *
     * The principal is identified by its name. This operation removes
     * the principal document from MongoDB permanently.
     *
     * @param principal the principal to delete
     */
    @Override
    public void deletePrincipal(IkasanPrincipal principal) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.NAME).is(principal.getName()));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(PRINCIPAL_TYPE));

        mongoTemplate.remove(query, MongoIkasanPrincipalRecord.class);
        logger.debug("Deleted principal with name: {}", principal.getName());
    }

    /**
     * Retrieves all principals from MongoDB.
     *
     * This method queries MongoDB for all principal documents and
     * loads their role relationships.
     *
     * @return a list of all principals, or an empty list if no principals exist
     */
    @Override
    public List<IkasanPrincipal> getAllPrincipals() {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(PRINCIPAL_TYPE));

        return mongoTemplate.find(query, MongoIkasanPrincipalRecord.class).stream()
            .map(this::convertRecordToPrincipal)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves principals with pagination and optional filtering.
     *
     * @param filter the filter criteria to apply (may be null)
     * @param limit the maximum number of principals to return (-1 for no limit)
     * @param offset the starting position in the result set (-1 for no offset)
     * @return a list of principals matching the criteria
     */
    @Override
    public List<IkasanPrincipal> getPrincipals(IkasanPrincipalFilter filter, int limit, int offset) {
        Query query = new Query();
        addFilterCriteria(query, filter);
        addSortingAndPaging(query, filter, limit, offset);

        return mongoTemplate.find(query, MongoIkasanPrincipalRecord.class).stream()
            .map(this::convertRecordToPrincipal)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves all lightweight principal representations from MongoDB.
     *
     * IkasanPrincipalLite objects contain less data than full IkasanPrincipal objects,
     * making them suitable for list operations and UI displays.
     *
     * @return a list of all principal lite objects, or an empty list if no principals exist
     */
    @Override
    public List<IkasanPrincipalLite> getAllPrincipalLites() {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(PRINCIPAL_TYPE));

        return mongoTemplate.find(query, MongoIkasanPrincipalRecord.class).stream()
            .map(p -> OBJECT_MAPPER.readValue(p.getPrincipal(), MongoIkasanPrincipalImpl.class))
            .map(this::convertToLite)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves lightweight principals with pagination and optional filtering.
     *
     * This method supports filtering by name, description, and type. Filters are combined
     * using AND logic. All text filters use case-insensitive regex matching.
     *
     * @param filter the filter criteria to apply (may be null for unfiltered results)
     * @param limit the maximum number of principals to return (-1 for no limit)
     * @param offset the starting position in the result set (-1 for no offset)
     * @return a list of principal lite objects matching the criteria
     */
    @Override
    public List<IkasanPrincipalLite> getPrincipalLites(IkasanPrincipalFilter filter, int limit, int offset) {
        Query query = new Query();
        addFilterCriteria(query, filter);
        addSortingAndPaging(query, filter, limit, offset);

        return mongoTemplate.find(query, MongoIkasanPrincipalRecord.class).stream()
            .map(p -> OBJECT_MAPPER.readValue(p.getPrincipal(), MongoIkasanPrincipalImpl.class))
            .map(this::convertToLite)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves a principal by its unique ID.
     *
     * This method performs an exact match query on the principal ID field.
     *
     * @param id the unique ID of the principal to retrieve
     * @return the IkasanPrincipal object if found, or null if no principal exists with the given ID
     */
    @Override
    public IkasanPrincipal findById(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }

        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ID).is(id));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(PRINCIPAL_TYPE));

        MongoIkasanPrincipalRecord result = mongoTemplate.findOne(query, MongoIkasanPrincipalRecord.class);
        if (result == null) {
            return null;
        }
        return convertRecordToPrincipal(result);
    }

    /**
     * Retrieves a principal by its exact name.
     *
     * This method performs an exact match query on the principal name field.
     *
     * @param name the exact name of the principal to retrieve
     * @return the IkasanPrincipal object if found, or null if no principal exists with the given name
     */
    @Override
    public IkasanPrincipal getPrincipalByName(String name) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.NAME).is(name));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(PRINCIPAL_TYPE));

        MongoIkasanPrincipalRecord result = mongoTemplate.findOne(query, MongoIkasanPrincipalRecord.class);
        if (result == null) {
            return null;
        }
        return convertRecordToPrincipal(result);
    }

    /**
     * Retrieves principals whose names contain the specified search term.
     *
     * This method performs a case-insensitive wildcard search on principal names.
     * The search matches any principal name that contains the given substring.
     *
     * @param name the search term to match against principal names
     * @return a list of principals whose names contain the search term, or an empty list if no matches found
     */
    @Override
    public List<IkasanPrincipal> getPrincipalByNameLike(String name) {
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.NAME).regex(".*" + name + ".*", "i"));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(PRINCIPAL_TYPE));

        return mongoTemplate.find(query, MongoIkasanPrincipalRecord.class).stream()
            .map(this::convertRecordToPrincipal)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves all principals associated with a specific role.
     *
     * This method uses the roleIds collection to efficiently query
     * principals that have the specified role assigned.
     *
     * @param roleName the name of the role
     * @return a list of principals that have the specified role, or an empty list if none found
     */
    @Override
    public List<IkasanPrincipal> getAllPrincipalsWithRole(String roleName) {
        if (roleName == null || roleName.isEmpty()) {
            return Collections.emptyList();
        }

        // Get the role to find its ID
        Role role = mongoRoleDaoImpl.getRoleByName(roleName);
        if (role == null) {
            return Collections.emptyList();
        }

        String roleId = role.getId().toString();
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ROLES_RELATED_ENTITY_COLLECTION).in(roleId));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(PRINCIPAL_TYPE));

        return mongoTemplate.find(query, MongoIkasanPrincipalRecord.class).stream()
            .map(this::convertRecordToPrincipal)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves lightweight principals associated with a specific role, with pagination and filtering.
     *
     * This method uses the roleIds collection to efficiently query
     * principals that have the specified role assigned. Additional filtering by name, description,
     * and type can be applied through the IkasanPrincipalFilter parameter.
     *
     * @param roleName the name of the role
     * @param filter the filter criteria to apply (may be null for no additional filtering)
     * @param limit the maximum number of principals to return (-1 for no limit)
     * @param offset the starting position in the result set (-1 for no offset)
     * @return a list of principal lite objects that have the specified role and match the filter criteria
     */
    @Override
    public List<IkasanPrincipalLite> getAllPrincipalsWithRole(String roleName, IkasanPrincipalFilter filter,
                                                              int limit, int offset) {
        if (roleName == null || roleName.isEmpty()) {
            return Collections.emptyList();
        }

        // Get the role to find its ID
        Role role = mongoRoleDaoImpl.getRoleByName(roleName);
        if (role == null) {
            return Collections.emptyList();
        }

        String roleId = role.getId().toString();
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ROLES_RELATED_ENTITY_COLLECTION).in(roleId));
        addFilterCriteria(query, filter);
        addSortingAndPaging(query, filter, limit, offset);

        return mongoTemplate.find(query, MongoIkasanPrincipalRecord.class).stream()
            .map(p -> OBJECT_MAPPER.readValue(p.getPrincipal(), MongoIkasanPrincipalImpl.class))
            .map(this::convertToLite)
            .collect(Collectors.toList());
    }

    /**
     * Efficiently retrieves only the names of principals with a specific role.
     *
     * This method retrieves only the name field, making it more efficient
     * than retrieving full principal objects when only names are needed.
     *
     * @param roleName the name of the role to filter by
     * @param filter optional filter for additional principal criteria
     * @param limit maximum number of results to return (-1 for no limit)
     * @param offset offset for pagination (-1 for no offset)
     * @return a list of principal names for principals with the specified role
     */
    @Override
    public List<String> getAllPrincipalNamesWithRole(String roleName, IkasanPrincipalFilter filter,
                                                     int limit, int offset) {
        if (roleName == null || roleName.isEmpty()) {
            return Collections.emptyList();
        }

        // Get the role to find its ID
        Role role = mongoRoleDaoImpl.getRoleByName(roleName);
        if (role == null) {
            return Collections.emptyList();
        }

        String roleId = role.getId().toString();
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ROLES_RELATED_ENTITY_COLLECTION).in(roleId));
        query.fields().include(EntityFields.PAYLOAD_CONTENT).include(EntityFields.NAME);
        addFilterCriteria(query, filter);
        addSortingAndPaging(query, filter, limit, offset);

        return mongoTemplate.find(query, MongoIkasanPrincipalRecord.class).stream()
            .map(p -> OBJECT_MAPPER.readValue(p.getPrincipal(), MongoIkasanPrincipalImpl.class))
            .map(MongoIkasanPrincipalImpl::getName)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves lightweight principals NOT associated with a specific role, with pagination and filtering.
     *
     * This method finds principals that do NOT have the specified role in their roleIds collection.
     * Additional filtering by name, description, and type can be applied through the IkasanPrincipalFilter parameter.
     *
     * @param roleName the name of the role to exclude
     * @param filter the filter criteria to apply (may be null for no additional filtering)
     * @param limit the maximum number of principals to return (-1 for no limit)
     * @param offset the starting position in the result set (-1 for no offset)
     * @return a list of principal lite objects that do NOT have the specified role
     */
    @Override
    public List<IkasanPrincipalLite> getAllPrincipalsWithoutRole(String roleName, IkasanPrincipalFilter filter,
                                                                 int limit, int offset) {
        if (roleName == null || roleName.isEmpty()) {
            return Collections.emptyList();
        }

        // Get the role to find its ID
        Role role = mongoRoleDaoImpl.getRoleByName(roleName);
        if (role == null) {
            // If role doesn't exist, all principals don't have it
            return getPrincipalLites(filter, limit, offset);
        }

        String roleId = role.getId().toString();
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ROLES_RELATED_ENTITY_COLLECTION).nin(roleId));
        addFilterCriteria(query, filter);
        addSortingAndPaging(query, filter, limit, offset);

        return mongoTemplate.find(query, MongoIkasanPrincipalRecord.class).stream()
            .map(p -> OBJECT_MAPPER.readValue(p.getPrincipal(), MongoIkasanPrincipalImpl.class))
            .map(this::convertToLite)
            .collect(Collectors.toList());
    }

    /**
     * Efficiently retrieves only the names of principals without a specific role.
     *
     * This method retrieves only the name field, making it more efficient
     * than retrieving full principal objects when only names are needed.
     *
     * @param roleName the name of the role to exclude
     * @param filter optional filter for additional principal criteria
     * @param limit maximum number of results to return (-1 for no limit)
     * @param offset offset for pagination (-1 for no offset)
     * @return a list of principal names for principals without the specified role
     */
    @Override
    public List<String> getAllPrincipalNamesWithoutRole(String roleName, IkasanPrincipalFilter filter,
                                                        int limit, int offset) {
        if (roleName == null || roleName.isEmpty()) {
            return Collections.emptyList();
        }

        // Get the role to find its ID
        Role role = mongoRoleDaoImpl.getRoleByName(roleName);
        if (role == null) {
            // If role doesn't exist, all principals don't have it
            Query query = new Query();
            query.fields().include(EntityFields.NAME).include(EntityFields.PAYLOAD_CONTENT);
            addFilterCriteria(query, filter);
            addSortingAndPaging(query, filter, limit, offset);

            return mongoTemplate.find(query, MongoIkasanPrincipalRecord.class).stream()
                .map(p -> OBJECT_MAPPER.readValue(p.getPrincipal(), MongoIkasanPrincipalImpl.class))
                .map(MongoIkasanPrincipalImpl::getName)
                .collect(Collectors.toList());
        }

        String roleId = role.getId().toString();
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ROLES_RELATED_ENTITY_COLLECTION).nin(roleId));
        query.fields().include(EntityFields.NAME).include(EntityFields.PAYLOAD_CONTENT);
        addFilterCriteria(query, filter);
        addSortingAndPaging(query, filter, limit, offset);

        this.getAllPrincipals();

        return mongoTemplate.find(query, MongoIkasanPrincipalRecord.class).stream()
            .map(p -> OBJECT_MAPPER.readValue(p.getPrincipal(), MongoIkasanPrincipalImpl.class))
            .map(MongoIkasanPrincipalImpl::getName)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves principals by their role names.
     *
     * This method queries MongoDB for principals that have ANY of the specified roles.
     * Multiple role names are combined using OR logic, so a principal matching any
     * of the provided roles will be included in the results.
     *
     * @param roleNames list of role names to search for
     * @return a list of principals that have at least one of the specified roles, or an empty list if none found
     */
    @Override
    public List<IkasanPrincipal> getPrincipalsByRoleNames(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Collections.emptyList();
        }

        // Get role IDs for all role names
        List<String> roleIds = roleNames.stream()
            .map(mongoRoleDaoImpl::getRoleByName)
            .filter(Objects::nonNull)
            .map(role -> role.getId().toString())
            .collect(Collectors.toList());

        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ROLES_RELATED_ENTITY_COLLECTION).in(roleIds));
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(PRINCIPAL_TYPE));

        return mongoTemplate.find(query, MongoIkasanPrincipalRecord.class).stream()
            .map(this::convertRecordToPrincipal)
            .collect(Collectors.toList());
    }

    /**
     * Gets the total count of principals matching the filter criteria.
     *
     * This method supports filtering by name, description, and type. Filters are combined
     * using AND logic. All text filters use case-insensitive regex matching.
     *
     * @param filter the filter criteria to apply (may be null for total count)
     * @return the count of matching principals
     */
    @Override
    public int getPrincipalCount(IkasanPrincipalFilter filter) {
        Query query = new Query();
        addFilterCriteria(query, filter);
        return (int) mongoTemplate.count(query, MongoIkasanPrincipalRecord.class);
    }

    /**
     * Gets the count of principals with a specific role.
     *
     * This method uses the roleIds collection to count principals
     * that have the specified role assigned. Additional filtering by name, description,
     * and type can be applied through the IkasanPrincipalFilter parameter.
     *
     * @param roleName the name of the role
     * @param filter the filter criteria to apply (may be null for no additional filtering)
     * @return the count of principals that have the specified role and match the filter criteria
     */
    @Override
    public int getPrincipalsWithRoleCount(String roleName, IkasanPrincipalFilter filter) {
        if (roleName == null || roleName.isEmpty()) {
            return 0;
        }

        // Get the role to find its ID
        Role role = mongoRoleDaoImpl.getRoleByName(roleName);
        if (role == null) {
            return 0;
        }

        String roleId = role.getId().toString();
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ROLES_RELATED_ENTITY_COLLECTION).in(roleId));
        addFilterCriteria(query, filter);

        return (int) mongoTemplate.count(query, MongoIkasanPrincipalRecord.class);
    }

    /**
     * Gets the count of principals without a specific role.
     *
     * This method finds principals that do NOT have the specified role in their roleIds collection.
     * Additional filtering by name, description, and type can be applied through the IkasanPrincipalFilter parameter.
     *
     * @param roleName the name of the role to exclude
     * @param filter the filter criteria to apply (may be null for no additional filtering)
     * @return the count of principals that do NOT have the specified role
     */
    @Override
    public int getPrincipalsWithoutRoleCount(String roleName, IkasanPrincipalFilter filter) {
        if (roleName == null || roleName.isEmpty()) {
            return 0;
        }

        // Get the role to find its ID
        Role role = mongoRoleDaoImpl.getRoleByName(roleName);
        if (role == null) {
            // If role doesn't exist, all principals don't have it
            return getPrincipalCount(filter);
        }

        String roleId = role.getId().toString();
        Query query = new Query();
        query.addCriteria(Criteria.where(EntityFields.ROLES_RELATED_ENTITY_COLLECTION).nin(roleId));
        addFilterCriteria(query, filter);

        return (int) mongoTemplate.count(query, MongoIkasanPrincipalRecord.class);
    }

    /**
     * Converts a SolrIkasanPrincipalRecord to an IkasanPrincipal object.
     *
     * This private helper method deserializes the JSON principal string stored in the
     * SolrIkasanPrincipalRecord into a SolrIkasanPrincipalImpl object using Jackson ObjectMapper.
     *
     * @param record the SolrIkasanPrincipalRecord to convert
     * @return the deserialized IkasanPrincipal object
     * @throws RuntimeException if the JSON deserialization fails
     */
    private IkasanPrincipal convertRecordToPrincipal(MongoIkasanPrincipalRecord record) {
        try {
            IkasanPrincipal ikasanPrincipal = OBJECT_MAPPER.readValue(record.getPrincipal()
                , MongoIkasanPrincipalImpl.class);

            if(record.getRelatedRoleIdentifiers() != null) {
                record.getRelatedRoleIdentifiers().forEach(roleId
                    -> ikasanPrincipal.addRole(this.mongoRoleDaoImpl.getRoleById(roleId)));
            }

            return ikasanPrincipal;
        } catch (JacksonException e) {
            throw new RuntimeException("Cannot convert SolrIkasanPrincipalRecord to IkasanPrincipal! [" + record.getName() + "]", e);
        }
    }

    /**
     * Converts a full IkasanPrincipal to a lightweight IkasanPrincipalLite.
     *
     * @param principal the principal to convert
     * @return an IkasanPrincipalLite object
     */
    private IkasanPrincipalLite convertToLite(MongoIkasanPrincipalImpl principal) {
        MongoIkasanPrincipalLiteImpl lite = new MongoIkasanPrincipalLiteImpl();
        lite.setId(principal.getId());
        lite.setName(principal.getName());
        lite.setType(principal.getType());
        lite.setDescription(principal.getDescription());
        return lite;
    }

    /**
     * Converts an IkasanPrincipal to a MongoIkasanPrincipalImpl.
     *
     * @param principal the principal to convert
     * @return the MongoIkasanPrincipalImpl
     */
    private MongoIkasanPrincipalRecord convertToMongoIkasanPrincipalRecord(IkasanPrincipal principal) {
        MongoIkasanPrincipalRecord record = new MongoIkasanPrincipalRecord();
        record.setId(principal.getName() + "-" + PRINCIPAL_TYPE);
        principal.setId(principal.getName() + "-" + PRINCIPAL_TYPE);
        record.setType(PRINCIPAL_TYPE);
        record.setName(principal.getName());
        record.setDescription(principal.getDescription());
        record.setPrincipalType(principal.getType());
        record.setTimestamp(principal.getCreatedDateTime() != null ? principal.getCreatedDateTime().getTime()
            : System.currentTimeMillis());
        record.setModifiedTimestamp(principal.getUpdatedDateTime() != null ? principal.getUpdatedDateTime().getTime()
            : System.currentTimeMillis());
        record.setRelatedRoleIdentifiers(principal.getRoles().stream()
            .map(Role::getId).map(String::valueOf).collect(Collectors.toList()));
        record.setExpiry(DO_NOT_EXPIRE);
        try {
            record.setPrincipal(OBJECT_MAPPER.writeValueAsString(principal));
        } catch (JacksonException e) {
            throw new RuntimeException("Cannot convert IkasanPrincipal to string! [" + principal.getName() + "]", e);
        }

        return record;
    }

    /**
     * Adds filter criteria to a MongoDB query based on the provided filter.
     *
     * @param query the query to modify
     * @param filter the filter criteria
     */
    private void addFilterCriteria(Query query, IkasanPrincipalFilter filter) {
        // Always add type filter to ensure we only query principal documents
        query.addCriteria(Criteria.where(EntityFields.TYPE).is(PRINCIPAL_TYPE));

        if (filter == null) {
            return;
        }

        if (filter.getTypeFilter() != null && !filter.getTypeFilter().isEmpty()) {
            query.addCriteria(Criteria.where(EntityFields.IKASAN_PRINCIPAL_TYPE).regex(filter.getTypeFilter(), "i"));
        }

        if (filter.getNameFilter() != null && !filter.getNameFilter().isEmpty()) {
            query.addCriteria(Criteria.where(EntityFields.NAME).regex(filter.getNameFilter(), "i"));
        }

        if (filter.getDescriptionFilter() != null && !filter.getDescriptionFilter().isEmpty()) {
            query.addCriteria(Criteria.where(EntityFields.DESCRIPTION).regex(filter.getDescriptionFilter(), "i"));
        }
    }

    /**
     * Adds sorting and paging to a MongoDB query.
     *
     * @param query the query to modify
     * @param filter the filter containing sort criteria
     * @param limit the maximum number of results (-1 for no limit)
     * @param offset the offset for pagination (-1 for no offset)
     */
    private void addSortingAndPaging(Query query, IkasanPrincipalFilter filter, int limit, int offset) {
        // Add sorting
        if (filter != null && filter.getSortColumn() != null && !filter.getSortColumn().isEmpty()) {
            Sort.Direction direction = (filter.getSortOrder() != null && filter.getSortOrder().equals("ASCENDING"))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
            query.with(Sort.by(direction, filter.getSortColumn()));
        } else {
            // Default sort by name ascending
            query.with(Sort.by(Sort.Direction.ASC, EntityFields.NAME));
        }

        // Add paging
        if (limit > 0 && offset >= 0) {
            Pageable pageable = PageRequest.of(offset / limit, limit);
            query.with(pageable);
        } else if (limit > 0) {
            query.limit(limit);
        } else if (offset > 0) {
            query.skip(offset);
        }
    }
}
