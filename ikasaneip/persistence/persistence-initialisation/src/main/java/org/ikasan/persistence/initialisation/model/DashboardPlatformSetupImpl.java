package org.ikasan.persistence.initialisation.model;

import org.ikasan.persistence.initialisation.util.SetupObjectMapperFactory;
import org.ikasan.spec.persistence.model.DashboardPlatformSetup;
import org.ikasan.spec.persistence.model.DashboardSetupItem;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Solr implementation of DashboardPlatformSetup.
 */
public class DashboardPlatformSetupImpl implements DashboardPlatformSetup {
    private final JsonMapper objectMapper = SetupObjectMapperFactory.newInstance();

    private String id;

    private String type;

    private String platformSetupItems;

    private long timestamp;

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
        this.platformSetupItems = this.objectMapper.writeValueAsString(dashboardSetupItems);
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
        return "DashboardPlatformSetupImpl{" +
            "id='" + id + '\'' +
            ", type='" + type + '\'' +
            ", platformSetupItems=" + platformSetupItems +
            ", timestamp=" + timestamp +
            ", modifiedTimestamp=" + modifiedTimestamp +
            '}';
    }
}
