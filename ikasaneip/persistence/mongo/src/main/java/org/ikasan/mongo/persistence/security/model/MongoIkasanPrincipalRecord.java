package org.ikasan.mongo.persistence.security.model;

import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.spec.entity.EntityFields;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

/**
 * Solr record for storing IkasanPrincipal data in the Solr index.
 *
 * @author Ikasan Development Team
 */
@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoIkasanPrincipalRecord {
    @Id
    private String id;

    @Field(EntityFields.TYPE)
    private String type;

    @Field(EntityFields.IKASAN_PRINCIPAL_TYPE)
    private String principalType;

    @Field(EntityFields.NAME)
    private String name;

    @Field(EntityFields.DESCRIPTION)
    private String description;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String principal;

    @Field(EntityFields.ROLES_RELATED_ENTITY_COLLECTION)
    private List<String> relatedRoleIdentifiers;

    @Field(EntityFields.CREATED_DATE_TIME)
    private long timestamp;

    @Field(EntityFields.UPDATED_DATE_TIME)
    private long modifiedTimestamp;

    @Field(EntityFields.EXPIRY)
    private long expiry;

    /**
     * Retrieves the unique identifier.
     *
     * @return the unique identifier as a String.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the identifier for the object.
     *
     * @param id the unique identifier to be assigned to the object
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Retrieves the type associated with this instance.
     *
     * @return the type as a String
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the object.
     *
     * @param type the type to be set; must be a non-null string representing the object's type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Retrieves the type of the principal associated with this record.
     *
     * @return the type of the principal as a String
     */
    public String getPrincipalType() {
        return principalType;
    }

    /**
     * Sets the principal type for this record.
     *
     * @param principalType the type of the principal to be set
     */
    public void setPrincipalType(String principalType) {
        this.principalType = principalType;
    }

    /**
     * Retrieves the name associated with this instance.
     *
     * @return the name as a String
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the instance.
     *
     * @param name the name to be set for the instance
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retrieves the description associated with this record.
     *
     * @return the description of the record
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description for this instance.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Retrieves the principal associated with this record.
     *
     * @return the principal as a String
     */
    public String getPrincipal() {
        return principal;
    }

    /**
     * Sets the principal value associated with this record.
     *
     * @param principal the principal to set, typically representing
     *                  an identity (e.g., user or system) tied to this record
     */
    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    /**
     * Retrieves the list of related role identifiers associated with this principal record.
     *
     * @return a list of strings representing the related role identifiers.
     */
    public List<String> getRelatedRoleIdentifiers() {
        return relatedRoleIdentifiers;
    }

    /**
     * Sets the related role identifiers associated with the principal.
     *
     * @param relatedRoleIdentifiers a list of role identifiers to be associated with the principal
     */
    public void setRelatedRoleIdentifiers(List<String> relatedRoleIdentifiers) {
        this.relatedRoleIdentifiers = relatedRoleIdentifiers;
    }

    /**
     * Retrieves the timestamp associated with this record.
     *
     * @return the timestamp value as a long.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp for this record.
     *
     * @param timestamp the timestamp value to set, represented as the number of milliseconds
     *                  since the epoch (January 1, 1970, 00:00:00 GMT).
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Retrieves the modified timestamp for the record.
     *
     * @return The modified timestamp as a long value.
     */
    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    /**
     * Sets the modified timestamp for this record.
     *
     * @param modifiedTimestamp the timestamp in milliseconds to set as the modified timestamp
     */
    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    /**
     * Retrieves the expiry timestamp associated with this record.
     *
     * @return the expiry timestamp as a long value.
     */
    public long getExpiry() {
        return expiry;
    }

    /**
     * Sets the expiry time for this entity.
     *
     * @param expiry the expiry timestamp in milliseconds since the epoch
     */
    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }
}
