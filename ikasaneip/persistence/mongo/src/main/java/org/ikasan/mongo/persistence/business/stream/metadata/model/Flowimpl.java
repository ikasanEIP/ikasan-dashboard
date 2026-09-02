package org.ikasan.mongo.persistence.business.stream.metadata.model;

import org.ikasan.spec.metadata.model.Correlator;
import org.ikasan.spec.metadata.model.Flow;

/**
 * Represents a flow in a business stream.
 * Contains flow metadata including position and correlation information.
 */
public class Flowimpl implements Flow {
    private CorrelatorImpl correlator;
    private String id;
    private String moduleName;
    private String flowName;
    private Integer x;
    private Integer y;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    @Override
    public String getFlowName() {
        return flowName;
    }

    @Override
    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    @Override
    public Integer getX() {
        return x;
    }

    @Override
    public void setX(Integer x) {
        this.x = x;
    }

    @Override
    public Integer getY() {
        return y;
    }

    @Override
    public void setY(Integer y) {
        this.y = y;
    }

    @Override
    public Correlator getCorrelator() {
        return correlator;
    }

    @Override
    public void setCorrelator(Correlator correlator) {
        this.correlator = (CorrelatorImpl) correlator;
    }
}
