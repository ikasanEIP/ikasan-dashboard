package org.ikasan.mongo.persistence.replay.model;
import org.ikasan.mongo.persistence.general.model.MongoConstants;

import org.ikasan.spec.entity.EntityFields;
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
@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoReplayAuditEventImpl implements ReplayAuditEvent<String> {

    @Id
    private String id;

    @Field(EntityFields.TYPE)
    private String type;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String replayAuditJson;

    private boolean success;

    private String resultMessage;

    @Indexed
    @Field(EntityFields.CREATED_DATE_TIME)
    private long timestamp;

    @Field(EntityFields.EXPIRY)
    private long expiry;

    // Transient field - not stored in MongoDB
    private transient ReplayAudit replayAudit;

    /**
     * Default constructor
     */
    public MongoReplayAuditEventImpl() {
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
    public MongoReplayAuditEventImpl(String id, ReplayAudit replayAudit, boolean success,
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    @Override
    public String toString() {
        return "MongoReplayAuditEventImpl{" +
                "id='" + id + '\'' +
                ", success=" + success +
                ", resultMessage='" + resultMessage + '\'' +
                ", timestamp=" + timestamp +
                ", expiry=" + expiry +
                '}';
    }
}
