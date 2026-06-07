package org.ikasan.dashboard.ui.visualisation.model.flow;

import org.ikasan.spec.metadata.model.ConfigurationMetaData;
import org.ikasan.spec.metadata.model.FlowElementMetaData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by stewmi on 08/11/2018.
 */
public class Module
{
    private String url;
	private String name;
	private String description;
	private String version;
	private List<Flow> flows;
	private HashMap<String, ConfigurationMetaData> configurationMap;
    private HashMap<String, FlowElementMetaData> componentMap;

    /**
     * Constructs a new Module instance with the specified attributes.
     *
     * @param url the URL associated with the module
     * @param name the name of the module
     * @param description a brief description of the module
     * @param version the version of the module
     * @param configurationMap a map containing configuration metadata, keyed by configuration name
     * @param componentMap a map containing flow element metadata, keyed by component name
     */
	public Module(String url, String name, String description, String version, HashMap<String
        , ConfigurationMetaData> configurationMap, HashMap<String, FlowElementMetaData> componentMap)
	{
	    this.url = url;
		this.name = name;
		this.description = description;
		this.version = version;
		this.configurationMap = configurationMap;
		this.componentMap = componentMap;
	}

    /**
     * Retrieves the URL associated with this Module.
     *
     * @return the URL as a String
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * Sets the URL for this instance.
     *
     * @param url The URL to be set. It should be a valid string representation of a URL.
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

    /**
     * Retrieves the name of the module.
     *
     * @return the name of the module.
     */
    public String getName()
	{
		return name;
	}

    /**
     * Retrieves the description of the module.
     *
     * @return the description of the module as a string
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Retrieves the version of the module.
     *
     * @return the version string of the module.
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * Retrieves the list of flows associated with the module.
     *
     * @return a list of Flow objects representing the flows in the module.
     *         Returns an empty list if no flows have been added.
     */
    public List<Flow> getFlows()
	{
		return flows;
	}

	/**
     * Adds a new flow to the list of flows in the module.
     * If the list of flows does not already exist, it will be initialized.
     *
     * @param flow the Flow object to be added to the module
     */
    public void addFlow(Flow flow)
    {
        if(flows == null)
        {
            flows = new ArrayList<>();
        }

        flows.add(flow);
    }

    /**
     * Retrieves the map of configuration metadata associated with this module.
     *
     * @return a HashMap where the keys are configuration names (as strings) and the values are
     *         corresponding ConfigurationMetaData objects that store configuration details.
     */
    public HashMap<String, ConfigurationMetaData> getConfigurationMap()
    {
        return configurationMap;
    }

    /**
     * Retrieves the map of flow element metadata associated with this module.
     *
     * @return a HashMap where the keys are component names (as strings) and the values
     *         are corresponding FlowElementMetaData objects that store metadata details
     *         for each component.
     */
    public HashMap<String, FlowElementMetaData> getComponentMap()
    {
        return componentMap;
    }

    @Override
    public String toString()
    {
        final StringBuffer sb = new StringBuffer("Module{");
        sb.append("url='").append(url).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", version='").append(version).append('\'');
        sb.append(", flows=").append(flows);
        sb.append(", configurationMap=").append(configurationMap);
        sb.append(", componentMap=").append(componentMap);
        sb.append('}');
        return sb.toString();
    }
}
