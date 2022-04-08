package org.ikasan.dashboard.ui.visualisation.model.general;

import java.util.List;

public class Image extends PositionedItem {
    private List<Port> ports;
    private String path;

    public List<Port> getPorts() {
        return ports;
    }

    public void setPorts(List<Port> ports) {
        this.ports = ports;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
