package org.ikasan.job.orchestration.model.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ikasan.job.orchestration.exception.EntityConversionException;
import org.ikasan.job.orchestration.util.ObjectMapperFactory;
import org.ikasan.spec.scheduled.profile.model.ContextProfile;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;

import java.util.ArrayList;
import java.util.List;

public class ContextProfileRecordImpl implements ContextProfileRecord {
    private static ObjectMapper objectMapper = ObjectMapperFactory.newInstance();

    private String id;
    private String profileName;
    private String contextName;
    private String owner;
    private String contextProfile;
    private String accessRoles;
    private String accessUsers;
    private long createdDateTime;
    private long modifiedDateTime;
    private String modifiedBy;

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
        try {
            return objectMapper.readValue(this.contextProfile, ContextProfileImpl.class);
        }
        catch (JsonProcessingException e) {
            throw new EntityConversionException("Could not convert string to entity: " + this.contextProfile, e);
        }
    }

    @Override
    public void setContextProfile(ContextProfile contextProfile) {
        try {
            this.contextProfile = objectMapper.writeValueAsString(contextProfile);
        }
        catch (JsonProcessingException e) {
            throw new EntityConversionException("Could not convert entity to string: " + contextProfile, e);
        }
    }

    @Override
    public List<String> getAccessRoles() {
        if(this.accessRoles == null) return null;
        try {
            return objectMapper.readValue(this.accessRoles, ArrayList.class);
        }
        catch (JsonProcessingException e) {
            throw new EntityConversionException("Could not convert string to entity: " + this.accessRoles, e);
        }
    }

    @Override
    public void setAccessRoles(List<String> accessRoles) {
        try {
            this.accessRoles = objectMapper.writeValueAsString(accessRoles);
        }
        catch (JsonProcessingException e) {
            throw new EntityConversionException("Could not convert entity to string: " + accessRoles, e);
        }
    }

    @Override
    public List<String> getAccessUsers() {
        if(this.accessUsers == null) return null;
        try {
            return objectMapper.readValue(this.accessUsers, ArrayList.class);
        }
        catch (JsonProcessingException e) {
            throw new EntityConversionException("Could not convert string to entity: " + this.accessUsers, e);
        }
    }

    @Override
    public void setAccessUsers(List<String> accessUsers) {
        try {
            this.accessUsers = objectMapper.writeValueAsString(accessUsers);
        }
        catch (JsonProcessingException e) {
            throw new EntityConversionException("Could not convert entity to string: " + accessUsers, e);
        }
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
