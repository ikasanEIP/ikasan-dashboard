package org.ikasan.mongo.persistence.configuration.metadata.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * MongoDB entity for storing component configuration metadata.
 * Stores the configuration metadata as a JSON string for efficient storage and retrieval.
 */
@Document(collection = "component_configuration_metadata")
public class MongoComponentConfigurationMetadata {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("configuration_id")
    private String configurationId;

    @Field("configuration_metadata_json")
    private String configurationMetadataJson;

    @Field("created_timestamp")
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
        this.configurationId = configurationId;
        this.createdTimestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
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
            "id='" + id + '\'' +
            ", configurationId='" + configurationId + '\'' +
            ", createdTimestamp=" + createdTimestamp +
            '}';
    }
}
