package org.ikasan.mongo.persistence.general.model;

import org.ikasan.spec.search.model.IkasanESBDocument;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * MongoDB implementation of IkasanESBDocument.
 * This class represents an event stored in MongoDB.
 */
@Document(collection = "ikasanDocument")
public class MongoIkasanDocument implements IkasanESBDocument {

    @Id
    private String id;

    @Field("payload")
    private String event;

    @Field("type")
    private String type;

    @Field("moduleName")
    private String moduleName;

    @Field("flowName")
    private String flowName;

    @Field("componentName")
    private String componentName;

    @Field("timestamp")
    private long timeStamp;

    @Field("expiry")
    private long expiry;

    @Field("event")
    private String eventId;

    @Field("errorAction")
    private String errorAction;

    @Field("errorUri")
    private String errorUri;

    @Field("errorDetail")
    private String errorDetail;

    @Field("errorMessage")
    private String errorMessage;

    @Field("exceptionClass")
    private String exceptionClass;

    @Field("payloadRaw")
    private byte[] payloadRaw;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getIdentifier() {
        return id;
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
        return this.timeStamp;
    }

    @Override
    public String getEvent() {
        return event;
    }

    @Override
    public long getExpiry() {
        return this.expiry;
    }

    @Override
    public String getEventId() {
        return this.eventId;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public long getTimeStamp() {
        return timeStamp;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void setEvent(String event) {
        this.event = event;
    }

    @Override
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    @Override
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    @Override
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    @Override
    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    @Override
    public void setEventId(String eventId) {
        this.eventId = eventId;
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
    public String getErrorAction() {
        return errorAction;
    }

    @Override
    public void setErrorAction(String errorAction) {
        this.errorAction = errorAction;
    }

    @Override
    public String getErrorDetail() {
        return errorDetail;
    }

    @Override
    public void setErrorDetail(String errorDetail) {
        this.errorDetail = errorDetail;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String getExceptionClass() {
        return exceptionClass;
    }

    @Override
    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    @Override
    public byte[] getPayloadRaw() {
        return payloadRaw;
    }

    @Override
    public void setPayloadRaw(byte[] payloadRaw) {
        this.payloadRaw = payloadRaw;
    }

    @Override
    public String toString() {
        return "MongoIkasanDocument{" +
            "id='" + id + '\'' +
            ", event='" + event + '\'' +
            ", type='" + type + '\'' +
            ", moduleName='" + moduleName + '\'' +
            ", flowName='" + flowName + '\'' +
            ", componentName='" + componentName + '\'' +
            ", timeStamp=" + timeStamp +
            ", expiry=" + expiry +
            ", eventId='" + eventId + '\'' +
            ", errorAction='" + errorAction + '\'' +
            ", errorUri='" + errorUri + '\'' +
            ", errorDetail='" + errorDetail + '\'' +
            ", errorMessage='" + errorMessage + '\'' +
            ", exceptionClass='" + exceptionClass + '\'' +
            '}';
    }
}
