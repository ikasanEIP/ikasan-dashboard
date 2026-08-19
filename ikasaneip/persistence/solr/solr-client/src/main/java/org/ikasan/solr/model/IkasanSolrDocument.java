package org.ikasan.solr.model;

import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.search.model.IkasanESBDocument;

/**
 * Solr implementation of IkasanDocument.
 * This class represents an event stored in Solr with fields annotated for Solr indexing.
 *
 * Created by Ikasan Development Team on 14/02/2017.
 */
public class IkasanSolrDocument implements IkasanESBDocument
{
    @Field(EntityFields.ID)
    private String id;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String event;

    @Field(EntityFields.TYPE)
    private String type;

    @Field(EntityFields.MODULE_NAME)
    private String moduleName;

    @Field(EntityFields.FLOW_NAME)
    private String flowName;

    @Field(EntityFields.COMPONENT_NAME)
    private String componentName;

    @Field(EntityFields.CREATED_DATE_TIME)
    private long timeStamp;

    @Field(EntityFields.EXPIRY)
    private long expiry;

    @Field(EntityFields.EVENT)
    private String eventId;

    @Field(EntityFields.ERROR_ACTION)
    private String errorAction;

    @Field(EntityFields.ERROR_URI)
    private String errorUri;

    @Field(EntityFields.ERROR_DETAIL)
    private String errorDetail;

    @Field(EntityFields.ERROR_MESSAGE)
    private String errorMessage;

    @Field(EntityFields.EXCEPTION_CLASS)
    private String exceptionClass;

    @Field(EntityFields.PAYLOAD_CONTENT_RAW)
    private byte[] payloadRaw;

    public String getId()
    {
        return id;
    }

    public String getIdentifier()
    {
        return id;
    }

    public String getModuleName()
    {
        return this.moduleName;
    }

    public String getFlowName()
    {
        return this.flowName;
    }

    public String getComponentName()
    {
        return this.componentName;
    }

    public long getTimestamp()
    {
        return this.timeStamp;
    }

    public String getEvent()
    {
        return event;
    }

    public long getExpiry()
    {
        return this.expiry;
    }

    public String getEventId()
    {
        return this.eventId;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public long getTimeStamp()
    {
        return timeStamp;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public void setEvent(String event)
    {
        this.event = event;
    }

    public void setModuleName(String moduleName)
    {
        this.moduleName = moduleName;
    }

    public void setFlowName(String flowName)
    {
        this.flowName = flowName;
    }

    public void setComponentName(String componentName)
    {
        this.componentName = componentName;
    }

    public void setTimeStamp(long timeStamp)
    {
        this.timeStamp = timeStamp;
    }

    public void setExpiry(long expiry)
    {
        this.expiry = expiry;
    }

    public void setEventId(String eventId)
    {
        this.eventId = eventId;
    }

    public String getErrorUri()
    {
        return errorUri;
    }

    public void setErrorUri(String errorUri)
    {
        this.errorUri = errorUri;
    }

    public String getErrorAction() {
        return errorAction;
    }

    public void setErrorAction(String errorAction) {
        this.errorAction = errorAction;
    }

    public String getErrorDetail()
    {
        return errorDetail;
    }

    public void setErrorDetail(String errorDetail)
    {
        this.errorDetail = errorDetail;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }

    public String getExceptionClass()
    {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass)
    {
        this.exceptionClass = exceptionClass;
    }

    public byte[] getPayloadRaw()
    {
        return payloadRaw;
    }

    public void setPayloadRaw(byte[] payloadRaw)
    {
        this.payloadRaw = payloadRaw;
    }

    @Override
    public String toString()
    {
        return "IkasanSolrDocument{" +
            "id='" + id + '\'' +
            ", event='" + event + '\'' +
            ", type='" + type + '\'' +
            ", moduleName='" + moduleName + '\'' +
            ", flowName='" + flowName + '\'' +
            ", componentName='" + componentName + '\'' +
            ", timeStamp=" + timeStamp +
            ", expiry=" + expiry +
            ", eventId='" + eventId + '\'' +
            ", errorUri='" + errorUri + '\'' +
            ", errorDetail='" + errorDetail + '\'' +
            ", errorMessage='" + errorMessage + '\'' +
            ", exceptionClass='" + exceptionClass + '\'' +
            '}';
    }
}
