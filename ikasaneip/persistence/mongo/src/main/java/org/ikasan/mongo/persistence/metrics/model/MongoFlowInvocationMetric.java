package org.ikasan.mongo.persistence.metrics.model;

import org.ikasan.spec.history.FlowInvocationMetric;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Collections;
import java.util.Set;

/**
 * MongoDB document for storing FlowInvocationMetric.
 *
 * @author Ikasan Development Team
 */
@Document(collection = "flow_invocation_metrics")
public class MongoFlowInvocationMetric implements FlowInvocationMetric<Object> {

    @Id
    private String id;

    @Indexed
    @Field("module_name")
    private String moduleName;

    @Indexed
    @Field("flow_name")
    private String flowName;

    @Indexed
    @Field("invocation_start_time")
    private long invocationStartTime;

    @Indexed
    @Field("invocation_end_time")
    private long invocationEndTime;

    @Field("final_action")
    private String finalAction;

    @Field("error_uri")
    private String errorUri;

    @Field("harvested")
    private Boolean harvested;

    @Field("expiry")
    private long expiry;

    @Indexed
    @Field("harvested_date_time")
    private long harvestedDateTime;

    @Indexed
    @Field("created_timestamp")
    private long createdTimestamp;

    public MongoFlowInvocationMetric() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getFlowName() {
        return flowName;
    }

    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    public long getInvocationStartTime() {
        return invocationStartTime;
    }

    public void setInvocationStartTime(long invocationStartTime) {
        this.invocationStartTime = invocationStartTime;
    }

    public long getInvocationEndTime() {
        return invocationEndTime;
    }

    public void setInvocationEndTime(long invocationEndTime) {
        this.invocationEndTime = invocationEndTime;
    }

    public String getFinalAction() {
        return finalAction;
    }

    public void setFinalAction(String finalAction) {
        this.finalAction = finalAction;
    }

    @Override
    public String getErrorUri() {
        return errorUri;
    }

    @Override
    public void setErrorUri(String errorUri) {
        this.errorUri = errorUri;
    }

    public Boolean getHarvested() {
        return harvested;
    }

    public void setHarvested(Boolean harvested) {
        this.harvested = harvested;
    }

    @Override
    public Set<Object> getFlowInvocationEvents() {
        return Collections.emptySet();
    }

    @Override
    public void setFlowInvocationEvents(Set<Object> flowInvocationEvents) {
        // Not stored separately in MongoDB - stored as JSON
    }

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    public long getHarvestedDateTime() {
        return harvestedDateTime;
    }

    public void setHarvestedDateTime(long harvestedDateTime) {
        this.harvestedDateTime = harvestedDateTime;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    @Override
    public String toString() {
        return "MongoFlowInvocationMetric{" +
                "id='" + id + '\'' +
                ", moduleName='" + moduleName + '\'' +
                ", flowName='" + flowName + '\'' +
                ", invocationStartTime=" + invocationStartTime +
                ", invocationEndTime=" + invocationEndTime +
                ", finalAction='" + finalAction + '\'' +
                ", errorUri='" + errorUri + '\'' +
                ", harvested=" + harvested +
                ", expiry=" + expiry +
                ", harvestedDateTime=" + harvestedDateTime +
                ", createdTimestamp=" + createdTimestamp +
                '}';
    }
}
