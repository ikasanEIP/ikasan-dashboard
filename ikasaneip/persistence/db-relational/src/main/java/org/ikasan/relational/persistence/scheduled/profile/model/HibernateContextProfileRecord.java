package org.ikasan.relational.persistence.scheduled.profile.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.ikasan.relational.persistence.scheduled.ScheduledConcurrentObjectMapperFactory;
import org.ikasan.spec.scheduled.profile.model.ContextProfile;
import org.ikasan.spec.scheduled.profile.model.ContextProfileRecord;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "context_profile")
public class HibernateContextProfileRecord implements ContextProfileRecord {

    private static final ObjectMapper objectMapper = ScheduledConcurrentObjectMapperFactory.newInstance();

    @Id
    @Column(name = "id", nullable = false, length = 767)
    private String id;

    @Column(name = "profile_name", nullable = false, length = 255)
    private String profileName;

    @Column(name = "context_name", nullable = false, length = 255)
    private String contextName;

    @Column(name = "owner", length = 255)
    private String owner;

    @Column(name = "context_profile", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String contextProfileJson;

    @Transient
    private ContextProfile contextProfile;

    @Column(name = "access_groups", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String accessGroupsJson;

    @Transient
    private List<String> accessGroups;

    @Column(name = "access_users", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String accessUsersJson;

    @Transient
    private List<String> accessUsers;

    @Column(name = "created_date_time", nullable = false)
    private long createdDateTime;

    @Column(name = "modified_date_time", nullable = false)
    private long modifiedDateTime;

    @Column(name = "modified_by", length = 255)
    private String modifiedBy;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        // Generate ID
        this.id = generateId(this.profileName, this.contextName);

        // Set timestamps
        if (this.createdDateTime == 0) {
            this.createdDateTime = System.currentTimeMillis();
        }
        this.modifiedDateTime = System.currentTimeMillis();
    }

    @PostLoad
    protected void onLoad() {
        // Deserialize context profile
        if (this.contextProfileJson != null) {
            try {
                this.contextProfile = objectMapper.readValue(this.contextProfileJson, ContextProfile.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize ContextProfile from JSON", e);
            }
        }

        // Deserialize access groups
        if (this.accessGroupsJson != null) {
            try {
                this.accessGroups = objectMapper.readValue(this.accessGroupsJson, new TypeReference<ArrayList<String>>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize accessGroups from JSON", e);
            }
        }

        // Deserialize access users
        if (this.accessUsersJson != null) {
            try {
                this.accessUsers = objectMapper.readValue(this.accessUsersJson, new TypeReference<ArrayList<String>>() {});
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize accessUsers from JSON", e);
            }
        }
    }

    private static String generateId(String profileName, String contextName) {
        return profileName + "_" + contextName;
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
        return contextProfile;
    }

    @Override
    public void setContextProfile(ContextProfile contextProfile) {
        this.contextProfile = contextProfile;
        // Serialize context profile
        if (this.contextProfile != null) {
            try {
                this.contextProfileJson = objectMapper.writeValueAsString(this.contextProfile);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize ContextProfile to JSON", e);
            }
        }
    }

    @Override
    public List<String> getAccessGroups() {
        return accessGroups;
    }

    @Override
    public void setAccessGroups(List<String> accessGroups) {
        this.accessGroups = accessGroups;
        // Serialize access groups
        if (this.accessGroups != null) {
            try {
                this.accessGroupsJson = objectMapper.writeValueAsString(this.accessGroups);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize accessGroups to JSON", e);
            }
        }
    }

    @Override
    public List<String> getAccessUsers() {
        return accessUsers;
    }

    @Override
    public void setAccessUsers(List<String> accessUsers) {
        this.accessUsers = accessUsers;
        // Serialize access users
        if (this.accessUsers != null) {
            try {
                this.accessUsersJson = objectMapper.writeValueAsString(this.accessUsers);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize accessUsers to JSON", e);
            }
        }
    }

    @Override
    public long getCreatedDateTime() {
        return createdDateTime;
    }

    @Override
    public void setCreatedDateTime(long createdDateTime) {
        this.createdDateTime = createdDateTime;
    }

    @Override
    public long getModifiedDateTime() {
        return modifiedDateTime;
    }

    @Override
    public void setModifiedDateTime(long modifiedDateTime) {
        this.modifiedDateTime = modifiedDateTime;
    }

    @Override
    public String getModifiedBy() {
        return modifiedBy;
    }

    @Override
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }
}
