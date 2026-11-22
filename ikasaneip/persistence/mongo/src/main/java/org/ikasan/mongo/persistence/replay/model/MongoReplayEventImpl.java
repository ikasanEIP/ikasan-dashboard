package org.ikasan.mongo.persistence.replay.model;
import org.ikasan.mongo.persistence.general.model.MongoConstants;

import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.replay.ReplayEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Objects;

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

    @Field(EntityFields.MODULE_NAME)
    private String moduleName;

    @Field(EntityFields.FLOW_NAME)
    private String flowName;

    @Field(EntityFields.EVENT)
    private String eventId;

    @Field(EntityFields.PAYLOAD_CONTENT_RAW)
    private byte[] payloadRaw;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String eventAsString;

    @Field(EntityFields.CREATED_DATE_TIME)
    private Long timestamp = 0L;

    @Field(EntityFields.EXPIRY)
    private Long expiry = 0L;

    @Field(EntityFields.HARVEST_RECEIVED_TIMESTAMP)
    private Long harvestReceivedTimestamp = 0L;

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

    public long getHarvestReceivedTimestamp() {
        return harvestReceivedTimestamp;
    }

    public void setHarvestReceivedTimestamp(long harvestReceivedTimestamp) {
        this.harvestReceivedTimestamp = harvestReceivedTimestamp;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MongoReplayEventImpl other = (MongoReplayEventImpl) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((eventId == null) ? 0 : eventId.hashCode());
        result = prime * result + ((flowName == null) ? 0 : flowName.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((moduleName == null) ? 0 : moduleName.hashCode());
        result = prime * result + ((timestamp == null) ? 0 : Long.hashCode(timestamp));
        return result;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("MongoReplayEventImpl{");
        sb.append("id='").append(id).append('\'');
        sb.append(", moduleName='").append(moduleName).append('\'');
        sb.append(", flowName='").append(flowName).append('\'');
        sb.append(", eventId='").append(eventId).append('\'');
        sb.append(", event='").append(payloadRaw).append('\'');
        sb.append(", eventAsString='").append(eventAsString).append('\'');
        sb.append(", timestamp=").append(timestamp);
        sb.append(", expiry=").append(expiry);
        sb.append('}');
        return sb.toString();
    }
}
