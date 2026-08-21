package org.ikasan.mongo.persistence.security.model;

import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.spec.entity.EntityFields;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoPolicyRecord {

    @Id
    private String id;

    @Field(EntityFields.TYPE)
    private String type;

    @Field(EntityFields.NAME)
    private String name;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String policy;

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
     * @param id the unique identifier to be set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Retrieves the type of the policy record.
     *
     * @return the type of the policy record as a String
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the policy record.
     *
     * @param type the type to set for this policy record; represents a category or classification
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Retrieves the name property of the object.
     *
     * @return the name as a String
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the policy record.
     *
     * @param name the name to be set for this policy record
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retrieves the policy content associated with this record.
     *
     * @return the policy content as a String
     */
    public String getPolicy() {
        return policy;
    }

    /**
     * Sets the policy value for the current object.
     *
     * @param policy the policy content to set; represents the configuration or rules associated with this entity
     */
    public void setPolicy(String policy) {
        this.policy = policy;
    }

    /**
     * Retrieves the list of role identifiers associated with this policy.
     *
     * @return a list of strings representing the related role identifiers.
     */
    public List<String> getRelatedRoleIdentifiers() {
        return relatedRoleIdentifiers;
    }

    /**
     * Sets the list of identifiers for related roles associated with the policy.
     *
     * @param relatedRoleIdentifiers the list of related role identifiers to be set
     */
    public void setRelatedRoleIdentifiers(List<String> relatedRoleIdentifiers) {
        this.relatedRoleIdentifiers = relatedRoleIdentifiers;
    }

    /**
     * Retrieves the timestamp representing the creation date and time.
     *
     * @return the timestamp value as a long.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp that represents the creation or relevant event time
     * associated with the policy record.
     *
     * @param timestamp the timestamp value to set, represented as a long.
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the timestamp indicating the last modification time
     * of this record in milliseconds since the epoch.
     *
     * @return the timestamp of the last modification
     */
    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    /**
     * Updates the timestamp marking the last modification time of this record.
     *
     * @param modifiedTimestamp the new modification timestamp to set, represented in milliseconds since the epoch
     */
    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    /**
     * Retrieves the expiry timestamp of the policy record.
     * This value represents the time at which the policy record becomes invalid or expires,
     * expressed in milliseconds since the epoch.
     *
     * @return the expiry timestamp as a long value in milliseconds since the epoch.
     */
    public long getExpiry() {
        return expiry;
    }

    /**
     * Sets the expiry time for the policy record.
     *
     * @param expiry the time, in milliseconds since the epoch, at which the policy record expires
     */
    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }
}
