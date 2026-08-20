package org.ikasan.mongo.persistence.configuration.metadata.model;
import org.ikasan.mongo.persistence.general.model.MongoConstants;

import org.ikasan.spec.entity.EntityFields;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * MongoDB entity for storing component configuration metadata.
 * Stores the configuration metadata as a JSON string for efficient storage and retrieval.
 */
@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoComponentConfigurationMetadata {

    @Id
    private String id;

    @Field(EntityFields.TYPE)
    private String type;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String configurationMetadataJson;

    @Field(EntityFields.CREATED_DATE_TIME)
    private long createdTimestamp;

    /**
     * Default constructor for MongoDB
     */
    public MongoComponentConfigurationMetadata() {
    }

    /**
     * Constructor with configuration ID
     */
    public MongoComponentConfigurationMetadata(String configurationId) {
        this.id = configurationId;
        this.createdTimestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getConfigurationMetadataJson() {
        return configurationMetadataJson;
    }

    public void setConfigurationMetadataJson(String configurationMetadataJson) {
        this.configurationMetadataJson = configurationMetadataJson;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    @Override
    public String toString() {
        return "MongoComponentConfigurationMetadata{" +
            "type='" + type + '\'' +
            ", configurationId='" + id + '\'' +
            ", createdTimestamp=" + createdTimestamp +
            '}';
    }
}
