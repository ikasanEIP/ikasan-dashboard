package org.ikasan.mongo.persistence.business.stream.metadata.model;
import org.ikasan.mongo.persistence.general.model.MongoConstants;

import org.ikasan.spec.entity.EntityFields;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.StringJoiner;

/**
 * MongoDB implementation of BusinessStreamImpl entity.
 *
 * This entity stores business stream metadata in MongoDB, providing:
 * - Efficient storage and retrieval of business stream metadata
 * - Indexing capabilities for search operations
 * - Text search on business stream metadata JSON payload
 */
@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoBusinessStream {

    @Id
    private String id;

    @Field(EntityFields.TYPE)
    private String type;

    @Indexed
    @Field(EntityFields.MODULE_NAME)
    private String name;

    @Field(EntityFields.FLOW_NAME)
    private String description;

    @Indexed
    @Field(EntityFields.PAYLOAD_CONTENT)
    private String businessStreamMetadata;

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

    @Override
    public String toString() {
        return new StringJoiner(", ", MongoBusinessStream.class.getSimpleName() + "[", "]")
            .add("id='" + id + "'")
            .add("type='" + type + "'")
            .add("name='" + name + "'")
            .add("description='" + description + "'")
            .add("businessStreamMetadata='" + businessStreamMetadata + "'")
            .toString();
    }
}
