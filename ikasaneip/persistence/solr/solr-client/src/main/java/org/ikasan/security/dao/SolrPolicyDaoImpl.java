package org.ikasan.security.dao;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.security.model.SolrPolicyImpl;
import org.ikasan.security.model.SolrPolicyRecord;
import org.ikasan.security.util.SolrSecurityObjectMapperFactory;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.security.dao.PolicyDao;
import org.ikasan.spec.security.model.Policy;
import org.ikasan.spec.solr.SolrDaoBase;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.stream.Collectors;

import static org.ikasan.security.dao.SolrRoleDaoImpl.ROLE_TYPE;

import static org.ikasan.spec.entity.EntityFields.*;

/**
 * Solr-based Data Access Object implementation for managing security policies.
 *
 * This DAO provides methods to perform CRUD operations on security policies stored
 * in a Solr index. Policies are serialized to JSON and stored as SolrPolicyRecord
 * documents with the type "securityPolicy".
 *
 * Key features:
 * <ul>
 *   <li>Policy storage and retrieval using Solr</li>
 *   <li>JSON serialization/deserialization of Policy objects</li>
 *   <li>Wildcard search capabilities by policy name</li>
 *   <li>PolicyLink objects are embedded within policies, not stored separately</li>
 *   <li>PolicyLinkTypes are predefined and not persisted in Solr</li>
 * </ul>
 *
 * @author Ikasan Development Team
 */
public class SolrPolicyDaoImpl extends SolrDaoBase<SolrPolicyRecord> implements PolicyDao {

    /** The Solr document type identifier for security policies */
    public static final String POLICY_TYPE = "securityPolicy";

    /** Jackson JsonMapper for JSON serialization/deserialization */
    private static final JsonMapper OBJECT_MAPPER = SolrSecurityObjectMapperFactory.newInstance();

