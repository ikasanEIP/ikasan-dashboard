package org.ikasan.mongo.persistence.wiretap.model;
import org.ikasan.mongo.persistence.general.model.MongoConstants;

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

    @Field("identifier")
    private Long identifier;

    @Indexed
    @Field("module_name")
    private String moduleName;

    @Indexed
    @Field("flow_name")
    private String flowName;

    @Indexed
    @Field("component_name")
    private String componentName;

    @Indexed
    @Field("event_id")
    private String eventId;

    @Field("related_event_id")
    private String relatedEventId;

    @Indexed
    @Field("timestamp")
    private long timestamp;

    @Indexed
    @Field("expiry")
    private long expiry;

    @Field("event")
    private String event;

    /**
     * Default constructor for MongoDB
     */
    public MongoWiretapEventImpl() {
    }

    /**
     * Constructor matching SolrWiretapEvent
     *
     * @param identifier the wiretap event identifier
     * @param moduleName the module name
     * @param flowName the flow name
     * @param componentName the component name
     * @param eventId the event ID
     * @param relatedEventId the related event ID
     * @param timestamp the event timestamp
     * @param event the event content as String
     */
    public MongoWiretapEventImpl(Long identifier, String moduleName, String flowName,
                                 String componentName, String eventId, String relatedEventId,
                                 long timestamp, String event) {
        this.identifier = identifier;
        this.id = moduleName + "-wiretap-" + identifier;
        this.moduleName = moduleName;
        this.flowName = flowName;
        this.componentName = componentName;
        this.eventId = eventId;
        this.relatedEventId = relatedEventId;
        this.timestamp = timestamp;
        this.event = event;
    }

    @Override
    public long getIdentifier() {
        return this.identifier != null ? this.identifier : 0L;
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

    public void setId(String id) {
        this.id = id;
    }

    public void setIdentifier(Long identifier) {
        this.identifier = identifier;
        // Update id when identifier changes
        if (this.moduleName != null && identifier != null) {
            this.id = this.moduleName + "-wiretap-" + identifier;
        }
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
        // Update id when module name changes
        if (moduleName != null && this.identifier != null) {
            this.id = moduleName + "-wiretap-" + this.identifier;
        }
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
                Objects.equals(identifier, that.identifier) &&
                Objects.equals(moduleName, that.moduleName) &&
                Objects.equals(flowName, that.flowName) &&
                Objects.equals(componentName, that.componentName) &&
                Objects.equals(eventId, that.eventId) &&
                Objects.equals(relatedEventId, that.relatedEventId) &&
                Objects.equals(event, that.event);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, identifier, moduleName, flowName, componentName,
                           eventId, relatedEventId, timestamp, expiry, event);
    }

    @Override
    public String toString() {
        return "MongoWiretapEventImpl{" +
                "id='" + id + '\'' +
                ", identifier=" + identifier +
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
