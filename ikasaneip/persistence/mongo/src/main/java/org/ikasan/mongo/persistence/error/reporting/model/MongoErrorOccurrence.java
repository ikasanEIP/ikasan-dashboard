package org.ikasan.mongo.persistence.error.reporting.model;

import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.harvest.HarvestEvent;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.error.reporting.ErrorOccurrence;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoErrorOccurrence implements ErrorOccurrence<byte[]>, HarvestEvent {
    @Id
    private String id;

    @Indexed
    @Field(EntityFields.ERROR_URI)
    private String errorUri;

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
    private String flowElementName;

    @Field(EntityFields.ERROR_ACTION)
    private String errorAction;

    @Field(EntityFields.ERROR_DETAIL)
    private String errorDetail;

    @Field(EntityFields.ERROR_MESSAGE)
    private String errorMessage;

    @Field(EntityFields.EXCEPTION_CLASS)
    private String exceptionClass;

    @Indexed
    @Field(EntityFields.EVENT)
    private String eventLifeIdentifier;

    @Field(EntityFields.RELATED_EVENT)
    private String eventRelatedIdentifier;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String eventAsString;

    @Indexed
    @Field(EntityFields.EXPIRY)
    private Long expiry;

    @Indexed
    @Field(EntityFields.CREATED_DATE_TIME)
    private long timestamp;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
    public long getExpiry() {
        return this.expiry;
    }

    @Override
    public void setExpiry(long expiry)
    {
        this.expiry = expiry;
    }

    /**
     * @return the userAction
     */
    @Override
    public String getUserAction()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * @param userAction the userAction to set
     */
    @Override
    public void setUserAction(String userAction)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the actionedBy
     */
    @Override
    public String getActionedBy()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * @param actionedBy the actionedBy to set
     */
    @Override
    public void setActionedBy(String actionedBy)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the userActionTimestamp
     */
    @Override
    public long getUserActionTimestamp()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * @param userActionTimestamp the userActionTimestamp to set
     */
    @Override
    public void setUserActionTimestamp(long userActionTimestamp)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHarvested(boolean harvested)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("MongoErrorOccurrence{");
        sb.append("id='").append(id).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", errorUri='").append(errorUri).append('\'');
        sb.append(", moduleName='").append(moduleName).append('\'');
        sb.append(", flowName='").append(flowName).append('\'');
        sb.append(", flowElementName='").append(flowElementName).append('\'');
        sb.append(", errorAction='").append(errorAction).append('\'');
        sb.append(", exceptionClass='").append(exceptionClass).append('\'');
        sb.append(", eventLifeIdentifier='").append(eventLifeIdentifier).append('\'');
        sb.append(", timestamp=").append(timestamp);
        sb.append('}');
        return sb.toString();
    }
}
