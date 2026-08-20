package org.ikasan.mongo.persistence.systemevent.model;
import org.ikasan.mongo.persistence.general.model.MongoConstants;

import org.ikasan.spec.systemevent.SystemEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * MongoDB implementation of SystemEvent.
 *
 * @author Ikasan Development Team
 */
@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoSystemEventImpl implements SystemEvent {

    @Id
    private String id;

    @Indexed
    @Field("module_name")
    private String moduleName;

    @Indexed
    @Field("actor")
    private String actor;

    @Field("payload")
    private String payload;

    @Indexed
    @Field("action")
    private String action;

    @Indexed
    @Field("subject")
    private String subject;

    @Indexed
    @Field("timestamp")
    private Date timestamp;

    @Indexed
    @Field("expiry")
    private Date expiry;

    private transient Long systemEventId;

    /**
     * Default constructor required for MongoDB
     */
    public MongoSystemEventImpl() {
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
    public MongoSystemEventImpl(String moduleName, String actor, String action,
                                  String subject, Date timestamp) {
        this.moduleName = moduleName;
        this.actor = actor;
        this.action = action;
        this.subject = subject;
        this.timestamp = timestamp;
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public String getActor() {
        return actor;
    }

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

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MongoSystemEventImpl that = (MongoSystemEventImpl) o;
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
        return new StringJoiner(", ", MongoSystemEventImpl.class.getSimpleName() + "[", "]")
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
