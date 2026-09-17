package org.ikasan.mongo.persistence.setup.model;

import org.ikasan.mongo.persistence.setup.util.MongoSetupObjectMapperFactory;
import org.ikasan.spec.entity.EntityFields;
import org.ikasan.spec.persistence.model.DashboardPlatformSetup;
import org.ikasan.spec.persistence.model.DashboardSetupItem;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB implementation of DashboardPlatformSetup.
 */
@Document(collection = "ikasan")
public class MongoDashboardPlatformSetupImpl implements DashboardPlatformSetup {
    @Transient
    private static final JsonMapper objectMapper = MongoSetupObjectMapperFactory.newInstance();

    @Id
    private String id;

    @Field(EntityFields.TYPE)
    private String type;

    @Field(EntityFields.PAYLOAD_CONTENT)
    private String platformSetupItems;

    @Field(EntityFields.CREATED_DATE_TIME)
    private long timestamp;

    @Field(EntityFields.UPDATED_DATE_TIME)
    private long modifiedTimestamp;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
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
    public List<DashboardSetupItem> getPlatformSetupItems() {
        if(this.platformSetupItems == null || this.platformSetupItems.isEmpty()) {
            return new ArrayList<>();
        }

        return this.objectMapper.readValue(platformSetupItems, new TypeReference<>() {});
    }

    @Override
    public void setPlatformSetupItems(List<DashboardSetupItem> dashboardSetupItems) {
        if (dashboardSetupItems == null || dashboardSetupItems.isEmpty()) {
            this.platformSetupItems = null;
        } else {
            this.platformSetupItems = this.objectMapper.writeValueAsString(dashboardSetupItems);
        }
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }

    @Override
    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }

    @Override
    public String toString() {
        return "MongoDashboardPlatformSetupImpl{" +
            "id='" + id + '\'' +
            ", type='" + type + '\'' +
            ", platformSetupItems=" + platformSetupItems +
            ", timestamp=" + timestamp +
            ", modifiedTimestamp=" + modifiedTimestamp +
            '}';
    }
}
