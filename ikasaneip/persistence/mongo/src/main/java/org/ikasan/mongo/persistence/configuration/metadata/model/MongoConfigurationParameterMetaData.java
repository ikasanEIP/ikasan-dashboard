package org.ikasan.mongo.persistence.configuration.metadata.model;

import org.ikasan.spec.metadata.model.ConfigurationParameterMetaData;

/**
 * MongoDB implementation of ConfigurationParameterMetaData.
 */
public class MongoConfigurationParameterMetaData implements ConfigurationParameterMetaData {
    private Long id;
    private String name;
    private Object value;
    private String description;
    private String implementingClass;

    /**
     * Constructor
     */
    public MongoConfigurationParameterMetaData(Long id, String name, Object value, String description, String implementingClass) {
        this.id = id;
        this.name = name;
        this.value = value;
        this.description = description;
        this.implementingClass = implementingClass;
    }

    /**
     * Default constructor
     */
    public MongoConfigurationParameterMetaData() {
    }

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Object getValue() {
        return this.value;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String getImplementingClass() {
        return this.implementingClass;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImplementingClass(String implementingClass) {
        this.implementingClass = implementingClass;
    }

    @Override
    public String toString() {
        return "MongoConfigurationParameterMetaData{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", value=" + value +
            ", description='" + description + '\'' +
            ", implementingClass='" + implementingClass + '\'' +
            '}';
    }
}
