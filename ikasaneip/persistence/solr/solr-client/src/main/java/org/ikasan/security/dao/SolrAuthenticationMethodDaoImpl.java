package org.ikasan.security.dao;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrInputDocument;
import org.ikasan.security.model.SolrAuthenticationMethodImpl;
import org.ikasan.security.model.SolrAuthenticationMethodRecord;
import org.ikasan.security.util.SolrSecurityObjectMapperFactory;
import org.ikasan.spec.search.SearchResults;
import org.ikasan.spec.security.dao.AuthenticationMethodDao;
import org.ikasan.spec.security.model.AuthenticationMethod;
import org.ikasan.spec.solr.SolrDaoBase;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.stream.Collectors;

import static org.ikasan.spec.entity.EntityFields.*;

/**
 * Solr-based Data Access Object implementation for managing authentication methods.
 *
 * This DAO provides methods to perform CRUD operations on authentication methods stored
 * in a Solr index. Authentication methods are serialized to JSON and stored as
 * SolrAuthenticationMethodRecord documents with the type "securityAuthenticationMethod".
 *
 * Key features:
 * <ul>
 *   <li>Authentication method storage and retrieval using Solr</li>
 *   <li>JSON serialization/deserialization of AuthenticationMethod objects</li>
 *   <li>Support for ordering authentication methods</li>
 *   <li>Query by order for authentication method priority</li>
 * </ul>
 *
 * @author Ikasan Development Team
 */
public class SolrAuthenticationMethodDaoImpl extends SolrDaoBase<SolrAuthenticationMethodRecord> implements AuthenticationMethodDao {

    /** Jackson JsonMapper for JSON serialization/deserialization */
    private static final JsonMapper OBJECT_MAPPER = SolrSecurityObjectMapperFactory.newInstance();

    /**
     * Converts a SolrAuthenticationMethodRecord entity into a Solr input document for indexing.
     *
     * This method maps the authentication method record fields to Solr document fields, including:
     * <ul>
     *   <li>ID: Composite key of authentication method name and type</li>
     *   <li>TYPE: Document type identifier (securityAuthenticationMethod)</li>
     *   <li>NAME: Authentication method name for searching</li>
     *   <li>ORDER: Order/priority of the authentication method</li>
     *   <li>CREATED_DATE_TIME: Original creation timestamp</li>
     *   <li>UPDATED_DATE_TIME: Current update timestamp</li>
     *   <li>EXPIRY: Document expiration time</li>
     *   <li>PAYLOAD_CONTENT: JSON serialized authentication method object</li>
     * </ul>
     *
     * @param expiry the expiration timestamp for the document
     * @param event the SolrAuthenticationMethodRecord to convert
     * @return a SolrInputDocument ready for indexing
     */
    @Override
    protected SolrInputDocument convertEntityToSolrInputDocument(Long expiry, SolrAuthenticationMethodRecord event) {
        SolrInputDocument document = new SolrInputDocument();

        document.addField(ID, event.getName() + "-" + AUTHENTICATION_METHOD_TYPE);
        document.addField(TYPE, AUTHENTICATION_METHOD_TYPE);
        document.addField(NAME, event.getName());
        document.addField(ORDER, event.getOrder());

        document.addField(CREATED_DATE_TIME, event.getTimestamp());
        document.addField(UPDATED_DATE_TIME, System.currentTimeMillis());
        document.setField(EXPIRY, DO_NOT_EXPIRE);
        document.addField(PAYLOAD_CONTENT, event.getAuthenticationMethod());

        return document;
    }

    /**
     * Creates a new empty AuthenticationMethod instance.
     *
     * @return a new SolrAuthenticationMethodImpl instance
     */
    @Override
    public AuthenticationMethod createAuthenticationMethod() {
        return new SolrAuthenticationMethodImpl();
    }

