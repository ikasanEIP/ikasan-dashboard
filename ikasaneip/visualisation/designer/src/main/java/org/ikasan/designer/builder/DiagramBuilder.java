package org.ikasan.designer.builder;

import java.util.ArrayList;

public class DiagramBuilder {

    private ArrayList<Object> items = new ArrayList<>();

    public ImageBuilder getImageBuilder() {
        return new ImageBuilder();
    }

    public PortBuilder getPortBuilder() {
        return new PortBuilder();
    }

    public RectangleBuilder getRectangleBuilder() {
        return new RectangleBuilder();
    }

    public ConnectionBuilder getConnectionBuilder() {
        return new ConnectionBuilder();
    }

    public ConnectionDetailsBuilder getConnectionDetailsBuilder() {
        return new ConnectionDetailsBuilder();
    }

    public void addItem(Object item) {
        this.items.add(item);
    }

    public ArrayList<Object> build() {
        return this.items;
    }
}
