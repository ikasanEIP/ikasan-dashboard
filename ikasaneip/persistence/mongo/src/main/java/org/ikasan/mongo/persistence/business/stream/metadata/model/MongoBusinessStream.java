package org.ikasan.mongo.persistence.business.stream.metadata.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * MongoDB implementation of BusinessStream entity.
 *
 * This entity stores business stream metadata in MongoDB, providing:
 * - Efficient storage and retrieval of business stream metadata
 * - Indexing capabilities for search operations
 * - Text search on business stream metadata JSON payload
 */
@Document(collection = "business_stream_metadata")
public class MongoBusinessStream {

    @Id
    private String id;

    @Indexed
    @Field("name")
    private String name;

    @Field("description")
    private String description;

    @Indexed
    @Field("business_stream_metadata")
    private String businessStreamMetadata;

    @Field("created_timestamp")
    private long createdTimestamp;

    /**
     * Default constructor for MongoDB
     */
    public MongoBusinessStream() {
    }

    /**
     * Constructor with ID
     */
    public MongoBusinessStream(String id) {
        this.id = id;
        this.createdTimestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBusinessStreamMetaData() {
        return businessStreamMetadata;
    }

    public void setBusinessStreamMetadata(String businessStreamMetadata) {
        this.businessStreamMetadata = businessStreamMetadata;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    @Override
    public String toString() {
        return "MongoBusinessStream{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", createdTimestamp=" + createdTimestamp +
            '}';
    }
}
