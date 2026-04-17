package org.ikasan.security.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.security.model.*;
import org.ikasan.security.util.SolrSecurityObjectMapperFactory;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.security.model.Policy;
import org.ikasan.spec.security.model.Role;
import org.ikasan.spec.security.model.RoleJobPlan;
import org.ikasan.spec.security.model.RoleModule;
import org.ikasan.spec.solr.SolrDaoBase;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Solr-based Data Access Object implementation for managing security roles.
 *
 * This DAO provides methods to perform CRUD operations on security roles stored
 * in a Solr index. Roles are serialized to JSON and stored as SolrRoleRecord
 * documents with the type "securityRole".
 *
 * Key features:
 * <ul>
 *   <li>Role storage and retrieval using Solr</li>
 *   <li>JSON serialization/deserialization of Role objects</li>
 *   <li>Wildcard search capabilities by role name</li>
 *   <li>RoleModule and RoleJobPlan objects are embedded within roles, not stored separately</li>
 *   <li>Support for role-based queries including ID and name lookups</li>
 * </ul>
 *
 * @author Ikasan Development Team
 */
public class SolrRoleDaoImpl extends SolrDaoBase<SolrRoleRecord> {

    /** The Solr document type identifier for security roles */
    public static final String ROLE_TYPE = "securityRole";

    /** Jackson ObjectMapper for JSON serialization/deserialization */
    private static final ObjectMapper OBJECT_MAPPER = SolrSecurityObjectMapperFactory.newInstance();

    private SolrPolicyDaoImpl solrPolicyDao;

    /**
     * Constructs a new instance of SolrRoleDaoImpl with the specified SolrPolicyDaoImpl.
     *
     * @param solrPolicyDao the SolrPolicyDaoImpl to be used for policy-related operations in this DAO implementation
     */
    public SolrRoleDaoImpl(SolrPolicyDaoImpl solrPolicyDao) {
        this.solrPolicyDao = solrPolicyDao;
    }

