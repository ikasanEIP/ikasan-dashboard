package org.ikasan.mongo.persistence.replay.model;

import org.ikasan.spec.replay.ReplayAudit;
import org.ikasan.spec.replay.ReplayAuditEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * MongoDB implementation of ReplayAuditEvent.
 *
 * @author Ikasan Development Team
 */
@Document(collection = "replay_audit_events")
public class MongoReplayAuditEvent implements ReplayAuditEvent<String> {

    @Id
    private String id;

    @Field("replay_audit_json")
    private String replayAuditJson;

    @Field("success")
    private boolean success;

    @Field("result_message")
    private String resultMessage;

    @Indexed
    @Field("timestamp")
    private long timestamp;

    @Field("expiry")
    private long expiry;

    @Indexed
    @Field("created_timestamp")
    private long createdTimestamp;

    // Transient field - not stored in MongoDB
    private transient ReplayAudit replayAudit;

    /**
     * Default constructor
     */
    public MongoReplayAuditEvent() {
    }

    /**
     * Constructor
     *
     * @param id
     * @param replayAudit
     * @param success
     * @param resultMessage
     * @param timestamp
     */
    public MongoReplayAuditEvent(String id, ReplayAudit replayAudit, boolean success,
                                 String resultMessage, long timestamp) {
        this.id = id;
        this.replayAudit = replayAudit;
        this.success = success;
        this.resultMessage = resultMessage;
        this.timestamp = timestamp;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public ReplayAudit getReplayAudit() {
        return replayAudit;
    }

    @Override
    public void setReplayAudit(ReplayAudit replayAudit) {
        this.replayAudit = replayAudit;
    }

    public String getReplayAuditJson() {
        return replayAuditJson;
    }

    public void setReplayAuditJson(String replayAuditJson) {
        this.replayAuditJson = replayAuditJson;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    public String getResultMessage() {
        return this.resultMessage;
    }

    @Override
    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    @Override
    public long getTimestamp() {
        return this.timestamp;
    }

    @Override
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
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
        return "MongoReplayAuditEvent{" +
                "id='" + id + '\'' +
                ", success=" + success +
                ", resultMessage='" + resultMessage + '\'' +
                ", timestamp=" + timestamp +
                ", expiry=" + expiry +
                ", createdTimestamp=" + createdTimestamp +
                '}';
    }
}
