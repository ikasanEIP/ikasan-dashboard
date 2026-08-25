package org.ikasan.mongo.persistence.wiretap.model;
import org.ikasan.mongo.persistence.general.model.MongoConstants;

import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.wiretap.WiretapEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Objects;

/**
 * MongoDB implementation of WiretapEvent.
 *
 * @author Ikasan Development Team
 */
@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoWiretapEventImpl implements WiretapEvent<String> {

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
    @Field(EntityFields.COMPONENT_NAME)
    private String componentName;

    @Indexed
    @Field(EntityFields.EVENT)
    private String eventId;

    @Field(EntityFields.RELATED_EVENT)
    private String relatedEventId;

    @Indexed
    @Field(EntityFields.CREATED_DATE_TIME)
    private long timestamp;

    @Indexed
    @Field(EntityFields.EXPIRY)
    private long expiry;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String event;

    /**
     * Default constructor for MongoDB
     */
    public MongoWiretapEventImpl() {
    }

    @Override
    public long getIdentifier() {
        if(id.contains("-")) {
            return new Long(id.substring(id.lastIndexOf("-")+1));
        }
        else {
            return new Long(id);
        }
    }

    @Override
    public String getModuleName() {
        return this.moduleName;
    }

    @Override
    public String getFlowName() {
        return this.flowName;
    }

    @Override
    public String getComponentName() {
        return this.componentName;
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public String getEvent() {
        return this.event;
    }

    @Override
    public long getExpiry() {
        return this.expiry;
    }

    @Override
    public String getEventId() {
        return this.eventId;
    }

    public String getId() {
        return this.id;
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

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getRelatedEventId() {
        return this.relatedEventId;
    }

    public void setRelatedEventId(String relatedEventId) {
        this.relatedEventId = relatedEventId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MongoWiretapEventImpl that = (MongoWiretapEventImpl) o;
        return timestamp == that.timestamp &&
                expiry == that.expiry &&
                Objects.equals(id, that.id) &&
                Objects.equals(moduleName, that.moduleName) &&
                Objects.equals(flowName, that.flowName) &&
                Objects.equals(componentName, that.componentName) &&
                Objects.equals(eventId, that.eventId) &&
                Objects.equals(relatedEventId, that.relatedEventId) &&
                Objects.equals(event, that.event);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, moduleName, flowName, componentName,
                           eventId, relatedEventId, timestamp, expiry, event);
    }

    @Override
    public String toString() {
        return "MongoWiretapEventImpl{" +
                "id='" + id + '\'' +
                ", moduleName='" + moduleName + '\'' +
                ", flowName='" + flowName + '\'' +
                ", componentName='" + componentName + '\'' +
                ", eventId='" + eventId + '\'' +
                ", relatedEventId='" + relatedEventId + '\'' +
                ", timestamp=" + timestamp +
                ", expiry=" + expiry +
                '}';
    }
}