    /**
     * Converts a SolrRoleRecord entity into a Solr input document for indexing.
     *
     * This method maps the role record fields to Solr document fields, including:
     * <ul>
     *   <li>ID: Composite key of role name and type</li>
     *   <li>TYPE: Document type identifier (securityRole)</li>
     *   <li>NAME: Role name for searching</li>
     *   <li>CREATED_DATE_TIME: Original creation timestamp</li>
     *   <li>UPDATED_DATE_TIME: Current update timestamp</li>
     *   <li>EXPIRY: Document expiration time</li>
     *   <li>PAYLOAD_CONTENT: JSON serialized role object</li>
     * </ul>
     *
     * @param expiry the expiration timestamp for the document
     * @param event the SolrRoleRecord to convert
     * @return a SolrInputDocument ready for indexing
     */
    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, SolrRoleRecord event) {
        SolrInputDocument document = new SolrInputDocument();

        document.addField(ID, event.getName() + "-" + ROLE_TYPE);
        document.addField(TYPE, ROLE_TYPE);
        document.addField(NAME, event.getName());
        document.addField(PAYLOAD_CONTENT, event.getRole());
        document.addField(ROLE_POLICY_RELATED_ENTITY_COLLECTION, event.getRelatedPolicies());
        document.addField(ROLE_JOB_PLAN_RELATED_ENTITY_COLLECTION, event.getRelatedJobPlans());
        document.addField(ROLE_MODULE_RELATED_ENTITY_COLLECTION, event.getRelatedModules());

        document.addField(CREATED_DATE_TIME, event.getTimestamp());
        document.addField(UPDATED_DATE_TIME, System.currentTimeMillis());
        document.setField(EXPIRY, DO_NOT_EXPIRE);

        return document;
    }

    /**
     * Creates a new empty Role instance.
     *
     * @return a new SolrRoleImpl instance
     */
    public Role createRole() {
        return new SolrRoleImpl();
    }

    /**
     * Saves or updates a role in the Solr index.
     *
     * This method converts the Role object to JSON and stores it as a SolrRoleRecord.
     * If the role already exists (same name), it will be updated. The role is identified
     * by its name, which must be unique.
     *
     * Date handling:
     * <ul>
     *   <li>If createdDateTime is null, uses current system time</li>
     *   <li>If updatedDateTime is null, uses current system time</li>
     * </ul>
     *
     * @param role the role to save or update
     * @throws RuntimeException if the role cannot be serialized to JSON
     */
    public void saveOrUpdateRole(Role role) {
        SolrRoleRecord record = new SolrRoleRecord();
        record.setName(role.getName());
        record.setTimestamp(role.getCreatedDateTime() != null ? role.getCreatedDateTime().getTime() : System.currentTimeMillis());
        record.setModifiedTimestamp(role.getUpdatedDateTime() != null ? role.getUpdatedDateTime().getTime() : System.currentTimeMillis());
        record.setRelatedPolicies(role.getPolicies().stream()
            .map(Policy::getId).map(String::valueOf).collect(Collectors.toList()));
        record.setRelatedModules(role.getRoleModules().stream()
            .map(RoleModule::getModuleName).collect(Collectors.toList()));
        record.setRelatedJobPlans(role.getRoleJobPlans().stream()
            .map(RoleJobPlan::getJobPlanName).collect(Collectors.toList()));

        record.getRelatedPolicies().forEach(policyId -> {
            SolrPolicyRecord policy = this.solrPolicyDao.getPolicyRecordById(policyId);
            if(policy.getRelatedRoleIdentifiers() == null) policy.setRelatedRoleIdentifiers(new ArrayList<>());
            if(!policy.getRelatedRoleIdentifiers().contains(role.getId())) {
                policy.getRelatedRoleIdentifiers().add((String) role.getId());
                this.solrPolicyDao.save(policy);
            }
        });

        try {
            record.setRole(OBJECT_MAPPER.writeValueAsString(role));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot convert Role to string! [" + role.getName() + "]", e);
        }

        super.save(record);
    }

    /**
     * Deletes a role from the Solr index.
     *
     * The role is identified by its name and document type. This operation removes
     * the role document from the Solr index permanently.
     *
     * @param role the role to delete
     */
    public void deleteRole(Role role) {
        super.removeById(ROLE_TYPE, role.getName() + "-" + ROLE_TYPE);
    }

    /**
     * Associates a RoleModule instance with its corresponding Role and persists the updated Role.
     *
     * This method retrieves the Role associated with the provided RoleModule by the Role's name.
     * The RoleModule is then added to the Role, and the updated Role is saved or updated in the system.
     *
     * @param roleModule the RoleModule to save and associate with its corresponding Role
     */
    public void saveRoleModule(RoleModule roleModule) {
        if(roleModule.getRole() == null) {
            throw new IllegalArgumentException("RoleModule must have a non-null Role");
        }
        Role role = this.getRoleById((String)roleModule.getRole().getId());
        role.addRoleModule(roleModule);
        this.saveOrUpdateRole(role);
    }

    /**
     * Deletes the specified RoleModule from its associated Role and persists the changes.
     *
     * This method retrieves the Role associated with the given RoleModule by its ID,
     * removes the RoleModule from the Role's list of RoleModules, and updates the Role
     * in the system to reflect the change.
     *
     * @param roleModule the RoleModule instance to be removed from its associated Role
     */
    public void deleteRoleModule(RoleModule roleModule) {
        if(roleModule.getRole() == null) {
            throw new IllegalArgumentException("RoleModule must have a non-null Role");
        }
        Role role = this.getRoleById((String) roleModule.getRole().getId());
        role.getRoleModules().remove(roleModule);
        this.saveOrUpdateRole(role);
    }

    /**
     * Associates a RoleJobPlan instance with its corresponding Role and persists the updated Role.
     *
     * This method retrieves the Role associated with the provided RoleJobPlan by the Role's ID.
     * The RoleJobPlan is then added to the Role, and the updated Role is saved or updated in the system.
     *
     * @param roleJobPlan the RoleJobPlan to save and associate with its corresponding Role
     * @throws IllegalArgumentException if the RoleJobPlan does not have an associated Role
     */
    public void saveRoleJobPlan(RoleJobPlan roleJobPlan) {
        if(roleJobPlan.getRole() == null) {
            throw new IllegalArgumentException("RoleJobPlan must have a non-null Role");
        }
        Role role = this.getRoleById((String) roleJobPlan.getRole().getId());
        role.addRoleJobPlan(roleJobPlan);
        this.saveOrUpdateRole(role);
    }

    /**
     * Deletes the specified RoleJobPlan from its associated Role and persists the changes.
     *
     * This method retrieves the Role associated with the given RoleJobPlan by its ID,
     * removes the RoleJobPlan from the Role's list of RoleJobPlans, and updates the Role
     * in the system to reflect the change.
     *
     * @param roleJobPlan the RoleJobPlan instance to be removed from its associated Role
     * @throws IllegalArgumentException if the RoleJobPlan does not have an associated Role
     */
    public void deleteRoleJobPlan(RoleJobPlan roleJobPlan) {
        if(roleJobPlan.getRole() == null) {
            throw new IllegalArgumentException("RoleJobPlan must have a non-null Role");
        }
        Role role = this.getRoleById((String) roleJobPlan.getRole().getId());
        role.getRoleJobPlans().remove(roleJobPlan);
        this.saveOrUpdateRole(role);
    }

    /**
     * Retrieves all roles from the Solr index.
     *
     * This method queries Solr for all documents with type "securityRole" and
     * deserializes them into Role objects.
     *
     * @return a list of all roles, or an empty list if no roles exist
     */
    public List<Role> getAllRoles() {
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + ROLE_TYPE + "\"");
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrRoleRecord> results = this.findByQuery(query, SolrRoleRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToRole)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves a role by its exact name.
     *
     * This method performs an exact match query on the role name field in Solr.
     *
     * @param name the exact name of the role to retrieve
     * @return the Role object if found, or {@code null} if no role exists with the given name
     */
    public Role getRoleByName(String name) {
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + ROLE_TYPE + "\" AND " + NAME + COLON + name);
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrRoleRecord> results = this.findByQuery(query, SolrRoleRecord.class);

        if(results.getResultList().size() > 0) {
            return this.convertRecordToRole(results.getResultList().get(0));
        }
        else {
            return null;
        }
    }

    /**
     * Retrieves a Role by its unique identifier from the Solr index.
     *
     * The identifier is the role name, and the query filters by both ID and
     * document type to ensure the correct role record is retrieved.
     *
     * @param id the unique identifier (name) of the role to retrieve
     * @return the Role object if a matching record is found, or {@code null}
     *         if no record exists for the provided identifier
     */
    public Role getRoleById(String id) {
        SolrQuery query = super.buildIdQuery(id, ROLE_TYPE);

        SearchResults<SolrRoleRecord> beans = this.findByQuery(query, SolrRoleRecord.class);

        if(beans.getResultList().size() > 0) {
            return this.convertRecordToRole(beans.getResultList().get(0));
        }
        else {
            return null;
        }
    }

    /**
     * Retrieves roles whose names contain the specified search term.
     *
     * This method performs a wildcard search on role names using the pattern
     * "*searchTerm*". The search is case-sensitive and matches any role name
     * that contains the given substring.
     *
     * @param name the search term to match against role names
     * @return a list of roles whose names contain the search term, or an empty list if no matches found
     */
    public List<Role> getRoleByNameLike(String name) {
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + ROLE_TYPE + "\" AND " + NAME + COLON + "*" + name + "*");
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrRoleRecord> results = this.findByQuery(query, SolrRoleRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToRole)
            .collect(Collectors.toList());
    }

    /**
     * Retrieves all role job plans associated with a specific job plan name.
     *
     * This method uses the relatedJobPlans collection in Solr to efficiently query
     * roles that have the specified job plan assigned. It returns all RoleJobPlan
     * objects from matching roles that have the given job plan name.
     *
     * @param jobPlanName the name of the job plan
     * @return a list of RoleJobPlan objects that match the job plan name, or an empty list if none found
     */
    public List<RoleJobPlan> getRoleJobPlansByJobPlanName(String jobPlanName) {
        if (jobPlanName == null || jobPlanName.isEmpty()) {
            return List.of();
        }

        // Query using the relatedJobPlans collection field
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + ROLE_TYPE + "\"" + AND
            + ROLE_JOB_PLAN_RELATED_ENTITY_COLLECTION + COLON + "\"" + jobPlanName + "\"");
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrRoleRecord> results = this.findByQuery(query, SolrRoleRecord.class);

        // Extract all RoleJobPlans with matching job plan name from all matching roles
        return results.getResultList().stream()
            .map(this::convertRecordToRole)
            .flatMap(role -> role.getRoleJobPlans().stream())
            .filter(roleJobPlan -> jobPlanName.equals(roleJobPlan.getJobPlanName()))
            .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of roles associated with the specified policy identifier.
     *
     * This method queries the Solr index to find all roles that are related to the given policy ID.
     * The roles are mapped from their Solr representations to Role objects.
     *
     * @param policyId the unique identifier of the policy for which associated roles need to be retrieved
     * @return a list of roles associated with the specified policy, or an empty list if no roles are found
     */
    public List<Role> getRolesAssociatedWithPolicy(Object policyId) {
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + ROLE_TYPE + "\"" + AND
            + ROLE_POLICY_RELATED_ENTITY_COLLECTION + COLON + "\"" + policyId + "\"");
        query.setRows(Integer.MAX_VALUE);

        SearchResults<SolrRoleRecord> results = this.findByQuery(query, SolrRoleRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToRole)
            .collect(Collectors.toList());
    }

    /**
     * Converts a SolrRoleRecord into a Role object, including its associated policies,
     * modules, and job plans.
     *
     * @param record the SolrRoleRecord object containing the role data,
     *               as well as related policies, modules, and job plans.
     * @return the Role object constructed from the provided SolrRoleRecord.
     * @throws RuntimeException if the conversion from JSON or role processing fails.
     */
    private Role convertRecordToRole(SolrRoleRecord record) {
        try {
            Role role = OBJECT_MAPPER.readValue(record.getRole(), SolrRoleImpl.class);
            role.setId(record.getId());

            if(record.getRelatedPolicies() != null) {
                record.getRelatedPolicies().forEach(policyId
                    -> role.addPolicy(this.solrPolicyDao.getPolicyById(policyId)));
            }

            if(record.getRelatedModules() != null) {
                record.getRelatedModules().forEach(moduleName
                    -> {
                    RoleModule roleModule =  new SolrRoleModuleImpl();
                    roleModule.setRole(role);
                    roleModule.setModuleName(moduleName);
                    role.addRoleModule(roleModule);
                });
            }

            if(record.getRelatedJobPlans() != null) {
                record.getRelatedJobPlans().forEach(jobPlanName
                    -> {
                    RoleJobPlan roleJobPlan =  new SolrRoleJobPlanImpl();
                    roleJobPlan.setRole(role);
                    roleJobPlan.setJobPlanName(jobPlanName);
                    role.addRoleJobPlan(roleJobPlan);
                });
            }

            return role;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot convert SolrRoleRecord to Role! [" + record.getName() + "]", e);
        }
    }
}
