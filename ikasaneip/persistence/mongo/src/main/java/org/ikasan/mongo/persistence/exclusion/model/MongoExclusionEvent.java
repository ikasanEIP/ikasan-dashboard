package org.ikasan.mongo.persistence.exclusion.model;

import org.ikasan.harvest.HarvestEvent;
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
@Document(collection = "exclusion_events")
public class MongoExclusionEvent implements ExclusionEvent<String>, HarvestEvent {

    @Id
    private String id;

    @Indexed
    @Field("module_name")
    private String moduleName;

    @Indexed
    @Field("flow_name")
    private String flowName;

    @Indexed
    @Field("identifier")
    private String identifier;

    @Field("event")
    private String event;

    @Indexed
    @Field("timestamp")
    private long timestamp;

    @Indexed
    @Field("error_uri")
    private String errorUri;

    @Field("harvested")
    private boolean harvested;

    @Field("expiry")
    private long expiry;

    @Indexed
    @Field("created_timestamp")
    private long createdTimestamp;

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

    public String getEventAsString() {
        return event;
    }

    public void setEventAsString(String event) {
        this.event = event;
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
        return harvested;
    }

    @Override
    public void setHarvested(boolean harvested) {
        this.harvested = harvested;
    }

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
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
                ", moduleName='" + moduleName + '\'' +
                ", flowName='" + flowName + '\'' +
                ", identifier='" + identifier + '\'' +
                ", event=" + event +
                ", timestamp=" + timestamp +
                ", errorUri='" + errorUri + '\'' +
                ", harvested=" + harvested +
                ", expiry=" + expiry +
                ", createdTimestamp=" + createdTimestamp +
                '}';
    }
}
