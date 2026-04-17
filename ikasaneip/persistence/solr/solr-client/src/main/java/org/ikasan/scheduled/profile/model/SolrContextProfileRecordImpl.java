package org.ikasan.scheduled.profile.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.solr.client.solrj.beans.Field;
import org.ikasan.scheduled.general.SolrEntityConversionException;
import org.ikasan.scheduled.util.ScheduledObjectMapperFactory;
import org.ikasan.spec.scheduled.profile.model.ContextProfile;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;
import org.ikasan.spec.solr.SolrDaoBase;

import java.util.ArrayList;
import java.util.List;

public class SolrContextProfileRecordImpl implements ContextProfileRecord {
    private static ObjectMapper objectMapper = ScheduledObjectMapperFactory.newInstance();

    @Field(SolrDaoBase.ID)
    private String id;

    @Field(SolrDaoBase.MODULE_NAME)
    private String profileName;

    @Field(SolrDaoBase.COMPONENT_NAME)
    private String contextName;

    @Field(SolrDaoBase.FLOW_NAME)
    private String owner;

    @Field(SolrDaoBase.PAYLOAD_CONTENT)
    private String contextProfile;

    @Field(SolrDaoBase.ACCESS_GROUPS)
    private String accessGroups;

    @Field(SolrDaoBase.ACCESS_USERS)
    private String accessUsers;

    @Field(SolrDaoBase.CREATED_DATE_TIME)
    private long createdDateTime;

    @Field(SolrDaoBase.UPDATED_DATE_TIME)
    private long modifiedDateTime;

    @Field(SolrDaoBase.MODIFIED_BY)
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
            return objectMapper.readValue(this.contextProfile, SolrContextProfileImpl.class);
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert string to entity: " + this.contextProfile, e);
        }
    }

    @Override
    public void setContextProfile(ContextProfile contextProfile) {
        try {
            this.contextProfile = objectMapper.writeValueAsString(contextProfile);
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert entity to string: " + contextProfile, e);
        }
    }

    @Override
    public List<String> getAccessGroups() {
        if(this.accessGroups == null) return null;
        try {
            return objectMapper.readValue(this.accessGroups, ArrayList.class);
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert string to entity: " + this.accessGroups, e);
        }
    }

    @Override
    public void setAccessGroups(List<String> accessGroups) {
        try {
            this.accessGroups = objectMapper.writeValueAsString(accessGroups);
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert entity to string: " + accessGroups, e);
        }
    }

    @Override
    public List<String> getAccessUsers() {
        if(this.accessUsers == null) return null;
        try {
            return objectMapper.readValue(this.accessUsers, ArrayList.class);
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert string to entity: " + this.accessUsers, e);
        }
    }

    @Override
    public void setAccessUsers(List<String> accessUsers) {
        try {
            this.accessUsers = objectMapper.writeValueAsString(accessUsers);
        }
        catch (JsonProcessingException e) {
            throw new SolrEntityConversionException("Could not convert entity to string: " + accessUsers, e);
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
