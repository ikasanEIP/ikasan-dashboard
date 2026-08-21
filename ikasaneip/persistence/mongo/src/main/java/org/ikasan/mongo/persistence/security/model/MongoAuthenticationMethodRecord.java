package org.ikasan.mongo.persistence.security.model;



import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.security.dao.AuthenticationMethodDao;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoAuthenticationMethodRecord {

    @Id
    private String id;

    @Field(EntityFields.TYPE)
    private String type;

    @Field(EntityFields.NAME)
    private String name;

    @Field(EntityFields.ORDER)
    private Long order;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String authenticationMethod;

    @Field(EntityFields.CREATED_DATE_TIME)
    private long timestamp;

    @Field(EntityFields.UPDATED_DATE_TIME)
    private long modifiedTimestamp;

    @Field(EntityFields.EXPIRY)
    private long expiry;

    /**
     * Retrieves the unique identifier.
     *
     * @return the unique identifier as a String
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier for the authentication method record.
     *
     * @param id the unique identifier to be set for the record; must represent a non-null String value.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Retrieves the type associated with this authentication method record.
     *
     * @return the type as a String, representing the classification or category of the authentication method.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the authentication method record.
     *
     * @param type the type to be set for the record; must represent a non-null String value
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Retrieves the name.
     *
     * @return the name as a String
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name for the authentication method record. Also updates the
     * unique identifier (id) for the record based on the provided name
     * and a predefined authentication method type.
     *
     * @param name the name to set for the authentication method record;
     *             must be a non-null and non-empty String
     */
    public void setName(String name) {
        this.name = name;
        this.id = name + "-" + AuthenticationMethodDao.AUTHENTICATION_METHOD_TYPE;
    }

    /**
     * Retrieves the order associated with this record.
     *
     * @return the order value as a Long, or null if not set.
     */
    public Long getOrder() {
        return order;
    }

    /**
     * Sets the order value for the authentication method record.
     *
     * @param order the order value to be set
     */
    public void setOrder(Long order) {
        this.order = order;
    }

    /**
     * Retrieves the authentication method associated with this record.
     *
     * @return the authentication method as a string
     */
    public String getAuthenticationMethod() {
        return authenticationMethod;
    }

    /**
     * Sets the authentication method for this record.
     *
     * @param authenticationMethod the authentication method to associate with this record
     */
    public void setAuthenticationMethod(String authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    /**
     * Retrieves the timestamp representing the creation date and time of this record.
     *
     * @return the creation timestamp as a long value.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the creation timestamp of the record.
     *
     * @param timestamp the timestamp to set, representing the creation date and time in milliseconds since epoch
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Retrieves the timestamp of the last modification.
     *
     * @return the last modified timestamp as a long value
     */
    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    /**
     * Sets the modified timestamp of the record.
     *
     * @param modifiedTimestamp the timestamp indicating the last modification time, expressed in milliseconds since epoch
     */
    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    /**
     * Retrieves the expiry timestamp associated with this record.
     *
     * @return the expiry timestamp as a long value
     */
    public long getExpiry() {
        return expiry;
    }

    /**
     * Sets the expiry time for the authentication method record.
     *
     * @param expiry the expiry timestamp to set, represented as a long value
     */
    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }
}
