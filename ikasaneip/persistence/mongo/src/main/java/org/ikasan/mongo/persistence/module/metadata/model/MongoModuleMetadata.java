package org.ikasan.mongo.persistence.module.metadata.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * MongoDB entity for storing module metadata.
 * Stores the module metadata as a JSON string for efficient storage and retrieval.
 */
@Document(collection = "module_metadata")
public class MongoModuleMetadata {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("module_name")
    private String moduleName;

    @Indexed
    @Field("module_metadata_json")
    private String moduleMetadataJson;

    @Field("created_timestamp")
    private long createdTimestamp;

    /**
     * Default constructor for MongoDB
     */
    public MongoModuleMetadata() {
    }

    /**
     * Constructor with module name
     */
    public MongoModuleMetadata(String moduleName) {
        this.id = moduleName;
        this.moduleName = moduleName;
        this.createdTimestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getModuleMetadataJson() {
        return moduleMetadataJson;
    }

    public void setModuleMetadataJson(String moduleMetadataJson) {
        this.moduleMetadataJson = moduleMetadataJson;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    @Override
    public String toString() {
        return "MongoModuleMetadata{" +
            "id='" + id + '\'' +
            ", moduleName='" + moduleName + '\'' +
            ", createdTimestamp=" + createdTimestamp +
            '}';
    }
}
