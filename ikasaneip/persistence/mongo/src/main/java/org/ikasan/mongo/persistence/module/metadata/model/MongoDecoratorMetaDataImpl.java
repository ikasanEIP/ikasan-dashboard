package org.ikasan.mongo.persistence.module.metadata.model;

import org.ikasan.spec.metadata.model.DecoratorMetaData;

/**
 * MongoDB implementation of DecoratorMetaData.
 */
public class MongoDecoratorMetaDataImpl implements DecoratorMetaData {
    private String type;
    private String name;
    private String configurationId;
    private boolean isConfigurable = false;

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
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
    public boolean isConfigurable() {
        return isConfigurable;
    }

    @Override
    public void setConfigurable(boolean configurable) {
        this.isConfigurable = configurable;
    }

    @Override
    public String getConfigurationId() {
        return this.configurationId;
    }

    @Override
    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    @Override
    public String toString() {
        return "MongoDecoratorMetaDataImpl{" +
            "type='" + type + '\'' +
            ", name='" + name + '\'' +
            ", configurationId='" + configurationId + '\'' +
            ", isConfigurable=" + isConfigurable +
            '}';
    }
}
