package org.ikasan.mongo.persistence.exclusion.model;
import org.ikasan.mongo.persistence.general.model.MongoConstants;

import org.ikasan.harvest.HarvestEvent;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.exclusion.ExclusionEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * MongoDB implementation of ExclusionEvent.
 *
 * @author Ikasan Development Team
 */
@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoExclusionEvent implements ExclusionEvent<String>, HarvestEvent {

    @Id
    private String id;

    @Field(EntityFields.TYPE)
    private String type;

    @Indexed
    @Field(EntityFields.MODULE_NAME)
    private String moduleName;

    @Indexed
    @Field(EntityFields.FLOW_NAME)
    private String flowName;

    @Indexed
    @Field(EntityFields.EVENT)
    private String identifier;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String event;

    @Indexed
    @Field(EntityFields.CREATED_DATE_TIME)
    private long timestamp;

    @Indexed
    @Field(EntityFields.ERROR_URI)
    private String errorUri;

    @Field(EntityFields.EXPIRY)
    private long expiry;

    /**
     * Default constructor
     */
    public MongoExclusionEvent() {
    }

    /**
     * Constructor
     *
     * @param moduleName
     * @param flowName
     * @param identifier
     * @param event
     * @param timestamp
     * @param errorUri
     */
    public MongoExclusionEvent(String moduleName, String flowName, String identifier,
                               String event, long timestamp, String errorUri) {
        this.moduleName = moduleName;
        this.flowName = flowName;
        this.identifier = identifier;
        this.event = event;
        this.timestamp = timestamp;
        this.errorUri = errorUri;
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

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    public String getFlowName() {
        return flowName;
    }

    @Override
    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public byte[] getEvent() {
        if (event != null) {
            return event.getBytes();
        }
        return "".getBytes();
    }

    @Override
    public void setEvent(byte[] event) {
        if (event != null) {
            this.event = new String(event);
        }
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String getErrorUri() {
        return errorUri;
    }

    @Override
    public void setErrorUri(String errorUri) {
        this.errorUri = errorUri;
    }

    @Override
    public boolean isHarvested() {
        // Not relevant for mongo implementation.
        return true;
    }

    @Override
    public void setHarvested(boolean harvested) {
        // Not relevant for mongo implementation.
    }

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MongoExclusionEvent that = (MongoExclusionEvent) o;

        if (!id.equals(that.id)) return false;
        if (!flowName.equals(that.flowName)) return false;
        if (!identifier.equals(that.identifier)) return false;
        if (!moduleName.equals(that.moduleName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = moduleName.hashCode();
        result = 31 * result + flowName.hashCode();
        result = 31 * result + identifier.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "MongoExclusionEvent{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", moduleName='" + moduleName + '\'' +
                ", flowName='" + flowName + '\'' +
                ", identifier='" + identifier + '\'' +
                ", event=" + event +
                ", timestamp=" + timestamp +
                ", errorUri='" + errorUri + '\'' +
                ", expiry=" + expiry +
                '}';
    }
}
