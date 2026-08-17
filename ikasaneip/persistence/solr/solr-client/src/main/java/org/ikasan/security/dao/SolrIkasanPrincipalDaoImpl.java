package org.ikasan.security.dao;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.security.model.SolrIkasanPrincipalImpl;
import org.ikasan.security.model.SolrIkasanPrincipalLiteImpl;
import org.ikasan.security.model.SolrIkasanPrincipalRecord;
import org.ikasan.security.util.SolrSecurityObjectMapperFactory;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.security.dao.IkasanPrincipalDao;
import org.ikasan.spec.security.model.IkasanPrincipal;
import org.ikasan.spec.security.model.IkasanPrincipalFilter;
import org.ikasan.spec.security.model.IkasanPrincipalLite;
import org.ikasan.spec.security.model.Role;
import org.ikasan.spec.solr.SolrDaoBase;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.ikasan.security.dao.SolrRoleDaoImpl.ROLE_TYPE;

/**
 * Solr-based Data Access Object implementation for managing Ikasan principals.
 *
 * This DAO provides methods to perform CRUD operations on Ikasan principals stored
 * in a Solr index. Principals are serialized to JSON and stored as SolrIkasanPrincipalRecord
 * documents with the type "securityPrincipal".
 *
 * Key features:
 * <ul>
 *   <li>Principal storage and retrieval using Solr</li>
 *   <li>JSON serialization/deserialization of IkasanPrincipal objects</li>
 *   <li>Wildcard search capabilities by principal name</li>
 *   <li>Support for principal filtering and pagination</li>
 *   <li>Lightweight IkasanPrincipalLite objects for list operations</li>
 * </ul>
 *
 * @author Ikasan Development Team
 */
public class SolrIkasanPrincipalDaoImpl extends SolrDaoBase<SolrIkasanPrincipalRecord> implements IkasanPrincipalDao {

    /** The Solr document type identifier for security principals */
    public static final String PRINCIPAL_TYPE = "securityPrincipal";

    /** Jackson JsonMapper for JSON serialization/deserialization */
    private static final JsonMapper OBJECT_MAPPER = SolrSecurityObjectMapperFactory.newInstance();

    private SolrRoleDaoImpl solrRoleDao;

    public SolrIkasanPrincipalDaoImpl(SolrRoleDaoImpl solrRoleDao) {
        this.solrRoleDao = solrRoleDao;
    }

