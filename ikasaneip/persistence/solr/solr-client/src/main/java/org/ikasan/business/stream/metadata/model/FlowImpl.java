package org.ikasan.business.stream.metadata.model;

import org.ikasan.spec.metadata.model.Correlator;
import org.ikasan.spec.metadata.model.Flow;

public class FlowImpl implements Flow
{
    private Correlator correlator;
    private String id;
    private String moduleName;
    private String flowName;
    private Integer x;
    private Integer y;


    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getFlowName()
    {
        return flowName;
    }

    public void setFlowName(String flowName)
    {
        this.flowName = flowName;
    }

    public Integer getX()
    {
        return x;
    }

    public void setX(Integer x)
    {
        this.x = x;
    }

    public Integer getY()
    {
        return y;
    }

    public void setY(Integer y)
    {
        this.y = y;
    }

    public Correlator getCorrelator()
    {
        return correlator;
    }

    public void setCorrelator(Correlator correlator)
    {
        this.correlator = correlator;
    }
}
