package org.ikasan.mongo.persistence.systemevent.model;

import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.systemevent.SystemEventRecord;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * MongoDB implementation of SystemEventRecord.
 *
 * @author Ikasan Development Team
 */
@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoSystemEventRecordImpl implements SystemEventRecord {

    @Id
    private String id;

    @Field(EntityFields.TYPE)
    private String type;

    @Indexed
    @Field(EntityFields.MODULE_NAME)
    private String moduleName;

    @Indexed
    @Field(EntityFields.ACTOR)
    private String actor;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String payload;

    @Indexed
    @Field(EntityFields.SYSTEM_EVENT_ACTION)
    private String action;

    @Indexed
    @Field(EntityFields.SYSTEM_EVENT_SUBJECT)
    private String subject;

    @Indexed
    @Field(EntityFields.CREATED_DATE_TIME)
    private Date timestamp;

    @Indexed
    @Field(EntityFields.EXPIRY)
    private Date expiry;

    private transient Long systemEventId;

    /**
     * Default constructor required for MongoDB
     */
    public MongoSystemEventRecordImpl() {
    }

    /**
     * Constructor
     *
     * @param moduleName the module name
     * @param actor the actor
     * @param action the action
     * @param subject the subject
     * @param timestamp the timestamp
     */
    public MongoSystemEventRecordImpl(String moduleName, String actor, String action,
                                      String subject, Date timestamp) {
        this.moduleName = moduleName;
        this.actor = actor;
        this.action = action;
        this.subject = subject;
        this.timestamp = timestamp;
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
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
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
    public String getActor() {
        return actor;
    }

    @Override
    public void setActor(String actor) {
        this.actor = actor;
    }

    @Override
    public Long getId() {
        if (this.systemEventId != null) {
            return this.systemEventId;
        }
        // Try to extract from MongoDB id if it follows the pattern
        if (this.id != null) {
            try {
                String[] parts = this.id.split("-");
                if (parts.length >= 3) {
                    return Long.parseLong(parts[parts.length - 1]);
                }
            } catch (NumberFormatException e) {
                // If parsing fails, return null
            }
        }
        return null;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMongoId() {
        return id;
    }

    public void setSystemEventId(Long systemEventId) {
        this.systemEventId = systemEventId;
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Override
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public Date getExpiry() {
        return expiry;
    }

    public void setExpiry(Date expiry) {
        this.expiry = expiry;
    }

    @Override
    public String getPayload() {
        return payload;
    }

    @Override
    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MongoSystemEventRecordImpl that = (MongoSystemEventRecordImpl) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(moduleName, that.moduleName) &&
                Objects.equals(actor, that.actor) &&
                Objects.equals(action, that.action) &&
                Objects.equals(subject, that.subject) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, moduleName, actor, action, subject, timestamp);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MongoSystemEventRecordImpl.class.getSimpleName() + "[", "]")
                .add("id='" + id + "'")
                .add("moduleName='" + moduleName + "'")
                .add("actor='" + actor + "'")
                .add("action='" + action + "'")
                .add("subject='" + subject + "'")
                .add("timestamp=" + timestamp)
                .add("expiry=" + expiry)
                .toString();
    }
}