    /**
     * Converts a SolrIkasanPrincipalRecord entity into a Solr input document for indexing.
     *
     * This method maps the principal record fields to Solr document fields, including:
     * <ul>
     *   <li>ID: Composite key of principal name and type</li>
     *   <li>TYPE: Document type identifier (securityPrincipal)</li>
     *   <li>NAME: Principal name for searching</li>
     *   <li>CREATED_DATE_TIME: Original creation timestamp</li>
     *   <li>UPDATED_DATE_TIME: Current update timestamp</li>
     *   <li>EXPIRY: Document expiration time</li>
     *   <li>PAYLOAD_CONTENT: JSON serialized principal object</li>
     * </ul>
     *
     * @param expiry the expiration timestamp for the document
     * @param event the SolrIkasanPrincipalRecord to convert
     * @return a SolrInputDocument ready for indexing
     */
    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, SolrIkasanPrincipalRecord event) {
        SolrInputDocument document = new SolrInputDocument();

        document.addField(ID, event.getName() + "-" + PRINCIPAL_TYPE);
        document.addField(TYPE, PRINCIPAL_TYPE);
        document.addField(NAME, event.getName());
        document.addField(IKASAN_PRINCIPAL_TYPE, event.getPrincipalType());
        document.addField(DESCRIPTION, event.getDescription());
        document.addField(PAYLOAD_CONTENT, event.getPrincipal());
        document.addField(ROLES_RELATED_ENTITY_COLLECTION, event.getRelatedRoleIdentifiers());
        document.addField(CREATED_DATE_TIME, event.getTimestamp());
        document.addField(UPDATED_DATE_TIME, System.currentTimeMillis());
        document.setField(EXPIRY, DO_NOT_EXPIRE);

        return document;
    }

    /**
     * Creates a new empty IkasanPrincipal instance.
     *
     * @return a new SolrIkasanPrincipalImpl instance
     */
    @Override
    public IkasanPrincipal createPrincipal() {
        return new SolrIkasanPrincipalImpl();
    }

    /**
     * Saves or updates a principal in the Solr index.
     *
     * This method converts the IkasanPrincipal object to JSON and stores it as a SolrIkasanPrincipalRecord.
     * If the principal already exists (same name), it will be updated. The principal is identified
     * by its name, which must be unique.
     *
     * Date handling:
     * <ul>
     *   <li>If createdDateTime is null, uses current system time</li>
     *   <li>If updatedDateTime is null, uses current system time</li>
     * </ul>
     *
     * @param principal the principal to save or update
     * @throws RuntimeException if the principal cannot be serialized to JSON
     */
    @Override
    public void saveOrUpdatePrincipal(IkasanPrincipal principal) {
        SolrIkasanPrincipalRecord record = new SolrIkasanPrincipalRecord();
        record.setName(principal.getName());
        record.setDescription(principal.getDescription());
        record.setPrincipalType(principal.getType());
        record.setTimestamp(principal.getCreatedDateTime() != null ? principal.getCreatedDateTime().getTime()
            : System.currentTimeMillis());
        record.setModifiedTimestamp(principal.getUpdatedDateTime() != null ? principal.getUpdatedDateTime().getTime()
            : System.currentTimeMillis());
        record.setRelatedRoleIdentifiers(principal.getRoles().stream()
            .map(Role::getId).map(String::valueOf).collect(Collectors.toList()));

        try {
            record.setPrincipal(OBJECT_MAPPER.writeValueAsString(principal));
        } catch (JacksonException e) {
            throw new RuntimeException("Cannot convert IkasanPrincipal to string! [" + principal.getName() + "]", e);
        }

        super.save(record);
    }

    /**
     * Saves or updates the provided list of IkasanPrincipal objects in the persistence layer.
     * Each principal is converted to a SolrIkasanPrincipalRecord and persisted.
     *
     * @param principals the list of IkasanPrincipal objects to save or update.
     *                   Each principal must include attributes such as name, description, type,
     *                   roles, and timestamp details. If createdDateTime or updatedDateTime
     *                   are null, the current system time will be used as a fallback.
     *                   Roles associated with each principal should be represented by their identifiers.
     *                   If an error occurs during the conversion of an IkasanPrincipal object to JSON,
     *                   a runtime exception is thrown.
     */
    @Override
    public void saveOrUpdatePrincipals(List<IkasanPrincipal> principals) {
        List<SolrIkasanPrincipalRecord> records = new ArrayList<>();

        principals.forEach(principal -> {
            SolrIkasanPrincipalRecord record = new SolrIkasanPrincipalRecord();
            record.setName(principal.getName());
            record.setDescription(principal.getDescription());
            record.setPrincipalType(principal.getType());
            record.setTimestamp(principal.getCreatedDateTime() != null ? principal.getCreatedDateTime().getTime()
                : System.currentTimeMillis());
            record.setModifiedTimestamp(principal.getUpdatedDateTime() != null ? principal.getUpdatedDateTime().getTime()
                : System.currentTimeMillis());
            record.setRelatedRoleIdentifiers(principal.getRoles().stream()
                .map(Role::getId).map(String::valueOf).collect(Collectors.toList()));

            try {
                record.setPrincipal(OBJECT_MAPPER.writeValueAsString(principal));
            } catch (JacksonException e) {
                throw new RuntimeException("Cannot convert IkasanPrincipal to string! [" + principal.getName() + "]", e);
            }

            records.add(record);
        });

        super.save(records);
    }

    /**
     * Deletes a principal from the Solr index.
     *
     * The principal is identified by its name and document type. This operation removes
     * the principal document from the Solr index permanently.
     *
     * @param principal the principal to delete
     */
    @Override
    public void deletePrincipal(IkasanPrincipal principal) {
        super.removeById(PRINCIPAL_TYPE, principal.getName() + "-" + PRINCIPAL_TYPE);
    }

    /**
     * Retrieves all principals from the Solr index.
     *
     * This method queries Solr for all documents with type "securityPrincipal" and
     * deserializes them into IkasanPrincipal objects.
     *
     * @return a list of all principals, or an empty list if no principals exist
     */
    @Override
    public List<IkasanPrincipal> getAllPrincipals() {
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + PRINCIPAL_TYPE + "\"");
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrIkasanPrincipalRecord> results = this.findByQuery(query, SolrIkasanPrincipalRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToPrincipal)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves principals with pagination and optional filtering.
     *
     * @param filter the filter criteria to apply (currently not implemented, may be null)
     * @param limit the maximum number of principals to return
     * @param offset the starting position in the result set
     * @return a list of principals matching the criteria
     */
    @Override
    public List<IkasanPrincipal> getPrincipals(IkasanPrincipalFilter filter, int limit, int offset) {
        StringBuilder queryBuffer = new StringBuilder(TYPE + COLON + "\"" + PRINCIPAL_TYPE + "\"");

        this.addOptionalFilteringLogic(filter, queryBuffer);

        SolrQuery query = new SolrQuery();
        query.setQuery(queryBuffer.toString());

        if(filter != null && filter.getSortColumn() != null && !filter.getSortColumn().isEmpty()) {
            query.setSort(filter.getSortColumn(), filter.getSortOrder() != null
                && filter.getSortOrder().equals("ASCENDING") ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
        }
        else {
            // Default search to created date time descending
            query.addSort(NAME, SolrQuery.ORDER.asc);
        }

        SearchResults<SolrIkasanPrincipalRecord> results = this.findByQuery(query, SolrIkasanPrincipalRecord.class, offset, limit);

        return results.getResultList().stream()
            .map(this::convertRecordToPrincipal)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves all lightweight principal representations from the Solr index.
     *
     * IkasanPrincipalLite objects contain less data than full IkasanPrincipal objects,
     * making them suitable for list operations and UI displays.
     *
     * @return a list of all principal lite objects, or an empty list if no principals exist
     */
    @Override
    public List<IkasanPrincipalLite> getAllPrincipalLites() {
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + PRINCIPAL_TYPE + "\"");
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrIkasanPrincipalRecord> results = this.findByQuery(query, SolrIkasanPrincipalRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToPrincipalLite)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves lightweight principals with pagination and optional filtering.
     *
     * This method supports filtering by name, description, and type. Filters are combined
     * using AND logic. All text filters use wildcard matching.
     *
     * @param filter the filter criteria to apply (may be null for unfiltered results)
     * @param limit the maximum number of principals to return
     * @param offset the starting position in the result set
     * @return a list of principal lite objects matching the criteria
     */
    @Override
    public List<IkasanPrincipalLite> getPrincipalLites(IkasanPrincipalFilter filter, int limit, int offset) {
        StringBuilder queryBuffer = new StringBuilder(TYPE + COLON + "\"" + PRINCIPAL_TYPE + "\"");

        this.addOptionalFilteringLogic(filter, queryBuffer);

        SolrQuery query = new SolrQuery();
        query.setQuery(queryBuffer.toString());

        if(filter != null && filter.getSortColumn() != null && !filter.getSortColumn().isEmpty()) {
            query.setSort(filter.getSortColumn(), filter.getSortOrder() != null
                && filter.getSortOrder().equals("ASCENDING") ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
        }
        else {
            // Default search to created date time descending
            query.addSort(NAME, SolrQuery.ORDER.asc);
        }

        SearchResults<SolrIkasanPrincipalRecord> results = this.findByQuery(query, SolrIkasanPrincipalRecord.class, offset, limit);

        return results.getResultList().stream()
            .map(this::convertRecordToPrincipalLite)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves a principal by its unique ID.
     *
     * This method performs an exact match query on the principal ID field in Solr.
     * The ID is expected to be in the format "{name}-{type}".
     *
     * @param id the unique ID of the principal to retrieve (format: name-securityPrincipal)
     * @return the IkasanPrincipal object if found, or {@code null} if no principal exists with the given ID
     */
    @Override
    public IkasanPrincipal findById(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }

        SolrQuery query = new SolrQuery();
        query.setQuery(ID + COLON + "\"" + id + "\"");
        query.setRows(1);

        SearchResults<SolrIkasanPrincipalRecord> results = this.findByQuery(query, SolrIkasanPrincipalRecord.class);

        if(results.getResultList().size() > 0) {
            return this.convertRecordToPrincipal(results.getResultList().get(0));
        }
        else {
            return null;
        }
    }

    /**
     * Retrieves a principal by its exact name.
     *
     * This method performs an exact match query on the principal name field in Solr.
     *
     * @param name the exact name of the principal to retrieve
     * @return the IkasanPrincipal object if found, or {@code null} if no principal exists with the given name
     */
    @Override
    public IkasanPrincipal getPrincipalByName(String name) {
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + PRINCIPAL_TYPE + "\" AND " + NAME + COLON + name);
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrIkasanPrincipalRecord> results = this.findByQuery(query, SolrIkasanPrincipalRecord.class);

        if(results.getResultList().size() > 0) {
            return this.convertRecordToPrincipal(results.getResultList().get(0));
        }
        else {
            return null;
        }
    }

    /**
     * Retrieves principals whose names contain the specified search term.
     *
     * This method performs a wildcard search on principal names using the pattern
     * "*searchTerm*". The search is case-sensitive and matches any principal name
     * that contains the given substring.
     *
     * @param name the search term to match against principal names
     * @return a list of principals whose names contain the search term, or an empty list if no matches found
     */
    @Override
    public List<IkasanPrincipal> getPrincipalByNameLike(String name) {
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + PRINCIPAL_TYPE + "\" AND " + NAME + COLON + "*" + name + "*");
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrIkasanPrincipalRecord> results = this.findByQuery(query, SolrIkasanPrincipalRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToPrincipal)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves all principals associated with a specific role.
     *
     * This method uses the relatedRoleIdentifiers collection in Solr to efficiently query
     * principals that have the specified role assigned. The role is identified by name,
     * and the method returns all matching principals.
     *
     * @param roleName the name of the role
     * @return a list of principals that have the specified role, or an empty list if none found
     */
    @Override
    public List<IkasanPrincipal> getAllPrincipalsWithRole(String roleName) {
        if (roleName == null || roleName.isEmpty()) {
            return List.of();
        }

        // Query using the relatedRoleIdentifiers collection field
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + PRINCIPAL_TYPE + "\"" + AND
            + ROLES_RELATED_ENTITY_COLLECTION + COLON + "\"" + roleName + "-" + ROLE_TYPE + "\"");
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrIkasanPrincipalRecord> results = this.findByQuery(query, SolrIkasanPrincipalRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToPrincipal)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves lightweight principals associated with a specific role, with pagination and filtering.
     *
     * This method uses the relatedRoleIdentifiers collection in Solr to efficiently query
     * principals that have the specified role assigned. Additional filtering by name, description,
     * and type can be applied through the IkasanPrincipalFilter parameter.
     *
     * @param roleName the name of the role
     * @param filter the filter criteria to apply (may be null for no additional filtering)
     * @param limit the maximum number of principals to return
     * @param offset the starting position in the result set
     * @return a list of principal lite objects that have the specified role and match the filter criteria
     */
    @Override
    public List<IkasanPrincipalLite> getAllPrincipalsWithRole(String roleName, IkasanPrincipalFilter filter, int limit, int offset) {
        if (roleName == null || roleName.isEmpty()) {
            return List.of();
        }

        // Build query starting with role constraint
        StringBuilder queryBuffer = new StringBuilder(TYPE + COLON + "\"" + PRINCIPAL_TYPE + "\" AND "
            + ROLES_RELATED_ENTITY_COLLECTION + COLON + "\"" + roleName + "-" + ROLE_TYPE + "\"");

        this.addOptionalFilteringLogic(filter, queryBuffer);

        SolrQuery query = new SolrQuery();
        query.setQuery(queryBuffer.toString());

        if(filter != null && filter.getSortColumn() != null && !filter.getSortColumn().isEmpty()) {
            query.setSort(filter.getSortColumn(), filter.getSortOrder() != null
                && filter.getSortOrder().equals("ASCENDING") ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
        }
        else {
            query.addSort(NAME, SolrQuery.ORDER.asc);
        }

        SearchResults<SolrIkasanPrincipalRecord> results = this.findByQuery(query, SolrIkasanPrincipalRecord.class, offset, limit);

        return results.getResultList().stream()
            .map(this::convertRecordToPrincipalLite)
            .collect(Collectors.toList());
    }

    /**
     * Efficiently retrieves only the names of principals with a specific role.
     *
     * This method delegates to Solr to retrieve only the 'name' field, making it much more
     * efficient than retrieving full principal objects when only names are needed.
     * It uses Solr's field list (fl) parameter to limit the fields returned.
     *
     * @param roleName the name of the role to filter by
     * @param filter optional filter for additional principal criteria
     * @param limit maximum number of results to return
     * @param offset offset for pagination
     * @return a list of principal names (strings) for principals with the specified role
     */
    @Override
    public List getAllPrincipalNamesWithRole(String roleName, IkasanPrincipalFilter filter, int limit, int offset) {
        if (roleName == null || roleName.isEmpty()) {
            return List.of();
        }

        // Build query starting with role constraint
        StringBuilder queryBuffer = new StringBuilder(TYPE + COLON + "\"" + PRINCIPAL_TYPE + "\" AND "
            + ROLES_RELATED_ENTITY_COLLECTION + COLON + "\"" + roleName + "-" + ROLE_TYPE + "\"");

        this.addOptionalFilteringLogic(filter, queryBuffer);

        SolrQuery query = new SolrQuery();
        query.setQuery(queryBuffer.toString());

        // Only retrieve the name field for efficiency
        query.setFields(NAME);

        if(filter != null && filter.getSortColumn() != null && !filter.getSortColumn().isEmpty()) {
            query.setSort(filter.getSortColumn(), filter.getSortOrder() != null
                && filter.getSortOrder().equals("ASCENDING") ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
        }
        else {
            query.addSort(NAME, SolrQuery.ORDER.asc);
        }

        SearchResults<SolrIkasanPrincipalRecord> results = this.findByQuery(query, SolrIkasanPrincipalRecord.class, offset, limit);

        return results.getResultList().stream()
            .map(SolrIkasanPrincipalRecord::getName)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves lightweight principals NOT associated with a specific role, with pagination and filtering.
     *
     * This method uses Solr's negation query syntax to find principals that do NOT have the
     * specified role in their relatedRoleIdentifiers collection. Additional filtering by name,
     * description, and type can be applied through the IkasanPrincipalFilter parameter.
     *
     * @param roleName the name of the role to exclude
     * @param filter the filter criteria to apply (may be null for no additional filtering)
     * @param limit the maximum number of principals to return
     * @param offset the starting position in the result set
     * @return a list of principal lite objects that do NOT have the specified role
     */
    @Override
    public List<IkasanPrincipalLite> getAllPrincipalsWithoutRole(String roleName, IkasanPrincipalFilter filter, int limit, int offset) {
        if (roleName == null || roleName.isEmpty()) {
            return List.of();
        }

        // Build query excluding the specified role using Solr's NOT syntax
        StringBuilder queryBuffer = new StringBuilder(TYPE + COLON + "\"" + PRINCIPAL_TYPE + "\"" + AND + NOT
            + ROLES_RELATED_ENTITY_COLLECTION + COLON + "\"" + roleName + "-" + ROLE_TYPE + "\"");

        this.addOptionalFilteringLogic(filter, queryBuffer);

        SolrQuery query = new SolrQuery();
        query.setQuery(queryBuffer.toString());

        if(filter != null && filter.getSortColumn() != null && !filter.getSortColumn().isEmpty()) {
            query.setSort(filter.getSortColumn(), filter.getSortOrder() != null
                && filter.getSortOrder().equals("ASCENDING") ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
        }
        else {
            query.addSort(NAME, SolrQuery.ORDER.asc);
        }

        SearchResults<SolrIkasanPrincipalRecord> results = this.findByQuery(query, SolrIkasanPrincipalRecord.class, offset, limit);

        return results.getResultList().stream()
            .map(this::convertRecordToPrincipalLite)
            .collect(Collectors.toList());
    }

    /**
     * Efficiently retrieves only the names of principals without a specific role.
     *
     * This method delegates to Solr to retrieve only the 'name' field, making it much more
     * efficient than retrieving full principal objects when only names are needed.
     * It uses Solr's field list (fl) parameter to limit the fields returned.
     *
     * @param roleName the name of the role to exclude
     * @param filter optional filter for additional principal criteria
     * @param limit maximum number of results to return
     * @param offset offset for pagination
     * @return a list of principal names (strings) for principals without the specified role
     */
    @Override
    public List getAllPrincipalNamesWithoutRole(String roleName, IkasanPrincipalFilter filter, int limit, int offset) {
        if (roleName == null || roleName.isEmpty()) {
            return List.of();
        }

        // Build query excluding the specified role using Solr's NOT syntax
        StringBuilder queryBuffer = new StringBuilder(TYPE + COLON + "\"" + PRINCIPAL_TYPE + "\"" + AND + NOT
            + ROLES_RELATED_ENTITY_COLLECTION + COLON + "\"" + roleName + "-" + ROLE_TYPE + "\"");

        this.addOptionalFilteringLogic(filter, queryBuffer);

        SolrQuery query = new SolrQuery();
        query.setQuery(queryBuffer.toString());

        // Only retrieve the name field for efficiency
        query.setFields(NAME);

        if(filter != null && filter.getSortColumn() != null && !filter.getSortColumn().isEmpty()) {
            query.setSort(filter.getSortColumn(), filter.getSortOrder() != null
                && filter.getSortOrder().equals("ASCENDING") ? SolrQuery.ORDER.asc : SolrQuery.ORDER.desc);
        }
        else {
            query.addSort(NAME, SolrQuery.ORDER.asc);
        }

        SearchResults<SolrIkasanPrincipalRecord> results = this.findByQuery(query, SolrIkasanPrincipalRecord.class, offset, limit);

        return results.getResultList().stream()
            .map(SolrIkasanPrincipalRecord::getName)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves principals by their role names.
     *
     * This method queries Solr for principals that have ANY of the specified roles.
     * Multiple role names are combined using OR logic, so a principal matching any
     * of the provided roles will be included in the results.
     *
     * @param roleNames list of role names to search for
     * @return a list of principals that have at least one of the specified roles, or an empty list if none found
     */
    @Override
    public List<IkasanPrincipal> getPrincipalsByRoleNames(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return List.of();
        }

        // Build query with OR logic for multiple role names
        StringBuilder queryBuffer = new StringBuilder(TYPE + COLON + "\"" + PRINCIPAL_TYPE + "\"");

        queryBuffer.append(AND).append(OPEN_BRACKET);
        List predicates = new ArrayList<>();
        roleNames.forEach(name -> predicates.add(ROLES_RELATED_ENTITY_COLLECTION +
            COLON + "\"" + name + "-" + ROLE_TYPE + "\""));
        queryBuffer.append(predicates.stream().collect(Collectors.joining(OR)));
        queryBuffer.append(CLOSE_BRACKET);


        SolrQuery query = new SolrQuery();
        query.setQuery(queryBuffer.toString());

        SearchResults<SolrIkasanPrincipalRecord> results = this.findByQuery(query, SolrIkasanPrincipalRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToPrincipal)
            .collect(Collectors.toList());
    }

    /**
     * Gets the total count of principals matching the filter criteria.
     *
     * This method supports filtering by name, description, and type. Filters are combined
     * using AND logic. All text filters use wildcard matching.
     *
     * @param filter the filter criteria to apply (may be null for total count)
     * @return the count of matching principals
     */
    @Override
    public int getPrincipalCount(IkasanPrincipalFilter filter) {
        StringBuilder queryBuffer = new StringBuilder(TYPE + COLON + "\"" + PRINCIPAL_TYPE + "\"");

        this.addOptionalFilteringLogic(filter, queryBuffer);

        SolrQuery query = new SolrQuery();
        query.setQuery(queryBuffer.toString());
        query.setRows(0);

        SearchResults<SolrIkasanPrincipalRecord> results = this.findByQuery(query, SolrIkasanPrincipalRecord.class);

        return (int) results.getTotalNumberOfResults();
    }

    /**
     * Gets the count of principals with a specific role.
     *
     * This method uses the relatedRoleIdentifiers collection in Solr to count principals
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

        // Build query starting with role constraint
        StringBuilder queryBuffer = new StringBuilder(TYPE + COLON + "\"" + PRINCIPAL_TYPE + "\"" + AND
            + ROLES_RELATED_ENTITY_COLLECTION + COLON + "\"" + roleName + "-" + ROLE_TYPE + "\"");

        this.addOptionalFilteringLogic(filter, queryBuffer);

        SolrQuery query = new SolrQuery();
        query.setQuery(queryBuffer.toString());
        query.setRows(0);

        SearchResults<SolrIkasanPrincipalRecord> results = this.findByQuery(query, SolrIkasanPrincipalRecord.class);

        return (int) results.getTotalNumberOfResults();
    }

    /**
     * Gets the count of principals without a specific role.
     *
     * This method uses Solr's negation query syntax to count principals that do NOT have
     * the specified role in their relatedRoleIdentifiers collection. Additional filtering by name,
     * description, and type can be applied through the IkasanPrincipalFilter parameter.
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

        // Build query excluding the specified role using Solr's NOT syntax
        StringBuilder queryBuffer = new StringBuilder(TYPE + COLON + "\"" + PRINCIPAL_TYPE + "\"" + AND + NOT
            + ROLES_RELATED_ENTITY_COLLECTION + COLON + "\"" + roleName + "-" + ROLE_TYPE + "\"");

        this.addOptionalFilteringLogic(filter, queryBuffer);

        SolrQuery query = new SolrQuery();
        query.setQuery(queryBuffer.toString());
        query.setRows(0);

        SearchResults<SolrIkasanPrincipalRecord> results = this.findByQuery(query, SolrIkasanPrincipalRecord.class);

        return (int) results.getTotalNumberOfResults();
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
    private IkasanPrincipal convertRecordToPrincipal(SolrIkasanPrincipalRecord record) {
        try {
            IkasanPrincipal ikasanPrincipal = OBJECT_MAPPER.readValue(record.getPrincipal()
                , SolrIkasanPrincipalImpl.class);

            if(record.getRelatedRoleIdentifiers() != null) {
                record.getRelatedRoleIdentifiers().forEach(roleId
                    -> ikasanPrincipal.addRole(this.solrRoleDao.getRoleById(roleId)));
            }

            return ikasanPrincipal;
        } catch (JacksonException e) {
            throw new RuntimeException("Cannot convert SolrIkasanPrincipalRecord to IkasanPrincipal! [" + record.getName() + "]", e);
        }
    }

    /**
     * Converts a SolrIkasanPrincipalRecord to an IkasanPrincipalLite object.
     *
     * This private helper method deserializes the JSON principal string stored in the
     * SolrIkasanPrincipalRecord into a SolrIkasanPrincipalLiteImpl object using Jackson ObjectMapper.
     *
     * @param record the SolrIkasanPrincipalRecord to convert
     * @return the deserialized IkasanPrincipalLite object
     * @throws RuntimeException if the JSON deserialization fails
     */
    private IkasanPrincipalLite convertRecordToPrincipalLite(SolrIkasanPrincipalRecord record) {
        try {
            return OBJECT_MAPPER.readValue(record.getPrincipal(), SolrIkasanPrincipalLiteImpl.class);
        } catch (JacksonException e) {
            throw new RuntimeException("Cannot convert SolrIkasanPrincipalRecord to IkasanPrincipalLite! [" + record.getName() + "]", e);
        }
    }

    /**
     * Adds optional filtering logic to the query buffer based on the provided {@code IkasanPrincipalFilter}.
     * The method appends conditions to the query buffer if the filter contains non-null and non-empty values
     * for type, name, or description filters.
     *
     * @param filter an {@code IkasanPrincipalFilter} containing the optional filter criteria, such as type,
     *               name, or description. If null or empty, no conditions are added to the query buffer.
     * @param queryBuffer a {@code StringBuilder} instance where the query conditions are appended
     *                    based on the filter criteria.
     */
    private void addOptionalFilteringLogic(IkasanPrincipalFilter filter, StringBuilder queryBuffer) {
        // Add optional filters
        if(filter != null && filter.getTypeFilter() != null && !filter.getTypeFilter().isEmpty()) {
            queryBuffer.append(" AND ").append(IKASAN_PRINCIPAL_TYPE).append(COLON)
                .append("*").append(filter.getTypeFilter()).append("*");
        }

        if (filter != null && filter.getNameFilter() != null && !filter.getNameFilter().isEmpty()) {
            queryBuffer.append(" AND ").append(NAME).append(COLON)
                .append("*").append(filter.getNameFilter()).append("*");
        }

        if (filter != null && filter.getDescriptionFilter() != null && !filter.getDescriptionFilter().isEmpty()) {
            queryBuffer.append(" AND ").append(DESCRIPTION).append(COLON)
                .append("*").append(filter.getDescriptionFilter()).append("*");
        }
    }
}
