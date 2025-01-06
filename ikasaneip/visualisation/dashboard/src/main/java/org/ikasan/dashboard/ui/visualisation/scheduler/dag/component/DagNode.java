package org.ikasan.dashboard.ui.visualisation.scheduler.dag.component;

import org.ikasan.dashboard.ui.util.IkasanColours;
import org.ikasan.spec.scheduled.instance.model.InstanceStatus;

public class DagNode {
    private String id;
    private String[] dependencies;
    private String data;
    private DagNode[] children;
    private String parentId;
    private boolean collapse;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String[] getDependencies() {
        return dependencies;
    }

    public void setDependencies(String[] dependencies) {
        this.dependencies = dependencies;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public DagNode[] getChildren() {
        return children;
    }

    public void setChildren(DagNode[] children) {
        this.children = children;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public boolean isCollapse() {
        return collapse;
    }

    public void setCollapse(boolean collapse) {
        this.collapse = collapse;
    }
}
