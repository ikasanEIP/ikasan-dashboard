package org.ikasan.module.metadata.model;

import org.ikasan.spec.metadata.FlowElementMetaData;
import org.ikasan.spec.metadata.FlowMetaData;
import org.ikasan.spec.metadata.Transition;

import java.util.ArrayList;
import java.util.List;

public class SolrFlowMetaDataImpl implements FlowMetaData
{
    private String name;
    private FlowElementMetaData consumer;
    private List<Transition> transitions = new ArrayList<>();
    private List<FlowElementMetaData> flowElements = new ArrayList<>();
    private String configurationId;
    private String flowStartupType;
    private String flowStartupComment;

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public void setConsumer(FlowElementMetaData consumer)
    {
        this.consumer = consumer;
    }

    @Override
    public FlowElementMetaData getConsumer()
    {
        return this.consumer;
    }

    @Override
    public List<Transition> getTransitions()
    {
        return this.transitions;
    }

    @Override
    public void setTransitions(List<Transition> transitions)
    {
        this.transitions = transitions;
    }

    @Override
    public List<FlowElementMetaData> getFlowElements()
    {
        return this.flowElements;
    }

    @Override
    public void setFlowElements(List<FlowElementMetaData> flowElements)
    {
        this.flowElements = flowElements;
    }

    @Override
    public String getConfigurationId()
    {
        return this.configurationId;
    }

    @Override
    public void setConfigurationId(String configurationId)
    {
        this.configurationId = configurationId;
    }

    @Override
    public String getFlowStartupType() {
        return flowStartupType;
    }

    @Override
    public void setFlowStartupType(String flowStartupType) {
        this.flowStartupType = flowStartupType;
    }

    @Override
    public String getFlowStartupComment() {
        return flowStartupComment;
    }

    @Override
    public void setFlowStartupComment(String flowStartupComment) {
        this.flowStartupComment = flowStartupComment;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("SolrFlowMetaDataImpl{");
        sb.append("name='").append(name).append('\'');
        sb.append(", consumer=").append(consumer);
        sb.append(", transitions=").append(transitions);
        sb.append(", flowElements=").append(flowElements);
        sb.append(", configurationId='").append(configurationId).append('\'');
        sb.append(", flowStartupType='").append(flowStartupType).append('\'');
        sb.append(", flowStartupComment='").append(flowStartupComment).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
