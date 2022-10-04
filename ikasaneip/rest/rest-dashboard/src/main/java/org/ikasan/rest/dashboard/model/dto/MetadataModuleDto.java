package org.ikasan.rest.dashboard.model.dto;

import org.ikasan.spec.module.ModuleType;

import java.util.List;

public class MetadataModuleDto
{
    private String name;
    private String url;
    private List<String> flows;
    private ModuleType moduleType;

    public MetadataModuleDto(String name, String url, ModuleType moduleType, List<String> flows)
    {
        this.name = name;
        this.url = url;
        this.moduleType = moduleType;
        this.flows = flows;
    }

    public MetadataModuleDto(String name, String url, List<String> flows)
    {
        this.name = name;
        this.url = url;
        this.flows = flows;
    }

    public MetadataModuleDto()
    {
    }


    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public List<String> getFlows()
    {
        return flows;
    }

    public void setFlows(List<String> flows)
    {
        this.flows = flows;
    }

    public ModuleType getModuleType() {
        return moduleType;
    }

    public void setModuleType(ModuleType moduleType) {
        this.moduleType = moduleType;
    }
}
