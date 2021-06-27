package org.ikasan.module.metadata.model;

import org.ikasan.spec.metadata.DecoratorMetaData;

public class SolrDecoratorMetaDataImpl implements DecoratorMetaData
{
    private String type;
    private String name;
    private String configurationId;
    private boolean isConfigurable = false;

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String getType()
    {
        return type;
    }

    @Override
    public void setType(String type)
    {
        this.type = type;
    }

    @Override
    public boolean isConfigurable()
    {
        return isConfigurable;
    }

    @Override
    public void setConfigurable(boolean configurable)
    {
        this.isConfigurable = configurable;
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
    public String toString() {
        final StringBuffer sb = new StringBuffer("SolrDecoratorMetaDataImpl{");
        sb.append("type='").append(type).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", configurationId='").append(configurationId).append('\'');
        sb.append(", isConfigurable=").append(isConfigurable);
        sb.append('}');
        return sb.toString();
    }
}