    /**
     * Saves or updates an authentication method in the Solr index.
     *
     * This method converts the AuthenticationMethod object to JSON and stores it as a
     * SolrAuthenticationMethodRecord. If the authentication method already exists (same name),
     * it will be updated. The authentication method is identified by its name, which must be unique.
     *
     * @param authenticationMethod the authentication method to save or update
     * @throws RuntimeException if the authentication method cannot be serialized to JSON
     */
    @Override
    public void saveOrUpdateAuthenticationMethod(AuthenticationMethod authenticationMethod) {
        SolrAuthenticationMethodRecord record = new SolrAuthenticationMethodRecord();
        record.setName(authenticationMethod.getName());
        record.setOrder(authenticationMethod.getOrder());
        record.setTimestamp(authenticationMethod.getLastSynchronised() != null
            ? authenticationMethod.getLastSynchronised().getTime()
            : System.currentTimeMillis());
        record.setModifiedTimestamp(System.currentTimeMillis());

        try {
            record.setAuthenticationMethod(OBJECT_MAPPER.writeValueAsString(authenticationMethod));
        } catch (JacksonException e) {
            throw new RuntimeException("Cannot convert AuthenticationMethod to string! ["
                + authenticationMethod.getName() + "]", e);
        }

        super.save(record);
    }

    /**
     * Retrieves an authentication method by its ID.
     *
     * @param id the unique identifier of the authentication method
     * @return the authentication method, or null if not found
     */
    @Override
    public AuthenticationMethod getAuthenticationMethod(Object id) {
        SolrQuery query = new SolrQuery();
        query.setQuery(ID + COLON + "\"" + id + "\"");

        SearchResults<SolrAuthenticationMethodRecord> results = this.findByQuery(query, SolrAuthenticationMethodRecord.class);

        if (results.getResultList().isEmpty()) {
            return null;
        }

        return convertRecordToAuthenticationMethod(results.getResultList().get(0));
    }

    /**
     * Retrieves all authentication methods from the Solr index.
     *
     * @return a list of all authentication methods, ordered by their order field
     */
    @Override
    public List<AuthenticationMethod> getAuthenticationMethods() {
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + AUTHENTICATION_METHOD_TYPE + "\"");
        query.setRows(Integer.MAX_VALUE);
        query.addSort(ORDER, SolrQuery.ORDER.asc);

        SearchResults<SolrAuthenticationMethodRecord> results = this.findByQuery(query, SolrAuthenticationMethodRecord.class);

        return results.getResultList().stream()
            .map(this::convertRecordToAuthenticationMethod)
            .collect(Collectors.toList());
    }

    /**
     * Gets the total count of authentication methods in the Solr index.
     *
     * @return the number of authentication methods
     */
    @Override
    public long getNumberOfAuthenticationMethods() {
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + AUTHENTICATION_METHOD_TYPE + "\"");
        query.setRows(0);

        SearchResults<SolrAuthenticationMethodRecord> results = this.findByQuery(query, SolrAuthenticationMethodRecord.class);

        return results.getTotalNumberOfResults();
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
        SolrQuery query = new SolrQuery();
        query.setQuery(TYPE + COLON + "\"" + AUTHENTICATION_METHOD_TYPE + "\"" + AND
            + ORDER + COLON + "\"" + order + "\"");

        SearchResults<SolrAuthenticationMethodRecord> results = this.findByQuery(query, SolrAuthenticationMethodRecord.class);

        if (results.getResultList().isEmpty()) {
            return null;
        }

        return convertRecordToAuthenticationMethod(results.getResultList().get(0));
    }

    /**
     * Deletes an authentication method from the Solr index.
     *
     * The authentication method is identified by its name and document type. This operation
     * removes the authentication method document from the Solr index permanently.
     *
     * @param authenticationMethod the authentication method to delete
     */
    @Override
    public void deleteAuthenticationMethod(AuthenticationMethod authenticationMethod) {
        super.removeById(AUTHENTICATION_METHOD_TYPE, authenticationMethod.getName() + "-" + AUTHENTICATION_METHOD_TYPE);
    }

    /**
     * Converts a SolrAuthenticationMethodRecord into an AuthenticationMethod object.
     *
     * This method deserializes the JSON payload stored in the record back into a
     * SolrAuthenticationMethodImpl object.
     *
     * @param record the SolrAuthenticationMethodRecord to convert
     * @return the deserialized AuthenticationMethod object
     * @throws RuntimeException if the JSON cannot be deserialized
     */
    private AuthenticationMethod convertRecordToAuthenticationMethod(SolrAuthenticationMethodRecord record) {
        try {
            return OBJECT_MAPPER.readValue(record.getAuthenticationMethod(), SolrAuthenticationMethodImpl.class);
        } catch (JacksonException e) {
            throw new RuntimeException("Cannot convert SolrAuthenticationMethodRecord to AuthenticationMethod! ["
                + record.getName() + "]", e);
        }
    }
}
