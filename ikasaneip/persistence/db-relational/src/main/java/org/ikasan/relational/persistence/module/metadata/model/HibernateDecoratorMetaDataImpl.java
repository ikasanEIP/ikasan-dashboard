package org.ikasan.relational.persistence.module.metadata.model;

import org.ikasan.spec.metadata.model.DecoratorMetaData;

import java.util.Objects;

/**
 * Hibernate/PostgreSQL implementation of DecoratorMetaData.
 *
 * This is a POJO implementation used for JSON serialization within the
 * HibernateFlowElementMetaDataImpl.
 */
public class HibernateDecoratorMetaDataImpl implements DecoratorMetaData {

    private String name;
    private String type;
    private boolean configurable = false;
    private String configurationId;

    public HibernateDecoratorMetaDataImpl() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isConfigurable() {
        return configurable;
    }

    public void setConfigurable(boolean configurable) {
        this.configurable = configurable;
    }

    public String getConfigurationId() {
        return configurationId;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    @Override
    public String toString() {
        return "HibernateDecoratorMetaDataImpl{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", configurable=" + configurable +
                ", configurationId='" + configurationId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HibernateDecoratorMetaDataImpl that = (HibernateDecoratorMetaDataImpl) o;
        return Objects.equals(name, that.name) &&
            Objects.equals(type, that.type) &&
            Objects.equals(configurationId, that.configurationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, configurationId);
    }
}
