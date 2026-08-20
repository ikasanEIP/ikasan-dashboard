package org.ikasan.mongo.persistence.replay.model;
import org.ikasan.mongo.persistence.general.model.MongoConstants;

import org.ikasan.spec.replay.ReplayEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * MongoDB implementation of ReplayEvent.
 *
 * @author Ikasan Development Team
 */
@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoReplayEvent implements ReplayEvent {

    @Id
    private String id;

    @Indexed
    @Field("module_name")
    private String moduleName;

    @Indexed
    @Field("flow_name")
    private String flowName;

    @Indexed
    @Field("event_id")
    private String eventId;

    @Field("payload_raw")
    private byte[] payloadRaw;

    @Field("event_as_string")
    private String eventAsString;

    @Field("related_event_identifier")
    private String relatedEventIdentifier;

    @Indexed
    @Field("timestamp")
    private long timestamp;

    @Field("expiry")
    private long expiry;

    @Indexed
    @Field("created_timestamp")
    private long createdTimestamp;

    /**
     * Default constructor
     */
    public MongoReplayEvent() {
    }

    /**
     * Constructor
     *
     * @param eventId
     * @param event
     * @param eventAsString
     * @param moduleName
     * @param flowName
     * @param timeToLiveDays
     */
    public MongoReplayEvent(String eventId, byte[] event, String eventAsString,
                            String moduleName, String flowName, int timeToLiveDays) {
        this.eventId = eventId;
        this.payloadRaw = event;
        this.eventAsString = eventAsString;
        this.moduleName = moduleName;
        this.flowName = flowName;
        this.timestamp = System.currentTimeMillis();
        this.expiry = System.currentTimeMillis() + ((long) timeToLiveDays * 60 * 60 * 24 * 1000);
    }

    @Override
    public Long getId() {
        if (id != null) {
            return Long.parseLong(id);
        }
        return null;
    }

    public void setId(Long id) {
        if (id != null) {
            this.id = id.toString();
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdAsString() {
        return id;
    }

    @Override
    public byte[] getEvent() {
        return this.payloadRaw;
    }

    @Override
    public void setEvent(byte[] event) {
        this.payloadRaw = event;
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
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    @Override
    public long getExpiry() {
        return expiry;
    }

    @Override
    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    @Override
    public String getEventAsString() {
        return eventAsString;
    }

    @Override
    public void setEventAsString(String eventAsString) {
        this.eventAsString = eventAsString;
    }

    public String getRelatedEventIdentifier() {
        return relatedEventIdentifier;
    }

    public void setRelatedEventIdentifier(String relatedEventIdentifier) {
        this.relatedEventIdentifier = relatedEventIdentifier;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    @Override
    public String toString() {
        return "MongoReplayEvent{" +
                "id='" + id + '\'' +
                ", moduleName='" + moduleName + '\'' +
                ", flowName='" + flowName + '\'' +
                ", eventId='" + eventId + '\'' +
                ", timestamp=" + timestamp +
                ", expiry=" + expiry +
                ", createdTimestamp=" + createdTimestamp +
                '}';
    }
}
