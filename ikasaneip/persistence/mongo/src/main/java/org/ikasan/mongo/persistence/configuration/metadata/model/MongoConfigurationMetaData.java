package org.ikasan.mongo.persistence.configuration.metadata.model;

import org.ikasan.spec.metadata.model.ConfigurationMetaData;

import java.util.List;

/**
 * MongoDB implementation of ConfigurationMetaData.
 */
public class MongoConfigurationMetaData implements ConfigurationMetaData<List<MongoConfigurationParameterMetaData>> {
    private String configurationId;
    private List<MongoConfigurationParameterMetaData> parameters;
    private String description;
    private String implementingClass;

    /**
     * Constructor
     */
    public MongoConfigurationMetaData(String configurationId, List<MongoConfigurationParameterMetaData> parameters,
                                      String description, String implementingClass) {
        this.configurationId = configurationId;
        this.parameters = parameters;
        this.description = description;
        this.implementingClass = implementingClass;
    }

    /**
     * Default constructor
     */
    public MongoConfigurationMetaData() {
    }

    @Override
    public String getConfigurationId() {
        return this.configurationId;
    }

    @Override
    public List<MongoConfigurationParameterMetaData> getParameters() {
        return this.parameters;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String getImplementingClass() {
        return this.implementingClass;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    public void setParameters(List<MongoConfigurationParameterMetaData> parameters) {
        this.parameters = parameters;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImplementingClass(String implementingClass) {
        this.implementingClass = implementingClass;
    }

    @Override
    public String toString() {
        return "MongoConfigurationMetaData{" +
            "configurationId='" + configurationId + '\'' +
            ", parameters=" + parameters +
            ", description='" + description + '\'' +
            ", implementingClass='" + implementingClass + '\'' +
            '}';
    }
}
