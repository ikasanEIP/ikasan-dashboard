package org.ikasan.mongo.persistence.replay.model;
import org.ikasan.mongo.persistence.general.model.MongoConstants;

import org.ikasan.spec.entity.EntityFields;
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
public class MongoReplayEventImpl implements ReplayEvent {

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
    private String eventId;

    @Field(EntityFields.PAYLOAD_CONTENT_RAW)
    private byte[] payloadRaw;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String eventAsString;

    @Indexed
    @Field(EntityFields.CREATED_DATE_TIME)
    private long timestamp;

    @Field(EntityFields.EXPIRY)
    private long expiry;

    /**
     * Default constructor
     */
    public MongoReplayEventImpl() {
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
    public MongoReplayEventImpl(String eventId, byte[] event, String eventAsString,
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    @Override
    public String toString() {
        return "MongoReplayEventImpl{" +
                "id='" + id + '\'' +
                ", moduleName='" + moduleName + '\'' +
                ", flowName='" + flowName + '\'' +
                ", eventId='" + eventId + '\'' +
                ", timestamp=" + timestamp +
                ", expiry=" + expiry +
                ", createdTimestamp=" + timestamp +
                '}';
    }
}
