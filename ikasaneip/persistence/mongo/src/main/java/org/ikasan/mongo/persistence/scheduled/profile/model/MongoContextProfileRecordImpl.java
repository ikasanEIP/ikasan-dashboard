package org.ikasan.mongo.persistence.scheduled.profile.model;

import org.ikasan.spec.scheduled.profile.model.ContextProfile;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB implementation of ContextProfileRecord.
 * This class represents a context profile stored in MongoDB.
 */
@Document(collection = "contextProfile")
public class MongoContextProfileRecordImpl implements ContextProfileRecord {

    @Id
    private String id;

    @Field("profileName")
    private String profileName;

    @Field("contextName")
    private String contextName;

    @Field("owner")
    private String owner;

    @Field("contextProfile")
    private ContextProfile contextProfile;

    @Field("accessGroups")
    private List<String> accessGroups;

    @Field("accessUsers")
    private List<String> accessUsers;

    @Field("createdDateTime")
    private long createdDateTime;

    @Field("modifiedDateTime")
    private long modifiedDateTime;

    @Field("modifiedBy")
    private String modifiedBy;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getProfileName() {
        return profileName;
    }

    @Override
    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    @Override
    public String getContextName() {
        return contextName;
    }

    @Override
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public ContextProfile getContextProfile() {
        if (this.contextProfile == null) {
            return null;
        }
        return this.contextProfile;
    }

    @Override
    public void setContextProfile(ContextProfile contextProfile) {
        this.contextProfile = contextProfile;
    }

    @Override
    public List<String> getAccessGroups() {
        return this.accessGroups;
    }

    @Override
    public void setAccessGroups(List<String> accessGroups) {
        this.accessGroups = accessGroups;
    }

    @Override
    public List<String> getAccessUsers() {
        return this.accessUsers;
    }

    @Override
    public void setAccessUsers(List<String> accessUsers) {
        this.accessUsers = accessUsers;
    }

    @Override
    public long getCreatedDateTime() {
        return this.createdDateTime;
    }

    @Override
    public void setCreatedDateTime(long createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    @Override
    public long getModifiedDateTime() {
        return this.modifiedDateTime;
    }

    @Override
    public void setModifiedDateTime(long modifiedDateTime) {
        this.modifiedDateTime = modifiedDateTime;
    }

    @Override
    public String getModifiedBy() {
        return this.modifiedBy;
    }

    @Override
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }
}