    /**
     * Converts a SolrPolicyRecord entity into a Solr input document for indexing.
     *
     * This method maps the policy record fields to Solr document fields, including:
     * <ul>
     *   <li>ID: Composite key of policy name and type</li>
     *   <li>TYPE: Document type identifier (securityPolicy)</li>
     *   <li>NAME: Policy name for searching</li>
     *   <li>CREATED_DATE_TIME: Original creation timestamp</li>
     *   <li>UPDATED_DATE_TIME: Current update timestamp</li>
     *   <li>EXPIRY: Document expiration time</li>
     *   <li>PAYLOAD_CONTENT: JSON serialized policy object</li>
     * </ul>
     *
     * @param expiry the expiration timestamp for the document
     * @param event the SolrPolicyRecord to convert
     * @return a SolrInputDocument ready for indexing
     */
    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, SolrPolicyRecord event) {
        SolrInputDocument document = new SolrInputDocument();

        document.addField(ID, event.getName() + "-" + POLICY_TYPE);
        document.addField(TYPE, POLICY_TYPE);
        document.addField(NAME, event.getName());

        document.addField(CREATED_DATE_TIME, event.getTimestamp());
        document.addField(UPDATED_DATE_TIME, System.currentTimeMillis());
        document.setField(EXPIRY, DO_NOT_EXPIRE);
        document.addField(PAYLOAD_CONTENT, event.getPolicy());
        document.addField(ROLES_RELATED_ENTITY_COLLECTION, event.getRelatedRoleIdentifiers());

        return document;
    }

    /**
     * Creates a new empty Policy instance.
     *
     * @return a new SolrPolicyImpl instance
     */
    @Override
    public Policy createPolicy() {
        return new SolrPolicyImpl();
    }

    /**
     * Saves or updates a policy in the Solr index.
     *
     * This method converts the Policy object to JSON and stores it as a SolrPolicyRecord.
     * If the policy already exists (same name), it will be updated. The policy is identified
     * by its name, which must be unique.
     *
     * Date handling:
     * <ul>
     *   <li>If createdDateTime is null, uses current system time</li>
     *   <li>If updatedDateTime is null, uses current system time</li>
     * </ul>
     *
     * @param policy the policy to save or update
     * @throws RuntimeException if the policy cannot be serialized to JSON
     */
    @Override
    public void saveOrUpdatePolicy(Policy policy) {
        SolrPolicyRecord record = new SolrPolicyRecord();
        record.setName(policy.getName());
        record.setTimestamp(policy.getCreatedDateTime() != null ? policy.getCreatedDateTime().getTime()
            : System.currentTimeMillis());
        record.setModifiedTimestamp(policy.getUpdatedDateTime() != null ? policy.getUpdatedDateTime().getTime()
            : System.currentTimeMillis());

        try {
            record.setPolicy(OBJECT_MAPPER.writeValueAsString(policy));
        } catch (JacksonException e) {
            throw new RuntimeException("Cannot convert Policy to string! [" + policy.getName() + "]", e);
        }

        super.save(record);
    }

    /**
     * Deletes a policy from the Solr index.
     *
     * The policy is identified by its name and document type. This operation removes
     * the policy document from the Solr index permanently.
     *
     * @param policy the policy to delete
     */
    @Override
    public void deletePolicy(Policy policy) {
        super.removeById(POLICY_TYPE, policy.getName() + "-" + POLICY_TYPE);
    }

    /**
     * Retrieves all policies from the Solr index.
     *
     * This method queries Solr for all documents with type "securityPolicy" and
     * deserializes them into Policy objects.
     *
     * @return a list of all policies, or an empty list if no policies exist
     */
    @Override
    public List<Policy> getAllPolicies() {
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + POLICY_TYPE + "\"");
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrPolicyRecord> results = this.findByQuery(query, SolrPolicyRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToPolicy)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves all policies associated with a specific role.
     *
     * This method uses the {@code relatedRoleIdentifiers} collection field on
     * SolrPolicyRecord to efficiently query for policies that are associated with
     * the specified role name. The query filters by document type and the role
     * identifier in the related roles collection.
     *
     * @param roleName the name of the role to filter policies by
     * @return a list of policies associated with the given role, or an empty list if
     *         the role name is null/empty or no policies are associated with the role
     */
    @Override
    public List<Policy> getAllPoliciesWithRole(String roleName) {
        if (roleName == null || roleName.isEmpty()) {
            return List.of();
        }

        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + POLICY_TYPE + "\" AND "
            + ROLES_RELATED_ENTITY_COLLECTION + COLON + "\"" + roleName + "-" + ROLE_TYPE + "\"");
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrPolicyRecord> results = this.findByQuery(query, SolrPolicyRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToPolicy)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves a policy by its exact name.
     *
     * This method performs an exact match query on the policy name field in Solr.
     *
     * @param name the exact name of the policy to retrieve
     * @return the Policy object if found, or {@code null} if no policy exists with the given name
     */
    @Override
    public Policy getPolicyByName(String name) {
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + POLICY_TYPE + "\" AND " + NAME + COLON + name);
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrPolicyRecord> results = this.findByQuery(query, SolrPolicyRecord.class);

        if(results.getResultList().size() > 0) {
            return this.convertRecordToPolicy(results.getResultList().get(0));
        }
        else {
            return null;
        }
    }

    /**
     * Retrieves policies whose names contain the specified search term.
     *
     * This method performs a wildcard search on policy names using the pattern
     * "*searchTerm*". The search is case-sensitive and matches any policy name
     * that contains the given substring.
     *
     * @param name the search term to match against policy names
     * @return a list of policies whose names contain the search term, or an empty list if no matches found
     */
    @Override
    public List<Policy> getPolicyByNameLike(String name) {
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + POLICY_TYPE + "\" AND " + NAME + COLON + "*" + name + "*");
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrPolicyRecord> results = this.findByQuery(query, SolrPolicyRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToPolicy)
            .collect(Collectors.toList());
    }

    /**
     * Converts a SolrPolicyRecord to a Policy object.
     *
     * This private helper method deserializes the JSON policy string stored in the
     * SolrPolicyRecord into a SolrPolicyImpl object using Jackson ObjectMapper.
     *
     * @param record the SolrPolicyRecord to convert
     * @return the deserialized Policy object
     * @throws RuntimeException if the JSON deserialization fails
     */
    private Policy convertRecordToPolicy(SolrPolicyRecord record) {
        try {
            Policy policy = OBJECT_MAPPER.readValue(record.getPolicy(), SolrPolicyImpl.class);
            policy.setId(record.getId());
            return policy;
        } catch (JacksonException e) {
            throw new RuntimeException("Cannot convert SolrPolicyRecord to Policy! [" + record.getName() + "]", e);
        }
    }


    /**
     * Retrieves a Policy by its unique identifier from the Solr index.
     *
     * The identifier is the policy name, and the query filters by both ID and
     * document type to ensure the correct policy record is retrieved.
     *
     * @param id the unique identifier (name) of the policy to retrieve
     * @return the Policy object if a matching record is found, or {@code null}
     *         if no record exists for the provided identifier
     */
    @Override
    public Policy getPolicyById(String id) {
        SolrQuery query = super.buildIdQuery(id, POLICY_TYPE);

        SearchResults<SolrPolicyRecord> beans = this.findByQuery(query, SolrPolicyRecord.class);

        if(!beans.getResultList().isEmpty()) {
            return this.convertRecordToPolicy(beans.getResultList().get(0));
        }
        else {
            return null;
        }
    }


    /**
     * Retrieves a SolrPolicyRecord by its unique identifier.
     *
     * This method constructs a Solr query using the provided ID and retrieves the
     * corresponding SolrPolicyRecord from the Solr index. If no matching record is
     * found, the method returns {@code null}.
     *
     * @param id the unique identifier of the SolrPolicyRecord to retrieve
     * @return the SolrPolicyRecord if a matching record is found, or {@code null} if no record exists for the given ID
     */
    public SolrPolicyRecord getPolicyRecordById(String id) {
        SolrQuery query = super.buildIdQuery(id, POLICY_TYPE);

        SearchResults<SolrPolicyRecord> beans = this.findByQuery(query, SolrPolicyRecord.class);

        if(!beans.getResultList().isEmpty()) {
            return beans.getResultList().get(0);
        }
        else {
            return null;
        }
    }
}
