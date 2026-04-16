package org.ikasan.relational.persistence.module.metadata.model;

import org.ikasan.spec.metadata.model.FlowElementMetaData;
import org.ikasan.spec.metadata.model.FlowMetaData;
import org.ikasan.spec.metadata.model.Transition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Hibernate/PostgreSQL implementation of FlowMetaData.
 *
 * This is a POJO implementation used for JSON serialization within the
 * HibernateModuleMetaDataImpl entity.
 */
public class HibernateFlowMetaDataImpl implements FlowMetaData {

    private String name;
    private FlowElementMetaData consumer;
    private List<Transition> transitions = new ArrayList<>();
    private List<FlowElementMetaData> flowElements = new ArrayList<>();
    private String configurationId;
    private String flowStartupType;
    private String flowStartupComment;

    public HibernateFlowMetaDataImpl() {
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setConsumer(FlowElementMetaData consumer) {
        this.consumer = consumer;
    }

    public FlowElementMetaData getConsumer() {
        return this.consumer;
    }

    public List<Transition> getTransitions() {
        if (transitions == null) {
            transitions = new ArrayList<>();
        }
        return this.transitions;
    }

    public void setTransitions(List<Transition> transitions) {
        this.transitions = transitions;
    }

    public List<FlowElementMetaData> getFlowElements() {
        if (flowElements == null) {
            flowElements = new ArrayList<>();
        }
        return this.flowElements;
    }

    public void setFlowElements(List<FlowElementMetaData> flowElements) {
        this.flowElements = flowElements;
    }

    public String getConfigurationId() {
        return this.configurationId;
    }

    public void setConfigurationId(String configurationId) {
        this.configurationId = configurationId;
    }

    public String getFlowStartupType() {
        return flowStartupType;
    }

    public void setFlowStartupType(String flowStartupType) {
        this.flowStartupType = flowStartupType;
    }

    public String getFlowStartupComment() {
        return flowStartupComment;
    }

    public void setFlowStartupComment(String flowStartupComment) {
        this.flowStartupComment = flowStartupComment;
    }

    @Override
    public String toString() {
        return "HibernateFlowMetaDataImpl{" +
                "name='" + name + '\'' +
                ", consumer=" + consumer +
                ", transitions=" + transitions +
                ", flowElements=" + flowElements +
                ", configurationId='" + configurationId + '\'' +
                ", flowStartupType='" + flowStartupType + '\'' +
                ", flowStartupComment='" + flowStartupComment + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HibernateFlowMetaDataImpl that = (HibernateFlowMetaDataImpl) o;
        return Objects.equals(name, that.name) &&
            Objects.equals(configurationId, that.configurationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, configurationId);
    }
}
