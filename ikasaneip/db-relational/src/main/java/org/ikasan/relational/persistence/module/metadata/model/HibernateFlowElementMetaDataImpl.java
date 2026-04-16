package org.ikasan.relational.persistence.module.metadata.model;

import org.ikasan.spec.metadata.model.DecoratorMetaData;
import org.ikasan.spec.metadata.model.FlowElementMetaData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Hibernate/PostgreSQL implementation of FlowElementMetaData.
 *
 * This is a POJO implementation used for JSON serialization within the
 * HibernateFlowMetaDataImpl.
 */
public class HibernateFlowElementMetaDataImpl implements FlowElementMetaData {

    private String componentName;
    private String description;
    private String componentType;
    private String implementingClass;
    private boolean isConfigurable = false;
    private String configurationId;
    private String invokerConfigurationId;
    private List<DecoratorMetaData> decorators;

    public HibernateFlowElementMetaDataImpl() {
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getComponentType() {
        return this.componentType;
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public String getImplementingClass() {
        return this.implementingClass;
    }

    public void setImplementingClass(String implementingClass) {
        this.implementingClass = implementingClass;
    }

    public boolean isConfigurable() {
        return this.isConfigurable;
    }

    public void setConfigurable(boolean configurable) {
        this.isConfigurable = configurable;
    }

    public String getConfigurationId() {
        return this.configurationId;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    public String getInvokerConfigurationId() {
        return invokerConfigurationId;
    }

    public void setInvokerConfigurationId(String configurationId) {
        this.invokerConfigurationId = configurationId;
    }

    public List<DecoratorMetaData> getDecorators() {
        if (decorators == null) {
            decorators = new ArrayList<>();
        }
        return this.decorators;
    }

    public void setDecorators(List<DecoratorMetaData> decorators) {
        this.decorators = decorators;
    }

    @Override
    public String toString() {
        return "HibernateFlowElementMetaDataImpl{" +
                "componentName='" + componentName + '\'' +
                ", description='" + description + '\'' +
                ", componentType='" + componentType + '\'' +
                ", implementingClass='" + implementingClass + '\'' +
                ", isConfigurable=" + isConfigurable +
                ", configurationId='" + configurationId + '\'' +
                ", invokerConfigurationId='" + invokerConfigurationId + '\'' +
                ", decorators=" + decorators +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HibernateFlowElementMetaDataImpl that = (HibernateFlowElementMetaDataImpl) o;
        return Objects.equals(componentName, that.componentName) &&
            Objects.equals(configurationId, that.configurationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentName, configurationId);
    }
}
