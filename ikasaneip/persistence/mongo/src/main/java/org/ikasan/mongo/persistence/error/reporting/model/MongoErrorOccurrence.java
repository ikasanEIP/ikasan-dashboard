package org.ikasan.mongo.persistence.error.reporting.model;

import org.ikasan.harvest.HarvestEvent;
import org.ikasan.spec.error.reporting.ErrorOccurrence;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "error_occurrences")
public class MongoErrorOccurrence implements ErrorOccurrence<byte[]>, HarvestEvent {
    @Id
    private String id;

    @Indexed
    @Field("error_uri")
    private String errorUri;

    @Indexed
    @Field("module_name")
    private String moduleName;

    @Indexed
    @Field("flow_name")
    private String flowName;

    @Indexed
    @Field("component_name")
    private String flowElementName;

    @Field("error_action")
    private String errorAction;

    @Field("error_detail")
    private String errorDetail;

    @Field("error_message")
    private String errorMessage;

    @Field("exception_class")
    private String exceptionClass;

    @Indexed
    @Field("event_life_identifier")
    private String eventLifeIdentifier;

    @Field("event_related_identifier")
    private String eventRelatedIdentifier;

    @Field("event_as_string")
    private String eventAsString;

    @Indexed
    @Field("timestamp")
    private long timestamp;

    @Field("user_action")
    private String userAction;

    @Field("actioned_by")
    private String actionedBy;

    @Field("user_action_timestamp")
    private long userActionTimestamp;

    @Field("expiry")
    private long expiry;

    @Field("harvested")
    private boolean harvested;

    @Field("error_occurrence_json")
    private String errorOccurrenceJson;

    @Indexed
    @Field("created_timestamp")
    private long createdTimestamp;

    public MongoErrorOccurrence() {
    }

    public MongoErrorOccurrence(String uri, String moduleName, String flowName, String flowElementName,
                                String action, String errorDetail, String errorMessage, String exceptionClass,
                                String eventLifeIdentifier, String eventRelatedIdentifier, String eventAsString,
                                long timestamp) {
        this.errorUri = uri;
        this.moduleName = moduleName;
        this.flowName = flowName;
        this.flowElementName = flowElementName;
        this.errorAction = action;
        this.errorDetail = errorDetail;
        this.errorMessage = errorMessage;
        this.exceptionClass = exceptionClass;
        this.eventLifeIdentifier = eventLifeIdentifier;
        this.eventRelatedIdentifier = eventRelatedIdentifier;
        this.eventAsString = eventAsString;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getUri() {
        return errorUri;
    }

    @Override
    public void setUri(String uri) {
        this.errorUri = uri;
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
    public String getFlowElementName() {
        return flowElementName;
    }

    @Override
    public void setFlowElementName(String flowElementName) {
        this.flowElementName = flowElementName;
    }

    @Override
    public String getAction() {
        return errorAction;
    }

    @Override
    public void setAction(String action) {
        this.errorAction = action;
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
    public String getEventLifeIdentifier() {
        return eventLifeIdentifier;
    }

    @Override
    public void setEventLifeIdentifier(String eventLifeIdentifier) {
        this.eventLifeIdentifier = eventLifeIdentifier;
    }

    @Override
    public String getEventRelatedIdentifier() {
        return eventRelatedIdentifier;
    }

    @Override
    public void setEventRelatedIdentifier(String eventRelatedIdentifier) {
        this.eventRelatedIdentifier = eventRelatedIdentifier;
    }

    public String getEventAsString() {
        return eventAsString;
    }

    public void setEventAsString(String eventAsString) {
        this.eventAsString = eventAsString;
    }

    @Override
    public byte[] getEvent() {
        if (this.eventAsString != null) {
            return this.eventAsString.getBytes();
        }
        return "".getBytes();
    }

    @Override
    public void setEvent(byte[] event) {
        if (event != null) {
            this.eventAsString = new String(event);
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
    public String getUserAction() {
        return userAction;
    }

    @Override
    public void setUserAction(String userAction) {
        this.userAction = userAction;
    }

    @Override
    public String getActionedBy() {
        return actionedBy;
    }

    @Override
    public void setActionedBy(String actionedBy) {
        this.actionedBy = actionedBy;
    }

    @Override
    public long getUserActionTimestamp() {
        return userActionTimestamp;
    }

    @Override
    public void setUserActionTimestamp(long userActionTimestamp) {
        this.userActionTimestamp = userActionTimestamp;
    }

    @Override
    public long getExpiry() {
        return expiry;
    }

    @Override
    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    public boolean isHarvested() {
        return harvested;
    }

    @Override
    public void setHarvested(boolean harvested) {
        this.harvested = harvested;
    }

    public String getErrorOccurrenceJson() {
        return errorOccurrenceJson;
    }

    public void setErrorOccurrenceJson(String errorOccurrenceJson) {
        this.errorOccurrenceJson = errorOccurrenceJson;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("MongoErrorOccurrence{");
        sb.append("id='").append(id).append('\'');
        sb.append(", errorUri='").append(errorUri).append('\'');
        sb.append(", moduleName='").append(moduleName).append('\'');
        sb.append(", flowName='").append(flowName).append('\'');
        sb.append(", flowElementName='").append(flowElementName).append('\'');
        sb.append(", errorAction='").append(errorAction).append('\'');
        sb.append(", exceptionClass='").append(exceptionClass).append('\'');
        sb.append(", eventLifeIdentifier='").append(eventLifeIdentifier).append('\'');
        sb.append(", timestamp=").append(timestamp);
        sb.append(", createdTimestamp=").append(createdTimestamp);
        sb.append('}');
        return sb.toString();
    }
}
