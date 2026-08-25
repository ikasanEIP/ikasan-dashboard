package org.ikasan.mongo.persistence.scheduled.profile.model;

import org.ikasan.job.orchestration.util.ConcurrentObjectMapperFactory;
import org.ikasan.mongo.persistence.general.model.MongoConstants;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.scheduled.profile.model.ContextProfile;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB implementation of ContextProfileRecord.
 * This class represents a context profile stored in MongoDB.
 */
@Document(collection = MongoConstants.IKASAN_COLLECTION_NAME)
public class MongoContextProfileRecordImpl implements ContextProfileRecord {
    private static final JsonMapper objectMapper = ConcurrentObjectMapperFactory.newInstance();

    @Id
    private String id;

    @Field(EntityFields.MODULE_NAME)
    private String profileName;

    @Field(EntityFields.COMPONENT_NAME)
    private String contextName;

    @Field(EntityFields.FLOW_NAME)
    private String owner;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String contextProfile;

    @Field(EntityFields.ACCESS_GROUPS)
    private String accessGroups;

    @Field(EntityFields.ACCESS_USERS)
    private String accessUsers;

    @Field(EntityFields.CREATED_DATE_TIME)
    private long createdDateTime;

    @Field(EntityFields.UPDATED_DATE_TIME)
    private long modifiedDateTime;

    @Field(EntityFields.MODIFIED_BY)
    private String modifiedBy;

    @Indexed
    @Field(EntityFields.TYPE)
    private String type;

    @Indexed
    @Field(EntityFields.EXPIRY)
    private long expiry;

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
        if(this.contextProfile == null) return null;
        return objectMapper.readValue(this.contextProfile, MongoContextProfileImpl.class);
    }

    @Override
    public void setContextProfile(ContextProfile contextProfile) {
        this.contextProfile = objectMapper.writeValueAsString(contextProfile);
    }

    @Override
    public List<String> getAccessGroups() {
        if(this.accessGroups == null) return null;

        return objectMapper.readValue(this.accessGroups, ArrayList.class);
    }

    @Override
    public void setAccessGroups(List<String> accessGroups) {
        this.accessGroups = objectMapper.writeValueAsString(accessGroups);
    }

    @Override
    public List<String> getAccessUsers() {
        if(this.accessUsers == null) return null;

        return objectMapper.readValue(this.accessUsers, ArrayList.class);
    }

    @Override
    public void setAccessUsers(List<String> accessUsers) {
        this.accessUsers = objectMapper.writeValueAsString(accessUsers);
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getExpiry() {
        return expiry;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }
}
