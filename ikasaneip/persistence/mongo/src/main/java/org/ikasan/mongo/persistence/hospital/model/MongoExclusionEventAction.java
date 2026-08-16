package org.ikasan.mongo.persistence.hospital.model;

import org.ikasan.harvest.HarvestEvent;
import org.ikasan.spec.hospital.model.ExclusionEventAction;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * MongoDB implementation of ExclusionEventAction.
 *
 * @author Ikasan Development Team
 */
@Document(collection = "exclusion_event_actions")
public class MongoExclusionEventAction implements ExclusionEventAction<String>, HarvestEvent {

    public static final String RESUBMIT = "re-submitted";
    public static final String IGNORED = "ignored";

    @Id
    private String id;

    @Indexed
    @Field("module_name")
    private String moduleName;

    @Indexed
    @Field("flow_name")
    private String flowName;

    @Indexed
    @Field("error_uri")
    private String errorUri;

    @Field("actioned_by")
    private String actionedBy;

    @Field("action")
    private String action;

    @Field("event")
    private String event;

    @Indexed
    @Field("timestamp")
    private long timestamp;

    @Field("comment")
    private String comment;

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
    public MongoExclusionEventAction() {
    }

    /**
     * Constructor
     *
     * @param moduleName
     * @param flowName
     * @param errorUri
     * @param actionedBy
     * @param action
     * @param event
     * @param timestamp
     * @param comment
     */
    public MongoExclusionEventAction(String moduleName, String flowName, String errorUri,
                                     String actionedBy, String action, String event,
                                     long timestamp, String comment) {
        this.moduleName = moduleName;
        this.flowName = flowName;
        this.errorUri = errorUri;
        this.actionedBy = actionedBy;
        this.action = action;
        this.event = event;
        this.timestamp = timestamp;
        this.comment = comment;
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
    public String getErrorUri() {
        return errorUri;
    }

    @Override
    public void setErrorUri(String errorUri) {
        this.errorUri = errorUri;
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
    public String getAction() {
        return action;
    }

    @Override
    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public String getEvent() {
        return event;
    }

    @Override
    public void setEvent(String event) {
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
    public String getComment() {
        return comment;
    }

    @Override
    public void setComment(String comment) {
        this.comment = comment;
    }

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
    public String toString() {
        final StringBuffer sb = new StringBuffer("MongoExclusionEventAction{");
        sb.append("id='").append(id).append('\'');
        sb.append(", moduleName='").append(moduleName).append('\'');
        sb.append(", flowName='").append(flowName).append('\'');
        sb.append(", errorUri='").append(errorUri).append('\'');
        sb.append(", actionedBy='").append(actionedBy).append('\'');
        sb.append(", action='").append(action).append('\'');
        sb.append(", event=").append(event).append('\'');
        sb.append(", timestamp=").append(timestamp);
        sb.append(", comment='").append(comment).append('\'');
        sb.append(", harvested=").append(harvested);
        sb.append(", expiry=").append(expiry);
        sb.append(", createdTimestamp=").append(createdTimestamp);
        sb.append('}');
        return sb.toString();
    }
}
